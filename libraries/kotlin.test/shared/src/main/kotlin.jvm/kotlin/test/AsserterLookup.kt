package kotlin.test

import java.util.*
import java.util.concurrent.atomic.*
import java.util.concurrent.locks.*

private val inited = AtomicBoolean()
private val lock = ReentrantLock()
private val contributors = ArrayList<AsserterContributor>()

internal fun lookup(): Asserter {
    initContributorsIfNeeded()

    for (contributor in contributors) {
        val asserter = contributor.contribute()
        if (asserter != null) {
            return asserter
        }
    }

    return DefaultAsserter()
}

private fun initContributors() {
    contributors.clear()
    @Suppress("DEPRECATION")
    val loader = ServiceLoader.load(AsserterContributor::class.javaClass)

    for (contributor in loader) {
        if (contributor != null) {
            contributors.add(contributor)
        }
    }
}

private fun initContributorsIfNeeded() {
    if (!inited.get()) {
        lock.withLock {
            if (inited.compareAndSet(false, true)) {
                initContributors()
            }
        }
    }
}

private inline fun Lock.withLock(block: () -> Unit) {
    lockInterruptibly()
    try {
        block()
    } finally {
        unlock()
    }
}