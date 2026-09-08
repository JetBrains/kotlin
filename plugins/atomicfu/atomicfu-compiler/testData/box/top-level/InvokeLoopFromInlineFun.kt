import kotlinx.atomicfu.*
import kotlin.test.*

private val i = atomic(0)
private val a = AtomicIntArray(1)

private var delta = 1

public inline fun <T> topLevel(block: () -> T): T = block()

private val i1 = topLevel {
    i.updateAndGet {
        it + delta
    }
}

private val i2 = topLevel {
    i.getAndUpdate {
        it + delta
    }
}

private val _v1 = 3.also { inc ->
    i.update {
        it + inc
    }
}

private val _v2 = 4.also {
    i.loop {
        assertEquals(5, it)
        return@also
    }
}

private val a1 = topLevel {
    a[0].updateAndGet {
        it + delta
    }
}

private val a2 = topLevel {
    a[0].getAndUpdate {
        it + delta
    }
}

private val _v3 = 0.also { idx ->
    a[idx].update {
        it + 3
    }
}

private val _v4 = 0.also { idx->
    a[idx].loop {
        assertEquals(5, it)
        return@also
    }
}

fun box(): String {
    assertEquals(1, i1)
    assertEquals(1, i2)
    assertEquals(5, i.value)

    assertEquals(1, a1)
    assertEquals(1, a2)
    assertEquals(5, a[0].value)
    return "OK"
}
