/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.jvm

import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest
import org.jetbrains.kotlin.gradle.testing.KotlinTaskTestRun
import org.jetbrains.kotlin.gradle.testing.requireCompilationOfTarget
import kotlin.properties.Delegates

internal class ClasspathOnlyTestRunSource(
    override val classpath: FileCollection,
    override val testClassesDirs: FileCollection
) : JvmClasspathTestRunSource

internal open class JvmCompilationsTestRunSource(
    val classpathCompilations: Iterable<KotlinJvmCompilation>,
    val testCompilations: Iterable<KotlinJvmCompilation>
) : JvmClasspathTestRunSource {
    private val project get() = testCompilations.first().target.project

    override val testClassesDirs: FileCollection
        get() = project.files(testCompilations.map { it.output.classesDirs })

    override val classpath: FileCollection
        get() = project.files(
            (testCompilations + classpathCompilations).distinct().map { it.output.allOutputs + it.runtimeDependencyFiles }
        )
}

internal class SingleJvmCompilationTestRunSource(
    override val compilation: KotlinJvmCompilation
) : JvmCompilationsTestRunSource(listOf(compilation), listOf(compilation)), CompilationExecutionSource<KotlinJvmCompilation>

open class KotlinJvmTestRun(testRunName: String, override val target: KotlinJvmTarget) :
    KotlinTaskTestRun<JvmClasspathTestRunSource, KotlinJvmTest>(testRunName, target),
    CompilationExecutionSourceSupport<KotlinJvmCompilation>,
    ClasspathTestRunSourceSupport {

    override fun setExecutionSourceFrom(classpath: FileCollection, testClassesDirs: FileCollection) {
        executionSource = ClasspathOnlyTestRunSource(classpath, testClassesDirs)
    }

    fun setExecutionSourceFrom(
        classpathCompilations: Iterable<KotlinJvmCompilation>,
        testClassesCompilations: Iterable<KotlinJvmCompilation>
    ) {
        classpathCompilations.forEach { requireCompilationOfTarget(it, target) }
        executionSource = JvmCompilationsTestRunSource(classpathCompilations, testClassesCompilations)
    }

    override fun setExecutionSourceFrom(compilation: KotlinJvmCompilation) {
        executionSource = SingleJvmCompilationTestRunSource(compilation)
    }

    private var _executionSource: JvmClasspathTestRunSource by Delegates.notNull()

    final override var executionSource: JvmClasspathTestRunSource
        get() = _executionSource
        private set(value) {
            setTestTaskClasspathAndClassesDirs(value.classpath, value.testClassesDirs)
            _executionSource = value
        }

    private fun setTestTaskClasspathAndClassesDirs(classpath: FileCollection, testClassesDirs: FileCollection) {
        executionTask.configure {
            it.classpath = classpath
            it.testClassesDirs = testClassesDirs
        }
    }

}