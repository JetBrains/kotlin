/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.dump

import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeClassLoader
import org.jetbrains.kotlin.konan.test.blackbox.support.util.dumpMetadata
import org.jetbrains.kotlin.konan.test.blackbox.testRunSettings
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.utils.bind
import java.io.File

abstract class AbstractNativeKlibDumpMetadataTest : AbstractKlibToolDumpTest() {
    override fun getDumpHandlers(): List<Constructor<AbstractKlibToolDumpHandler<*>>> = supportedDumpModes.map { dumpMode ->
        ::KlibToolMetadataDumpHandler.bind(dumpMode)
    }

    companion object {
        private val supportedDumpModes: List<String?> = listOf(null, "compact-with-stable-order", "ultracompact-with-stable-order")
    }
}

private class MetadataDumpModeVariation(val dumpMode: String?) : KlibToolDumpHandlerVariation {
    override val dumpFileSuffix get() = dumpMode?.let { ".$it" }.orEmpty()
}

private class KlibToolMetadataDumpHandler(
    testServices: TestServices,
    dumpMode: String?,
) : AbstractKlibToolDumpHandler<MetadataDumpModeVariation>(testServices) {
    override val variation = MetadataDumpModeVariation(dumpMode)

    override fun makeDump(klib: File, module: TestModule) = klib.dumpMetadata(
        kotlinNativeClassLoader = testServices.testRunSettings.get<KotlinNativeClassLoader>().classLoader,
        metadataTestMode = variation.dumpMode,
    )
}
