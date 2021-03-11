/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.introduce.ui

import com.intellij.openapi.project.Project
import com.intellij.refactoring.ui.MethodSignatureComponent
import org.jetbrains.kotlin.idea.KotlinFileType

class KotlinSignatureComponent(
    signature: String, project: Project
) : MethodSignatureComponent(signature, project, KotlinFileType.INSTANCE) {
    private val myFileName = "dummy." + KotlinFileType.EXTENSION

    override fun getFileName(): String = myFileName
}
