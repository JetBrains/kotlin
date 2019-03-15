/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.index

import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase

class IndexExceptionTest : KotlinLightCodeInsightFixtureTestCase() {
    fun testRecursionInTypeParameters() {
        myFixture.configureByText("test.kt", "fun <T: T> T.some() {}")
    }

    override fun getProjectDescriptor(): LightProjectDescriptor = JAVA_LATEST
}