package kotlin.test

import java.util.ServiceLoader

private val contributors = ServiceLoader.load(AsserterContributor::class.java).toList()

private val defaultAsserter = DefaultAsserter()

internal actual fun lookupAsserter(): Asserter {
    for (contributor in contributors) {
        val asserter = contributor.contribute()
        if (asserter != null) return asserter
    }
    return defaultAsserter
}