package s8

fun main() {
    println(benchHeap(2000))
    println(benchDirect(2000))
    println(benchDirectShortView(2000))
    println(benchPanama(2000))
    println(benchDirectPacked(2000))
    println(benchPanamaPacked(2000))
    println(benchPersistent())
    println(leakPanama(100_000))
    println(leakDirectPooled(100_000))
    println(leakDirectDropped(100_000))
}
