package org.jetbrains.teamcity

import org.gradle.api.tasks.testing.TestFilter
import java.io.File

class TestFilterWrapper(
    private val filter: TestFilter
) {
    fun applyFile(file: File) {
        if (!file.exists()) return
        file.forEachLine { line ->
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                try {
                    val method = filter.javaClass.getMethod("includeCommandLineMatching", String::class.java)
                    method.invoke(filter, trimmed)
                } catch (ignored: Throwable) {
                    try {
                        val method = filter.javaClass.getMethod("includeTestsMatching", String::class.java)
                        method.invoke(filter, trimmed)
                    } catch (ignored2: Throwable) {}
                }
            }
        }
    }
}
