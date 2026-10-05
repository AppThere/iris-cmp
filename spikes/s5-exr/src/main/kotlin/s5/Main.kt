package s5

import java.io.File
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random
import kotlin.time.TimeSource

fun main(args: Array<String>) {
    when (args[0]) {
        "write" -> writeSamples(File(args[1]))
        "compare" -> compare(File(args[1]), File(args[2]), args[3].split(","))
        "roundtrip" -> roundTrip()
        "bench" -> bench(args.getOrNull(1)?.toInt() ?: 4096)
        "same" -> same(File(args[1]), File(args[2]))
    }
}

/** Half bits for a value in [0, 1): exact for multiples of 1/1024 (what the samples use) and fine for noise. */
fun floatToHalf(f: Float): Int {
    val bits = f.toRawBits()
    val sign = (bits ushr 16) and 0x8000
    val exp = ((bits ushr 23) and 0xff) - 127 + 15
    val mant = bits and 0x7fffff
    if (exp <= 0) return sign
    if (exp >= 31) return sign or 0x7c00
    var h = sign or (exp shl 10) or (mant ushr 13)
    val rest = mant and 0x1fff
    if (rest > 0x1000 || (rest == 0x1000 && h and 1 == 1)) h++ // round to nearest even
    return h
}

fun halfToFloat(h: Int): Float {
    val sign = if (h and 0x8000 != 0) -1f else 1f
    val exp = (h ushr 10) and 0x1f
    val mant = h and 0x3ff
    return when (exp) {
        0 -> sign * mant / 1024f / 16384f
        31 -> if (mant == 0) sign * Float.POSITIVE_INFINITY else Float.NaN
        else -> sign * (1f + mant / 1024f) * Math.scalb(1f, exp - 15)
    }
}

/** A painted-looking image: smooth gradients plus a little noise; alpha is 1. */
fun sampleImage(width: Int, height: Int, channels: List<Channel>, seed: Int = 1): ExrImage {
    val image = ExrImage(width, height, channels)
    val random = Random(seed)
    image.channels.forEachIndexed { c, ch ->
        for (y in 0 until height) for (x in 0 until width) {
            val v = when (ch.name) {
                "A" -> 1f
                "Z" -> 10f + x * 0.001f + y * 0.002f
                else -> (0.5f + 0.4f * sin(x * 0.01f + c) * sin(y * 0.013f)).let { kotlin.math.round(it * 1024) / 1024 } + random.nextInt(4) / 4096f
            }
            image.data[c][y * width + x] = if (ch.type == PixelType.HALF) floatToHalf(v) else v.toRawBits()
        }
    }
    return image
}

private val RGBA = listOf("R", "G", "B", "A").map { Channel(it, PixelType.HALF) }

private fun writeSamples(dir: File) {
    dir.mkdirs()
    // 1000 x 700 with 256 tiles: the right column and bottom row of tiles are partial.
    val rgba = sampleImage(1000, 700, RGBA)
    File(dir, "ours_rgba_half_zip.exr").writeBytes(writeTiledExr(rgba, 256, Compression.ZIP))
    File(dir, "ours_rgba_half_none.exr").writeBytes(writeTiledExr(rgba, 256, Compression.NONE))
    val mixed = sampleImage(640, 480, listOf("R", "G", "B").map { Channel(it, PixelType.HALF) } + Channel("Z", PixelType.FLOAT))
    File(dir, "ours_rgbz_mixed_zip.exr").writeBytes(writeTiledExr(mixed, 128, Compression.ZIP))
    println("wrote samples to $dir")
}

/**
 * Compares our decoding of [exr] with a reference decoding [raw] (float32 per channel, interleaved in the
 * order [order], as `magick <exr> -define quantum:format=floating-point -depth 32 RGBA:<raw>` writes it).
 */
private fun compare(exr: File, raw: File, order: List<String>) {
    val image = readExr(exr.readBytes())
    val ref = java.nio.ByteBuffer.wrap(raw.readBytes()).order(java.nio.ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
    var worst = 0f
    for (y in 0 until image.height) for (x in 0 until image.width) order.forEachIndexed { k, name ->
        val c = image.channels.indexOfFirst { it.name == name }
        if (c < 0) return@forEachIndexed
        val ours = image.bits(c, x, y).let { if (image.channels[c].type == PixelType.HALF) halfToFloat(it) else Float.fromBits(it) }
        worst = maxOf(worst, abs(ours - ref.get((y * image.width + x) * order.size + k)))
    }
    println("${exr.name}: ${image.width}x${image.height} ${image.channels.joinToString { it.name + ":" + it.type }}; max |ours - reference| = $worst")
}

private fun roundTrip() {
    val images = listOf(sampleImage(1000, 700, RGBA), sampleImage(640, 480, listOf("R", "G", "B").map { Channel(it, PixelType.HALF) } + Channel("Z", PixelType.FLOAT)))
    images.forEach { image ->
        listOf(Compression.NONE, Compression.ZIP).forEach { compression ->
            val back = readExr(writeTiledExr(image, 256, compression))
            check(image.data.indices.all { image.data[it].contentEquals(back.data[it]) }) { "round trip differs ($compression)" }
        }
    }
    println("round trip: bit-exact for RGBA half and RGB half + Z float, NONE and ZIP")
}

private fun bench(size: Int) {
    val image = sampleImage(size, size, RGBA)
    val mib = size.toLong() * size * 4 * 2 / (1024.0 * 1024.0)
    listOf(Compression.NONE to 0, Compression.ZIP to 1, Compression.ZIP to 4, Compression.ZIP to 6).forEach { (compression, level) ->
        repeat(2) { writeTiledExr(image, 256, compression, level) } // warm up
        val t0 = TimeSource.Monotonic.markNow()
        val bytes = writeTiledExr(image, 256, compression, level)
        val write = t0.elapsedNow()
        val t1 = TimeSource.Monotonic.markNow()
        readExr(bytes)
        val read = t1.elapsedNow()
        println("${size}x$size RGBA half, 256 tiles, $compression level $level: ${bytes.size / 1024 / 1024} MiB (ratio ${"%.2f".format(mib * 1024 * 1024 / bytes.size)}), " +
            "write ${"%.0f".format(mib / (write.inWholeMilliseconds / 1000.0))} MiB/s, read ${"%.0f".format(mib / (read.inWholeMilliseconds / 1000.0))} MiB/s")
    }
}

/** Decodes both files and requires identical channels and identical sample bits. */
private fun same(a: File, b: File) {
    val x = readExr(a.readBytes())
    val y = readExr(b.readBytes())
    check(x.width == y.width && x.height == y.height && x.channels == y.channels) { "shape differs" }
    val differing = x.data.indices.sumOf { c -> x.data[c].indices.count { x.data[c][it] != y.data[c][it] } }
    println("${a.name} vs ${b.name}: ${x.width}x${x.height} ${x.channels.joinToString { it.name + ":" + it.type }}, differing samples: $differing")
}
