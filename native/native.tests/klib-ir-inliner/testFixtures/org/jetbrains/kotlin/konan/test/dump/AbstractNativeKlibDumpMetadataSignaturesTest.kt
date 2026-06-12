/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.dump

import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact.KLIB
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeClassLoader
import org.jetbrains.kotlin.konan.test.blackbox.support.util.dumpMetadataSignatures
import org.jetbrains.kotlin.konan.test.blackbox.testRunSettings
import org.jetbrains.kotlin.library.KotlinIrSignatureVersion
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.utils.bind
import java.io.File

abstract class AbstractNativeKlibDumpMetadataSignaturesTest : AbstractKlibToolDumpTest() {
    override fun getDumpHandlers(): List<Constructor<AbstractKlibToolDumpHandler>> =
        KotlinIrSignatureVersion.CURRENTLY_SUPPORTED_VERSIONS.map { signatureVersion ->
            ::KlibToolMetadataSignaturesDumpHandler.bind(signatureVersion)
        }
}

private class KlibToolMetadataSignaturesDumpHandler(
    testServices: TestServices,
    override val signatureVersion: KotlinIrSignatureVersion,
) : AbstractKlibToolDumpHandler(testServices, suffix = "metadata-signatures") {
    override fun makeDump(klib: File): String = KLIB(klib).dumpMetadataSignatures(
        testServices.testRunSettings.get<KotlinNativeClassLoader>().classLoader,
        signatureVersion
    )
}
