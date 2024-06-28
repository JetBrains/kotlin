package apt

import java.io.File
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic.Kind.*

annotation class Anno

class SampleApt : AbstractProcessor() {
    private companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        val kaptKotlinGeneratedDir = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME] ?: run {
            processingEnv.messager.printMessage(ERROR, "Can't find the target directory for generated Kotlin files.")
            return false
        }

        val baseDir = File(kaptKotlinGeneratedDir, "generated")
        baseDir.mkdirs()

        for (element in roundEnv.getElementsAnnotatedWith(Anno::class.java)) {
            val generatedSimpleName = element.simpleName.toString().capitalize()
            val file = File(baseDir, "$generatedSimpleName.kt")
            file.writeText("package generated\n@apt.Anno\nclass $generatedSimpleName")
        }

        return true
    }

    override fun getSupportedOptions() = emptySet<String>()
    override fun getSupportedSourceVersion() = SourceVersion.RELEASE_8
    override fun getSupportedAnnotationTypes() = setOf("apt.Anno")
}