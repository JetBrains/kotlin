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
                filter.includeTestsMatching(trimmed)
            }
        }
    }
}
