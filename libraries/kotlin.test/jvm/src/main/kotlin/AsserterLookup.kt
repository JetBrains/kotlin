package kotlin.test

import java.util.*
import java.util.concurrent.atomic.*
import java.util.concurrent.locks.*
import kotlin.concurrent.withLock

private val inited = AtomicBoolean()
private val lock = ReentrantLock()
private val contributors = ArrayList<AsserterContributor>()

internal actual fun lookupAsserter(): Asserter = lookup()

private val defaultAsserter = DefaultAsserter()

internal fun lookup(): Asserter {
    initContributorsIfNeeded()

    for (contributor in contributors) {
        val asserter = contributor.contribute()
        if (asserter != null) {
            return asserter
        }
    }

    return defaultAsserter
}

private fun initContributors() {
    contributors.clear()
    val loader = ServiceLoader.load(AsserterContributor::class.java)

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
