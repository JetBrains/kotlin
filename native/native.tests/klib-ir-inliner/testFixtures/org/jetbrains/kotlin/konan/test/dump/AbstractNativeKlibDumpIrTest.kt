/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.dump

import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeClassLoader
import org.jetbrains.kotlin.konan.test.blackbox.support.util.dumpIr
import org.jetbrains.kotlin.konan.test.blackbox.testRunSettings
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.independentSourceDirectoryPathsTransitive
import java.io.File

abstract class AbstractNativeKlibDumpIrTest : AbstractKlibToolDumpTest() {
    override fun getDumpHandlers(): List<Constructor<AbstractKlibToolDumpHandler<*>>> = listOf(::KlibToolIrDumpHandler)
}

private object IrDumpSingleVariation : KlibToolDumpHandlerVariation {
    override val dumpFileSuffix: String
        get() = ".ir" // TODO: test for all signature versions, KT-62828
}

private class KlibToolIrDumpHandler(testServices: TestServices) : AbstractKlibToolDumpHandler<IrDumpSingleVariation>(testServices) {
    override val variation get() = IrDumpSingleVariation

    override fun makeDump(klib: File, module: TestModule): String {
        return klib.dumpIr(
            kotlinNativeClassLoader = testServices.testRunSettings.get<KotlinNativeClassLoader>().classLoader,
            absolutePathPrefixes = module.independentSourceDirectoryPathsTransitive(testServices),
        )
    }
}
