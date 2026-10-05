package s4

fun main(args: Array<String>) {
    val total = (args.firstOrNull()?.toLong() ?: 1024L) * 1024 * 1024
    listOf(OkioCodec, KorlibsCodec, PureKotlinCodec).forEach { codec ->
        listOf(1, 6).forEach { level -> println(benchDeflate(codec, total, level)) }
    }
}
