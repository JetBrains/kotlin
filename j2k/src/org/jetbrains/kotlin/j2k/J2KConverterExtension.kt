package org.jetbrains.kotlin.j2k

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.extensions.AbstractExtensionPointBean
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
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
        private const val newJ2kByDefault = true
        private const val optionName = "kotlin.use.new.j2k"

        var isNewJ2k
            get() = PropertiesComponent.getInstance().getBoolean(optionName, newJ2kByDefault)
            set(value) {
                PropertiesComponent.getInstance().setValue(optionName, value, newJ2kByDefault)
            }

        val EP_NAME = ExtensionPointName.create<J2kConverterExtension>("org.jetbrains.kotlin.j2kConverterExtension")

        fun extension(useNewJ2k: Boolean = isNewJ2k): J2kConverterExtension =
            EP_NAME.extensions.first { it.isNewJ2k == useNewJ2k }
    }
}

