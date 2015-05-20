package org.jetbrains.kotlin.annotation

import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement

private class RoundEnvironmentWrapper(
        val processingEnv: ProcessingEnvironment,
        val parent: RoundEnvironment,
        val roundNumber: Int,
        val annotatedElementDescriptors: Map<String, Set<AnnotatedElementDescriptor>>
) : RoundEnvironment {

    override fun getRootElements(): MutableSet<out Element>? {
        return parent.getRootElements()
    }

    override fun getElementsAnnotatedWith(a: TypeElement): MutableSet<out Element>? {
        val elements = parent.getElementsAnnotatedWith(a).toHashSet()
        elements.addAll(resolveKotlinElements(a.getQualifiedName().toString()))
        return elements
    }

    override fun getElementsAnnotatedWith(a: Class<out Annotation>): MutableSet<out Element>? {
        val elements = parent.getElementsAnnotatedWith(a).toHashSet()
        elements.addAll(resolveKotlinElements(a.getName()))
        return elements
    }

    override fun processingOver() = parent.processingOver()

    override fun errorRaised() = parent.errorRaised()

    private fun TypeElement.filterEnclosedElements(kind: ElementKind, name: String): List<Element> {
        return getEnclosedElements().filter { it.getKind() == kind && it.getSimpleName().toString() == name }
    }

    private fun TypeElement.filterEnclosedElements(kind: ElementKind): List<Element> {
        return getEnclosedElements().filter { it.getKind() == kind }
    }

    private fun Element.hasAnnotation(annotationFqName: String): Boolean {
        return getAnnotationMirrors().any { annotationFqName == it.getAnnotationType().asElement().toString() }
    }

    private fun resolveKotlinElements(annotationFqName: String): Set<Element> {
        if (roundNumber > 1) return setOf()

        val descriptors = annotatedElementDescriptors.get(annotationFqName) ?: setOf()
        return descriptors.fold(hashSetOf<Element>()) { set, descriptor ->
            val clazz = processingEnv.getElementUtils().getTypeElement(descriptor.classFqName) ?: return@fold set
            when (descriptor) {
                is AnnotatedClassDescriptor -> set.add(clazz)
                is AnnotatedConstructorDescriptor -> {
                    set.addAll(clazz.filterEnclosedElements(ElementKind.CONSTRUCTOR)
                            .filter { it.hasAnnotation(annotationFqName) })
                }
                is AnnotatedFieldDescriptor -> {
                    set.addAll(clazz.filterEnclosedElements(ElementKind.FIELD, descriptor.fieldName)
                            .filter { it.hasAnnotation(annotationFqName) })
                }
                is AnnotatedMethodDescriptor -> {
                    set.addAll(clazz.filterEnclosedElements(ElementKind.METHOD, descriptor.methodName)
                            .filter { it.hasAnnotation(annotationFqName) })
                }
            }
            set
        }
    }
}
