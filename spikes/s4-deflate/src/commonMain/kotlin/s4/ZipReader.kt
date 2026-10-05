package s4

/** Reads the central directory (with ZIP64), then entries on demand: raw compressed bytes or CRC-checked content. */
class ZipReader(private val source: ZipSource) {
    val entries: List<ZipEntry> = readCentralDirectory()

    fun raw(entry: ZipEntry): ByteArray {
        val local = ByteArray(30).also { source.read(entry.localHeaderOffset, it) }
        check(local.u32(0) == LOCAL_SIG) { "No local header for ${entry.name}" }
        val dataStart = entry.localHeaderOffset + 30 + local.u16(26) + local.u16(28)
        return ByteArray(entry.compressedSize.toInt()).also { source.read(dataStart, it) }
    }

    fun content(entry: ZipEntry, codec: Codec): ByteArray {
        val raw = raw(entry)
        val data = if (entry.method == Method.DEFLATED) codec.inflate(raw, entry.uncompressedSize.toInt(), raw = true) else raw
        check(Crc32.update(0, data) == entry.crc32) { "CRC mismatch in ${entry.name}" }
        return data
    }

    private fun readCentralDirectory(): List<ZipEntry> {
        val tailSize = minOf(source.size, 65_557L).toInt()
        val tail = ByteArray(tailSize).also { source.read(source.size - tailSize, it) }
        val eocd = (tailSize - 22 downTo 0).first { tail.u32(it) == EOCD_SIG }
        var count = tail.u16(eocd + 10).toLong()
        var cdOffset = tail.u32L(eocd + 16)
        if (count == MAX16.toLong() || cdOffset == MAX32) {
            val locator = eocd - 20
            check(locator >= 0 && tail.u32(locator) == ZIP64_LOCATOR_SIG) { "ZIP64 locator missing" }
            val record = ByteArray(56).also { source.read(tail.u64(locator + 8), it) }
            check(record.u32(0) == ZIP64_EOCD_SIG) { "ZIP64 end record missing" }
            count = record.u64(32)
            cdOffset = record.u64(48)
        }
        val cdBytes = ByteArray((source.size - cdOffset).toInt()).also { source.read(cdOffset, it) }
        var p = 0
        return List(count.toInt()) {
            check(cdBytes.u32(p) == CENTRAL_SIG) { "Bad central directory entry $it" }
            val nameLen = cdBytes.u16(p + 28)
            val extraLen = cdBytes.u16(p + 30)
            val commentLen = cdBytes.u16(p + 32)
            var uncompressed = cdBytes.u32L(p + 24)
            var compressed = cdBytes.u32L(p + 20)
            var offset = cdBytes.u32L(p + 42)
            val name = cdBytes.decodeToString(p + 46, p + 46 + nameLen)
            var e = p + 46 + nameLen
            while (e < p + 46 + nameLen + extraLen) {
                val id = cdBytes.u16(e)
                val len = cdBytes.u16(e + 2)
                if (id == ZIP64_EXTRA) {
                    var f = e + 4
                    if (uncompressed == MAX32) { uncompressed = cdBytes.u64(f); f += 8 }
                    if (compressed == MAX32) { compressed = cdBytes.u64(f); f += 8 }
                    if (offset == MAX32) { offset = cdBytes.u64(f) }
                }
                e += 4 + len
            }
            val method = if (cdBytes.u16(p + 10) == 8) Method.DEFLATED else Method.STORED
            val entry = ZipEntry(name, method, cdBytes.u32(p + 16), compressed, uncompressed, offset)
            p += 46 + nameLen + extraLen + commentLen
            entry
        }
    }
}
