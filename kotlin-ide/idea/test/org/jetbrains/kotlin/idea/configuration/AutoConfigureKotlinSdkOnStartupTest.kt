/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import org.jetbrains.kotlin.idea.framework.KotlinSdkType
import org.jetbrains.kotlin.idea.util.getProjectJdkTableSafe
import org.junit.internal.runners.JUnit38ClassRunner
import org.junit.Assert
import org.junit.runner.RunWith

// Do not add new tests here since application is initialized only once
@RunWith(JUnit38ClassRunner::class)
class AutoConfigureKotlinSdkOnStartupTest : AbstractConfigureKotlinInTempDirTest() {
    fun testKotlinSdkAdded() {
        Assert.assertTrue(getProjectJdkTableSafe().allJdks.any { it.sdkType is KotlinSdkType })
    }
}