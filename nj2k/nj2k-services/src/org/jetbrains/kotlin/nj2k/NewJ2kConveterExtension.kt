package org.jetbrains.kotlin.nj2k

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.j2k.*
import org.jetbrains.kotlin.nj2k.postProcessing.NewJ2kPostProcessor

class NewJ2kConverterExtension : J2kConverterExtension() {
    override val isNewJ2k = true

    override fun createJavaToKotlinConverter(
        project: Project,
        settings: ConverterSettings,
        services: JavaToKotlinConverterServices
    ): JavaToKotlinConverter =
        NewJavaToKotlinConverter(project, settings, services)

    override fun createPostProcessor(formatCode: Boolean): PostProcessor =
        NewJ2kPostProcessor()
}