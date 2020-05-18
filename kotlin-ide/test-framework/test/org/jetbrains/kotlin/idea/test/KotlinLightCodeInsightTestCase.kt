/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused", "IncompatibleAPI", "PropertyName")

package org.jetbrains.kotlin.idea.test

import org.jetbrains.kotlin.test.TestMetadataUtil

@Suppress("DEPRECATION")
@Deprecated("Use KotlinLightCodeInsightFixtureTestCase instead")
abstract class KotlinLightCodeInsightTestCase : com.intellij.testFramework.LightCodeInsightTestCase() {
    override fun getTestDataPath(): String = TestMetadataUtil.getTestData(javaClass)?.absolutePath ?: super.getTestDataPath()
}