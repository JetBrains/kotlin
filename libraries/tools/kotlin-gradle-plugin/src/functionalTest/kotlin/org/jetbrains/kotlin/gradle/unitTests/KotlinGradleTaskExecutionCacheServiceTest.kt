/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.cache.DefaultKotlinGradleTaskExecutionCache
import org.jetbrains.kotlin.gradle.cache.KotlinGradleTaskExecutionCacheWithMetrics
import org.jetbrains.kotlin.gradle.cache.kotlinGradleTaskExecutionCache
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_TASK_EXECUTION_CACHE_METRICS_FILE
import org.jetbrains.kotlin.gradle.util.buildProject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.fail

class KotlinGradleTaskExecutionCacheServiceTest {

    @Test
    fun testDefaultCacheIsUsedWhenMetricsFileIsNotSet() {
        val project = buildProject()
        assertIs<DefaultKotlinGradleTaskExecutionCache>(project.kotlinGradleTaskExecutionCache.get())
    }

    @Test
    fun testMetricsFilePropertyEnablesMeasuringCacheAndDumpsCsvOnClose() {
        val project = buildProject {
            extensions.extraProperties.set(KOTLIN_TASK_EXECUTION_CACHE_METRICS_FILE, "build/reports/cache-metrics.csv")
        }
        val cache = assertIs<KotlinGradleTaskExecutionCacheWithMetrics>(project.kotlinGradleTaskExecutionCache.get())

        assertEquals("value", cache.getOrCompute("key") { "value" })
        assertEquals("value", cache.getOrCompute<String>("key") { fail("'key' should be already cached") })
        cache.close()

        val csv = project.rootDir.resolve("build/reports/cache-metrics.csv").readLines()
        assertEquals(1, csv.size, "Expected exactly one entry, but got: $csv")
        assertTrue(
            // headerless CSV: key,hits,computationTimeNanos
            Regex("""key,1,\d+""").matches(csv[0]),
            "Expected a single entry with a single hit, but got: ${csv[0]}"
        )
    }
}
