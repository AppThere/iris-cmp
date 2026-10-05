package s4

// Spike-quality pure-Kotlin raw Deflate (RFC 1951) compressor: LZ77 with hash chains and one-step lazy
// matching, dynamic Huffman blocks with length-limited codes (package-merge). Measures what pure Kotlin
// can do; not product code.

private const val WINDOW = 1 shl 15
private const val HASH_BITS = 15
private const val MIN_MATCH = 3
private const val MAX_MATCH = 258
private const val BLOCK_SYMBOLS = 1 shl 15

private val LENGTH_BASE = intArrayOf(3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 15, 17, 19, 23, 27, 31, 35, 43, 51, 59, 67, 83, 99, 115, 131, 163, 195, 227, 258)
private val LENGTH_EXTRA = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 0)
private val DIST_BASE = intArrayOf(1, 2, 3, 4, 5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129, 193, 257, 385, 513, 769, 1025, 1537, 2049, 3073, 4097, 6145, 8193, 12289, 16385, 24577)
private val DIST_EXTRA = intArrayOf(0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13)
private val CL_ORDER = intArrayOf(16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15)
private val LENGTH_CODE = IntArray(MAX_MATCH + 1).also { t -> for (c in 0 until 29) for (l in LENGTH_BASE[c] until (if (c == 28) 259 else LENGTH_BASE[c + 1])) t[l] = c }

private fun distCode(d: Int): Int {
    var c = 0
    while (c < 29 && DIST_BASE[c + 1] <= d) c++
    return c
}

private class BitWriter(capacity: Int) {
    var buf = ByteArray(capacity)
    var size = 0
    private var acc = 0L
    private var bits = 0

    fun write(value: Int, count: Int) {
        acc = acc or ((value.toLong() and ((1L shl count) - 1)) shl bits)
        bits += count
        while (bits >= 8) {
            if (size == buf.size) buf = buf.copyOf(buf.size * 2)
            buf[size++] = acc.toByte()
            acc = acc ushr 8
            bits -= 8
        }
    }

    /** Huffman codes go most significant bit first. */
    fun writeCode(code: Int, length: Int) = write(Integer32.reverse(code, length), length)

    fun flush(): ByteArray {
        if (bits > 0) write(0, 8 - bits)
        return buf.copyOf(size)
    }
}

private object Integer32 {
    fun reverse(code: Int, length: Int): Int {
        var r = 0
        var c = code
        repeat(length) { r = (r shl 1) or (c and 1); c = c ushr 1 }
        return r
    }
}

/** Length-limited Huffman code lengths by package-merge. */
private fun codeLengths(freqs: IntArray, maxBits: Int): IntArray {
    val lengths = IntArray(freqs.size)
    val used = freqs.indices.filter { freqs[it] > 0 }.sortedBy { freqs[it] }
    if (used.isEmpty()) return lengths
    if (used.size == 1) { lengths[used[0]] = 1; return lengths }
    // Nodes: leaf symbol >= 0, or a package with children.
    class Node(val weight: Long, val symbol: Int, val left: Node?, val right: Node?)
    val leaves = used.map { Node(freqs[it].toLong(), it, null, null) }
    var list = leaves
    repeat(maxBits - 1) {
        val packages = (0 until list.size / 2).map { Node(list[2 * it].weight + list[2 * it + 1].weight, -1, list[2 * it], list[2 * it + 1]) }
        list = (leaves + packages).sortedBy { it.weight }
    }
    fun count(n: Node) { if (n.symbol >= 0) lengths[n.symbol]++ else { count(n.left!!); count(n.right!!) } }
    list.take(2 * used.size - 2).forEach(::count)
    return lengths
}

private fun canonicalCodes(lengths: IntArray): IntArray {
    val blCount = IntArray(16)
    lengths.forEach { if (it > 0) blCount[it]++ }
    val next = IntArray(16)
    var code = 0
    for (bits in 1..15) { code = (code + blCount[bits - 1]) shl 1; next[bits] = code }
    return IntArray(lengths.size) { if (lengths[it] > 0) next[lengths[it]]++ else 0 }
}

