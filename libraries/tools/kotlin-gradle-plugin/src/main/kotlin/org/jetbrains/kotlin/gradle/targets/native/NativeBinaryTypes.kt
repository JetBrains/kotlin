package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Named
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework.BitcodeEmbeddingMode
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget

enum class NativeBuildType(
    val optimized: Boolean,
    val debuggable: Boolean,
    internal val iosEmbedBitcode: BitcodeEmbeddingMode
) : Named {
    RELEASE(true, false, BitcodeEmbeddingMode.BITCODE),
    DEBUG(false, true, BitcodeEmbeddingMode.MARKER);

    override fun getName(): String = name.toLowerCase()

    companion object {
        val DEFAULT_BUILD_TYPES = setOf(DEBUG, RELEASE)
    }
}

enum class NativeOutputKind(
    val compilerOutputKind: CompilerOutputKind,
    val taskNameClassifier: String,
    val description: String = taskNameClassifier
) {
    EXECUTABLE(
        CompilerOutputKind.PROGRAM,
        "executable",
        description = "an executable"
    ),
    DYNAMIC(
        CompilerOutputKind.DYNAMIC,
        "shared",
        description = "a dynamic library"
    ) {
        override fun availableFor(target: KonanTarget) = target != KonanTarget.WASM32
    },
    STATIC(
        CompilerOutputKind.STATIC,
        "static",
        description = "a static library"
    ) {
        override fun availableFor(target: KonanTarget) = target != KonanTarget.WASM32
    },
    FRAMEWORK(
        CompilerOutputKind.FRAMEWORK,
        "framework",
        description = "a framework"
    ) {
        override fun availableFor(target: KonanTarget) =
            target.family == Family.OSX || target.family == Family.IOS
    };

    open fun availableFor(target: KonanTarget) = true
}