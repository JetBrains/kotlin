/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.compilation

import com.intellij.openapi.util.text.StringUtil.substringAfter
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.GroupingMessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollector

/**
 * Special implementation of [GroupingMessageCollector] that raises the severity of unexpected
 * compiler warnings to errors, this way failing the tests.
 */
internal class NativeTestGroupingMessageCollector(
    compilerArgs: Array<String>,
    delegate: MessageCollector,
) : GroupingMessageCollector(
    delegate,
    /*treatWarningsAsErrors =*/ false,
    /*reportAllWarnings =*/ true, // Any warning that survived to this place is actually a strong_warning, and it should not be suppressed.
) {
    private var hasWarningsWithRaisedSeverity: Boolean = false

    private val languageFeaturesInCompilerArgs: Set<String> by lazy {
        compilerArgs.mapNotNullTo(hashSetOf(), ::parseLanguageFeatureArg)
    }

    private val pathOfCachedLibraryWithTests: String? by lazy {
        var isProducingStaticCache = false
        var includedLibraryName: String? = null
        var cachedLibraryName: String? = null

        for ((index, arg) in compilerArgs.withIndex()) {
            if ((arg == "-p" || arg == "-produce")
                && index < compilerArgs.size - 1
                && compilerArgs[index + 1] == "static_cache"
            ) {
                isProducingStaticCache = true
            } else {
                includedLibraryName = includedLibraryName ?: substringAfter(arg, "-Xinclude=")
                cachedLibraryName = cachedLibraryName ?: substringAfter(arg, "-Xadd-cache=")
            }

            if (isProducingStaticCache && includedLibraryName != null && cachedLibraryName != null)
                return@lazy cachedLibraryName.takeIf { includedLibraryName == cachedLibraryName }
        }

        null
    }

    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) =
        super.report(adjustSeverity(severity, message, location), message, location)

    private fun adjustSeverity(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) =
        when {
            !severity.isWarning -> {
                // Don't adjust severity for non-warnings.
                severity
            }
            location != null -> {
                // Don't adjust severity for any syntax and semantic warnings.
                severity
            }
            isPreReleaseBinariesWarning(message)
                    || isUnsafeCompilerArgumentsWarning(message)
                    || isLibraryIncludedMoreThanOnceWarning(message)
                    || isK2Experimental(message)
                    || isPartialLinkageWarning(message)
                    || isKlibResolver(message)
                    || isContextReceiversWarning(message)
                    || isK1LanguageVersionWarning(message)
                -> {
                // These warnings are known and should not be reported as errors.
                severity
            }
            else -> {
                hasWarningsWithRaisedSeverity = true
                CompilerMessageSeverity.ERROR
            }
        }

    private fun isPreReleaseBinariesWarning(message: String): Boolean {
        val languageFeatures = substringAfter(message, PRE_RELEASE_WARNING_PREFIX)
            ?.split(", ")
            ?.takeIf(Collection<String>::isNotEmpty)
            ?: return false

        return languageFeaturesInCompilerArgs.containsAll(languageFeatures)
    }

    private fun isUnsafeCompilerArgumentsWarning(message: String): Boolean {
        val languageFeatures = substringAfter(message, UNSAFE_COMPILER_ARGS_WARNING_PREFIX)
            ?.lineSequence()
            ?.takeWhile(String::isNotBlank)
            ?.map { parseLanguageFeatureArg(it) ?: "<non-parsable command line argument>" }
            ?.toList()
            ?.takeIf(Collection<String>::isNotEmpty)
            ?: return false

        return languageFeaturesInCompilerArgs.containsAll(languageFeatures)
    }

    private fun isLibraryIncludedMoreThanOnceWarning(message: String): Boolean {
        val libraryPath = substringAfter(message, LIBRARY_INCLUDED_MORE_THAN_ONCE_WARNING_PREFIX)
            ?.takeIf(String::isNotBlank)
            ?: return false

        return libraryPath == pathOfCachedLibraryWithTests
    }

    private fun isK2Experimental(message: String): Boolean = message.startsWith(K2_NATIVE_EXPERIMENTAL_WARNING_PREFIX)

    private fun isKlibResolver(message: String): Boolean = message.startsWith(KLIB_RESOLVER_WARNING_PREFIX)

    private fun isPartialLinkageWarning(message: String): Boolean = message.matches(PARTIAL_LINKAGE_WARNING_REGEX)

    private fun isContextReceiversWarning(message: String): Boolean = message.startsWith(CONTEXT_RECEIVERS_WARNING_PREFIX)

    private fun isK1LanguageVersionWarning(message: String): Boolean = message.matches(K1_LANGUAGE_VERSIONS_WARNING_REGEX)

    override fun hasErrors() = hasWarningsWithRaisedSeverity || super.hasErrors()

    companion object {
        private const val PRE_RELEASE_WARNING_PREFIX = "Following manually enabled features will force generation of pre-release binaries: "
        private const val UNSAFE_COMPILER_ARGS_WARNING_PREFIX = "ATTENTION!\nThis build uses unsafe internal compiler arguments:\n\n"
        private const val LIBRARY_INCLUDED_MORE_THAN_ONCE_WARNING_PREFIX = "library included more than once: "
        private const val K2_NATIVE_EXPERIMENTAL_WARNING_PREFIX = "Language version 2.0 is experimental"
        private const val KLIB_RESOLVER_WARNING_PREFIX = "KLIB resolver: "
        private const val CONTEXT_RECEIVERS_WARNING_PREFIX = "Experimental context receivers are superseded by context parameters"

        private val K1_LANGUAGE_VERSIONS_WARNING_REGEX = Regex("Language version 1.[0-9.]+ is deprecated and its support will be removed in a future version of Kotlin")
        private val PARTIAL_LINKAGE_WARNING_REGEX = Regex("^<[^<>]+>( @ (?:(?!: ).)+)?: .*")

        private fun parseLanguageFeatureArg(arg: String): String? =
            substringAfter(arg, "-XXLanguage:-") ?: substringAfter(arg, "-XXLanguage:+")
    }
}
