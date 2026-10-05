package s8

import kotlinx.cinterop.LongVar
import kotlinx.cinterop.ShortVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.free
import kotlinx.cinterop.get
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.set
import kotlinx.cinterop.toKString
import platform.posix.fclose
import platform.posix.fgets
import platform.posix.fopen

private fun rssMib(): Long {
    val f = fopen("/proc/self/status", "r") ?: return -1
    val line = nativeHeap.allocArray<kotlinx.cinterop.ByteVar>(256)
    var rss = -1L
    while (fgets(line, 256, f) != null) {
        val s = line.toKString()
        if (s.startsWith("VmRSS:")) rss = s.split(Regex("\\s+"))[1].toLong() / 1024
    }
    fclose(f)
    nativeHeap.free(line)
    return rss
}

fun main() {
    val heapSrc = ShortArray(SAMPLES)
    val heapDst = ShortArray(SAMPLES)
    fillTiles({ i, v -> heapSrc[i] = v.toShort() }, { i, v -> heapDst[i] = v.toShort() })
    println(benchBlend("K/N ShortArray (baseline)", 1000, { heapDst.sumOf { it.toLong() and 0xffff } }) {
        blendTile({ heapSrc[it].toInt() and 0xffff }, { heapDst[it].toInt() and 0xffff }, { i, v -> heapDst[i] = v.toShort() })
    })

    val src = nativeHeap.allocArray<ShortVar>(SAMPLES)
    val dst = nativeHeap.allocArray<ShortVar>(SAMPLES)
    fillTiles({ i, v -> src[i] = v.toShort() }, { i, v -> dst[i] = v.toShort() })
    println(benchBlend("K/N nativeHeap ShortVar", 1000, { (0 until SAMPLES).sumOf { dst[it].toLong() and 0xffff } }) {
        blendTile({ src[it].toInt() and 0xffff }, { dst[it].toInt() and 0xffff }, { i, v -> dst[i] = v.toShort() })
    })
    fillTiles({ i, v -> src[i] = v.toShort() }, { i, v -> dst[i] = v.toShort() })
    val srcL = src.reinterpret<LongVar>()
    val dstL = dst.reinterpret<LongVar>()
    println(benchBlend("K/N nativeHeap, packed pixel", 1000, { (0 until SAMPLES).sumOf { dst[it].toLong() and 0xffff } }) {
        blendTilePacked({ srcL[it] }, { dstL[it] }, { p, v -> dstL[p] = v })
    })
    nativeHeap.free(src)
    nativeHeap.free(dst)

    println(benchPersistent())

    // Leak test: allocate, touch every page, free explicitly.
    val start = rssMib()
    var peak = start
    repeat(100_000) {
        val tile = nativeHeap.allocArray<kotlinx.cinterop.ByteVar>(SAMPLES * 2)
        var i = 0
        while (i < SAMPLES * 2) { tile[i] = 1; i += 4096 }
        nativeHeap.free(tile)
        if (it % 1000 == 0) peak = maxOf(peak, rssMib())
    }
    println("K/N nativeHeap, freed per tile: 100000 x 512 KiB; RSS $start -> peak $peak MiB -> end ${rssMib()} MiB")
}
