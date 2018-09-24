/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.getFileByName
import org.jetbrains.kotlin.gradle.util.getFilesByNames
import org.jetbrains.kotlin.gradle.util.modify
import org.junit.Test

class IncrementalJavaChangeDefaultIT : IncrementalCompilationJavaChangesBase(usePreciseJavaTracking = null) {
    @Test
    override fun testModifySignatureTrackedJavaInLib() {
        doTest(trackedJavaClass, changeSignature, expectedAffectedSources = listOf("TrackedJavaClassChild.kt", "useTrackedJavaClass.kt"))
    }

    @Test
    override fun testModifyBodyTrackedJavaInLib() {
        doTest(trackedJavaClass, changeBody, expectedAffectedSources = listOf())
    }
}

class IncrementalJavaChangePreciseIT : IncrementalCompilationJavaChangesBase(usePreciseJavaTracking = true) {
    @Test
    override fun testModifySignatureTrackedJavaInLib() {
        doTest(trackedJavaClass, changeSignature, expectedAffectedSources = listOf("TrackedJavaClassChild.kt", "useTrackedJavaClass.kt"))
    }

    @Test
    override fun testModifyBodyTrackedJavaInLib() {
        doTest(trackedJavaClass, changeBody, expectedAffectedSources = listOf())
    }
}

class IncrementalJavaChangeDisablePreciseIT : IncrementalCompilationJavaChangesBase(usePreciseJavaTracking = false) {
    @Test
    override fun testModifySignatureTrackedJavaInLib() {
        doTest(
            trackedJavaClass, changeSignature,
            expectedAffectedSources = listOf(
                "TrackedJavaClassChild.kt", "useTrackedJavaClass.kt", "useTrackedJavaClassFooMethodUsage.kt",
                "useTrackedJavaClassSameModule.kt"
            )
        )
    }

    @Test
    override fun testModifyBodyTrackedJavaInLib() {
        doTest(
            trackedJavaClass, changeBody,
            expectedAffectedSources = listOf(
                "TrackedJavaClassChild.kt", "useTrackedJavaClass.kt", "useTrackedJavaClassFooMethodUsage.kt",
                "useTrackedJavaClassSameModule.kt"
            )
        )
    }
}

abstract class IncrementalCompilationJavaChangesBase(val usePreciseJavaTracking: Boolean?) : BaseGradleIT() {
    override fun defaultBuildOptions(): BuildOptions =
        super.defaultBuildOptions().copy(withDaemon = true, incremental = true)

    protected val trackedJavaClass = "TrackedJavaClass.java"
    private val javaClass = "JavaClass.java"
    protected val changeBody: (String) -> String = { it.replace("Hello, World!", "Hello, World!!!!") }
    protected val changeSignature: (String) -> String = { it.replace("String getString", "Object getString") }

    @Test
    fun testModifySignatureJavaInLib() {
        doTest(
            javaClass, changeBody,
            expectedAffectedSources = listOf("JavaClassChild.kt", "useJavaClass.kt", "useJavaClassFooMethodUsage.kt")
        )
    }

    @Test
    fun testModifyBodyJavaInLib() {
        doTest(
            javaClass, changeBody,
            expectedAffectedSources = listOf("JavaClassChild.kt", "useJavaClass.kt", "useJavaClassFooMethodUsage.kt")
        )
    }

    abstract fun testModifySignatureTrackedJavaInLib()
    abstract fun testModifyBodyTrackedJavaInLib()

    protected fun doTest(
        fileToModify: String,
        transformFile: (String) -> String,
        expectedAffectedSources: Collection<String>
    ) {
        val project = Project("incrementalMultiproject")

        val options = defaultBuildOptions().copy(usePreciseJavaTracking = usePreciseJavaTracking)
        project.build("build", options = options) {
            assertSuccessful()
        }

        val javaClassJava = project.projectDir.getFileByName(fileToModify)
        javaClassJava.modify(transformFile)

        project.build("build", options = options) {
            assertSuccessful()
            val affectedSources = project.projectDir.getFilesByNames(*expectedAffectedSources.toTypedArray())
            val relativePaths = project.relativize(affectedSources)
            assertCompiledKotlinSources(relativePaths)
        }
    }
}
