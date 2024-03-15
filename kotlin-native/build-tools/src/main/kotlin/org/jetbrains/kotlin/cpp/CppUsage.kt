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

    @JvmField
    val USAGE_ATTRIBUTE: Attribute<Usage> = Usage.USAGE_ATTRIBUTE
}