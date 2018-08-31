/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.getFileByName
import org.jetbrains.kotlin.gradle.util.modify
import org.junit.Test
import java.io.File

class NewInferenceIT : BaseGradleIT() {
    companion object {
        private const val LOCAL_PROPERTIES = "local.properties"
        private const val GRADLE_PROPERTIES = "gradle.properties"
    }

    @Test
    fun testCoroutinesProjectDSL() {
        val project = Project("newInferenceProjectDSL")
        project.build("assemble") {
            assertSuccessful()
//            assertContains("-XnewInference=enable")
        }

        project.projectDir.getFileByName("build.gradle").modify {
            it.replace("newInference 'enable'", "newInference 'disable'")
        }

        project.build("assemble") {
            assertFailed()
//            assertContains("-XnewInference=error")
        }
    }

    @Test
    fun testCoroutinesJvmEnabled() {
        jvmProject.doTest("enable", GRADLE_PROPERTIES)
    }

    @Test
    fun testCoroutinesJvmWarn() {
        jvmProject.doTest("warn", GRADLE_PROPERTIES)
    }

    @Test
    fun testCoroutinesJvmError() {
        jvmProject.doTest("error", GRADLE_PROPERTIES)
    }

    @Test
    fun testCoroutinesJvmDefault() {
        jvmProject.doTest("warn", null)
    }

    @Test
    fun testCoroutinesJvmLocalProperties() {
        jvmProject.doTest("enable", LOCAL_PROPERTIES)
    }

    @Test
    fun testCoroutinesJsLocalProperties() {
        jsProject.doTest("enable", LOCAL_PROPERTIES)
    }

    @Test
    fun testCoroutinesJsEnabled() {
        jsProject.doTest("enable", GRADLE_PROPERTIES)
    }

    @Test
    fun testCoroutinesJsWarn() {
        jsProject.doTest("warn", GRADLE_PROPERTIES)
    }

    @Test
    fun testCoroutinesJsError() {
        jsProject.doTest("error", GRADLE_PROPERTIES)
    }

    @Test
    fun testCoroutinesJsDefault() {
        jsProject.doTest("warn", null)
    }

    // todo: replace with project that actually uses coroutines after their syntax is finalized
    private val jvmProject: Project
        get() = Project("kotlinProject")

    private val jsProject: Project
        get() = Project("kotlin2JsProject")

    private fun Project.doTest(coroutineSupport: String, propertyFileName: String?) {
        if (propertyFileName != null) {
            setupWorkingDir()
            val propertyFile = File(projectDir, propertyFileName)
            val coroutinesProperty = "kotlin.coroutines=$coroutineSupport"
            propertyFile.writeText(coroutinesProperty)
        }

        build("build") {
            assertContains("args.coroutinesState=$coroutineSupport")
        }
    }
}