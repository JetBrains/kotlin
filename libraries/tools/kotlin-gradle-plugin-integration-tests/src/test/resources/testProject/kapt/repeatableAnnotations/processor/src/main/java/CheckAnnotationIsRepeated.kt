package processor

import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

class CheckAnnotationIsRepeated : AbstractProcessor() {
    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment): Boolean {
        val element = processingEnv.elementUtils.getTypeElement("example.TestClass")
        val containerAnnotation = element.annotationMirrors.singleOrNull {
            it.annotationType.asElement().simpleName.contentEquals("Container") &&
                    it.annotationType.asElement().enclosingElement.simpleName.contentEquals("Anno")
        }
        if (containerAnnotation == null) {
            processingEnv.messager.printMessage(
                Diagnostic.Kind.ERROR,
                "Repeatable container annotation class example.Anno.Container is not found. " +
                        "The problem is likely in the fact that JVM IR is NOT enabled for kapt stub generation.",
                element,
            )
            return true
        }

        val expected = "{value()={@example.Anno(\"1\"), @example.Anno(\"2\")}}"
        val actual = containerAnnotation.elementValues.toString()
        if (actual != expected) {
            processingEnv.messager.printMessage(
                Diagnostic.Kind.ERROR,
                "Repeatable annotation values are incorrect: $actual"
            )
        }

        return true
    }

    override fun getSupportedSourceVersion(): SourceVersion =
        SourceVersion.RELEASE_6

    override fun getSupportedAnnotationTypes(): Set<String> =
        setOf("example.ToBeChecked")
}
