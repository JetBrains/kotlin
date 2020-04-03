/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.script

import com.intellij.openapi.Disposable
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project

fun <T> addExtensionPointInTest(
    pointName: ExtensionPointName<T>,
    project: Project,
    provider: T,
    testRootDisposable: Disposable
) {
    pointName.getPoint(project).registerExtension(provider, testRootDisposable)
}