package s4

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import kotlin.time.TimeSource

class FileSink(file: File) : ZipSink, AutoCloseable {
    private val out = BufferedOutputStream(FileOutputStream(file), 1 shl 20)
    override var position = 0L
        private set

    override fun write(bytes: ByteArray, offset: Int, length: Int) {
        out.write(bytes, offset, length)
        position += length
    }

    override fun close() = out.close()
}

class FileSource(file: File) : ZipSource, AutoCloseable {
    private val raf = RandomAccessFile(file, "r")
    override val size = raf.length()

    override fun read(position: Long, into: ByteArray, offset: Int, length: Int) {
        raf.seek(position)
        raf.readFully(into, offset, length)
    }

    override fun close() = raf.close()
}

private val xml = ("<iris:layer id=\"l1\" name=\"Paint\" opacity=\"0.8\" blend=\"multiply\"/>\n").repeat(2000).encodeToByteArray()

fun zipMain(args: List<String>) {
    val dir = File(args.firstOrNull() ?: error("usage: zip <work dir> [huge]")).apply { mkdirs() }
    val tiles = List(16) { exrLikeTile(it) }

    // 1. A 1 GiB package: STORED tile entries (EXR is compressed inside) plus DEFLATED XML parts.
    val original = File(dir, "original.zip")
    timed("write 1 GiB package (2048 STORED tiles + 64 DEFLATED parts)") {
        FileSink(original).use { sink ->
            ZipWriter(sink).apply {
                repeat(2048) { add("iris/layers/l1/c_${it % 64}_${it / 64}.exr", tiles[it % 16], Method.STORED) }
                repeat(64) { add("iris/layers/l$it/layer.xml", xml, Method.DEFLATED, OkioCodec) }
                finish()
            }
        }
    }

    // 2. Raw copy: every entry copied without recompression, one tile replaced.
    val copy = File(dir, "copy.zip")
    timed("raw copy of all entries, one tile replaced") {
        FileSource(original).use { src ->
            val reader = ZipReader(src)
            FileSink(copy).use { sink ->
                ZipWriter(sink).apply {
                    reader.entries.forEach { e ->
                        if (e.name == "iris/layers/l1/c_5_5.exr") add(e.name, tiles[7], Method.STORED)
                        else addRaw(e.name, e.method, e.crc32, e.uncompressedSize, reader.raw(e))
                    }
                    finish()
                }
            }
        }
    }
    FileSource(copy).use { src ->
        val reader = ZipReader(src)
        reader.entries.forEach { reader.content(it, OkioCodec) } // CRC check of every entry
        println("  copy reread: ${reader.entries.size} entries, all CRCs ok")
    }

    // 3. ZIP64 by entry count.
    val many = File(dir, "many.zip")
    timed("write 70 000 entries (ZIP64 entry count)") {
        FileSink(many).use { sink -> ZipWriter(sink).apply { repeat(70_000) { add("p/$it.xml", "<p n=\"$it\"/>".encodeToByteArray(), Method.STORED) }; finish() } }
    }
    FileSource(many).use { println("  reread: ${ZipReader(it).entries.size} entries") }

    // 4. ZIP64 by offset: more than 4 GiB of entries.
    if ("huge" in args) {
        val huge = File(dir, "huge.zip")
        val block = ByteArray(64 shl 20) { (it * 31 + 7).toByte() }
        timed("write 4.5 GiB package (72 x 64 MiB STORED, ZIP64 offsets)") {
            FileSink(huge).use { sink -> ZipWriter(sink).apply { repeat(72) { add("big/$it.bin", block, Method.STORED) }; finish() } }
        }
        FileSource(huge).use { src ->
            val reader = ZipReader(src)
            val last = reader.entries.last()
            check(reader.content(last, OkioCodec).contentEquals(block))
            println("  reread: ${reader.entries.size} entries, last at offset ${last.localHeaderOffset} (> 4 GiB: ${last.localHeaderOffset > MAX32}), CRC ok")
        }
    }

    // 5. Archives from other tools, if the caller made them.
    dir.listFiles { f -> f.name.startsWith("foreign-") }?.sorted()?.forEach { f ->
        FileSource(f).use { src ->
            val reader = ZipReader(src)
            reader.entries.forEach { reader.content(it, OkioCodec) }
            println("  read ${f.name}: ${reader.entries.size} entries, all CRCs ok")
        }
    }
}

private fun timed(label: String, block: () -> Unit) {
    val start = TimeSource.Monotonic.markNow()
    block()
    println("$label: ${start.elapsedNow().inWholeMilliseconds} ms")
}
