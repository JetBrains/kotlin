/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.getFileByName
import org.jetbrains.kotlin.gradle.util.modify
import org.junit.Test
import java.io.File

class CoroutinesIT: BaseGradleIT() {
    companion object {
        private const val GRADLE_VERSION = "2.14.1"
        private const val LOCAL_PROPERTIES = "local.properties"
        private const val GRADLE_PROPERTIES = "gradle.properties"
    }

    @Test
    fun testCoroutinesProjectDSL() {
        val project = Project("coroutinesProjectDSL", GRADLE_VERSION)
        project.build("assemble") {
            assertSuccessful()
            assertContains("-Xcoroutines=enable")
        }

        project.projectDir.getFileByName("build.gradle").modify {
            it.replace("coroutines 'enable'", "coroutines 'error'")
        }

        project.build("assemble") {
            assertFailed()
            assertContains("-Xcoroutines=error")
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
            get() = Project("kotlinProject", GRADLE_VERSION)

    private val jsProject: Project
            get() = Project("kotlin2JsProject", GRADLE_VERSION)

    private fun Project.doTest(coroutineSupport: String, propertyFileName: String?) {
        if (propertyFileName != null) {
            setupWorkingDir()
            val propertyFile = File(projectDir, propertyFileName)
            val coroutinesProperty = "kotlin.coroutines=$coroutineSupport"
            propertyFile.writeText(coroutinesProperty)
        }

        build("build") {
            assertContains("-Xcoroutines=$coroutineSupport")
        }
    }
}