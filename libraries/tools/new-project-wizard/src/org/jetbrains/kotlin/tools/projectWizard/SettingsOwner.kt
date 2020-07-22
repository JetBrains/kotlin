package org.jetbrains.kotlin.tools.projectWizard

import org.jetbrains.kotlin.tools.projectWizard.core.Parser
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.*
import org.jetbrains.kotlin.tools.projectWizard.core.enumParser
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem
import kotlin.properties.ReadOnlyProperty


interface SettingsOwner {
    fun <V : Any, T : SettingType<V>> settingDelegate(
        prefix: String,
        create: (path: String) -> SettingBuilder<V, T>
    ): ReadOnlyProperty<Any, Setting<V, T>>

    // the functions to create different kinds of settings
    // for now should be overridden with specific settings types :(
    // TODO think a way yo emulate higher-kind types

    fun <V : DisplayableSettingItem> dropDownSetting(
        title: String,
        neededAtPhase: GenerationPhase,
        parser: Parser<V>,
        prefix: String = "",
        init: DropDownSettingType.Builder<V>.() -> Unit = {}
    ): ReadOnlyProperty<Any, Setting<V, DropDownSettingType<V>>> = settingDelegate(prefix) { path ->
        DropDownSettingType.Builder(path, title, neededAtPhase, parser).apply(init)
    }

    fun stringSetting(
        title: String,
        neededAtPhase: GenerationPhase,
        prefix: String = "",
        init: StringSettingType.Builder.() -> Unit = {}
    ) = settingDelegate(prefix) { path ->
        StringSettingType.Builder(path, title, neededAtPhase).apply(init)
    }

    fun booleanSetting(
        title: String,
        neededAtPhase: GenerationPhase,
        prefix: String = "",
        init: BooleanSettingType.Builder.() -> Unit = {}
    ) = settingDelegate(prefix) { path ->
        BooleanSettingType.Builder(path, title, neededAtPhase).apply(init)
    }

    fun <V : Any> valueSetting(
        title: String,
        neededAtPhase: GenerationPhase,
        parser: Parser<V>,
        prefix: String = "",
        init: ValueSettingType.Builder<V>.() -> Unit = {}
    ) = settingDelegate(prefix) { path ->
        ValueSettingType.Builder(path, title, neededAtPhase, parser).apply(init)
    }

    fun versionSetting(
        title: String,
        neededAtPhase: GenerationPhase,
        prefix: String = "",
        init: VersionSettingType.Builder.() -> Unit = {}
    ) = settingDelegate(prefix) { path ->
        VersionSettingType.Builder(path, title, neededAtPhase).apply(init)
    }

    fun <V : Any> listSetting(
        title: String,
        neededAtPhase: GenerationPhase,
        parser: Parser<V>,
        prefix: String = "",
        init: ListSettingType.Builder<V>.() -> Unit = {}
    ) = settingDelegate(prefix) { path ->
        ListSettingType.Builder(path, title, neededAtPhase, parser).apply(init)
    }

    fun pathSetting(
        title: String,
        neededAtPhase: GenerationPhase,
        prefix: String = "",
        init: PathSettingType.Builder.() -> Unit = {}
    ) = settingDelegate(prefix) { path ->
        PathSettingType.Builder(path, title, neededAtPhase).apply(init)
    }
}


inline fun <reified E> SettingsOwner.enumSettingImpl(
    title: String,
    neededAtPhase: GenerationPhase,
    prefix: String = "",
    crossinline init: DropDownSettingType.Builder<E>.() -> Unit = {}
) where E : Enum<E>, E : DisplayableSettingItem = dropDownSetting<E>(title, neededAtPhase, enumParser(), prefix) {
    values = enumValues<E>().asList()
    //
    init()
}