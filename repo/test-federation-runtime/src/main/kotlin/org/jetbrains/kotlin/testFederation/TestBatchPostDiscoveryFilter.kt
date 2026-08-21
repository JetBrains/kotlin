/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

import org.junit.platform.engine.FilterResult
import org.junit.platform.engine.FilterResult.excluded
import org.junit.platform.engine.FilterResult.included
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.launcher.PostDiscoveryFilter
import java.util.zip.CRC32
import kotlin.math.absoluteValue

class TestBatchPostDiscoveryFilter : PostDiscoveryFilter {

    private val currentBatch = System.getProperty("tests.currentBatch")?.toIntOrNull() ?: -1
    private val totalBatches = System.getProperty("tests.totalBatches")?.toIntOrNull() ?: -1
    private val batchSeed = System.getProperty("tests.batchSeed")?.toIntOrNull() ?: 0

    override fun apply(test: TestDescriptor): FilterResult {
        if (currentBatch < 0 || totalBatches < 0) return included("No batches configured")
        val isTestMethod = test.type == TestDescriptor.Type.TEST || test.mayRegisterTests()
        if (!isTestMethod) return included("Classes/Containers are always enabled")
        val checksum = CRC32()

        checksum.update(batchSeed)
        checksum.update(batchSeed.shr(8))
        checksum.update(batchSeed.shr(16))
        checksum.update(batchSeed.shr(24))

        checksum.update(distributionKey(test).encodeToByteArray())
        val thisTestBatch = (checksum.value.toInt().absoluteValue % totalBatches) + 1

        return if (thisTestBatch == currentBatch) {
            included("Current batch: '$currentBatch'. Test batch: '$thisTestBatch'")
        } else {
            excluded("Current batch: '$currentBatch'. Test batch: '$thisTestBatch'")
        }
    }

    /**
     * Creates a String based key, which will be used to distribute a test across buckets.
     * Returning the same key, will result in the same bucket being assigned for the test.
     */
    private fun distributionKey(test: TestDescriptor): String {
        val uniqueId = test.uniqueId

        /*
        Certain test engines may at least require some thought on how to put tests into buckets.
        While the junit-jupiter's behavior of using the uniqueId directly is a reasonable default,
        it was deliberately chosen to fail on unexpected test engines, to ensure them being handled
        and thought about instead of silently behaving undesirably
         */
        return when (uniqueId.engineId.get()) {
            "junit-jupiter" -> uniqueId.toString()

            /*
            The compiler grouping test engine tries to group work done within a test class, we therefore select
            the test class (parent) as the uniqueId instead of the full testId (which may be method based)
             */
            "kotlin-compiler-grouping-engine" -> uniqueId.removeLastSegment().toString()
            else -> error("Unexpected Test Engine ID: ${uniqueId.engineId}")
        }
    }
}
