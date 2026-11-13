package apt

import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.tools.StandardLocation

annotation class Inspect

/** Reports the modifiers of every @Inspect class and of its superclass, which is a stub or a class file. */
class InspectProcessor : AbstractProcessor() {
    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        val elements = roundEnv.getElementsAnnotatedWith(Inspect::class.java)
        if (elements.isEmpty()) return true

        val report = elements.sortedBy { it.simpleName.toString() }.joinToString("\n") { element ->
            val superclass = ((element as TypeElement).superclass as? DeclaredType)?.asElement()
                ?.takeIf { it.simpleName.toString() != "Object" }
            element.describe() + (superclass?.let { ", superclass " + it.describe() } ?: "")
        }
        processingEnv.filer.createResource(StandardLocation.SOURCE_OUTPUT, "", "report.txt").openWriter().use { it.write(report) }
        return true
    }

    private fun Element.describe(): String = "$simpleName " + modifiers.map { it.toString() }.sorted()

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latestSupported()
    override fun getSupportedAnnotationTypes() = setOf("apt.Inspect")
}
