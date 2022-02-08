/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.cli

internal enum class TaskType(
    val alias: String,
    val description: String,
    val optionTypes: List<OptionType<*>>,
    val taskConstructor: (Collection<Option<*>>) -> Task
) {
    NATIVE_DIST_COMMONIZE(
        "native-dist-commonize",
        "Commonize platform-specific libraries in Kotlin/Native distribution",
        listOf(
            NativeDistributionOptionType,
            OutputOptionType,
            NativeTargetsOptionType,
            OutputCommonizerTargetsOptionType,
            BooleanOptionType(
                COPY_STDLIB_ALIAS,
                "Boolean (default false);\nwhether to copy Kotlin/Native endorsed libraries to the destination",
                mandatory = false
            ),
            BooleanOptionType(
                COPY_ENDORSED_LIBS_ALIAS,
                "Boolean (default false);\nwhether to copy Kotlin/Native endorsed libraries to the destination",
                mandatory = false
            ),
            StatsTypeOptionType,
            LogLevelOptionType,
        ) + ADDITIONAL_COMMONIZER_SETTINGS,
        ::NativeDistributionCommonize
    ),

    NATIVE_DIST_LIST_TARGETS(
        "native-dist-print-targets",
        "Print all hardware targets inside of the Kotlin/Native distribution",
        listOf(
            NativeDistributionOptionType
        ),
        ::NativeDistributionListTargets
    ),

    NATIVE_KLIB_COMMONIZE(
        "native-klib-commonize",
        "Commonize any platform-specific libraries",
        listOf(
            NativeDistributionOptionType,
            OutputOptionType,
            InputLibrariesOptionType,
            DependencyLibrariesOptionType,
            OutputCommonizerTargetsOptionType,
            LogLevelOptionType
        ) + ADDITIONAL_COMMONIZER_SETTINGS,
        ::NativeKlibCommonize
    )
    ;

    companion object {
        fun getByAlias(alias: String) = values().firstOrNull { it.alias == alias }
    }
}
