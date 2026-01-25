/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.klib

import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.test.frontend.fir.getTransitivesAndFriends
import org.jetbrains.kotlin.test.klib.CustomKlibCompiler
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerArtifacts
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerArtifacts.Companion.propertyNotFound
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerArtifacts.Companion.readProperty
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.utils.mapToSetOrEmpty
import java.io.File
import kotlin.test.fail


/**
 * A current or a "custom" (alternative) Kotlin/Native compiler and the relevant artifacts (kotlin-test)
 * which are used in KLIB backward/forward compatibility tests.
 */
interface CustomNativeCompilerSettings {
    val version: String
    val compiler: CustomKlibCompiler
    val nativeHome: File
}

val CustomNativeCompilerSettings.defaultLanguageVersion: LanguageVersion
    get() = LanguageVersion.fromFullVersionString(version)
        ?: fail("Cannot deduce the default LV from the compiler version: $version")

class CustomNativeCompilerSettingsImpl(lazyArtifacts: () -> CustomKlibCompilerArtifacts): CustomNativeCompilerSettings {
    private val artifacts: CustomKlibCompilerArtifacts by lazy(lazyArtifacts)
    override val version: String
        get() = artifacts.version
    override val compiler: CustomKlibCompiler by lazy {
        CustomKlibCompiler(artifacts.compilerClassPath, "org.jetbrains.kotlin.cli.bc.K2Native", "execFullPathsInMessages")
    }
    override val nativeHome: File by lazy {
        artifacts.compilerDist ?: throw IllegalStateException("Custom compiler distribution is not specified")
    }
}

/**
 * An accessor to "custom" (alternative) Kotlin/Native compiler and the relevant artifacts (stdlib, kotlin-test)
 * which are used in KLIB backward/forward compatibility tests.
 */
val customNativeCompilerSettings: CustomNativeCompilerSettings by lazy {
    CustomNativeCompilerSettingsImpl {
        CustomKlibCompilerArtifacts.create(
            compilerClassPathPropertyName = "kotlin.internal.native.test.compat.customCompilerClasspath",
            runtimeDependenciesPropertyName = null, // After OSIP-740, make it non-nullable to always provide stdlib
            versionPropertyName = "kotlin.internal.native.test.compat.customCompilerVersion",
            compilerDistPropertyName= "kotlin.internal.native.test.compat.customCompilerDist",
        )
    }
}

val currentCustomNativeCompilerSettings: CustomNativeCompilerSettings by lazy {
    CustomNativeCompilerSettingsImpl {
        val propertyName = "kotlin.internal.native.test.nativeHome"
        readProperty(propertyName)?.let {
            val compilerDist = File(it)
            CustomKlibCompilerArtifacts.create(
                version = LanguageVersion.LATEST_STABLE,
                compilerDist = compilerDist,
                compilerClassPath = listOf(
                    compilerDist.resolve("konan").resolve("lib").resolve("kotlin-native-compiler-embeddable.jar")
                ),
            )
        } ?: propertyNotFound(propertyName)
    }
}

/**
 * Note: To be used only internally in [CustomNativeCompilerFirstStageFacade] and [NativeCompilerSecondStageFacade].
 */
internal fun TestModule.collectDependencies(
    testServices: TestServices,
): Pair<Set<String>, Set<String>> {
    val (transitiveLibraries: List<File>, friendLibraries: List<File>) = getTransitivesAndFriends(module = this, testServices)

    val regularDependencies: Set<String> = buildSet {
        // After OSIP-740, add stdlib here as runtime dependency
        transitiveLibraries.mapTo(this) { it.absolutePath }
    }

    val friendDependencies: Set<String> = friendLibraries.mapToSetOrEmpty { it.absolutePath }

    return regularDependencies to friendDependencies
}
