package org.jetbrains.kotlin.tools.projectWizard

import org.jetbrains.kotlin.tools.projectWizard.core.EntitiesOwnerDescriptor
import org.jetbrains.kotlin.tools.projectWizard.core.Parser
import org.jetbrains.kotlin.tools.projectWizard.core.entity.*
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem
import kotlin.properties.ReadOnlyProperty


interface SettingsOwner : EntitiesOwnerDescriptor {
    fun <V : Any, T : SettingType<V>> settingDelegate(
        create: (path: String) -> SettingBuilder<V, T>
    ): ReadOnlyProperty<Any, Setting<V, T>>

    // the functions to create different kinds of settings
    // for now should be overridden with specific settings types :(
    // TODO think a way yo emulate higher-kind types

    fun <V : DisplayableSettingItem> dropDownSetting(
        title: String,
        neededAtPhase: GenerationPhase,
        parser: Parser<V>,
        init: DropDownSettingType.Builder<V>.() -> Unit = {}
    ): ReadOnlyProperty<Any, Setting<V, DropDownSettingType<V>>> = settingDelegate { path ->
        DropDownSettingType.Builder(path, title, neededAtPhase, parser).apply(init)
    }

    fun stringSetting(
        title: String,
        neededAtPhase: GenerationPhase,
        init: StringSettingType.Builder.() -> Unit = {}
    ) = settingDelegate { path ->
        StringSettingType.Builder(path, title, neededAtPhase).apply(init)
    }

    fun booleanSetting(
        title: String,
        neededAtPhase: GenerationPhase,
        init: BooleanSettingType.Builder.() -> Unit = {}
    ) = settingDelegate { path ->
        BooleanSettingType.Builder(path, title, neededAtPhase).apply(init)
    }

    fun <V : Any> valueSetting(
        title: String,
        neededAtPhase: GenerationPhase,
        parser: Parser<V>,
        init: ValueSettingType.Builder<V>.() -> Unit = {}
    ) = settingDelegate { path ->
        ValueSettingType.Builder(path, title, neededAtPhase, parser).apply(init)
    }

    fun versionSetting(
        title: String,
        neededAtPhase: GenerationPhase,
        init: VersionSettingType.Builder.() -> Unit = {}
    ) = settingDelegate { path ->
        VersionSettingType.Builder(path, title, neededAtPhase).apply(init)
    }

    fun <V : Any> listSetting(
        title: String,
        neededAtPhase: GenerationPhase,
        parser: Parser<V>,
        init: ListSettingType.Builder<V>.() -> Unit = {}
    ) = settingDelegate { path ->
        ListSettingType.Builder(path, title, neededAtPhase, parser).apply(init)
    }

    fun pathSetting(
        title: String,
        neededAtPhase: GenerationPhase,
        init: PathSettingType.Builder.() -> Unit = {}
    ) = settingDelegate { path ->
        PathSettingType.Builder(path, title, neededAtPhase).apply(init)
    }
}

