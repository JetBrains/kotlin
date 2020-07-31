package org.jetbrains.kotlin.nj2k

import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.j2k.IdeaJavaToKotlinServices
import org.jetbrains.kotlin.j2k.ConverterSettings
import org.jetbrains.kotlin.nj2k.postProcessing.NewJ2kPostProcessor

abstract class AbstractPartialConverterTest : AbstractNewJavaToKotlinConverterSingleFileTest() {
    override fun fileToKotlin(text: String, settings: ConverterSettings, project: Project): String {
        val file = createJavaFile(text)
        val element = myFixture.elementAtCaret
        return NewJavaToKotlinConverter(project, module, settings, IdeaJavaToKotlinServices).filesToKotlin(
            listOf(file),
            NewJ2kPostProcessor(),
            EmptyProgressIndicator()
        ) { it == element }.results.single()
    }
}
