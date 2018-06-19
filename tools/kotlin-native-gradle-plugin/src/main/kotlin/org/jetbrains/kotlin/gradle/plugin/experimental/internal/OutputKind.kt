package org.jetbrains.kotlin.gradle.plugin.experimental.internal

import org.gradle.api.attributes.Usage
import org.jetbrains.kotlin.gradle.plugin.experimental.KotlinNativeBinary
import org.jetbrains.kotlin.konan.target.CompilerOutputKind

enum class OutputKind(
        val compilerOutputKind: CompilerOutputKind,
        val binaryClass: Class<out KotlinNativeBinary>,
        private val developmentBinaryPriority: Int,
        val runtimeUsageName: String? = null,
        val linkUsageName: String? = null
) {
    EXECUTABLE(CompilerOutputKind.PROGRAM,
            KotlinNativeExecutableImpl::class.java,
            1,
            Usage.NATIVE_RUNTIME,
            null
    ),
    KLIBRARY(CompilerOutputKind.LIBRARY,
            KotlinNativeKLibraryImpl::class.java,
            0,
            null,
            KotlinNativeUsage.KLIB
    );

    companion object {
        internal fun Collection<OutputKind>.getDevelopmentKind() = maxBy { it.developmentBinaryPriority }
    }
}