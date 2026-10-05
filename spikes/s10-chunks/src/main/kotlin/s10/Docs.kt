package s10

import s5.Channel
import s5.ExrImage
import s5.PixelType
import s5.floatToHalf

const val TILE = 256

/** How a layer looks: painted (smooth, little noise) or photo (textured, noisy). */
enum class Content { PAINTED, PHOTO }

/** A layer covers tile-aligned rectangles (in tiles) of the document. */
class LayerSpec(val id: Int, val content: Content, val covered: List<IntArray>) {
    fun coversTile(tx: Int, ty: Int) = covered.any { tx >= it[0] && ty >= it[1] && tx < it[2] && ty < it[3] }
}

/** A reference document: size, depth class (8i stores HALF codes/255, 16i stores FLOAT codes/65535) and layers. */
class DocSpec(val name: String, val width: Int, val height: Int, val depth16: Boolean, val layers: List<LayerSpec>) {
    val type get() = if (depth16) PixelType.FLOAT else PixelType.HALF
    val tilesX get() = (width + TILE - 1) / TILE
    val tilesY get() = (height + TILE - 1) / TILE
}

/** R1: 4096 x 4096, 30 layers, 8-bit: a full background, 4 layers at about 60 %, 10 at 25 %, 15 small details. */
fun referenceIllustration(): DocSpec {
    val n = 16
    val layers = ArrayList<LayerSpec>()
    layers += LayerSpec(0, Content.PAINTED, listOf(intArrayOf(0, 0, n, n)))
    repeat(4) { i -> layers += LayerSpec(1 + i, Content.PAINTED, listOf(intArrayOf(i, i, i + 12, i + 13))) }
    repeat(10) { i -> layers += LayerSpec(5 + i, Content.PAINTED, listOf(intArrayOf(i % 8, (i * 3) % 8, i % 8 + 8, (i * 3) % 8 + 8))) }
    repeat(15) { i -> layers += LayerSpec(15 + i, Content.PAINTED, listOf(intArrayOf((i * 5) % 14, (i * 7) % 14, (i * 5) % 14 + 2, (i * 7) % 14 + 2))) }
    return DocSpec("R1 illustration 4096x4096, 30 layers, 8i", 4096, 4096, false, layers)
}

/** R2: 10000 x 10000 (100 MP), 3 full layers, 16-bit, photo-like. */
fun referenceLarge16(): DocSpec {
    val t = (10000 + TILE - 1) / TILE
    return DocSpec("R2 large 10000x10000, 3 layers, 16i", 10000, 10000, true, List(3) { LayerSpec(it, Content.PHOTO, listOf(intArrayOf(0, 0, t, t))) })
}

/** Fast deterministic per-pixel noise (no allocation). */
private fun hash(x: Int, y: Int, s: Int): Int {
    var h = x * 374761393 + y * 668265263 + s * 2147483647
    h = (h xor (h ushr 13)) * 1274126177
    return h xor (h ushr 16)
}

/** Sample code (0..max) for channel c at document pixel (x, y). */
fun code(doc: DocSpec, layer: LayerSpec, c: Int, x: Int, y: Int, edit: Int): Int {
    val max = if (doc.depth16) 65535 else 255
    if (c == 3) return max // alpha
    val gradient = ((x * 3 + y * 2 + layer.id * 997 + c * 331 + edit * 77) % 4096) / 4096.0
    val noise = when (layer.content) {
        Content.PAINTED -> (hash(x, y, layer.id * 4 + c) and 3) / 1024.0
        Content.PHOTO -> ((hash(x, y, layer.id * 4 + c) and 1023) / 1023.0 - 0.5) * 0.08 + ((hash(x / 3, y / 3, c) and 255) / 255.0) * 0.1
    }
    return ((gradient * 0.8 + noise).coerceIn(0.0, 1.0) * max).toInt()
}

/**
 * The EXR image for chunk (cx, cy) of [layer], or null if the chunk has no covered tiles: the tile-aligned
 * bounding box of covered tiles, with uncovered tiles inside it left as zeros (file-format §5.2).
 */
fun chunkImage(doc: DocSpec, layer: LayerSpec, chunk: Int, cx: Int, cy: Int, edit: Int = 0): ExrImage? {
    val per = chunk / TILE
    val tiles = (0 until per).flatMap { j -> (0 until per).map { i -> cx * per + i to cy * per + j } }
        .filter { (tx, ty) -> tx < doc.tilesX && ty < doc.tilesY && layer.coversTile(tx, ty) }
    if (tiles.isEmpty()) return null
    val x0 = tiles.minOf { it.first } * TILE
    val y0 = tiles.minOf { it.second } * TILE
    val x1 = minOf(doc.width, (tiles.maxOf { it.first } + 1) * TILE)
    val y1 = minOf(doc.height, (tiles.maxOf { it.second } + 1) * TILE)
    val image = ExrImage(x1 - x0, y1 - y0, listOf("R", "G", "B", "A").map { Channel(it, doc.type) })
    val max = if (doc.depth16) 65535f else 255f
    for ((tx, ty) in tiles) for (y in ty * TILE until minOf(y1, (ty + 1) * TILE)) for (x in tx * TILE until minOf(x1, (tx + 1) * TILE)) {
        image.channels.forEachIndexed { c, ch ->
            val v = code(doc, layer, "RGBA".indexOf(ch.name), x, y, edit) / max
            image.data[c][(y - y0) * image.width + (x - x0)] = if (doc.depth16) v.toRawBits() else floatToHalf(v)
        }
    }
    return image
}
