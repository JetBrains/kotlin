package org.jetbrains.kotlin.tools.projectWizard.core

import org.jetbrains.kotlin.tools.projectWizard.core.entity.*
import org.jetbrains.kotlin.tools.projectWizard.core.service.WizardService
import org.jetbrains.kotlin.tools.projectWizard.core.service.ServicesManager
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

@Suppress("UNCHECKED_CAST")
open class ValuesReadingContext(val context: Context, private val servicesManager: ServicesManager) {
    inline fun <reified S : WizardService> service(noinline filter: (S) -> Boolean = { true }) = serviceByClass(S::class, filter)

    fun <S : WizardService> serviceByClass(klass: KClass<S>, filter: (S) -> Boolean = { true }) =
        servicesManager.serviceByClass(klass, filter)

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