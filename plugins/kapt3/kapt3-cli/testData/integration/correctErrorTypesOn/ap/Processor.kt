package apt

import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.*
import javax.lang.model.type.DeclaredType
import javax.tools.Diagnostic.Kind.*

annotation class Anno

class SampleApt : AbstractProcessor() {
    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        for (element in roundEnv.getElementsAnnotatedWith(Anno::class.java).filterIsInstance<VariableElement>()) {
            val type = element.asType() as? DeclaredType ?: continue
            if (type.toString() == "error.NonExistentClass") {
                processingEnv.messager.printMessage(ERROR, "NonExistentClass type occurred")
            }

            val generatedSimpleName = element.simpleName.toString().capitalize()
            processingEnv.filer.createSourceFile("generated.$generatedSimpleName").openWriter().use {
                it.write("package generated;\npublic class $generatedSimpleName {}")
            }
        }

        return true
    }

    override fun getSupportedOptions() = emptySet<String>()
    override fun getSupportedSourceVersion() = SourceVersion.RELEASE_8
    override fun getSupportedAnnotationTypes() = setOf("apt.Anno")
}