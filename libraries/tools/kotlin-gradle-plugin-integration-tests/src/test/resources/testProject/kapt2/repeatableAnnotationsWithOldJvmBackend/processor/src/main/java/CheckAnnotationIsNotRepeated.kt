package processor

import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

class CheckAnnotationIsNotRepeated : AbstractProcessor() {
    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment): Boolean {
        val element = processingEnv.elementUtils.getTypeElement("example.TestClass")
        val containerAnnotation = element.annotationMirrors.singleOrNull {
            it.annotationType.asElement().simpleName.contentEquals("Container") &&
                    it.annotationType.asElement().enclosingElement.simpleName.contentEquals("Anno")
        }
        if (containerAnnotation != null) {
            processingEnv.messager.printMessage(
                Diagnostic.Kind.ERROR,
                "Repeatable container annotation class example.Anno.Container is found. " +
                        "The problem is likely in the fact that JVM IR is NOT DISABLED for kapt stub generation " +
                        "even though kapt.use.jvm.ir=false is specified in gradle.properties.",
                element,
            )
        }

        return true
    }

    override fun getSupportedSourceVersion(): SourceVersion =
        SourceVersion.RELEASE_6

    override fun getSupportedAnnotationTypes(): Set<String> =
        setOf("example.ToBeChecked")
}
