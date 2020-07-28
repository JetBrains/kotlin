/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.framework

import com.intellij.ide.util.projectWizard.JavaModuleBuilder
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.SettingsStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.projectRoots.SdkTypeId
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ui.configuration.ModulesProvider
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.idea.projectWizard.WizardStatsService.*
import org.jetbrains.kotlin.idea.projectWizard.WizardStatsService.Companion.logDataOnProjectGenerated
import org.jetbrains.kotlin.idea.roots.migrateNonJvmSourceFolders
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.isJvm
import javax.swing.Icon

class KotlinModuleBuilder(
    val targetPlatform: TargetPlatform,
    @NonNls private val builderName: String,
    @Nls private val builderPresentableName: String,
    @Nls private val builderDescription: String,
    val icon: Icon
) : JavaModuleBuilder() {
    private var wizardContext: WizardContext? = null

    override fun getBuilderId() = "kotlin.module.builder"
    override fun getName() = builderName
    override fun getPresentableName() = builderPresentableName
    override fun getDescription() = builderDescription
    override fun getNodeIcon() = icon
    override fun getGroupName() = KotlinTemplatesFactory.KOTLIN_GROUP_NAME

    override fun createWizardSteps(wizardContext: WizardContext, modulesProvider: ModulesProvider): Array<out ModuleWizardStep>? {
        this.wizardContext = wizardContext
        return ModuleWizardStep.EMPTY_ARRAY
    }

    override fun modifySettingsStep(settingsStep: SettingsStep): ModuleWizardStep {
        return KotlinModuleSettingStep(targetPlatform, this, settingsStep, wizardContext)
    }

    override fun isSuitableSdkType(sdkType: SdkTypeId?) = when {
        targetPlatform.isJvm() -> super.isSuitableSdkType(sdkType)
        else -> sdkType is KotlinSdkType
    }

    override fun setupRootModel(rootModel: ModifiableRootModel) {
        val projectCreationStats = ProjectCreationStats("Kotlin", this.builderName, "jps")
        logDataOnProjectGenerated(projectCreationStats)
        super.setupRootModel(rootModel)
        if (!targetPlatform.isJvm()) {
            migrateNonJvmSourceFolders(rootModel)
        }
    }
}
