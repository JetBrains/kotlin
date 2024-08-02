/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.internal.compilerRunner.native

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.gradle.internal.properties.NativeProperties
import org.jetbrains.kotlin.gradle.utils.property
import java.io.File

internal fun Provider<Boolean>.nativeDaemonEntryPoint() = map { useXcodeMessageStyle ->
    if (useXcodeMessageStyle) "daemonMainWithXcodeRenderer" else "daemonMain"
}

internal val NativeProperties.kotlinNativeCompilerJar: Provider<File>
    get() = isUseEmbeddableCompilerJar.zip(actualNativeHomeDirectory) { useJar, nativeHomeDir ->
        if (useJar) {
            nativeHomeDir.resolve("konan/lib/kotlin-native-compiler-embeddable.jar")
        } else {
            nativeHomeDir.resolve("konan/lib/kotlin-native.jar")
        }
    }

internal fun ObjectFactory.nativeCompilerClasspath(
    kotlinNativeCompilerJar: Provider<File>,
    actualNativeHomeDirectory: Provider<File>,
) = fileCollection().from(
    kotlinNativeCompilerJar,
    actualNativeHomeDirectory.map { it.resolve("konan/lib/trove4j.jar") },
)

internal fun nativeExecSystemProperties(
    useXcodeMessageStyle: Provider<Boolean>
) = useXcodeMessageStyle.map {
    val messageRenderer = if (it) MessageRenderer.XCODE_STYLE else MessageRenderer.GRADLE_STYLE
    mapOf(MessageRenderer.PROPERTY_KEY to messageRenderer.name)
}.get()

internal val nativeExecLLVMEnvironment = mapOf<String, String>(
    "LIBCLANG_DISABLE_CRASH_RECOVERY" to "1"
)

internal val ObjectFactory.nativeMainClass get() = property("org.jetbrains.kotlin.cli.utilities.MainKt")
