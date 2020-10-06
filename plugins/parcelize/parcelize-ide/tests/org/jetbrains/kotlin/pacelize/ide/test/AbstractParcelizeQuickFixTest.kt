/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.pacelize.ide.test

import org.jetbrains.kotlin.idea.quickfix.AbstractQuickFixTest
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.test.KotlinTestUtils

abstract class AbstractParcelizeQuickFixTest : AbstractQuickFixTest() {
    override fun setUp() {
        super.setUp()

        val androidJar = KotlinTestUtils.findAndroidApiJar()
        ConfigLibraryUtil.addLibrary(module, "androidJar", androidJar.parentFile.absolutePath, arrayOf(androidJar.name))
        ConfigLibraryUtil.addLibrary(module, "androidExtensionsRuntime", "dist/kotlinc/lib", arrayOf("parcelize-runtime.jar"))
    }

    override fun tearDown() {
        ConfigLibraryUtil.removeLibrary(module, "androidJar")
        ConfigLibraryUtil.removeLibrary(module, "androidExtensionsRuntime")

        super.tearDown()
    }
}