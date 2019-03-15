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

import org.junit.Test
import java.io.File

class CoroutinesIT : BaseGradleIT() {
    @Test
    fun testCoroutinesJvmDefault() {
        jvmProject.doTest("default", null)
    }

    @Test
    fun testCoroutinesJsDefault() {
        jsProject.doTest("default", null)
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