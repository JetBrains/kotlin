package org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators

import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleType
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem

interface TargetConfiguratorGroup

interface DisplayableTargetConfiguratorGroup : TargetConfiguratorGroup, DisplayableSettingItem {
    val moduleType: ModuleType
}

interface TargetConfiguratorGroupWithSubItems: TargetConfiguratorGroup {
    val subItems: List<DisplayableSettingItem>
}

data class StepTargetConfiguratorGroup(
    override val text: String,
    override val moduleType: ModuleType,
    override val subItems: List<DisplayableSettingItem>
): DisplayableTargetConfiguratorGroup, TargetConfiguratorGroupWithSubItems

data class FinalTargetConfiguratorGroup(
    override val text: String,
    override val moduleType: ModuleType,
    override val subItems: List<DisplayableSettingItem>
): DisplayableTargetConfiguratorGroup, TargetConfiguratorGroupWithSubItems

data class FirstStepTargetConfiguratorGroup(
    override val subItems: List<DisplayableSettingItem>
): TargetConfiguratorGroup, TargetConfiguratorGroupWithSubItems