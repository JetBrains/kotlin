/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cli

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
            BooleanOptionType(
                "copy-stdlib",
                "Boolean (default false);\nwhether to copy Kotlin/Native endorsed libraries to the destination",
                mandatory = false
            ),
            BooleanOptionType(
                "copy-endorsed-libs",
                "Boolean (default false);\nwhether to copy Kotlin/Native endorsed libraries to the destination",
                mandatory = false
            ),
            StatsTypeOptionType
        ),
        ::NativeDistributionCommonize
    ),

    NATIVE_DIST_LIST_TARGETS(
        "native-dist-print-targets",
        "Print all hardware targets inside of the Kotlin/Native distribution",
        listOf(
            NativeDistributionOptionType
        ),
        ::NativeDistributionListTargets
    );

    companion object {
        fun getByAlias(alias: String) = values().firstOrNull { it.alias == alias }
    }
}
