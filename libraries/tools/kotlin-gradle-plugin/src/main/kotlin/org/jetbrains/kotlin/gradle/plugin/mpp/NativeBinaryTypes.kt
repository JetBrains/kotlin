package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Named
import org.gradle.api.attributes.Usage
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget

object KotlinNativeUsage {
    const val KLIB = "kotlin-native-klib"
    const val FRAMEWORK = "kotlin-native-framework"
}

enum class NativeBuildType(val optimized: Boolean, val debuggable: Boolean): Named {
    RELEASE(true, false),
    DEBUG(false, true);

    override fun getName(): String = name.toLowerCase()
}

enum class NativeOutputKind(
    val compilerOutputKind: CompilerOutputKind,
    val taskNameClassifier: String,
    val outputDirectoryName: String = taskNameClassifier,
    val description: String = taskNameClassifier,
    val additionalCompilerFlags: List<String> = emptyList(),
    val runtimeUsageName: String? = null,
    val linkUsageName: String? = null,
    val publishable: Boolean = true // Not used yet.
) {
    EXECUTABLE(
        CompilerOutputKind.PROGRAM,
        "executable",
        description = "an executable",
        runtimeUsageName = Usage.NATIVE_RUNTIME,
        publishable = false
    ),
    DYNAMIC(
        CompilerOutputKind.DYNAMIC,
        "shared",
        description = "a dynamic library",
        runtimeUsageName = Usage.NATIVE_RUNTIME,
        linkUsageName = Usage.NATIVE_LINK,
        publishable = false
    ) {
        override fun availableFor(target: KonanTarget) = target != KonanTarget.WASM32
    },
    STATIC(
        CompilerOutputKind.STATIC,
        "static",
        description = "a static library",
        runtimeUsageName = Usage.NATIVE_RUNTIME,
        linkUsageName = Usage.NATIVE_LINK,
        publishable = false
    ) {
        override fun availableFor(target: KonanTarget) = target != KonanTarget.WASM32
    },
    FRAMEWORK(
        CompilerOutputKind.FRAMEWORK,
        "framework",
        description = "an Objective-C framework",
        linkUsageName = KotlinNativeUsage.FRAMEWORK,
        publishable = false
    ) {
        override fun availableFor(target: KonanTarget) =
            target.family == Family.OSX || target.family == Family.IOS
    };

    open fun availableFor(target: KonanTarget) = true
}