/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollector.Companion.NONE
import org.jetbrains.kotlin.config.AnalysisFlag
import org.jetbrains.kotlin.config.ConstraintSystemForOverloadResolutionMode
import org.jetbrains.kotlin.config.ExplicitApiMode
import org.jetbrains.kotlin.config.JvmDefaultMode
import org.jetbrains.kotlin.load.java.JavaTypeEnhancementState

internal fun IdeaKpmFragmentAnalysisFlags(compilerArguments: CommonCompilerArguments?): IdeaKpmFragmentAnalysisFlags =
    compilerArguments?.configureAnalysisFlags(NONE, compilerArguments.toLanguageVersionSettings(NONE).languageVersion)
        ?.entries
        ?.mapNotNull { (flag, value) -> IdeaKpmFragmentAnalysisFlag(flag, value, compilerArguments) }
        .let { IdeaKpmFragmentAnalysisFlagsImpl(it.orEmpty()) }

internal fun IdeaKpmFragmentAnalysisFlag(
    analysisFlag: AnalysisFlag<*>,
    value: Any,
    compilerArguments: CommonCompilerArguments
): IdeaKpmFragmentAnalysisFlag? {
    val flagName = analysisFlag.toString()
    val newValue = when (flagName) {
        in hashSetOf(
            "skipMetadataVersionCheck", "skipPrereleaseCheck", "multiPlatformDoNotCheckActual", "expectActualLinker", "explicitApiVersion",
            "ignoreDataFlowInAssert", "allowResultReturnType", "ideMode", "allowUnstableDependencies", "libraryToSourceAnalysis",
            "extendedCompilerChecks", "allowKotlinPackage", "builtInsFromSources", "allowFullyQualifiedNameInKClass",
            "eagerResolveOfLightClasses", "strictMetadataVersionSemantics", "inheritMultifileParts", "sanitizeParentheses",
            "suppressMissingBuiltinsError", "disableUltraLightClasses", "enableJvmPreview", "useIR"
        ) -> (value as Boolean).toString()

        "optIn" -> (value as List<String>).joinToString(separator = ";")
        "explicitApiMode" -> (value as ExplicitApiMode).state
        "constraintSystemForOverloadResolution" -> (value as ConstraintSystemForOverloadResolutionMode).name
        "javaTypeEnhancementState" -> (compilerArguments as K2JVMCompilerArguments).let {
            arrayOf(it.jsr305, it.supportCompatqualCheckerFrameworkAnnotations, it.jspecifyAnnotations, it.nullabilityAnnotations)
                .joinToString(separator = ";")
        }

        "jvmDefaultMode" -> (value as JvmDefaultMode).description
        else -> return null // Log or somehow contribute information tp model
    }
    return IdeaKpmFragmentAnalysisFlagImpl(flagName, newValue)
}