package example

import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

public class ExampleAnnotationProcessor : AbstractProcessor() {

    private companion object {
        val ANNOTATION_FQ_NAME = javaClass<ExampleAnnotation>().getCanonicalName()
    }

    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment): Boolean {
        val elements = roundEnv.getElementsAnnotatedWith(javaClass<ExampleAnnotation>())

        val elementUtils = processingEnv.getElementUtils()
        val filer = processingEnv.getFiler()

        for (element in elements) {
            val packageName = elementUtils.getPackageOf(element).getQualifiedName().toString()
            val className = element.getSimpleName().toString().capitalize() + "Generated"

            filer.createSourceFile(className).openWriter().use { with(it) {
                appendln("package $packageName;")
                appendln()
                appendln("public final class $className {}")
            }}
        }

        return true;
    }

    override fun getSupportedSourceVersion() = SourceVersion.RELEASE_6

    override fun getSupportedAnnotationTypes() = setOf(ANNOTATION_FQ_NAME)
}