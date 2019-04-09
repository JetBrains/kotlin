package org.jetbrains.kotlin.r4a

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils.NO_EXPECTED_TYPE
import org.jetbrains.kotlin.types.TypeUtils.UNIT_EXPECTED_TYPE
import org.jetbrains.kotlin.types.typeUtil.replaceAnnotations

object R4aFqNames {
    val Composable = R4aUtils.r4aFqName("Composable")
    val Pivotal = R4aUtils.r4aFqName("Pivotal")
    val Children = R4aUtils.r4aFqName("Children")
    val Stateful = R4aUtils.r4aFqName("Stateful")
    val Emittable = R4aUtils.r4aFqName("Emittable")
    val HiddenAttribute = R4aUtils.r4aFqName("HiddenAttribute")

    fun makeComposableAnnotation(module: ModuleDescriptor): AnnotationDescriptor =
        object : AnnotationDescriptor {
            override val type: KotlinType
                get() = module.findClassAcrossModuleDependencies(
                    ClassId.topLevel(Composable)
                )!!.defaultType
            override val allValueArguments: Map<Name, ConstantValue<*>> get() = emptyMap()
            override val source: SourceElement get() = SourceElement.NO_SOURCE
            override fun toString() = "[@Composable]"
        }
}

fun KotlinType.makeComposable(module: ModuleDescriptor): KotlinType {
    if (hasComposableAnnotation()) return this
    val annotation = R4aFqNames.makeComposableAnnotation(module)
    return replaceAnnotations(Annotations.create(annotations + annotation))
}

fun KotlinType.hasComposableAnnotation(): Boolean =
    !isSpecialType && annotations.findAnnotation(R4aFqNames.Composable) != null
fun Annotated.hasComposableAnnotation(): Boolean =
    annotations.findAnnotation(R4aFqNames.Composable) != null
fun Annotated.hasPivotalAnnotation(): Boolean =
    annotations.findAnnotation(R4aFqNames.Pivotal) != null
fun Annotated.hasChildrenAnnotation(): Boolean =
    annotations.findAnnotation(R4aFqNames.Children) != null
fun Annotated.hasStatefulAnnotation(): Boolean =
    annotations.findAnnotation(R4aFqNames.Stateful) != null
fun Annotated.hasEmittableAnnotation(): Boolean =
    annotations.findAnnotation(R4aFqNames.Emittable) != null
fun Annotated.hasHiddenAttributeAnnotation(): Boolean =
    annotations.findAnnotation(R4aFqNames.HiddenAttribute) != null

fun Annotated.isComposableFromChildrenAnnotation(): Boolean {
    val childrenAnnotation = annotations.findAnnotation(R4aFqNames.Children) ?: return false
    return childrenAnnotation.isComposableChildrenAnnotation
}

private val KotlinType.isSpecialType: Boolean get() =
    this === NO_EXPECTED_TYPE || this === UNIT_EXPECTED_TYPE

val AnnotationDescriptor.isComposableAnnotation: Boolean get() = fqName == R4aFqNames.Composable
val AnnotationDescriptor.isChildrenAnnotation: Boolean get() = fqName == R4aFqNames.Children
val AnnotationDescriptor.isComposableChildrenAnnotation: Boolean
    get() {
        if (fqName != R4aFqNames.Children) return false
        val composableValueArgument = argumentValue("composable")?.value
        return composableValueArgument == null || composableValueArgument == true
    }