/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.group

import org.jetbrains.kotlin.konan.test.blackbox.support.TestCaseGroup
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCaseGroupId
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.ExternalSourceTransformersProvider
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Settings
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.TestRoots
import org.jetbrains.kotlin.konan.test.blackbox.support.util.ExternalSourceTransformers
import org.jetbrains.kotlin.konan.test.blackbox.support.util.ThreadSafeCache
import org.jetbrains.kotlin.test.utils.TransformersFunctions.removeOptionalJvmInlineAnnotation
import java.io.File

/**
 * TestCaseGroup provider that eagerly creates test groups.
 *
 * Provider creates test groups as big as possible by grouping them by [TestRoots].
 * Comparing to [ExtTestCaseGroupProvider] makes all groups eagerly on first request for the current id's test root.
 *
 * @see TestCaseGroupProvider
 * @see TestCaseGroup.MetaGroup
 */
internal class EagerExtTestCaseGroupProvider : ExtTestCaseGroupProvider() {
    private val metaGroup = ThreadSafeCache<TestCaseGroupId.TestDataDir, TestCaseGroup.MetaGroup?>()

    override fun getTestCaseGroup(testCaseGroupId: TestCaseGroupId, settings: Settings): TestCaseGroup? {
        check(testCaseGroupId is TestCaseGroupId.TestDataDir)

        val testRoot = settings.findTestRoot(testCaseGroupId.dir)
        val testRootDataDir = TestCaseGroupId.TestDataDir(testRoot)

        return metaGroup.computeIfAbsent(testRootDataDir) {
            val groups = testRootDataDir.dir.walkTopDown()
                .filter { f -> f.isDirectory }
                .mapNotNull {
                    val extendedSettings = object : Settings(
                        parent = settings,
                        settings = listOf(ExternalSourceTransformersProvider::class to JvmInlineAnnotationRemover)
                    ) {}
                    super.getTestCaseGroup(TestCaseGroupId.TestDataDir(it), extendedSettings)
                }.toSet()

            TestCaseGroup.MetaGroup(testRootDataDir, groups)
        }
    }

    private fun Settings.findTestRoot(file: File) =
        get<TestRoots>().roots.single {
            val fileComponents = file.absolutePath.split(File.separator)
            val rootComponents = it.absolutePath.split(File.separator)

            fileComponents.forEachIndexed { index, s ->
                if (index < rootComponents.size && s != rootComponents[index]) {
                    return@single false
                }
            }
            true
        }

    /*
     * It is necessary to use this source processor as soon as it is being used in inline classes tests.
     * This processor is registered in the class constructor, but during the test grouping it is not accessible.
     * This happens because we eagerly iterate through the test data, while JUnit does not create actual test instances.
     */
    private object JvmInlineAnnotationRemover : ExternalSourceTransformersProvider {
        override fun getSourceTransformers(testDataFile: File): ExternalSourceTransformers = listOf(removeOptionalJvmInlineAnnotation)
    }
}
