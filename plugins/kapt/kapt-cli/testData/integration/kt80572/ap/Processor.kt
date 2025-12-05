package ap

import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

class Processor : AbstractProcessor() {
    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        return true
    }

    override fun getSupportedAnnotationTypes() = setOf(
        "Does",
        "Not",
        "Matter",
    )

    override fun getSupportedSourceVersion() = SourceVersion.RELEASE_8
}
