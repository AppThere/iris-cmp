package s8

/** Half to float through a 64K-entry table (allocated once); float to half by bit manipulation, round to nearest even. */
object Half {
    val toFloat = FloatArray(65536) { decode(it) }

    private fun decode(h: Int): Float {
        val sign = if (h and 0x8000 != 0) -1f else 1f
        val exp = (h ushr 10) and 0x1f
        val mant = h and 0x3ff
        return when (exp) {
            0 -> sign * mant * 5.9604645E-8f
            31 -> if (mant == 0) sign * Float.POSITIVE_INFINITY else Float.NaN
            else -> Float.fromBits(((h and 0x8000) shl 16) or ((exp + 112) shl 23) or (mant shl 13))
        }
    }

    fun fromFloat(f: Float): Int {
        val bits = f.toRawBits()
        val sign = (bits ushr 16) and 0x8000
        val exp = ((bits ushr 23) and 0xff) - 112
        if (exp <= 0) return sign
        if (exp >= 31) return sign or 0x7c00
        val mant = bits and 0x7fffff
        var h = sign or (exp shl 10) or (mant ushr 13)
        val rest = mant and 0x1fff
        if (rest > 0x1000 || (rest == 0x1000 && (h and 1) == 1)) h++
        return h
    }
}
