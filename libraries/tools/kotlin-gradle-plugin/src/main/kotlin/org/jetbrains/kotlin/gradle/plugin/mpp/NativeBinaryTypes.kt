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
    val description: String = taskNameClassifier,
    val additionalCompilerFlags: List<String> = emptyList(),
    val runtimeUsageName: String? = null,
    val linkUsageName: String? = null,
    val publishable: Boolean = true
) {
    EXECUTABLE(
        CompilerOutputKind.PROGRAM,
        "executable",
        description = "an executable",
        runtimeUsageName = Usage.NATIVE_RUNTIME
    ),
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