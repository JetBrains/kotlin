/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.konan.target.SanitizerKind
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty

// Note: options defined in this class are a part of user interface, including the names:
// users can pass these options using a -Xbinary=name=value compiler argument or corresponding Gradle DSL.
object BinaryOptions : BinaryOptionRegistry() {
    val runtimeAssertionsMode by option<RuntimeAssertsMode>()

    val memoryModel by option<MemoryModel>()

    val freezing by option<Freezing>()

    val stripDebugInfoFromNativeLibs by booleanOption()

    val sourceInfoType by option<SourceInfoType>()

    val androidProgramType by option<AndroidProgramType>()

    val unitSuspendFunctionObjCExport by option<UnitSuspendFunctionObjCExport>()

    val objcExportSuspendFunctionLaunchThreadRestriction by option<ObjCExportSuspendFunctionLaunchThreadRestriction>()

    val objcExportDisableSwiftMemberNameMangling by booleanOption()

    val gcSchedulerType by option<GCSchedulerType>()

    val gcMarkSingleThreaded by booleanOption()

    val linkRuntime by option<RuntimeLinkageStrategyBinaryOption>()

    val bundleId by stringOption()
    val bundleShortVersionString by stringOption()
    val bundleVersion by stringOption()

    val appStateTracking by option<AppStateTracking>()

    val sanitizer by option<SanitizerKind>()

    val mimallocUseDefaultOptions by booleanOption()

    val mimallocUseCompaction by booleanOption()

    val preview19LLVMPipeline by booleanOption()
}

open class BinaryOption<T : Any>(
        val name: String,
        val valueParser: ValueParser<T>,
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

    protected fun booleanOption(): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, CompilerConfigurationKey<Boolean>>> =
            PropertyDelegateProvider { _, property ->
                val option = BinaryOption(property.name, BooleanValueParser)
                register(option)
                ReadOnlyProperty { _, _ ->
                    option.compilerConfigurationKey
                }
            }

    protected fun stringOption(): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, CompilerConfigurationKey<String>>> =
            PropertyDelegateProvider { _, property ->
                val option = BinaryOption(property.name, StringValueParser)
                register(option)
                ReadOnlyProperty { _, _ ->
                    option.compilerConfigurationKey
                }
            }

    protected inline fun <reified T : Enum<T>> option(): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, CompilerConfigurationKey<T>>> =
            PropertyDelegateProvider { _, property ->
                val option = BinaryOption(property.name, EnumValueParser(enumValues<T>().toList()))
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

private object StringValueParser : BinaryOption.ValueParser<String> {
    override fun parse(value: String) = value
    override val validValuesHint: String?
        get() = null
}

@PublishedApi
internal class EnumValueParser<T : Enum<T>>(val values: List<T>) : BinaryOption.ValueParser<T> {
    // TODO: should we really ignore case here?
    override fun parse(value: String): T? = values.firstOrNull { it.name.equals(value, ignoreCase = true) }

    override val validValuesHint: String?
        get() = values.joinToString("|")
}
