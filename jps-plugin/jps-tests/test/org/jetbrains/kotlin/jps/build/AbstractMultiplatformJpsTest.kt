/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.build

abstract class AbstractMultiplatformJpsTest : AbstractIncrementalJpsTest() {
    override fun doTest(testDataPath: String) {
        // temporary ignore jps-plugin/testData/incremental/multiplatform/multiModule tests
    }
}