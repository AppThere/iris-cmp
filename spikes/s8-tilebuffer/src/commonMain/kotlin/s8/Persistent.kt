package s8

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentHashMapOf
import kotlin.time.TimeSource

/** A tile key packed into a Long (signed tx, ty), as the tile store would key its map. */
fun tileKey(tx: Int, ty: Int): Long = (tx.toLong() shl 32) or (ty.toLong() and 0xffffffffL)

/** Builds a 100 000-entry persistent map, then times single-entry updates and checks structural sharing. */
fun benchPersistent(): String {
    var t = TimeSource.Monotonic.markNow()
    var map: PersistentMap<Long, Int> = persistentHashMapOf()
    for (i in 0 until 100_000) map = map.put(tileKey(i % 400 - 200, i / 400 - 125), i)
    val buildMs = t.elapsedNow().inWholeMilliseconds
    val before = map
    t = TimeSource.Monotonic.markNow()
    var updated = map
    repeat(100_000) { updated = updated.put(tileKey(it % 400 - 200, 0), -it) }
    val updateNs = t.elapsedNow().inWholeNanoseconds / 100_000
    t = TimeSource.Monotonic.markNow()
    var sum = 0L
    repeat(1_000_000) { sum += updated[tileKey(it % 400 - 200, (it / 400) % 250 - 125)] ?: 0 }
    val getNs = t.elapsedNow().inWholeNanoseconds / 1_000_000
    check(before[tileKey(0, 0)] != updated[tileKey(0, 0)]) { "update did not take" }
    check(before.size == 100_000 && before[tileKey(-200, 0)] == 50_000) { "old version changed" }
    return "persistent map (kotlinx.collections.immutable 0.5.2): build 100 000 entries $buildMs ms, update ${updateNs} ns, get ${getNs} ns; old version unchanged (checksum $sum)"
}
