/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.cli

// General

private const val DEPENDENCY_LIBRARIES_ALIAS: String = "dependency-libraries"
private const val INPUT_LIBRARIES_ALIAS: String = "input-libraries"
private const val LOG_LEVEL_ALIAS: String = "log-level"
private const val NATIVE_DISTRIBUTION_PATH_ALIAS: String = "distribution-path"
private const val OUTPUT_COMMONIZER_TARGETS_ALIAS: String = "output-targets"
private const val OUTPUT_PATH_ALIAS: String = "output-path"
private const val STATS_TYPE_ALIAS: String = "log-stats"
private const val NATIVE_TARGETS_ALIAS: String = "targets"
private const val COPY_STDLIB_ALIAS: String = "copy-stdlib"
private const val COPY_ENDORSED_LIBS_ALIAS: String = "copy-endorsed-libs"

public val DEPENDENCY_LIBRARIES_OPTION_ALIAS: OptionAlias = OptionAlias(DEPENDENCY_LIBRARIES_ALIAS)
public val INPUT_LIBRARIES_OPTION_ALIAS: OptionAlias = OptionAlias(INPUT_LIBRARIES_ALIAS)
public val LOG_LEVEL_OPTION_ALIAS: OptionAlias = OptionAlias(LOG_LEVEL_ALIAS)
public val NATIVE_DISTRIBUTION_OPTION_ALIAS: OptionAlias = OptionAlias(NATIVE_DISTRIBUTION_PATH_ALIAS)
public val OUTPUT_COMMONIZER_TARGET_OPTION_ALIAS: OptionAlias = OptionAlias(OUTPUT_COMMONIZER_TARGETS_ALIAS)
public val OUTPUT_OPTION_ALIAS: OptionAlias = OptionAlias(OUTPUT_PATH_ALIAS)
public val STATS_TYPE_OPTION_ALIAS: OptionAlias = OptionAlias(STATS_TYPE_ALIAS)
public val NATIVE_TARGETS_OPTION_ALIAS: OptionAlias = OptionAlias(NATIVE_TARGETS_ALIAS)
public val COPY_STDLIB_OPTION_ALIAS: OptionAlias = OptionAlias(COPY_STDLIB_ALIAS)
public val COPY_ENDORSED_LIBS_OPTION_ALIAS: OptionAlias = OptionAlias(COPY_ENDORSED_LIBS_ALIAS)

// Commonizer settings
private const val OPTIMISTIC_NUMBER_COMMONIZATION_ENABLED_ALIAS: String = "optimistic-numbers"

public val OPTIMISTIC_NUMBER_COMMONIZATION_ENABLED_OPTION_ALIAS: OptionAlias = OptionAlias(OPTIMISTIC_NUMBER_COMMONIZATION_ENABLED_ALIAS)

@JvmInline
public value class OptionAlias(public val aliasString: String) {
    public val argumentString: String
        get() = "-$aliasString"

    override fun toString(): String = aliasString
}
