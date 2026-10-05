package s4

import java.util.zip.Deflater
import java.util.zip.Inflater

fun main(args: Array<String>) {
    when (args.firstOrNull() ?: "deflate") {
        "deflate" -> {
            val total = (args.getOrNull(1)?.toLong() ?: 1024L) * 1024 * 1024
            crossCheck()
            listOf(OkioCodec, KorlibsCodec, PureKotlinCodec).forEach { codec ->
                listOf(1, 6).forEach { level -> println(benchDeflate(codec, total, level)) }
            }
        }
        "zip" -> zipMain(args.drop(1))
    }
}

/** Each codec's raw Deflate output must inflate with java.util.zip, and java.util.zip's with each codec. */
private fun crossCheck() {
    val tile = exrLikeTile(42)
    val reference = Deflater(6, true).run {
        setInput(tile); finish()
        val out = ByteArray(tile.size * 2); val n = deflate(out); end(); out.copyOf(n)
    }
    listOf(OkioCodec, KorlibsCodec, PureKotlinCodec).forEach { codec ->
        val theirs = codec.deflate(tile, 6, raw = true)
        val back = Inflater(true).run { setInput(theirs); val out = ByteArray(tile.size); inflate(out); end(); out }
        check(back.contentEquals(tile)) { "${codec.name} output does not inflate with java.util.zip" }
        check(codec.inflate(reference, tile.size, raw = true).contentEquals(tile)) { "${codec.name} cannot inflate java.util.zip output" }
    }
    println("cross-check with java.util.zip: ok for ${listOf(OkioCodec, KorlibsCodec, PureKotlinCodec).joinToString { it.name }}")
}
