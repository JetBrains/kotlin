/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config.nativeBinaryOptions

import org.jetbrains.kotlin.backend.konan.CachePolicy
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty

// Note: options defined in this class are a part of user interface, including the names:
// users can pass these options using a -Xbinary=name=value compiler argument or corresponding Gradle DSL.
// Each option must declare its [CachePolicy]: whether it should be passed to the compilations building the compiler caches,
// and whether it should be a part of the cache flavor.
object BinaryOptions : BinaryOptionRegistry() {
    val runtimeAssertionsMode by option<RuntimeAssertsMode>(CachePolicy.AFFECTS_RUNTIME)

    val checkStateAtExternalCalls by booleanOption(CachePolicy.AFFECTS_CODEGEN)

    val memoryModel by option<MemoryModel>(CachePolicy.NOT_PROPAGATED)

    val freezing by option<Freezing>(CachePolicy.NOT_PROPAGATED)

    val stripDebugInfoFromNativeLibs by booleanOption(CachePolicy.AFFECTS_RUNTIME)

    val sourceInfoType by option<SourceInfoType>(CachePolicy.NOT_PROPAGATED)
    val coreSymbolicationImageListType by option<CoreSymbolicationImageListType>(CachePolicy.NOT_PROPAGATED)

    val androidProgramType by option<AndroidProgramType>(CachePolicy.NOT_PROPAGATED)

    val unitSuspendFunctionObjCExport by option<UnitSuspendFunctionObjCExport>(CachePolicy.NOT_PROPAGATED)

    val objcExportSuspendFunctionLaunchThreadRestriction by option<ObjCExportSuspendFunctionLaunchThreadRestriction>(CachePolicy.NOT_PROPAGATED)

    val objcExportDisableSwiftMemberNameMangling by booleanOption(CachePolicy.NOT_PROPAGATED)

    val objcExportIgnoreInterfaceMethodCollisions by booleanOption(CachePolicy.NOT_PROPAGATED)

    val objcExportReportNameCollisions by booleanOption(CachePolicy.NOT_PROPAGATED)

    val objcExportErrorOnNameCollisions by booleanOption(CachePolicy.NOT_PROPAGATED)

    val objcExportEntryPointsPath by stringOption(CachePolicy.NOT_PROPAGATED)
    val objcExportExpandEntryPoints by booleanOption(CachePolicy.NOT_PROPAGATED)

    val objcExportExplicitMethodFamily by booleanOption(CachePolicy.NOT_PROPAGATED)

    val objcExportBlockExplicitParameterNames by booleanOption(CachePolicy.NOT_PROPAGATED)

    val dumpObjcSelectorToSignatureMapping by stringOption(CachePolicy.NOT_PROPAGATED)

    val gc by option<GC>(CachePolicy.AFFECTS_RUNTIME, shortcut = { it.shortcut })

    val gcSchedulerType by option<GCSchedulerType>(CachePolicy.AFFECTS_RUNTIME, hideValue = { it.deprecatedWithReplacement != null })

    val gcMarkSingleThreaded by booleanOption(CachePolicy.AFFECTS_RUNTIME)

    val fixedBlockPageSize by uintOption(CachePolicy.AFFECTS_RUNTIME)

    val concurrentWeakSweep by booleanOption(CachePolicy.AFFECTS_RUNTIME)

    val concurrentMarkMaxIterations by uintOption(CachePolicy.NOT_PROPAGATED)

    val gcMutatorsCooperate by booleanOption(CachePolicy.NOT_PROPAGATED)

    val auxGCThreads by uintOption(CachePolicy.NOT_PROPAGATED)

    val linkRuntime by option<RuntimeLinkageStrategy>(CachePolicy.AFFECTS_RUNTIME)

    val bundleId by stringOption(CachePolicy.NOT_PROPAGATED)
    val bundleShortVersionString by stringOption(CachePolicy.NOT_PROPAGATED)
    val bundleVersion by stringOption(CachePolicy.NOT_PROPAGATED)

