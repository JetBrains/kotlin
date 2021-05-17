/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.junit.Test

class IncrementalJavaChangeDefaultIT : IncrementalCompilationJavaChangesBase(usePreciseJavaTracking = null) {
    @Test
    override fun testAbiChangeInLib_changeMethodSignature_tracked() {
        doTest(trackedJavaClass, changeSignature, expectedAffectedFileNames = listOf("TrackedJavaClassChild.kt", "useTrackedJavaClass.kt"))
    }

    @Test
    override fun testNonAbiChangeInLib_changeMethodBody_tracked() {
        doTest(trackedJavaClass, changeBody, expectedAffectedFileNames = listOf())
    }
}

class IncrementalJavaChangePreciseIT : IncrementalCompilationJavaChangesBase(usePreciseJavaTracking = true) {
    @Test
    override fun testAbiChangeInLib_changeMethodSignature_tracked() {
        doTest(trackedJavaClass, changeSignature, expectedAffectedFileNames = listOf("TrackedJavaClassChild.kt", "useTrackedJavaClass.kt"))
    }

    @Test
    override fun testNonAbiChangeInLib_changeMethodBody_tracked() {
        doTest(trackedJavaClass, changeBody, expectedAffectedFileNames = listOf())
    }
}

open class IncrementalJavaChangeDisablePreciseIT : IncrementalCompilationJavaChangesBase(usePreciseJavaTracking = false) {
    @Test
    override fun testAbiChangeInLib_changeMethodSignature_tracked() {
        doTest(
            trackedJavaClass, changeSignature,
            expectedAffectedFileNames = listOf(
                "TrackedJavaClassChild.kt", "useTrackedJavaClass.kt", "useTrackedJavaClassFooMethodUsage.kt",
                "useTrackedJavaClassSameModule.kt"
            )
        )
    }

    @Test
    override fun testNonAbiChangeInLib_changeMethodBody_tracked() {
        doTest(
            trackedJavaClass, changeBody,
            expectedAffectedFileNames = listOf(
                "TrackedJavaClassChild.kt", "useTrackedJavaClass.kt", "useTrackedJavaClassFooMethodUsage.kt",
                "useTrackedJavaClassSameModule.kt"
            )
        )
    }
}

class IncrementalFirJavaChangeDisablePreciseIT : IncrementalJavaChangeDisablePreciseIT() {
    override fun defaultBuildOptions(): BuildOptions {
        return super.defaultBuildOptions().copy(useFir = true)
    }
}

abstract class IncrementalCompilationJavaChangesBase(val usePreciseJavaTracking: Boolean?) : IncrementalCompilationBaseIT() {
    override fun defaultProject() = Project("incrementalMultiproject")

    override fun defaultBuildOptions(): BuildOptions =
        super.defaultBuildOptions().copy(withDaemon = true, incremental = true, usePreciseJavaTracking = usePreciseJavaTracking)

    protected val trackedJavaClass = "TrackedJavaClass.java"
    private val javaClass = "JavaClass.java"
    protected val changeBody: (String) -> String = { it.replace("Hello, World!", "Hello, World!!!!") }
    protected val changeSignature: (String) -> String = { it.replace("String getString", "Object getString") }

    @Test
    fun testAbiChangeInLib_changeMethodSignature() {
        doTest(
            javaClass, changeBody,
            expectedAffectedFileNames = listOf("JavaClassChild.kt", "useJavaClass.kt", "useJavaClassFooMethodUsage.kt")
        )
    }

    @Test
    fun testNonAbiChangeInLib_changeMethodBody() {
        doTest(
            javaClass, changeBody,
            expectedAffectedFileNames = listOf("JavaClassChild.kt", "useJavaClass.kt", "useJavaClassFooMethodUsage.kt")
        )
    }

    abstract fun testAbiChangeInLib_changeMethodSignature_tracked()
    abstract fun testNonAbiChangeInLib_changeMethodBody_tracked()
}
