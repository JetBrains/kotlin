/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.test

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightPlatformCodeInsightTestCase

// BUNCH: 191
abstract class KotlinLightPlatformCodeInsightTestCase : LightPlatformCodeInsightTestCase() {
    protected inline val project_: Project get() = project
    protected inline val editor_: Editor get() = editor
}