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

package org.jetbrains.kotlin.android

import org.jetbrains.kotlin.android.quickfix.AbstractAndroidQuickFixMultiFileTest
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractParcelQuickFixTest : AbstractAndroidQuickFixMultiFileTest() {
    override fun setUp() {
        super.setUp()

        val androidSdk = KotlinTestUtils.findAndroidSdk()
        val androidJarDir = File(androidSdk, "platforms").listFiles().first { it.name.startsWith("android-") }
        ConfigLibraryUtil.addLibrary(myModule, "androidJar", androidJarDir.absolutePath, arrayOf("android.jar"))

        ConfigLibraryUtil.addLibrary(myModule, "androidExtensionsRuntime", "dist/kotlinc/lib", arrayOf("android-extensions-runtime.jar"))
    }

    override fun tearDown() {
        ConfigLibraryUtil.removeLibrary(myModule, "androidJar")
        ConfigLibraryUtil.removeLibrary(myModule, "androidExtensionsRuntime")

        super.tearDown()
    }
}