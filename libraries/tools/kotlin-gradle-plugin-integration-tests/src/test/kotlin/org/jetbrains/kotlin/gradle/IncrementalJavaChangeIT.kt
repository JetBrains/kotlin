/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.getFilesByNames
import org.junit.Test
import java.io.File

open class IncrementalJavaChangeDefaultIT : IncrementalCompilationJavaChangesBase(usePreciseJavaTracking = null) {
    @Test
    override fun testAbiChangeInLib_changeMethodSignature_tracked() {
        doTest(
            trackedJavaClassInLib, changeMethodSignature,
            expectedCompiledFileNames = listOf("TrackedJavaClassChild.kt", "useTrackedJavaClass.kt") // In app
        )
    }

    @Test
    override fun testNonAbiChangeInLib_changeMethodBody_tracked() {
        doTest(
            trackedJavaClassInLib, changeMethodBody,
            expectedCompiledFileNames = emptyList()
        )
    }
}

class IncrementalJavaChangeClasspathSnapshotIT : IncrementalJavaChangeDefaultIT() {

    override fun defaultBuildOptions() = super.defaultBuildOptions().copy(useClasspathSnapshot = true)

    @Test
    override fun testAbiChangeInLib_changeMethodSignature() {
        doTest(
            javaClassInLib, changeMethodSignature,
            assertResults = {
                // Fewer Kotlin files are recompiled
                assertCompiledKotlinFiles(
                    File(project.projectDir, "app").getFilesByNames("JavaClassChild.kt", "useJavaClass.kt")
                )
            }
        )
    }

    @Test
    override fun testNonAbiChangeInLib_changeMethodBody() {
        doTest(
            javaClassInLib, changeMethodBody,
            assertResults = {
                assertTasksExecuted(":lib:compileKotlin")
                assertTasksUpToDate(":app:compileKotlin") // App compilation has 'compile avoidance'
                assertCompiledKotlinFiles(emptyList())
            }
        )
    }

    @Test
    fun testAddingInnerClass() {
        doTest(
            "A.kt",
            { content: String -> content.substringBeforeLast("}") + " class InnerClass }" },
            assertResults = {
                assertTasksExecuted(":lib:compileKotlin", ":app:compileKotlin")
                assertCompiledKotlinFiles(project.projectDir.getFilesByNames("AAA.kt", "AA.kt", "BB.kt", "A.kt", "B.kt"))
            }
        )
    }
}

class IncrementalJavaChangePreciseIT : IncrementalCompilationJavaChangesBase(usePreciseJavaTracking = true) {
    @Test
    override fun testAbiChangeInLib_changeMethodSignature_tracked() {
        doTest(
            trackedJavaClassInLib,
            changeMethodSignature,
            expectedCompiledFileNames = listOf("TrackedJavaClassChild.kt", "useTrackedJavaClass.kt") // In app
        )
    }

    @Test
    override fun testNonAbiChangeInLib_changeMethodBody_tracked() {
        doTest(trackedJavaClassInLib, changeMethodBody, expectedCompiledFileNames = emptyList())
    }
}

open class IncrementalJavaChangeDisablePreciseIT : IncrementalCompilationJavaChangesBase(usePreciseJavaTracking = false) {
    @Test
    override fun testAbiChangeInLib_changeMethodSignature_tracked() {
        doTest(
            trackedJavaClassInLib, changeMethodSignature,
            expectedCompiledFileNames = listOf(
                "TrackedJavaClassChild.kt", "useTrackedJavaClass.kt", "useTrackedJavaClassFooMethodUsage.kt", // In app
                "useTrackedJavaClassSameModule.kt" // In lib
            )
        )
    }

    @Test
    override fun testNonAbiChangeInLib_changeMethodBody_tracked() {
        doTest(
            trackedJavaClassInLib, changeMethodBody,
            expectedCompiledFileNames = listOf(
                "TrackedJavaClassChild.kt", "useTrackedJavaClass.kt", "useTrackedJavaClassFooMethodUsage.kt", // In app
                "useTrackedJavaClassSameModule.kt" // In lib
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
    override fun defaultBuildOptions() = super.defaultBuildOptions().copy(usePreciseJavaTracking = usePreciseJavaTracking)

    protected val javaClassInLib = "JavaClass.java"
    protected val trackedJavaClassInLib = "TrackedJavaClass.java"
    protected val changeMethodSignature: (String) -> String = { it.replace("String getString", "Object getString") }
    protected val changeMethodBody: (String) -> String = { it.replace("Hello, World!", "Hello, World!!!!") }

    @Test
    open fun testAbiChangeInLib_changeMethodSignature() {
        doTest(
            javaClassInLib, changeMethodSignature,
            expectedCompiledFileNames = listOf("JavaClassChild.kt", "useJavaClass.kt", "useJavaClassFooMethodUsage.kt") // In app
        )
    }

    @Test
    open fun testNonAbiChangeInLib_changeMethodBody() {
        doTest(
            javaClassInLib, changeMethodBody,
            expectedCompiledFileNames = listOf("JavaClassChild.kt", "useJavaClass.kt", "useJavaClassFooMethodUsage.kt") // In app
        )
    }

    abstract fun testAbiChangeInLib_changeMethodSignature_tracked()
    abstract fun testNonAbiChangeInLib_changeMethodBody_tracked()
}
