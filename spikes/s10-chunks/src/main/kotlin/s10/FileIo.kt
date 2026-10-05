package s10

import s4.ZipSink
import s4.ZipSource
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile

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
