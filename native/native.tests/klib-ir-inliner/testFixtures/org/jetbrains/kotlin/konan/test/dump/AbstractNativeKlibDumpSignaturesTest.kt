/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.dump

import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact.KLIB
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeClassLoader
import org.jetbrains.kotlin.konan.test.blackbox.support.util.dumpSignatures
import org.jetbrains.kotlin.konan.test.blackbox.testRunSettings
import org.jetbrains.kotlin.library.KotlinIrSignatureVersion
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.utils.bind
import java.io.File

abstract class AbstractNativeKlibDumpSignaturesTest : AbstractKlibToolDumpTest() {
    override fun getDumpHandlers(): List<Constructor<AbstractKlibToolDumpHandler>> =
        KotlinIrSignatureVersion.CURRENTLY_SUPPORTED_VERSIONS.flatMap { signatureVersion ->
            listOf(
                ::KlibToolSignaturesDumpHandler.bind(/* signatureVersion= */ signatureVersion, /* onlyTopLevelSignatures= */ false),
                ::KlibToolSignaturesDumpHandler.bind(/* signatureVersion= */ signatureVersion, /* onlyTopLevelSignatures= */ true),
            )
        }
}

private class KlibToolSignaturesDumpHandler(
    testServices: TestServices,
    override val signatureVersion: KotlinIrSignatureVersion,
    private val onlyTopLevelSignatures: Boolean,
) : AbstractKlibToolDumpHandler(testServices, suffix = if (onlyTopLevelSignatures) "tl" else null) {
    override fun makeDump(klib: File) = KLIB(klib).dumpSignatures(
        testServices.testRunSettings.get<KotlinNativeClassLoader>().classLoader,
        signatureVersion,
        onlyTopLevelSignatures,
    )
}
