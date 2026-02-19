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
     * Native library to use during link time.
     * Can be either static (.a) or shared (.so/.dylib/.dll) library.
     * @see LIBRARY_RUNTIME
     */
    @JvmField
    val LIBRARY_LINK = Usage.NATIVE_LINK

    /**
     * Native library to use during execution time.
     * Can only be a shared (.so/.dylib/.dll) library.
     * @see LIBRARY_LINK
     */
    @JvmField
    val LIBRARY_RUNTIME = Usage.NATIVE_RUNTIME

    @JvmField
    val USAGE_ATTRIBUTE: Attribute<Usage> = Usage.USAGE_ATTRIBUTE
}