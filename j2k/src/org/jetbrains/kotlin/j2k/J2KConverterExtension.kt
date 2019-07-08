package org.jetbrains.kotlin.j2k

import com.intellij.openapi.extensions.AbstractExtensionPointBean
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiJavaFile

abstract class J2kConverterExtension : AbstractExtensionPointBean() {
    abstract val isNewJ2k: Boolean

    abstract fun createJavaToKotlinConverter(
        project: Project,
        targetModule: Module?,
        settings: ConverterSettings,
        services: JavaToKotlinConverterServices
    ): JavaToKotlinConverter

    abstract fun createPostProcessor(formatCode: Boolean): PostProcessor

    open fun doCheckBeforeConversion(project: Project, module: Module): Boolean =
        true

    abstract fun createWithProgressProcessor(
        progress: ProgressIndicator?,
        files: List<PsiJavaFile>?,
        phasesCount: Int
    ): WithProgressProcessor

    companion object {
        private fun useNewJ2k() = Registry.`is`("kotlin.use.new.j2k", true)

        val EP_NAME = ExtensionPointName.create<J2kConverterExtension>("org.jetbrains.kotlin.j2kConverterExtension")

        val extension: J2kConverterExtension
            get() = EP_NAME.extensions.first { it.isNewJ2k == useNewJ2k() }
    }
}

