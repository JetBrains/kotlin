/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.noarg

import org.jetbrains.kotlin.checkers.AbstractDiagnosticsTest
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.AbstractBlackBoxCodegenTest
import org.jetbrains.kotlin.codegen.AbstractBytecodeListingTest
import org.jetbrains.kotlin.test.TargetBackend

internal val NOARG_ANNOTATIONS = listOf("NoArg", "NoArg2", "test.NoArg")

abstract class AbstractBlackBoxCodegenTestForNoArg : AbstractBlackBoxCodegenTest() {
    override fun loadMultiFiles(files: MutableList<TestFile>) {
        NoArgComponentRegistrar.registerNoArgComponents(
            myEnvironment.project,
            NOARG_ANNOTATIONS,
            backend.isIR,
            files.any { it.directives.contains("INVOKE_INITIALIZERS") },
        )

        super.loadMultiFiles(files)
    }
}

abstract class AbstractBytecodeListingTestForNoArg : AbstractBytecodeListingTest() {
    override fun setupEnvironment(environment: KotlinCoreEnvironment) {
        NoArgComponentRegistrar.registerNoArgComponents(environment.project, NOARG_ANNOTATIONS, backend.isIR, false)
    }
}

abstract class AbstractIrBlackBoxCodegenTestForNoArg : AbstractBlackBoxCodegenTestForNoArg() {
    override val backend: TargetBackend get() = TargetBackend.JVM_IR
}

abstract class AbstractIrBytecodeListingTestForNoArg : AbstractBytecodeListingTestForNoArg() {
    override val backend: TargetBackend get() = TargetBackend.JVM_IR
}

abstract class AbstractDiagnosticsTestForNoArg : AbstractDiagnosticsTest() {
    override fun setupEnvironment(environment: KotlinCoreEnvironment) {
        NoArgComponentRegistrar.registerNoArgComponents(environment.project, NOARG_ANNOTATIONS, backend.isIR, false)
    }
}
