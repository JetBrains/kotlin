package repro.processor

import java.io.IOException
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeKind
import javax.lang.model.util.ElementFilter
import javax.tools.Diagnostic
import javax.tools.StandardLocation

@Target(AnnotationTarget.CLASS)
annotation class Trigger

@SupportedAnnotationTypes("repro.processor.Trigger")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
class InspectTypesProcessor : AbstractProcessor() {
    private var wroteMarker = false

    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment): Boolean {
        if (roundEnv.processingOver() || wroteMarker) return false

        val typeElement = roundEnv.getElementsAnnotatedWith(Trigger::class.java).singleOrNull() as? TypeElement ?: return false
        val testMethod = ElementFilter.methodsIn(typeElement.enclosedElements).single { it.simpleName.contentEquals("test") }
        val parameter = testMethod.parameters.single()
        val parameterType = parameter.asType()

        if (parameterType.kind == TypeKind.ERROR) {
            processingEnv.messager.printMessage(
                Diagnostic.Kind.ERROR,
                "Detected error type from compiler plugin: $parameterType",
                parameter,
            )
            return false
        }

        try {
            processingEnv.filer.createResource(StandardLocation.CLASS_OUTPUT, "", "type.txt").openWriter().use {
                it.write(parameterType.toString())
            }
            wroteMarker = true
        } catch (_: IOException) {
            // Another round may try to recreate the marker; ignore it.
        }

        return false
    }
}
