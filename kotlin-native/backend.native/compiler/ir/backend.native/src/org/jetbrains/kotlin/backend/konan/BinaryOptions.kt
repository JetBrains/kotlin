/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.config.CompilerConfigurationKey
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.*

// Note: options defined in this class are a part of user interface, including the names:
// users can pass these options using a -Xbinary=name=value compiler argument or corresponding Gradle DSL.
object BinaryOptions : BinaryOptionRegistry() {
    val runtimeAssertionsMode by option<RuntimeAssertsMode>()

    val memoryManager by option<MemoryManager>()

    @Deprecated("Use memoryManager instead")
    val memoryModel by option<MemoryModel>().replacedBy(this::memoryManager) {
        when (it) {
            MemoryModel.RELAXED -> null
            MemoryModel.STRICT -> MemoryManager.LEGACY
            MemoryModel.EXPERIMENTAL -> MemoryManager.UNRESTRICTED
        }
    }

    val freezing by option<Freezing>()

    val stripDebugInfoFromNativeLibs by booleanOption()

    val sourceInfoType by option<SourceInfoType>()

    val androidProgramType by option<AndroidProgramType>()

    val unitSuspendFunctionObjCExport by option<UnitSuspendFunctionObjCExport>()

    val gcSchedulerType by option<GCSchedulerType>()
}

open class BinaryOption<T : Any>(
        val name: String,
        val valueParser: ValueParser<T>,
        val compilerConfigurationKey: CompilerConfigurationKey<T> = CompilerConfigurationKey.create(name)
) {
    interface ValueParser<T : Any> {
        fun parse(value: String): T?
        val validValuesHint: String?
        val warning: String? get() = null
    }
}

open class BinaryOptionRegistry {
    private val registeredOptionsByName = mutableMapOf<String, BinaryOption<*>>()

    protected fun register(option: BinaryOption<*>, canReplace: Boolean = false) {
        val previousOption = registeredOptionsByName[option.name]
        if (previousOption != null && !canReplace) {
            error("option '${option.name}' is registered twice")
        }
        registeredOptionsByName[option.name] = option
    }

    fun getByName(name: String): BinaryOption<*>? = registeredOptionsByName[name]

    class OptionDelegate<T : Any>(val option: BinaryOption<T>) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>) = option.compilerConfigurationKey
    }

    protected fun booleanOption(): PropertyDelegateProvider<Any?, OptionDelegate<Boolean>> =
            PropertyDelegateProvider { _, property ->
                val option = BinaryOption(property.name, BooleanValueParser)
                register(option)
                OptionDelegate(option)
            }

    protected inline fun <reified T : Enum<T>> option(): PropertyDelegateProvider<Any?, OptionDelegate<T>> =
            PropertyDelegateProvider { _, property ->
                val option = BinaryOption(property.name, EnumValueParser(enumValues<T>().toList()))
                register(option)
                OptionDelegate(option)
            }

    protected inline fun <reified T : Any, reified S : Any> PropertyDelegateProvider<Any?, OptionDelegate<T>>.replacedBy(
            replace: KProperty0<CompilerConfigurationKey<S>>,
            noinline convert: (T) -> S?
    ): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, Unit>> = PropertyDelegateProvider { thisRef, property ->
        val optionDelegate = this.provideDelegate(thisRef, property)
        val oldOption = optionDelegate.option
        val option = BinaryOption(property.name, ReplaceValueParser(oldOption, convert, replace.name), replace.get())
        register(option, canReplace = true)
        ReadOnlyProperty { _, _ -> }
    }
}

private object BooleanValueParser : BinaryOption.ValueParser<Boolean> {
    override fun parse(value: String): Boolean? = value.toBooleanStrictOrNull()

    override val validValuesHint: String?
        get() = "true|false"
}

@PublishedApi
internal class EnumValueParser<T : Enum<T>>(val values: List<T>) : BinaryOption.ValueParser<T> {
    // TODO: should we really ignore case here?
    override fun parse(value: String): T? = values.firstOrNull { it.name.equals(value, ignoreCase = true) }

    override val validValuesHint: String?
        get() = values.joinToString("|")
}

@PublishedApi
internal class ReplaceValueParser<T : Any, S: Any>(val oldOption: BinaryOption<T>, val convert: (T) -> S?, val replaceName:String) : BinaryOption.ValueParser<S> {
    override fun parse(value: String): S? = oldOption.valueParser.parse(value)?.let { convert(it) }

    override val warning: String? = "Option ${oldOption.name} is deprecated. Use $replaceName instead"

    override val validValuesHint: String? = "no possible values. $warning"
}