/** Compresses [input] as raw Deflate; higher [level] searches longer hash chains. */
fun miniDeflate(input: ByteArray, level: Int): ByteArray {
    val maxChain = if (level <= 1) 4 else if (level <= 5) 16 else 64
    val lazy = level >= 4
    val head = IntArray(1 shl HASH_BITS) { -1 }
    val prev = IntArray(WINDOW)
    val out = BitWriter(input.size / 2 + 64)
    val symbols = IntArray(BLOCK_SYMBOLS * 2) // literal, or (length shl 16 | distance) marked by high bit
    var count = 0
    fun hash(i: Int) = (((input[i].toInt() and 0xff) shl 10) xor ((input[i + 1].toInt() and 0xff) shl 5) xor (input[i + 2].toInt() and 0xff)) and ((1 shl HASH_BITS) - 1)
    var nextInsert = 0
    fun insertUpTo(end: Int) {
        while (nextInsert < end) {
            if (nextInsert + 2 < input.size) { val h = hash(nextInsert); prev[nextInsert and (WINDOW - 1)] = head[h]; head[h] = nextInsert }
            nextInsert++
        }
    }
    fun longestMatch(i: Int): Long {
        if (i + 2 >= input.size) return 0
        var candidate = head[hash(i)]
        var best = 0
        var bestDist = 0
        var chain = maxChain
        val limit = minOf(MAX_MATCH, input.size - i)
        while (candidate >= 0 && i - candidate <= WINDOW && chain-- > 0) {
            var l = 0
            while (l < limit && input[candidate + l] == input[i + l]) l++
            if (l > best) { best = l; bestDist = i - candidate; if (l == limit) break }
            val p = prev[candidate and (WINDOW - 1)]
            if (p >= candidate) break
            candidate = p
        }
        return if (best >= MIN_MATCH) (best.toLong() shl 32) or bestDist.toLong() else 0
    }
    var i = 0
    while (i < input.size) {
        insertUpTo(i)
        var m = longestMatch(i)
        if (m != 0L && lazy && i + 1 < input.size) {
            insertUpTo(i + 1)
            val next = longestMatch(i + 1)
            if ((next ushr 32) > (m ushr 32)) { symbols[count++] = input[i].toInt() and 0xff; i++; m = next }
        }
        if (m == 0L) {
            symbols[count++] = input[i].toInt() and 0xff
            i++
        } else {
            val len = (m ushr 32).toInt()
            symbols[count++] = Int.MIN_VALUE or (len shl 16) or m.toInt()
            i += len
        }
        if (count >= BLOCK_SYMBOLS) { writeBlock(out, symbols, count, last = false); count = 0 }
    }
    writeBlock(out, symbols, count, last = true)
    return out.flush()
}

private fun writeBlock(out: BitWriter, symbols: IntArray, count: Int, last: Boolean) {
    val litFreq = IntArray(286)
    val distFreq = IntArray(30)
    for (s in 0 until count) {
        val v = symbols[s]
        if (v >= 0) litFreq[v]++ else { litFreq[257 + LENGTH_CODE[(v ushr 16) and 0x1ff]]++; distFreq[distCode(v and 0xffff)]++ }
    }
    litFreq[256] = 1
    if (distFreq.count { it > 0 } < 2) { distFreq[0] = maxOf(distFreq[0], 1); distFreq[1] = maxOf(distFreq[1], 1) }
    val litLen = codeLengths(litFreq, 15)
    val distLen = codeLengths(distFreq, 15)
    val litCodes = canonicalCodes(litLen)
    val distCodes = canonicalCodes(distLen)
    val hlit = maxOf(257, litLen.indexOfLast { it > 0 } + 1)
    val hdist = maxOf(1, distLen.indexOfLast { it > 0 } + 1)
    // Run-length encode the code lengths with symbols 0-18.
    val all = litLen.copyOf(hlit) + distLen.copyOf(hdist)
    val rle = ArrayList<Int>() // symbol or (symbol shl 8 | extra)
    var p = 0
    while (p < all.size) {
        val v = all[p]
        var run = 1
        while (p + run < all.size && all[p + run] == v) run++
        if (v == 0 && run >= 3) {
            val r = minOf(run, 138)
            rle += if (r >= 11) (18 shl 8) or (r - 11) else (17 shl 8) or (r - 3)
            p += r
        } else if (v != 0 && run >= 4) {
            rle += v shl 8 or 0x100000
            val r = minOf(run - 1, 6)
            rle += (16 shl 8) or (r - 3)
            p += 1 + r
        } else {
            rle += v shl 8 or 0x100000
            p++
        }
    }
    val clFreq = IntArray(19)
    rle.forEach { clFreq[(it ushr 8) and 0xff]++ }
    if (clFreq.count { it > 0 } < 2) clFreq[if (clFreq[0] == 0) 0 else 1]++ // keep the code complete
    val clLen = codeLengths(clFreq, 7)
    val clCodes = canonicalCodes(clLen)
    var hclen = 19
    while (hclen > 4 && clLen[CL_ORDER[hclen - 1]] == 0) hclen--
    out.write(if (last) 1 else 0, 1)
    out.write(2, 2)
    out.write(hlit - 257, 5)
    out.write(hdist - 1, 5)
    out.write(hclen - 4, 4)
    for (k in 0 until hclen) out.write(clLen[CL_ORDER[k]], 3)
    rle.forEach {
        val sym = (it ushr 8) and 0xff
        out.writeCode(clCodes[sym], clLen[sym])
        when (sym) {
            16 -> out.write(it and 0xff, 2)
            17 -> out.write(it and 0xff, 3)
            18 -> out.write(it and 0xff, 7)
        }
    }
    for (s in 0 until count) {
        val v = symbols[s]
        if (v >= 0) out.writeCode(litCodes[v], litLen[v]) else {
            val len = (v ushr 16) and 0x1ff
            val dist = v and 0xffff
            val lc = LENGTH_CODE[len]
            out.writeCode(litCodes[257 + lc], litLen[257 + lc])
            out.write(len - LENGTH_BASE[lc], LENGTH_EXTRA[lc])
            val dc = distCode(dist)
            out.writeCode(distCodes[dc], distLen[dc])
            out.write(dist - DIST_BASE[dc], DIST_EXTRA[dc])
        }
    }
    out.writeCode(litCodes[256], litLen[256])
}
