package apt

import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

class InfoAsWarningsProcessor : AbstractProcessor() {
    override fun process(
        annotations: Set<TypeElement>,
        roundEnv: RoundEnvironment,
    ): Boolean {
        processingEnv.messager.printMessage(
            Diagnostic.Kind.NOTE,
            "info-as-warnings test message",
        )
        return false
    }

    override fun getSupportedAnnotationTypes() = setOf("*")

    override fun getSupportedSourceVersion() = SourceVersion.RELEASE_8
}
