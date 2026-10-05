package s8

/** Entry point for the ART run (dalvikvm on the emulator): everything except Panama, which Android lacks. */
object AndroidMain {
    @JvmStatic
    fun main(args: Array<String>) {
        println(benchHeap(300))
        println(benchDirect(300))
        println(benchDirectShortView(300))
        println(benchDirectPacked(300))
        println(benchPersistent())
        println(leakDirectPooled(20_000))
        println(leakDirectDropped(20_000))
    }
}
