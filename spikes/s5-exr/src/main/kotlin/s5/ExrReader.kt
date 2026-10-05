package s5

/** Reads single-part scanline or ONE_LEVEL tiled EXR files with NONE, ZIPS or ZIP compression. */
fun readExr(file: ByteArray): ExrImage {
    val r = LeReader(file)
    check(r.i32() == 20000630) { "Not an EXR file" }
    val version = r.i32()
    check(version and 0xff == 2) { "Unsupported version ${version and 0xff}" }
    check(version and 0x1800 == 0) { "Deep or multi-part files are not supported" }
    val attrs = HashMap<String, Pair<String, ByteArray>>()
    while (true) {
        val name = r.str()
        if (name.isEmpty()) break
        val type = r.str()
        val size = r.i32()
        attrs[name] = type to file.copyOfRange(r.pos, r.pos + size)
        r.pos += size
    }
    val channels = parseChannels(attrs.getValue("channels").second)
    val window = LeReader(attrs.getValue("dataWindow").second).let { IntArray(4) { _ -> it.i32() } }
    val width = window[2] - window[0] + 1
    val height = window[3] - window[1] + 1
    val compression = Compression.entries.firstOrNull { it.code == attrs.getValue("compression").second[0].toInt() }
        ?: error("Unsupported compression ${attrs.getValue("compression").second[0]}")
    val image = ExrImage(width, height, channels)
    val tiles = attrs["tiles"]?.second?.let { LeReader(it) }
    if (tiles != null) {
        val tw = tiles.i32()
        val th = tiles.i32()
        check(tiles.u8() and 0xf == 0) { "Only ONE_LEVEL tiled files are supported" }
        val count = ((width + tw - 1) / tw) * ((height + th - 1) / th)
        val offsets = LongArray(count) { r.i64() }
        offsets.forEach { off ->
            val c = LeReader(file, off.toInt())
            val tx = c.i32()
            val ty = c.i32()
            c.i32(); c.i32()
            val size = c.i32()
            val x0 = tx * tw
            val y0 = ty * th
            decodeBlock(image, file.copyOfRange(c.pos, c.pos + size), x0, y0, minOf(tw, width - x0), minOf(th, height - y0), compression)
        }
    } else {
        val lines = compression.linesPerBlock
        val count = (height + lines - 1) / lines
        val offsets = LongArray(count) { r.i64() }
        offsets.forEach { off ->
            val c = LeReader(file, off.toInt())
            val y = c.i32() - window[1]
            val size = c.i32()
            decodeBlock(image, file.copyOfRange(c.pos, c.pos + size), 0, y, width, minOf(lines, height - y), compression)
        }
    }
    return image
}

private fun parseChannels(bytes: ByteArray): List<Channel> {
    val r = LeReader(bytes)
    val out = ArrayList<Channel>()
    while (true) {
        val name = r.str()
        if (name.isEmpty()) break
        val code = r.i32()
        val type = PixelType.entries.first { it.code == code }
        r.u8(); r.u8(); r.u8(); r.u8()
        check(r.i32() == 1 && r.i32() == 1) { "Subsampled channels are not supported" }
        out += Channel(name, type)
    }
    return out
}

private fun decodeBlock(image: ExrImage, data: ByteArray, x0: Int, y0: Int, w: Int, h: Int, compression: Compression) {
    val expected = w * h * image.channels.sumOf { it.type.bytes }
    val raw = if (compression == Compression.NONE || data.size == expected) data else zipUnpredict(Zlib.uncompress(data, expected))
    val r = LeReader(raw)
    for (y in y0 until y0 + h) image.channels.forEachIndexed { c, ch ->
        val row = image.data[c]
        for (x in x0 until x0 + w) row[y * image.width + x] = if (ch.type == PixelType.HALF) r.u8() or (r.u8() shl 8) else r.i32()
    }
}
