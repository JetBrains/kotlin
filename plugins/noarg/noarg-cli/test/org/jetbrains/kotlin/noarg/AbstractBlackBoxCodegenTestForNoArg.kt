/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.noarg

import org.jetbrains.kotlin.codegen.AbstractBlackBoxCodegenTest
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor.Companion.registerExtension
import org.jetbrains.kotlin.noarg.AbstractBytecodeListingTestForNoArg.Companion.NOARG_ANNOTATIONS

abstract class AbstractBlackBoxCodegenTestForNoArg : AbstractBlackBoxCodegenTest() {
    override fun loadMultiFiles(files: MutableList<TestFile>) {
        val project = myEnvironment.project
        registerExtension(project, CliNoArgComponentContainerContributor(NOARG_ANNOTATIONS))
        val invokeInitializers = files.any { "// INVOKE_INITIALIZERS" in it.content }
        ExpressionCodegenExtension.registerExtension(project, CliNoArgExpressionCodegenExtension(NOARG_ANNOTATIONS, invokeInitializers))

        super.loadMultiFiles(files)
    }
}