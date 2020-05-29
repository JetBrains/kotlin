/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.test

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightPlatformCodeInsightTestCase
import com.intellij.util.ThrowableRunnable

// FIX ME WHEN BUNCH 191 REMOVED
abstract class KotlinLightPlatformCodeInsightTestCase : LightPlatformCodeInsightTestCase() {
    protected inline val project_: Project get() = project
    protected inline val editor_: Editor get() = editor

    override fun setUp() {
        super.setUp()
        enableKotlinOfficialCodeStyle(project_)
    }

    override fun tearDown() = runAll(
        ThrowableRunnable { disableKotlinOfficialCodeStyle(project_) },
        ThrowableRunnable { super.tearDown() },
    )
}
