package org.jetbrains.kotlin.tools.projectWizard.core

import org.jetbrains.kotlin.tools.projectWizard.core.entity.*
import org.jetbrains.kotlin.tools.projectWizard.core.service.WizardService
import org.jetbrains.kotlin.tools.projectWizard.core.service.ServicesManager
import org.jetbrains.kotlin.tools.projectWizard.core.service.SettingSavingWizardService
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

open class ReadingContext(
    val context: Context,
    private val servicesManager: ServicesManager,
    val isUnitTestMode: Boolean
) {
    inline fun <reified S : WizardService> service(noinline filter: (S) -> Boolean = { true }): S =
        serviceByClass(S::class, filter)

    fun <S : WizardService> serviceByClass(klass: KClass<S>, filter: (S) -> Boolean = { true }): S =
        servicesManager.serviceByClass(klass, filter) ?: error("Service ${klass.simpleName} was not found")

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

    val <V : Any, T : SettingType<V>> PluginSettingReference<V, T>.pluginSetting: Setting<V, T>
        get() = context.settingContext.getPluginSetting(this)

    fun <V : Any> Setting<V, SettingType<V>>.getSavedValueForSetting(): V? {
        if (!isSavable || this !is PluginSetting<*, *>) return null
        val serializer = type.serializer.safeAs<SerializerImpl<V>>() ?: return null
        val savedValue = service<SettingSavingWizardService>().getSettingValue(path) ?: return null
        return serializer.fromString(savedValue)
    }

    val <V : Any> Setting<V, SettingType<V>>.savedOrDefaultValue: V?
        get() = getSavedValueForSetting() ?: defaultValue

}