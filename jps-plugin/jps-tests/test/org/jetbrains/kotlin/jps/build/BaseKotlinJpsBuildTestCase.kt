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

package org.jetbrains.kotlin.jps.build

import com.intellij.util.ThrowableRunnable
import org.jetbrains.jps.builders.JpsBuildTestCase
import org.jetbrains.jps.model.library.JpsLibrary
import org.jetbrains.kotlin.compilerRunner.JpsKotlinCompilerRunner
import org.jetbrains.kotlin.test.WithMutedInDatabaseRunTest
import org.jetbrains.kotlin.test.runTest

@WithMutedInDatabaseRunTest
abstract class BaseKotlinJpsBuildTestCase : JpsBuildTestCase() {
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        System.setProperty("kotlin.jps.tests", "true")
    }

    @Throws(Exception::class)
    override fun tearDown() {
        System.clearProperty("kotlin.jps.tests")
        myModel = null
        myBuildParams.clear()
        JpsKotlinCompilerRunner.releaseCompileServiceSession()
        super.tearDown()
    }

    private val libraries = mutableMapOf<String, JpsLibrary>()

    protected fun requireLibrary(library: KotlinJpsLibrary) = libraries.getOrPut(library.id) {
        library.create(myProject)
    }

    override fun runTestRunnable(testRunnable: ThrowableRunnable<Throwable>) {
        runTest {
            super.runTestRunnable(testRunnable)
        }
    }
}