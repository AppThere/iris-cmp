package s8

import java.lang.foreign.Arena
import java.lang.foreign.ValueLayout
import java.nio.ByteOrder

private val SHORT = ValueLayout.JAVA_SHORT_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN)

fun benchPanama(iterations: Int): String =
    Arena.ofConfined().use { arena ->
        val src = arena.allocate(SAMPLES * 2L, 16)
        val dst = arena.allocate(SAMPLES * 2L, 16)
        fillTiles({ i, v -> src.set(SHORT, i * 2L, v.toShort()) }, { i, v -> dst.set(SHORT, i * 2L, v.toShort()) })
        benchBlend("Panama MemorySegment", iterations, { (0 until SAMPLES).sumOf { dst.get(SHORT, it * 2L).toLong() and 0xffff } }) {
            blendTile({ src.get(SHORT, it * 2L).toInt() and 0xffff }, { dst.get(SHORT, it * 2L).toInt() and 0xffff }, { i, v -> dst.set(SHORT, i * 2L, v.toShort()) })
        }
    }

/** Allocates and explicitly frees [count] tiles (one confined arena each): memory returns immediately. */
fun leakPanama(count: Int): String {
    val start = rssMib()
    var peak = start
    val t = kotlin.time.TimeSource.Monotonic.markNow()
    repeat(count) {
        Arena.ofConfined().use { arena -> arena.allocate(SAMPLES * 2L, 16).set(ValueLayout.JAVA_BYTE, 0, 1) }
        if (it % 1000 == 0) peak = maxOf(peak, rssMib())
    }
    return "Panama, arena closed per tile: $count x 512 KiB in ${t.elapsedNow().inWholeMilliseconds} ms; RSS $start -> peak $peak MiB -> end ${rssMib()} MiB"
}

private val LONG = ValueLayout.JAVA_LONG_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN)

fun benchPanamaPacked(iterations: Int): String =
    Arena.ofConfined().use { arena ->
        val src = arena.allocate(SAMPLES * 2L, 16)
        val dst = arena.allocate(SAMPLES * 2L, 16)
        fillTiles({ i, v -> src.set(SHORT, i * 2L, v.toShort()) }, { i, v -> dst.set(SHORT, i * 2L, v.toShort()) })
        benchBlend("Panama, packed pixel", iterations, { (0 until SAMPLES).sumOf { dst.get(SHORT, it * 2L).toLong() and 0xffff } }) {
            blendTilePacked({ src.get(LONG, it * 8L) }, { dst.get(LONG, it * 8L) }, { p, v -> dst.set(LONG, p * 8L, v) })
        }
    }
