package s10



import s4.Method
import s4.ZipReader
import s4.ZipWriter
import s5.Compression
import s5.readExr
import s5.writeTiledExr
import java.io.File
import java.util.concurrent.Executors
import kotlin.time.TimeSource

private val pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())

fun main(args: Array<String>) {
    val dir = File(args[0]).apply { mkdirs() }
    if (args[1] == "sample") {
        // Uncompressed 2048 chunks for comparing compressions with the reference tools.
        File(dir, "r1_painted_half_2048.exr").writeBytes(writeTiledExr(chunkImage(referenceIllustration(), referenceIllustration().layers[0], 2048, 0, 0)!!, TILE, Compression.NONE))
        File(dir, "r2_photo_float_2048.exr").writeBytes(writeTiledExr(chunkImage(referenceLarge16(), referenceLarge16().layers[0], 2048, 0, 0)!!, TILE, Compression.NONE))
        println("samples written")
        return
    }
    val docs = args[1].split(",").map { if (it == "R1") referenceIllustration() else referenceLarge16() }
    val chunks = args[2].split(",").map { it.toInt() }
    val compressions = args[3].split(",").map { it.split(":").let { (c, l) -> Compression.valueOf(c) to l.toInt() } }
    println("cores=${Runtime.getRuntime().availableProcessors()}")
    for (doc in docs) for (chunk in chunks) for ((compression, level) in compressions) run(dir, doc, chunk, compression, level)
    pool.shutdown()
}

private fun entryName(layer: Int, cx: Int, cy: Int) = "iris/layers/l$layer/c_${cx}_$cy.exr"

private fun run(dir: File, doc: DocSpec, chunk: Int, compression: Compression, level: Int) {
    val per = (doc.width + chunk - 1) / chunk
    val jobs = doc.layers.flatMap { l -> (0 until per).flatMap { cy -> (0 until per).map { cx -> Triple(l, cx, cy) } } }
    // Full save: encode every non-empty chunk in parallel, then write the package (STORED entries).
    // Pixel generation is excluded: each job times only its EXR encode; the package write is timed separately.
    val encodeNanos = java.util.concurrent.atomic.AtomicLong()
    val encoded = jobs.map { (l, cx, cy) ->
        pool.submit<Pair<String, ByteArray>?> {
            chunkImage(doc, l, chunk, cx, cy)?.let { image ->
                val t = System.nanoTime()
                val bytes = writeTiledExr(image, TILE, compression, level)
                encodeNanos.addAndGet(System.nanoTime() - t)
                entryName(l.id, cx, cy) to bytes
            }
        }
    }.mapNotNull { it.get() }
    val pkg = File(dir, "pkg.zip")
    val t0 = TimeSource.Monotonic.markNow()
    FileSink(pkg).use { sink -> ZipWriter(sink).apply { encoded.forEach { (n, b) -> add(n, b, Method.STORED) }; finish() } }
    val cores = Runtime.getRuntime().availableProcessors()
    val encodeCpuMs = encodeNanos.get() / 1_000_000
    val saveMs = encodeCpuMs / cores + t0.elapsedNow().inWholeMilliseconds // parallel encode estimate + package write
    // Full load: read every entry and decode every tile in parallel.
    val t1 = TimeSource.Monotonic.markNow()
    FileSource(pkg).use { src ->
        val reader = ZipReader(src)
        reader.entries.map { e -> val raw = reader.raw(e); pool.submit<Int> { readExr(raw).width } }.forEach { it.get() }
    }
    val loadMs = t1.elapsedNow().inWholeMilliseconds
    // Incremental save of a single-tile edit: re-encode the chunk holding layer 0's tile (5, 5); raw-copy the rest.
    val editLayer = doc.layers[0]
    val ecx = 5 * TILE / chunk
    val edited = chunkImage(doc, editLayer, chunk, ecx, ecx, edit = 1)!!
    val t2 = TimeSource.Monotonic.markNow()
    val newChunk = writeTiledExr(edited, TILE, compression, level)
    val encodeOneMs = t2.elapsedNow().inWholeMilliseconds
    val pkg2 = File(dir, "pkg2.zip")
    FileSource(pkg).use { src ->
        val reader = ZipReader(src)
        FileSink(pkg2).use { sink ->
            ZipWriter(sink).apply {
                reader.entries.forEach { e ->
                    if (e.name == entryName(editLayer.id, ecx, ecx)) add(e.name, newChunk, Method.STORED)
                    else addRaw(e.name, e.method, e.crc32, e.uncompressedSize, reader.raw(e))
                }
                finish()
            }
        }
    }
    val incrementalMs = t2.elapsedNow().inWholeMilliseconds
    println(
        "${doc.name} | chunk $chunk | $compression L$level | entries ${encoded.size} | package ${pkg.length() / (1 shl 20)} MiB | " +
            "save ${saveMs} ms (encode CPU ${encodeCpuMs} ms) | load ${loadMs} ms | incremental ${incrementalMs} ms (chunk encode ${encodeOneMs} ms, largest chunk ${encoded.maxOf { it.second.size } / 1024} KiB)",
    )
    pkg.delete(); pkg2.delete()
}
