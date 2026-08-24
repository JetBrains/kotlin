/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.dump

import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeClassLoader
import org.jetbrains.kotlin.konan.test.blackbox.support.util.dumpMetadata
import org.jetbrains.kotlin.konan.test.blackbox.testRunSettings
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import java.io.File

abstract class AbstractNativeKlibDumpMetadataSerializationTest : AbstractKlibToolDumpTest() {
    override fun getDumpHandlers(): List<Constructor<AbstractKlibToolDumpHandler<*>>> = listOf(::DefaultKlibToolMetadataDumpHandler)
}

private class EmptyDumpModeVariation : KlibToolDumpHandlerVariation {
    override val dumpFileSuffix get() = ""
}

private class DefaultKlibToolMetadataDumpHandler(
    testServices: TestServices,
) : AbstractKlibToolDumpHandler<EmptyDumpModeVariation>(testServices) {
    override val variation = EmptyDumpModeVariation()

    override fun makeDump(klib: File, module: TestModule) = klib.dumpMetadata(
        kotlinNativeClassLoader = testServices.testRunSettings.get<KotlinNativeClassLoader>().classLoader,
        metadataTestMode = null,
    )
}
