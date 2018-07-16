package org.jetbrains.kotlin.gradle.plugin.experimental.internal

import org.gradle.api.attributes.Usage
import org.jetbrains.kotlin.gradle.plugin.experimental.KotlinNativeBinary
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget

enum class OutputKind(
        val compilerOutputKind: CompilerOutputKind,
        val binaryClass: Class<out KotlinNativeBinary>,
        private val developmentBinaryPriority: Int,
        val runtimeUsageName: String? = null,
        val linkUsageName: String? = null,
        val publishable: Boolean = true
) {
    EXECUTABLE(
        CompilerOutputKind.PROGRAM,
        KotlinNativeExecutableImpl::class.java,
        0,
        Usage.NATIVE_RUNTIME,
        null
    ),
    KLIBRARY(
        CompilerOutputKind.LIBRARY,
        KotlinNativeLibraryImpl::class.java,
        1,
        null,
        KotlinNativeUsage.KLIB
    ),
    FRAMEWORK(
        CompilerOutputKind.FRAMEWORK,
        KotlinNativeFrameworkImpl::class.java,
        2,
        null,
        KotlinNativeUsage.FRAMEWORK,
        false
    ) {
        override fun availableFor(target: KonanTarget) =
            target.family == Family.OSX || target.family == Family.IOS
    };

    open fun availableFor(target: KonanTarget) = true

    companion object {
        internal fun Collection<OutputKind>.getDevelopmentKind() = minBy { it.developmentBinaryPriority }
    }
}