    val appStateTracking by option<AppStateTracking>(CachePolicy.NOT_PROPAGATED)

    val sanitizer by option<SanitizerKind>(CachePolicy.AFFECTS_CODEGEN)

    val compileBitcodeWithXcodeLlvm by booleanOption(CachePolicy.PROPAGATED)

    val objcDisposeOnMain by booleanOption(CachePolicy.NOT_PROPAGATED)

    val objcDisposeWithRunLoop by booleanOption(CachePolicy.NOT_PROPAGATED)

    val disableMmap by booleanOption(CachePolicy.AFFECTS_RUNTIME)

    val mmapTag by uintOption(CachePolicy.NOT_PROPAGATED)

    val enableSafepointSignposts by booleanOption(CachePolicy.NOT_PROPAGATED)

    val forceNativeThreadStateForFunctions by listOption(CachePolicy.INCOMPATIBLE_WITH_CACHES, StringValueParser)

    val packFields by booleanOption(CachePolicy.AFFECTS_CODEGEN)

    val cInterfaceMode by option<CInterfaceGenerationMode>(CachePolicy.NOT_PROPAGATED)

    val globalDataLazyInit by booleanOption(CachePolicy.NOT_PROPAGATED)

    val swiftExport by booleanOption(CachePolicy.AFFECTS_CODEGEN)

    val genericSafeCasts by booleanOption(CachePolicy.AFFECTS_CODEGEN)

    val smallBinary by booleanOption(CachePolicy.AFFECTS_CODEGEN)

    // The pre-codegen inliner isn't supported with caches.
    val preCodegenInlineThreshold by uintOption(CachePolicy.NOT_PROPAGATED)

    val enableDebugTransparentStepping by booleanOption(CachePolicy.AFFECTS_CODEGEN)

    val debugCompilationDir by stringOption(CachePolicy.AFFECTS_CODEGEN)

    val pagedAllocator by booleanOption(CachePolicy.AFFECTS_RUNTIME)

    val latin1Strings by booleanOption(CachePolicy.AFFECTS_CODEGEN)

    val stackProtector by option<StackProtectorMode>(CachePolicy.AFFECTS_CODEGEN)

    val minidumpLocation by stringOption(CachePolicy.AFFECTS_RUNTIME)

    val minidumpOnSIGTERM by booleanOption(CachePolicy.NOT_PROPAGATED)

    val cCallMode by option<CCallMode>(CachePolicy.AFFECTS_CODEGEN)

    /**
     * Generate a macOS Catalyst binary for the given architecture
     */
    val macabi by booleanOption(CachePolicy.AFFECTS_CODEGEN)

    val escapeAnalysisPropagateExiledToHeapObjects by booleanOption(CachePolicy.AFFECTS_CODEGEN)

    val perFileCacheForStdlib by booleanOption(CachePolicy.AFFECTS_CODEGEN)

    /**
     * When `true`, llvm postprocessing will be done in Kotlin compiler;
     * when `false`, new custom llvm C++ passes will be used.
     * This option exists as a workaround, if new passes introduce regressions.
     */
    val runLLVMPassesInCompiler by booleanOption(CachePolicy.AFFECTS_CODEGEN)

    val enableReleaseBinaryCache by booleanOption(CachePolicy.PROPAGATED)
}

open class BinaryOption<T : Any>(
        val name: String,
        val valueParser: ValueParser<T>,
        val cachePolicy: CachePolicy,
        val compilerConfigurationKey: CompilerConfigurationKey<T> = CompilerConfigurationKey.create(name)
) {
    interface ValueParser<T : Any> {
        fun parse(value: String): T?
        val validValuesHint: String?
    }
}

open class BinaryOptionRegistry {
    private val registeredOptionsByName = mutableMapOf<String, BinaryOption<*>>()

    protected fun register(option: BinaryOption<*>) {
        val previousOption = registeredOptionsByName[option.name]
        if (previousOption != null) {
            error("option '${option.name}' is registered twice")
        }
        registeredOptionsByName[option.name] = option
    }

