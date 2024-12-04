/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("TYPEALIAS_EXPANSION_DEPRECATION")

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.file.FileCollection

/**
 * # C interoperability settings
 *
 * This interface represents the configuration settings for invoking the [cinterop tool](https://kotlinlang.org/docs/native-c-interop.html)
 * in Kotlin/Native projects.
 * The cinterop tool provides the ability to use C libraries inside Kotlin projects.
 *
 * **Important:** Use the [CInteropSettings] API instead of directly accessing tasks, configurations,
 * and other related domain objects through the Gradle API.
 *
 * ## Example
 *
 * Here is an example of how to use [CInteropSettings] to configure a cinterop task for the `linuxX64` target:
 *
 * ```kotlin
 * //build.gradle.kts
 * kotlin {
 *     linuxX64() {
 *         compilations.getByName("main") {
 *             cinterops {
 *                 val cinteropForLinuxX64 by creating {
 *                      // Configure the CInteropSettings here
 *                 }
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * In this example, we've added a `cinterop` setting named `cinteropForLinuxX64` to the `linuxX64` `main` [KotlinCompilation].
 * These settings are used to create and configure a `cinterop` task, along with the necessary dependencies for the compile task.
 */
interface CInteropSettings : Named {

    /**
     *  A collection of directories to look for headers.
     */
    interface IncludeDirectories {
        /**
         * A collection of directories to search for headers.
         * It's the equivalent of the `-I<path>` compiler option.
         *
         * #### Usage example
         *
         * The following example demonstrates
         * how to add multiple directories containing header files in a `build.gradle.kts` file:
         *
         * ```kotlin
         * //build.gradle.kts
         * kotlin {
         *     linuxX64() {
         *         compilations.getByName("main") {
         *             cinterops {
         *                 val cinterop by creating {
         *                     defFile(project.file("custom.def"))
         *                     includeDirs {
         *                         allHeaders(project.file("src/main/headersDir1"), project.file("src/main/headersDir2"))
         *                     }
         *                 }
         *             }
         *         }
         *     }
         * }
         * ```
         *
         * In the example, the directories `src/main/headersDir1` and `src/main/headersDir2` in the project directory
         * are specified as locations containing the header files required for the cinterop process.
         *
         * @param includeDirs The directories to be included.
         */
        fun allHeaders(vararg includeDirs: Any)

        /**
         * A collection of directories to search for headers.
         * It's the equivalent of the `-I<path>` compiler option.
         *
         * #### Usage example
         *
         * The following example demonstrates
         * how to add multiple directories containing header files in a `build.gradle.kts` file:
         *
         * ```kotlin
         * //build.gradle.kts
         * kotlin {
         *     linuxX64() {
         *         compilations.getByName("main") {
         *             cinterops {
         *                 val cinterop by creating {
         *                     defFile(project.file("custom.def"))
         *                     includeDirs {
         *                         allHeaders(listOf(project.file("src/main/headersDir1"), project.file("src/main/headersDir2")))
         *                     }
         *                 }
         *             }
         *         }
         *     }
         * }
         * ```
         *
         * In the example, the directories `src/main/headersDir1` and `src/main/headersDir2` in the project directory
         * are specified as locations containing the header files required for the cinterop process.
         *
         * @param includeDirs The collection of directories to be included
         * @see [allHeaders]
         */
        fun allHeaders(includeDirs: Collection<Any>)

        /**
         * Additional directories to search for headers listed in the `headers` property in the `.def` file.
         * It's equivalent to the `-headerFilterAdditionalSearchPrefix` cinterop tool option.
         *
         * #### Usage example
         *
         * The following example demonstrates how to add multiple directories containing header files in a `build.gradle.kts` file:
         *
         * ```kotlin
         * //build.gradle.kts
         * kotlin {
         *     linuxX64() {
         *         compilations.getByName("main") {
         *             cinterops {
         *                 val cinterop by creating {
         *                     defFile(project.file("custom.def"))
         *                     includeDirs {
         *                         headerFilterOnly(project.file("include/libs"))
         *                     }
         *                 }
         *             }
         *         }
         *     }
         * }
         * ```
         *
         * In the example, the directory `include/libs` is specified as the prefix for the directories listed in the `headers`
         * declared in the `custom.def`.
         *
         * @param includeDirs The directories to be included as prefixes for the header filters.
         */
        fun headerFilterOnly(vararg includeDirs: Any)

        /**
         * Additional directories to search for headers listed in the `headers` property in the `.def` file.
         * It's equivalent to the `-headerFilterAdditionalSearchPrefix` cinterop tool option.
         *
         * #### Usage example
         *
         * The following example demonstrates how to add multiple directories containing header files in a `build.gradle.kts` file:
         *
         * ```kotlin
         * //build.gradle.kts
         * kotlin {
         *     linuxX64() {
         *         compilations.getByName("main") {
         *             cinterops {
         *                 val cinterop by creating {
         *                     defFile(project.file("custom.def"))
         *                     includeDirs {
         *                         headerFilterOnly(listOf(project.file("include/libs")))
         *                     }
         *                 }
         *             }
         *         }
         *     }
         * }
         * ```
         *
         * In the example, the directory `include/libs` is specified as the prefix for the directories listed in the `headers`
         * declared in the `custom.def`.
         *
         * @param includeDirs The collection of directories to be included as prefixes for the header filters.
         * @see [headerFilterOnly]
         */
        fun headerFilterOnly(includeDirs: Collection<Any>)
    }

