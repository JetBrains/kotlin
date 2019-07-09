package org.jetbrains.kotlin.nj2k

import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.idea.configuration.getAbleToRunConfigurators
import org.jetbrains.kotlin.idea.configuration.hasAnyKotlinRuntimeInScope
import org.jetbrains.kotlin.idea.configuration.isModuleConfigured
import org.jetbrains.kotlin.idea.configuration.toModuleGroup
import org.jetbrains.kotlin.j2k.*
import org.jetbrains.kotlin.nj2k.postProcessing.NewJ2kPostProcessor
import org.jetbrains.kotlin.platform.jvm.isJvm

class NewJ2kConverterExtension : J2kConverterExtension() {
    override val isNewJ2k = true

    override fun createJavaToKotlinConverter(
        project: Project,
        targetModule: Module?,
        settings: ConverterSettings,
        services: JavaToKotlinConverterServices
    ): JavaToKotlinConverter =
        NewJavaToKotlinConverter(project, targetModule, settings, services)

    override fun createPostProcessor(formatCode: Boolean): PostProcessor =
        NewJ2kPostProcessor()

    override fun doCheckBeforeConversion(project: Project, module: Module): Boolean =
        checkKotlinIsConfigured(project, module)

    override fun createWithProgressProcessor(
        progress: ProgressIndicator?,
        files: List<PsiJavaFile>?,
        phasesCount: Int
    ): WithProgressProcessor =
        NewJ2kWithProgressProcessor(progress, files, phasesCount)

    private fun checkKotlinIsConfigured(project: Project, module: Module): Boolean {
        val kotlinIsConfigured =
            hasAnyKotlinRuntimeInScope(module) || isModuleConfigured(module.toModuleGroup())
        if (kotlinIsConfigured) return true

        val title = "Kotlin is not configured in the project"
        if (Messages.showOkCancelDialog(
                project,
                "You will have to configure Kotlin in project before performing a conversion.",
                title,
                "OK, configure Kotlin in the project",
                "No, cancel conversion",
                Messages.getWarningIcon()
            ) == Messages.OK
        ) {
            val configurators = getAbleToRunConfigurators(module).filter { it.targetPlatform.isJvm() }
            when {
                configurators.isEmpty() -> Messages.showErrorDialog("There aren't configurators available", title)
                configurators.size == 1 -> configurators.single().configure(project, emptyList())
                else -> {
                    val resultIndex = Messages.showChooseDialog(//TODO a better dialog?
                        project,
                        "Choose Configurator",
                        title,
                        null,
                        configurators.map { it.presentableText }.toTypedArray(),
                        configurators.first().presentableText
                    )
                    configurators.getOrNull(resultIndex)?.configure(project, emptyList())
                }
            }
        }

        return false
    }
}