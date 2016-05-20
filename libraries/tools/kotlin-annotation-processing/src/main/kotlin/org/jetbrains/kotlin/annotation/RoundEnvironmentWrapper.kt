package org.jetbrains.kotlin.annotation

import java.lang.annotation.Inherited
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.lang.model.type.NoType

internal class RoundEnvironmentWrapper(
        val processingEnv: ProcessingEnvironment,
        val parent: RoundEnvironment,
        val roundNumber: Int,
        val kotlinAnnotationsProvider: KotlinAnnotationProvider
) : RoundEnvironment {

    override fun getRootElements(): MutableSet<out Element>? {
        return parent.rootElements
    }

    override fun getElementsAnnotatedWith(a: TypeElement): MutableSet<out Element>? {
        val elements = parent.getElementsAnnotatedWith(a).toHashSet()
        elements.addAll(resolveKotlinElements(a.qualifiedName.toString()))
        return elements
    }

    override fun getElementsAnnotatedWith(a: Class<out Annotation>): MutableSet<out Element>? {
        val elements = parent.getElementsAnnotatedWith(a).toHashSet()
        elements.addAll(resolveKotlinElements(a.name))
        return elements
    }

    override fun processingOver() = parent.processingOver()

    override fun errorRaised() = parent.errorRaised()

    private fun TypeElement.filterEnclosedElements(kind: ElementKind, name: String): List<Element> {
        return enclosedElements.filter { it.kind == kind && it.simpleName.toString() == name }
    }

    private fun TypeElement.filterEnclosedElements(kind: ElementKind): List<Element> {
        return enclosedElements.filter { it.kind == kind }
    }

    private fun Element.hasAnnotation(annotationFqName: String): Boolean {
        return annotationMirrors.any { annotationFqName == it.annotationType.asElement().toString() }
    }

    private fun TypeElement.hasInheritedAnnotation(annotationFqName: String): Boolean {
        if (hasAnnotation(annotationFqName)) return true

        val superclassMirror = superclass
        if (superclassMirror is NoType) return false

        val superClass = processingEnv.typeUtils.asElement(superclassMirror)
        if (superClass !is TypeElement) return false

        return superClass.hasInheritedAnnotation(annotationFqName)
    }

    private fun resolveKotlinElements(annotationFqName: String): Set<Element> {
        if (roundNumber > 1) return setOf()

        val descriptors = kotlinAnnotationsProvider.annotatedKotlinElements.get(annotationFqName) ?: setOf()
        val descriptorsWithKotlin = descriptors.fold(hashSetOf<Element>()) { set, descriptor ->
            val clazz = processingEnv.elementUtils.getTypeElement(descriptor.classFqName) ?: return@fold set
            when (descriptor) {
                is AnnotatedElement.Class -> set.add(clazz)
                is AnnotatedElement.Constructor -> {
                    set.addAll(clazz.filterEnclosedElements(ElementKind.CONSTRUCTOR)
                            .filter { it.hasAnnotation(annotationFqName) })
                }
                is AnnotatedElement.Field -> {
                    set.addAll(clazz.filterEnclosedElements(ElementKind.FIELD, descriptor.fieldName)
                            .filter { it.hasAnnotation(annotationFqName) })
                }
                is AnnotatedElement.Method -> {
                    set.addAll(clazz.filterEnclosedElements(ElementKind.METHOD, descriptor.methodName)
                            .filter { it.hasAnnotation(annotationFqName) })
                }
            }
            set
        }

        if (kotlinAnnotationsProvider.supportInheritedAnnotations) {
            val isInherited = processingEnv.elementUtils.getTypeElement(annotationFqName)
                    ?.hasAnnotation(Inherited::class.java.canonicalName) ?: false

            if (isInherited) {
                kotlinAnnotationsProvider.kotlinClasses.forEach { classFqName ->
                    val clazz = processingEnv.elementUtils.getTypeElement(classFqName) ?: return@forEach
                    if (clazz.hasInheritedAnnotation(annotationFqName)) {
                        descriptorsWithKotlin.add(clazz)
                    }
                }
            }
        }

        return descriptorsWithKotlin
    }
}
