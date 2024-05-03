/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

abstract class CompilerTestsExtension(private val project: Project) {
    val stdlibRuntimeForTests: Configuration = project.configurations.create("stdlibRuntimeForTests") {
        isTransitive = false
    }
    val stdlibMinimalRuntimeForTests: Configuration = project.configurations.create("stdlibMinimalRuntimeForTests") {
        isTransitive = false
    }
    val kotlinReflectJarForTests: Configuration = project.configurations.create("kotlinReflectJarForTests") {
        isTransitive = false
    }
    val stdlibCommonRuntimeForTests: Configuration = project.configurations.create("stdlibCommonRuntimeForTests") {
        isTransitive = false
    }
    val scriptRuntimeForTests: Configuration = project.configurations.create("scriptRuntimeForTests") {
        isTransitive = false
    }
    val kotlinTestJarForTests: Configuration = project.configurations.create("kotlinTestJarForTests") {
        isTransitive = false
    }
    val kotlinAnnotationsForTests: Configuration = project.configurations.create("kotlinAnnotationsForTests") {
        isTransitive = false
    }
    val scriptingPluginForTests: Configuration = project.configurations.create("scriptingPluginForTests") {
        isTransitive = false
    }
    val stdlibJsRuntimeForTests: Configuration = project.configurations.create("stdlibJsRuntimeForTests") {
        isTransitive = false
    }
    val testJsRuntimeForTests: Configuration = project.configurations.create("testJsRuntimeForTests") {
        isTransitive = false
    }

    init {
        project.dependencies {
            stdlibRuntimeForTests(project(":kotlin-stdlib"))
            stdlibMinimalRuntimeForTests(project(":kotlin-stdlib-jvm-minimal-for-test"))
            kotlinReflectJarForTests(project(":kotlin-reflect"))
        }
    }

    fun withStdlibCommon() {
        project.dependencies { stdlibCommonRuntimeForTests(project(":kotlin-stdlib-common")) }
    }

    fun withScriptRuntime() {
        project.dependencies { scriptRuntimeForTests(project(":kotlin-script-runtime")) }
    }

    fun withTestJar() {
        project.dependencies { kotlinTestJarForTests(project(":kotlin-test")) }
    }

    fun withAnnotations() {
        project.dependencies { kotlinAnnotationsForTests(project(":kotlin-annotations-jvm")) }
    }

    fun withStdlibJsRuntime() {
        project.dependencies { stdlibJsRuntimeForTests(project(":kotlin-stdlib", "distJsKlib")) }
    }

    fun withTestJsRuntime() {
        project.dependencies { testJsRuntimeForTests(project(":kotlin-test", "jsRuntimeElements")) }
    }

    fun withScriptingPlugin() {
        project.dependencies {
            scriptingPluginForTests(project(":kotlin-scripting-compiler"))
            scriptingPluginForTests(project(":kotlin-scripting-compiler-impl"))
            scriptingPluginForTests(project(":kotlin-scripting-common"))
            scriptingPluginForTests(project(":kotlin-scripting-jvm"))
        }
        /*
        KOTLIN_SCRIPTING_COMPILER_PLUGIN_JAR
        KOTLIN_SCRIPTING_COMPILER_IMPL_JAR
        KOTLIN_SCRIPTING_COMMON_JAR
        KOTLIN_SCRIPTING_JVM_JAR
        */
    }
}
