package s4

/**
 * Writes a ZIP with sizes and CRCs in the local headers (no data descriptors). ZIP64 extra fields appear only
 * for values that do not fit 32 bits; ZIP64 end records only when the entry count or offsets need them.
 */
class ZipWriter(private val sink: ZipSink) {
    private val entries = mutableListOf<ZipEntry>()

    /** Compresses [data] with [codec] when [method] is DEFLATED. */
    fun add(name: String, data: ByteArray, method: Method, codec: Codec? = null, level: Int = 6) {
        val payload = if (method == Method.DEFLATED) codec!!.deflate(data, level, raw = true) else data
        addRaw(name, method, Crc32.update(0, data), data.size.toLong(), payload)
    }

    /** Copies already-compressed bytes as they are (raw entry copy). */
    fun addRaw(name: String, method: Method, crc32: Int, uncompressedSize: Long, compressed: ByteArray) {
        val offset = sink.position
        val nameBytes = name.encodeToByteArray()
        val zip64 = uncompressedSize >= MAX32 || compressed.size.toLong() >= MAX32
        val extra = if (zip64) Bytes().u16(ZIP64_EXTRA).u16(16).u64(uncompressedSize).u64(compressed.size.toLong()).toByteArray() else ByteArray(0)
        val header = Bytes().u32(LOCAL_SIG).u16(if (zip64) 45 else 20).u16(1 shl 11).u16(method.code)
            .u16(DOS_TIME).u16(DOS_DATE).u32(crc32)
            .u32(if (zip64) MAX32 else compressed.size.toLong()).u32(if (zip64) MAX32 else uncompressedSize)
            .u16(nameBytes.size).u16(extra.size).bytes(nameBytes).bytes(extra)
        sink.write(header.toByteArray())
        sink.write(compressed)
        entries += ZipEntry(name, method, crc32, compressed.size.toLong(), uncompressedSize, offset)
    }

    fun finish() {
        val cdStart = sink.position
        entries.forEach { sink.write(centralHeader(it)) }
        val cdSize = sink.position - cdStart
        val needs64 = entries.size >= MAX16 || cdStart >= MAX32 || cdSize >= MAX32
        if (needs64) {
            val eocd64At = sink.position
            sink.write(Bytes().u32(ZIP64_EOCD_SIG).u64(44).u16(45).u16(45).u32(0).u32(0)
                .u64(entries.size.toLong()).u64(entries.size.toLong()).u64(cdSize).u64(cdStart).toByteArray())
            sink.write(Bytes().u32(ZIP64_LOCATOR_SIG).u32(0).u64(eocd64At).u32(1).toByteArray())
        }
        val count = if (needs64) MAX16 else entries.size
        sink.write(Bytes().u32(EOCD_SIG).u16(0).u16(0).u16(count).u16(count)
            .u32(if (needs64) MAX32 else cdSize).u32(if (needs64) MAX32 else cdStart).u16(0).toByteArray())
    }

    private fun centralHeader(e: ZipEntry): ByteArray {
        val name = e.name.encodeToByteArray()
        val big = listOf(e.uncompressedSize, e.compressedSize, e.localHeaderOffset).map { it >= MAX32 }
        val extra = Bytes()
        if (big.any { it }) {
            val fields = listOf(e.uncompressedSize, e.compressedSize, e.localHeaderOffset).filterIndexed { i, _ -> big[i] }
            extra.u16(ZIP64_EXTRA).u16(fields.size * 8)
            fields.forEach { extra.u64(it) }
        }
        val version = if (big.any { it }) 45 else 20
        return Bytes().u32(CENTRAL_SIG).u16(version).u16(version).u16(1 shl 11).u16(e.method.code)
            .u16(DOS_TIME).u16(DOS_DATE).u32(e.crc32)
            .u32(if (big[1]) MAX32 else e.compressedSize).u32(if (big[0]) MAX32 else e.uncompressedSize)
            .u16(name.size).u16(extra.size).u16(0).u16(0).u16(0).u32(0)
            .u32(if (big[2]) MAX32 else e.localHeaderOffset).bytes(name).bytes(extra.toByteArray()).toByteArray()
    }
}
