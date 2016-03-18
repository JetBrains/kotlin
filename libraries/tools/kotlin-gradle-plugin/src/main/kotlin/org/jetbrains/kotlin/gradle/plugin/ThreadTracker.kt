package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.Logging
import java.util.*

class ThreadTracker {
    val log = Logging.getLogger(this.javaClass)
    private var before: Collection<Thread>? = getThreads()

    private fun getThreads(): Collection<Thread> = Thread.getAllStackTraces().keys

    fun checkThreadLeak(gradle: Gradle?) {
        try {
            val testThreads = gradle != null && gradle.rootProject.hasProperty(ASSERT_THREAD_LEAKS_PROPERTY)

            Thread.sleep(if (testThreads) 200L else 50L)

            val after = HashSet(getThreads())
            after.removeAll(before!!)

            for (thread in after) {
                if (thread == Thread.currentThread()) continue

                val name = thread.name

                if (testThreads) {
                    throw RuntimeException("Thread leaked: $thread: $name\n ${thread.stackTrace.joinToString(separator = "\n", prefix = " at ")}")
                }
                else {
                    log.info("Thread leaked: $thread: $name")
                }
            }
        } finally {
            before = null
        }
    }

    companion object {
        const val ASSERT_THREAD_LEAKS_PROPERTY = "kotlin.gradle.test.assertThreadLeaks"
    }
}