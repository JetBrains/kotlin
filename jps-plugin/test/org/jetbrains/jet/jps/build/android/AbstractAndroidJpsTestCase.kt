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

public abstract class AbstractAndroidJpsTestCase : JpsBuildTestCase() {
    private val SDK_NAME = "Android_SDK"
    override fun setUp() {
        super.setUp()
        System.setProperty("kotlin.jps.tests", "true")
    }
    public fun doTest(path: String) {
        addJdk(SDK_NAME, getHomePath() + "/androidSDK/platforms/android-17" + "/android.jar")
        loadProject(path + getTestName(true) + ".ipr")
        rebuildAll()
        makeAll().assertSuccessful()
    }
}