    fun getByName(name: String): BinaryOption<*>? = registeredOptionsByName[name]

    val all: Collection<BinaryOption<*>>
        get() = registeredOptionsByName.values

    protected fun booleanOption(cachePolicy: CachePolicy): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, CompilerConfigurationKey<Boolean>>> =
            PropertyDelegateProvider { _, property ->
                val option = BinaryOption(property.name, BooleanValueParser, cachePolicy)
                register(option)
                ReadOnlyProperty { _, _ ->
                    option.compilerConfigurationKey
                }
            }

    protected fun uintOption(cachePolicy: CachePolicy): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, CompilerConfigurationKey<UInt>>> =
            PropertyDelegateProvider { _, property ->
                val option = BinaryOption(property.name, UIntValueParser, cachePolicy)
                register(option)
                ReadOnlyProperty { _, _ ->
                    option.compilerConfigurationKey
                }
            }

    protected fun stringOption(cachePolicy: CachePolicy): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, CompilerConfigurationKey<String>>> =
            PropertyDelegateProvider { _, property ->
                val option = BinaryOption(property.name, StringValueParser, cachePolicy)
                register(option)
                ReadOnlyProperty { _, _ ->
                    option.compilerConfigurationKey
                }
            }

    protected fun <T : Any> listOption(
            cachePolicy: CachePolicy,
            elementValueParser: BinaryOption.ValueParser<T>
    ): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, CompilerConfigurationKey<List<T>>>> = PropertyDelegateProvider { _, property ->
        val option = BinaryOption(property.name, ListValueParser(elementValueParser), cachePolicy)
        register(option)
        ReadOnlyProperty { _, _ ->
            option.compilerConfigurationKey
        }
    }

    protected inline fun <reified T : Enum<T>> option(
            cachePolicy: CachePolicy,
            noinline shortcut: (T) -> String? = { null },
            noinline hideValue: (T) -> Boolean = { false },
    ): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, CompilerConfigurationKey<T>>> =
            PropertyDelegateProvider { _, property ->
                val option = BinaryOption(property.name, EnumValueParser(enumValues<T>().toList(), shortcut, hideValue), cachePolicy)
                register(option)
                ReadOnlyProperty { _, _ ->
                    option.compilerConfigurationKey
                }
            }
}

private object BooleanValueParser : BinaryOption.ValueParser<Boolean> {
    override fun parse(value: String): Boolean? = value.toBooleanStrictOrNull()

    override val validValuesHint: String?
        get() = "true|false"
}

private object UIntValueParser : BinaryOption.ValueParser<UInt> {
    override fun parse(value: String): UInt? = value.toUIntOrNull()

    override val validValuesHint: String?
        get() = "non-negative-number"
}

private object StringValueParser : BinaryOption.ValueParser<String> {
    override fun parse(value: String) = value
    override val validValuesHint: String?
        get() = "string"
}

private class ListValueParser<T : Any>(
        val elementValueParser: BinaryOption.ValueParser<T>
) : BinaryOption.ValueParser<List<T>> {
    override fun parse(value: String): List<T>? {
        if (value == "") return emptyList()

        return value.split(";").map {
            elementValueParser.parse(it) ?: return null
        }
    }

    override val validValuesHint: String?
        get() = "semicolon-separated list of ${elementValueParser.validValuesHint}"

}

@PublishedApi
internal class EnumValueParser<T : Enum<T>>(
    val values: List<T>,
    val shortcut: (T) -> String?,
    val hideValue: (T) -> Boolean,
) : BinaryOption.ValueParser<T> {
    override fun parse(value: String): T? = values.firstOrNull {
        // TODO: should we really ignore case here?
        it.name.equals(value, ignoreCase = true) || (shortcut(it)?.equals(value, ignoreCase = true) ?: false)
    }

    override val validValuesHint: String?
        get() = values.filter { !hideValue(it) }.map {
            val fullName = "$it".lowercase()
            shortcut(it)?.let { short ->
                "$fullName (or: $short)"
            } ?: fullName
        }.joinToString("|")
}
