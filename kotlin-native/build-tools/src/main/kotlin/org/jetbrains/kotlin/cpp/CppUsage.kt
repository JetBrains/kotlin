package org.jetbrains.kotlin.cpp

import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Usage

/**
 * Extending [Usage] constants for C++ projects.
 */
object CppUsage {
    /**
     * LLVM bitcode of a component.
     */
    @JvmField
    val LLVM_BITCODE = "llvm-bitcode"

    /**
     * [JSON Compilation Database](https://clang.llvm.org/docs/JSONCompilationDatabase.html) of a component.
     */
    @JvmField
    val COMPILATION_DATABASE = "llvm-compilation-database"

    /**
     * Directory with public headers of a component.
     */
    @JvmField
    val API = Usage.C_PLUS_PLUS_API

    /**
     * Shared or static library.
     *
     * Can be used during compilation to link against it.
     *
     * @see LIBRARY_RUNTIME
     */
    @JvmField
    val LIBRARY_LINK = Usage.NATIVE_LINK

    /**
     * Shared library.
     *
     * Can be used for building distribution to package all shared libraries.
     *
     * @see LIBRARY_LINK
     */
    @JvmField
    val LIBRARY_RUNTIME = Usage.NATIVE_RUNTIME

    @JvmField
    val USAGE_ATTRIBUTE: Attribute<Usage> = Usage.USAGE_ATTRIBUTE
}