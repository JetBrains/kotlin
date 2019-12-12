package org.jetbrains.kotlin.tools.projectWizard.core

import org.jetbrains.kotlin.tools.projectWizard.core.entity.*
import org.jetbrains.kotlin.tools.projectWizard.core.service.Service
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmName

@Suppress("UNCHECKED_CAST")
open class ValuesReadingContext(val context: Context, private val services: List<Service>) {
    inline fun <reified S : Service> service() = serviceByClass(S::class)

    @Suppress("UNCHECKED_CAST")
    fun <S : Service> serviceByClass(klass: KClass<S>) =
        services.firstOrNull { it::class.isSubclassOf(klass) } as? S ?: error("No service ${klass.jvmName}")

    inline val <reified T : Any> PropertyReference<T>.propertyValue: T
        get() = context.propertyContext[this] as T

    inline val <reified V : Any, T : SettingType<V>> SettingReference<V, T>.settingValue: V
        get() = context.settingContext[this] ?: error("No value is present for setting `$this`")

    inline val <reified V : Any> KProperty1<out Plugin, PluginSetting<V, SettingType<V>>>.settingValue: V
        get() = reference.settingValue

    inline fun <reified V : Any> KProperty1<out Plugin, PluginSetting<V, SettingType<V>>>.settingValue(): V =
        this.reference.settingValue

    inline fun <reified V : Any, T : SettingType<V>> SettingReference<V, T>.settingValue(): V =
        context.settingContext[this] ?: error("No value is present for setting `$this`")

    val <V : Any, T : SettingType<V>> SettingReference<V, T>.notRequiredSettingValue: V?
        get() = context.settingContext[this]

    fun <V : Any, T : SettingType<V>> SettingReference<V, T>.notRequiredSettingValue(): V? =
        context.settingContext[this]
}