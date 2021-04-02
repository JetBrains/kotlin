/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.native.interop.tool

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.*
import org.jetbrains.kotlin.native.interop.gen.jvm.GenerationMode

const val HEADER_FILTER_ADDITIONAL_SEARCH_PREFIX = "headerFilterAdditionalSearchPrefix"
const val NODEFAULTLIBS_DEPRECATED = "nodefaultlibs"
const val NODEFAULTLIBS = "no-default-libs"
const val NOENDORSEDLIBS = "no-endorsed-libs"
const val PURGE_USER_LIBS = "Xpurge-user-libs"
const val TEMP_DIR = "Xtemporary-files-dir"
const val NOPACK = "nopack"
const val COMPILE_SOURCES = "Xcompile-source"
const val SHORT_MODULE_NAME = "Xshort-module-name"
const val FOREIGN_EXCEPTION_MODE = "Xforeign-exception-mode"
const val DUMP_BRIDGES = "Xdump-bridges"

// TODO: unify camel and snake cases.
// Possible solution is to accept both cases
open class CommonInteropArguments(val argParser: ArgParser) {
    val verbose by argParser.option(ArgType.Boolean, description = "Enable verbose logging output").default(false)
    val pkg by argParser.option(ArgType.String, description = "place generated bindings to the package")
    val output by argParser.option(ArgType.String, shortName = "o", description = "specifies the resulting library file")
            .default("nativelib")
    val libraryPath by argParser.option(ArgType.String,  description = "add a library search path")
            .multiple().delimiter(",")
    val staticLibrary by argParser.option(ArgType.String, description = "embed static library to the result")
            .multiple().delimiter(",")
    val library by argParser.option(ArgType.String, shortName = "l", description = "library to use for building")
            .multiple()
    val repo by argParser.option(ArgType.String, shortName = "r", description = "repository to resolve dependencies")
            .multiple()
    val mode by argParser.option(ArgType.Choice<GenerationMode>(), description = "the way interop library is generated")
            .default(DEFAULT_MODE)
    val nodefaultlibs by argParser.option(ArgType.Boolean, NODEFAULTLIBS,
            description = "don't link the libraries from dist/klib automatically").default(false)
    val nodefaultlibsDeprecated by argParser.option(ArgType.Boolean, NODEFAULTLIBS_DEPRECATED,
            description = "don't link the libraries from dist/klib automatically",
            deprecatedWarning = "Old form of flag. Please, use $NODEFAULTLIBS.").default(false)
    val noendorsedlibs by argParser.option(ArgType.Boolean, NOENDORSEDLIBS,
            description = "don't link the endorsed libraries from dist automatically").default(false)
    val purgeUserLibs by argParser.option(ArgType.Boolean, PURGE_USER_LIBS,
            description = "don't link unused libraries even explicitly specified").default(false)
    val nopack by argParser.option(ArgType.Boolean, fullName = NOPACK,
            description = "Don't pack the produced library into a klib file").default(false)
    val tempDir by argParser.option(ArgType.String, TEMP_DIR,
            description = "save temporary files to the given directory")
    val kotlincOption by argParser.option(ArgType.String, "Xkotlinc-option",
            description = "additional kotlinc compiler option").multiple()

    companion object {
        val DEFAULT_MODE = GenerationMode.METADATA
    }
}

open class CInteropArguments(argParser: ArgParser =
                                ArgParser("cinterop",
                                        prefixStyle = ArgParser.OptionPrefixStyle.JVM)): CommonInteropArguments(argParser) {
    val target by argParser.option(ArgType.String, description = "native target to compile to").default("host")
    val def by argParser.option(ArgType.String, description = "the library definition file")
    val header by argParser.option(ArgType.String, description = "header file to produce kotlin bindings for")
            .multiple().delimiter(",")
    val headerFilterPrefix by argParser.option(ArgType.String, HEADER_FILTER_ADDITIONAL_SEARCH_PREFIX, "hfasp",
            "header file to produce kotlin bindings for").multiple().delimiter(",")
    val compilerOpts by argParser.option(ArgType.String,
            description = "additional compiler options (allows to add several options separated by spaces)",
            deprecatedWarning = "-compilerOpts is deprecated. Please use -compiler-options.")
            .multiple().delimiter(" ")
    val compilerOptions by argParser.option(ArgType.String, "compiler-options",
            description = "additional compiler options (allows to add several options separated by spaces)")
            .multiple().delimiter(" ")
    val linkerOpts = argParser.option(ArgType.String, "linkerOpts",
            description = "additional linker options (allows to add several options separated by spaces)",
            deprecatedWarning = "-linkerOpts is deprecated. Please use -linker-options.")
            .multiple().delimiter(" ")
    val linkerOptions = argParser.option(ArgType.String, "linker-options",
            description = "additional linker options (allows to add several options separated by spaces)")
            .multiple().delimiter(" ")
    val compilerOption by argParser.option(ArgType.String, "compiler-option",
            description = "additional compiler option").multiple()
    val linkerOption = argParser.option(ArgType.String, "linker-option",
            description = "additional linker option").multiple()
    val linker by argParser.option(ArgType.String, description = "use specified linker")

    val compileSource by argParser.option(ArgType.String,
            fullName = COMPILE_SOURCES,
            description = "additional C/C++ sources to be compiled into resulting library"
    ).multiple()

    val sourceCompileOptions by argParser.option(ArgType.String,
            fullName = "Xsource-compiler-option",
            description = "compiler options for sources provided via -$COMPILE_SOURCES"
    ).multiple()

    val shortModuleName by argParser.option(ArgType.String,
            fullName = SHORT_MODULE_NAME,
            description = "A short name used to denote this library in the IDE"
    )

    val moduleName by argParser.option(ArgType.String,
            fullName = "Xmodule-name",
            description = "A full name of the library used for dependency resolution"
    )

    val foreignExceptionMode by argParser.option(ArgType.String, FOREIGN_EXCEPTION_MODE,
            description = "Handle native exception in Kotlin: <terminate|objc-wrap>")

    val dumpBridges by argParser.option(ArgType.Boolean, DUMP_BRIDGES,
            description = "Dump generated bridges")
}

class JSInteropArguments(argParser: ArgParser = ArgParser("jsinterop",
        prefixStyle = ArgParser.OptionPrefixStyle.JVM)): CommonInteropArguments(argParser) {
    enum class TargetType {
        WASM32;

        override fun toString() = name.toLowerCase()
    }
    val target by argParser.option(ArgType.Choice<TargetType>(),
            description = "wasm target to compile to").default(TargetType.WASM32)
}

internal fun warn(msg: String) {
    println("warning: $msg")
}
