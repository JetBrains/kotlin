package apt

import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic.Kind.*
import javax.tools.StandardLocation

annotation class Anno

class SampleApt : AbstractProcessor() {
    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        val writeKotlinFiles = processingEnv.options["kapt.test.writeKotlinFiles"] == "true"

        for (element in roundEnv.getElementsAnnotatedWith(Anno::class.java)) {
            val generatedSimpleName = element.simpleName.toString().capitalize()

            val file = when (writeKotlinFiles) {
                true -> processingEnv.filer.createResource(StandardLocation.SOURCE_OUTPUT, "generated", "$generatedSimpleName.kt")
                false -> processingEnv.filer.createSourceFile("generated.$generatedSimpleName")
            }

            file.openWriter().use {
                it.write("package generated;\npublic class $generatedSimpleName {}")
            }
        }

        return true
    }

    override fun getSupportedOptions() = setOf("kapt.test.writeKotlinFiles")
    override fun getSupportedSourceVersion() = SourceVersion.RELEASE_8
    override fun getSupportedAnnotationTypes() = setOf("apt.Anno")
}