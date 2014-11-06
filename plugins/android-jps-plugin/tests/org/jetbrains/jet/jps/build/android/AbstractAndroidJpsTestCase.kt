/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.jps.build.android

import org.jetbrains.jps.builders.JpsBuildTestCase
import org.jetbrains.jps.android.model.JpsAndroidSdkProperties
import org.jetbrains.jps.android.model.JpsAndroidSdkType
import org.jetbrains.jps.model.library.JpsOrderRootType
import org.jetbrains.jps.model.JpsSimpleElement
import org.jetbrains.jps.model.library.sdk.JpsSdk
import org.jetbrains.jps.model.impl.JpsSimpleElementImpl

import java.io.File

public abstract class AbstractAndroidJpsTestCase : JpsBuildTestCase() {

    private val SDK_NAME = "Android API 17 Platform"

    override fun setUp() {
        super.setUp()
        System.setProperty("kotlin.jps.tests", "true")
    }

    public fun doTest(path: String) {
        addJdkAndAndroidSdk()
        loadProject(path + getTestName(true) + ".ipr")
        rebuildAll()
        makeAll().assertSuccessful()
        deleteDirectory(File(path + "/out"))
    }

    public fun deleteDirectory(path: File): Boolean {
        if (path.exists() && path.isDirectory()) {
            val files = path.listFiles()!!
            for (i in files.indices) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i])
                }
                else {
                    files[i].delete()
                }
            }
        }
        return (path.delete())
    }

    private fun addJdkAndAndroidSdk(): JpsSdk<JpsSimpleElement<JpsAndroidSdkProperties>> {
        val jdkName = "java_sdk"
        addJdk(jdkName)
        val properties = JpsAndroidSdkProperties("android-17", jdkName)
        val sdkPath = getHomePath() + "/androidSDK/"
        val library = myModel!!.getGlobal().addSdk<JpsSimpleElement<JpsAndroidSdkProperties>>(SDK_NAME, sdkPath, "", JpsAndroidSdkType.INSTANCE, JpsSimpleElementImpl(properties))
        library!!.addRoot(File(sdkPath + "/platforms/android-17/android.jar"), JpsOrderRootType.COMPILED)
        return library.getProperties()
    }
}
