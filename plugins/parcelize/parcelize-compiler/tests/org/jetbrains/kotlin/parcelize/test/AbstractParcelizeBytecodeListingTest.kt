/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.test

import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.AbstractAsmLikeInstructionListingTest
import org.jetbrains.kotlin.parcelize.ParcelizeComponentRegistrar
import org.jetbrains.kotlin.test.TargetBackend
import java.io.File

abstract class AbstractParcelizeBytecodeListingTest : AbstractAsmLikeInstructionListingTest() {
    override fun setupEnvironment(environment: KotlinCoreEnvironment) {
        ParcelizeComponentRegistrar.registerParcelizeComponents(environment.project)
        addParcelizeRuntimeLibrary(environment)
        addAndroidJarLibrary(environment)
    }
}

abstract class AbstractParcelizeIrBytecodeListingTest : AbstractParcelizeBytecodeListingTest() {
    override val backend = TargetBackend.JVM_IR

    override fun getExpectedTextFileName(wholeFile: File): String {
        return wholeFile.nameWithoutExtension + ".ir.txt"
    }
}