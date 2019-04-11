package org.jetbrains.kotlin.nj2k

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.j2k.*

class NewJ2kConverterExtension : J2kConverterExtension() {
    override val isNewJ2k = true

    override fun createJavaToKotlinConverter(
        project: Project,
        settings: ConverterSettings,
        services: JavaToKotlinConverterServices
    ): JavaToKotlinConverter =
        NewJavaToKotlinConverter(project, settings, services)

    override fun createPostProcessor(formatCode: Boolean, settings: ConverterSettings): PostProcessor =
        NewJ2kPostProcessor(formatCode, settings)
}