    /**
     * The collection of libraries used for building during the C interoperability process.
     * It's equivalent to the `-library`/`-l` cinterop tool options.
     */
    var dependencyFiles: FileCollection

    /**
     * Specifies the path to the `.def` file that declares bindings for the C libraries.
     * This function serves as a setter for the `.def` file path and is equivalent to passing `-def` to the cinterop tool.
     *
     * #### Usage example
     *
     * The example below shows how to set a custom `.def` file path in a `build.gradle.kts` file:
     *
     * ```kotlin
     * //build.gradle.kts
     * kotlin {
     *     linuxX64 {
     *         compilations.getByName("main").cinterops.create("customCinterop") {
     *             defFile(project.file("custom.def"))
     *         }
     *     }
     *}
     *```
     *
     * In the example, the `custom.def` file located in the project directory is set as the `.def` file.
     *
     * @param file The path to the `.def` file to be used for C interoperability.
     * **Default value:** `src/nativeInterop/cinterop/{name_of_the_cinterop}.def`
     */
    fun defFile(file: Any)

    /**
     * Defines the package name for the generated bindings.
     * It is equivalent to passing `-pkg` to the cinterop tool.
     *
     * #### Example
     *
     * ```kotlin
     * //build.gradle.kts
     *kotlin {
     *    linuxX64 {
     *        compilations.getByName("main").cinterops.create("customCinterop") {
     *            defFile(project.file("custom.def"))
     *            packageName = "com.test.cinterop"
     *        }
     *    }
     *}
     * ```
     *
     * In the example, the com.test.cinterop package contains all the declarations collected from headers by the cinterop tool.
     * These declarations are then available in this package during the final Kotlin compilation.
     *
     * @param value The package name to be assigned.
     */
    fun packageName(value: String)

    /**
     * Adds a header file to produce Kotlin bindings.
     * It's equivalent to the `-header` cinterop tool option.
     *
     * #### Usage example
     *
     * ```kotlin
     * kotlin {
     *     linuxX64() {
     *         compilations.getByName("main") {
     *             cinterops {
     *                 val cinterop by creating {
     *                     defFile(project.file("custom. def"))
     *                     header(project.file("custom.h"))
     *                 }
     *             }
     *         }
     *     }
     * }
     * ```
     *
     * @param file The header file to be included for interoperability with C.
     */
    fun header(file: Any) = headers(file)

    /**
     * Adds header files to produce Kotlin bindings.
     * It's equivalent to the `-header` cinterop tool option.
     *
     * #### Usage example
     *
     * ```kotlin
     * kotlin {
     *     linuxX64() {
     *         compilations.getByName("main") {
     *             cinterops {
     *                 val cinterop by creating {
     *                     defFile(project.file("custom. def"))
     *                     headers(project.file("custom.h"))
     *                 }
     *             }
     *         }
     *     }
     * }
     * ```
     *
     * @param files The header files to be included for interoperability with C.
     * @see [header]
     */
    fun headers(vararg files: Any)

    /**
     * Adds header files to produce Kotlin bindings.
     * It's equivalent to the `-header` cinterop tool option.
     *
     * #### Usage example
     *
     * ```kotlin
     * kotlin {
     *     linuxX64() {
     *         compilations.getByName("main") {
     *             cinterops {
     *                 val cinterop by creating {
     *                     defFile(project.file("custom. def"))
     *                     headers(listOf(project.file("custom.h")))
     *                 }
     *             }
     *         }
     *     }
     * }
     * ```
     *
     * @param files The collection of header files to be included for interoperability with C.
     * @see [headers]
     */
    fun headers(files: FileCollection)

    /**
     * A collection of directories to search for headers.
     *
     * #### Usage example
     *
     * The following example demonstrates how to add multiple directories containing header files in a `build.gradle.kts` file:
     *
     * ```kotlin
     * //build.gradle.kts
     * kotlin {
     *     linuxX64() {
     *         compilations.getByName("main") {
     *             cinterops {
     *                 val cinterop by creating {
     *                     defFile(project.file("custom.def"))
     *                     includeDirs(project.file("include/libs"))
     *                 }
     *             }
     *         }
     *     }
     * }
     * ```
     *
     * In the example, the directory `include/libs` is specified as the prefix for the directories listed in the `headers`
     * declared in the `custom.def`.
     *
     * @param values The directories to be included.
     */
    fun includeDirs(vararg values: Any)

