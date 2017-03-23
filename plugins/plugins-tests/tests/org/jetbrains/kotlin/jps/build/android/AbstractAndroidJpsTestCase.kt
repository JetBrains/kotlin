/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.jps.build.android

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.jps.android.model.JpsAndroidSdkProperties
import org.jetbrains.jps.model.JpsSimpleElement
import org.jetbrains.jps.model.library.sdk.JpsSdk
import org.jetbrains.jps.android.model.JpsAndroidSdkType
import org.jetbrains.jps.model.impl.JpsSimpleElementImpl
import org.jetbrains.jps.model.library.JpsOrderRootType
import org.jetbrains.kotlin.jps.build.BaseKotlinJpsBuildTestCase
import java.io.File

abstract class AbstractAndroidJpsTestCase : BaseKotlinJpsBuildTestCase() {

    private val SDK_NAME = "Android API 21 Platform"

    fun doTest(path: String) {
        addJdkAndAndroidSdk()
        loadProject(path + getTestName(true) + ".ipr")
        rebuildAll()
        makeAll().assertSuccessful()
        FileUtil.delete(File(path + "/out"))
    }

    private fun addJdkAndAndroidSdk(): JpsSdk<JpsSimpleElement<JpsAndroidSdkProperties>> {
        val jdkName = "java_sdk"
        addJdk(jdkName)
        val properties = JpsAndroidSdkProperties("android-23", jdkName)
        val sdkPath = homePath + "/../dependencies/androidSDK"
        val library = myModel.global.addSdk(SDK_NAME, sdkPath, "", JpsAndroidSdkType.INSTANCE, JpsSimpleElementImpl(properties))
        library.addRoot(File(sdkPath + "/platforms/android-23/android.jar"), JpsOrderRootType.COMPILED)
        return library.properties
    }
}
