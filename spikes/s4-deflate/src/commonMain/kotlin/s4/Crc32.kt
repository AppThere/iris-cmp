package s4

/** CRC-32 (IEEE 802.3, as ZIP uses), table driven, pure Kotlin. */
object Crc32 {
    private val table = IntArray(256) { n ->
        var c = n
        repeat(8) { c = if (c and 1 != 0) (c ushr 1) xor 0xEDB88320.toInt() else c ushr 1 }
        c
    }

    fun update(crc: Int, bytes: ByteArray, offset: Int = 0, length: Int = bytes.size): Int {
        var c = crc.inv()
        for (i in offset until offset + length) c = table[(c xor bytes[i].toInt()) and 0xff] xor (c ushr 8)
        return c.inv()
    }
}