    /**
     * A collection of directories to search for headers.
     *
     * #### Usage example
     *
     * The following example demonstrates how to add multiple directories containing header files in a `build.gradle.kts` file:
     *
     * ```kotlin
     * //build.gradle.kts
     * kotlin {
     *     linuxX64() {
     *         compilations.getByName("main") {
     *             cinterops {
     *                 val cinterop by creating {
     *                     defFile(project.file("custom.def"))
     *                     includeDirs(Action { allHeaders(project.file("include/libs")) })
     *                 }
     *             }
     *         }
     *     }
     * }
     * ```
     *
     * In the example, the directory `include/libs` is specified as the prefix for the directories listed in the `headers`
     * declared in the `custom.def`.
     *
     * @param action Action to declare included directories
     * @see [includeDirs]
     */
    fun includeDirs(action: Action<IncludeDirectories>)

    /**
     * A collection of directories to search for headers.
     *
     * #### Usage example
     *
     * The following example demonstrates how to add multiple directories containing header files in a `build.gradle.kts` file:
     * ```kotlin
     * //build.gradle.kts
     * kotlin {
     *     linuxX64() {
     *         compilations.getByName("main") {
     *             cinterops {
     *                 val cinterop by creating {
     *                     defFile(project.file("custom.def"))
     *                     includeDirs { allHeaders(project.file("include/libs")) }
     *                 }
     *             }
     *         }
     *     }
     * }
     * ```
     *
     * In the example, the directory `include/libs` is specified as the prefix for the directories listed in the `headers`
     * declared in the `custom.def`.
     *
     * @param configure [IncludeDirectories] configuration
     * @see [includeDirs]
     */
    fun includeDirs(configure: IncludeDirectories.() -> Unit)

    /**
     * The options that are passed to the compiler by the cinterop tool.
     *
     * #### Usage example
     *
     * ```kotlin
     * kotlin {
     *     linuxX64() {
     *         compilations.getByName("main") {
     *             cinterops {
     *                 val cinterop by creating {
     *                     defFile(project.file("custom.def"))
     *                     compilerOpts("-Ipath/to/headers")
     *                 }
     *             }
     *         }
     *     }
     * }
     * ```
     *
     * In the example, the `-Ipath/to/headers` compiler option is passed to the cinterop tool.
     *
     * @param values compiler options
     */
    fun compilerOpts(vararg values: String)

    /**
     * The options that are passed to the compiler by the cinterop tool.
     *
     * #### Usage example
     *
     * ```kotlin
     * kotlin {
     *     linuxX64() {
     *         compilations.getByName("main") {
     *             cinterops {
     *                 val cinterop by creating {
     *                     defFile(project.file("custom.def"))
     *                     compilerOpts(listOf("-Ipath/to/headers"))
     *                 }
     *             }
     *         }
     *     }
     * }
     * ```
     * In the example, the `-Ipath/to/headers` compiler option is passed to the cinterop tool.
     *
     * @see [compilerOpts]
     */
    fun compilerOpts(values: List<String>)

    /**
     * Adds additional linker options.
     * It's equivalent to the `-linker-options` cinterop tool option.
     *
     * #### Usage example
     *
     * ```kotlin
     * kotlin {
     *     linuxX64() {
     *         compilations.getByName("main") {
     *             cinterops {
     *                 val cinterop by creating {
     *                     defFile(project.file("custom. def"))
     *                     linkerOpts("-lNativeBase64")
     *                 }
     *             }
     *         }
     *     }
     * }
     * ```
     *
     * @param values linker options
     */
    fun linkerOpts(vararg values: String)

    /**
     * Adds additional linker options.
     * It's equivalent to the `-linker-options` cinterop tool option.
     *
     * #### Usage example
     *
     * ```kotlin
     * kotlin {
     *     linuxX64() {
     *         compilations.getByName("main") {
     *             cinterops {
     *                 val cinterop by creating {
     *                     defFile(project.file("custom. def"))
     *                     linkerOpts(listOf("-lNativeBase64"))
     *                 }
     *             }
     *         }
     *     }
     * }
     * ```
     *
     * @param values List of linker options
     * @see [linkerOpts]
     */
    fun linkerOpts(values: List<String>)

    /**
     * Adds additional options that are passed to the cinterop tool.
     *
     * #### Usage example
     *
     * ```kotlin
     * kotlin {
     *     linuxX64() {
     *         compilations.getByName("main") {
     *             cinterops {
     *                 val cinterop by creating {
     *                     defFile(project.file("custom. def"))
     *                     extraOpts("-nopack")
     *                 }
     *             }
     *         }
     *     }
     * }
     * ```
     *
     * @param values Additional cinterop tool options that are not present in [CInteropSettings]
     */
    fun extraOpts(vararg values: Any)

    /**
     * Adds additional options that are passed to the cinterop tool.
     *
     * #### Usage example
     *
     * ```kotlin
     * kotlin {
     *     linuxX64() {
     *         compilations.getByName("main") {
     *             cinterops {
     *                 val cinterop by creating {
     *                     defFile(project.file("custom. def"))
     *                     extraOpts(listOf("-nopack"))
     *                 }
     *             }
     *         }
     *     }
     * }
     * ```
     *
     * @param values List of extra options
     * @see [extraOpts]
     */
    fun extraOpts(values: List<Any>)
}
