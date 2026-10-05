package s5

/** Writes a single-part, ONE_LEVEL tiled EXR. Edge tiles are partial; ZIP falls back to raw when it does not help. */
fun writeTiledExr(image: ExrImage, tile: Int, compression: Compression, level: Int = 4): ByteArray {
    val tilesX = (image.width + tile - 1) / tile
    val tilesY = (image.height + tile - 1) / tile
    val chunks = ArrayList<ByteArray>(tilesX * tilesY)
    for (ty in 0 until tilesY) for (tx in 0 until tilesX) {
        val raw = tileBytes(image, tx * tile, ty * tile, tile)
        val data = if (compression == Compression.NONE) raw else Zlib.compress(zipPredict(raw), level).let { if (it.size < raw.size) it else raw }
        chunks += LeWriter(data.size + 20).i32(tx).i32(ty).i32(0).i32(0).i32(data.size).bytes(data).toByteArray()
    }
    val header = header(image, tile, compression)
    val out = LeWriter(header.size + chunks.sumOf { it.size } + chunks.size * 8 + 8)
    out.i32(20000630).i32(2 or 0x200).bytes(header)
    var offset = (out.size + chunks.size * 8).toLong()
    chunks.forEach { out.i64(offset); offset += it.size }
    chunks.forEach { out.bytes(it) }
    return out.toByteArray()
}

/** Uncompressed tile layout: for each scanline, for each channel (sorted by name), the row's samples. */
private fun tileBytes(image: ExrImage, x0: Int, y0: Int, tile: Int): ByteArray {
    val w = minOf(tile, image.width - x0)
    val h = minOf(tile, image.height - y0)
    val out = LeWriter(w * h * image.channels.sumOf { it.type.bytes })
    for (y in y0 until y0 + h) image.channels.forEachIndexed { c, ch ->
        for (x in x0 until x0 + w) {
            val v = image.bits(c, x, y)
            if (ch.type == PixelType.HALF) { out.u8(v); out.u8(v ushr 8) } else out.i32(v)
        }
    }
    return out.toByteArray()
}

private fun header(image: ExrImage, tile: Int, compression: Compression): ByteArray {
    val h = LeWriter()
    fun attr(name: String, type: String, value: LeWriter) { h.str(name).str(type).i32(value.size).bytes(value.toByteArray()) }
    val chlist = LeWriter()
    image.channels.forEach { chlist.str(it.name).i32(it.type.code).u8(0).u8(0).u8(0).u8(0).i32(1).i32(1) }
    chlist.u8(0)
    val box = LeWriter().i32(0).i32(0).i32(image.width - 1).i32(image.height - 1)
    attr("channels", "chlist", chlist)
    attr("compression", "compression", LeWriter().u8(compression.code))
    attr("dataWindow", "box2i", box)
    attr("displayWindow", "box2i", box)
    attr("lineOrder", "lineOrder", LeWriter().u8(0))
    attr("pixelAspectRatio", "float", LeWriter().f32(1f))
    attr("screenWindowCenter", "v2f", LeWriter().f32(0f).f32(0f))
    attr("screenWindowWidth", "float", LeWriter().f32(1f))
    attr("tiles", "tiledesc", LeWriter().i32(tile).i32(tile).u8(0))
    h.u8(0)
    return h.toByteArray()
}
