/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.resolve

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.ValueArgument
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyAnnotationDescriptor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.KotlinTypeFactory

internal fun ClassConstructorDescriptor.isSerializationCtor(): Boolean {
    /*kind == CallableMemberDescriptor.Kind.SYNTHESIZED does not work because DeserializedClassConstructorDescriptor loses its kind*/
    return valueParameters.lastOrNull()?.run {
        name == SerialEntityNames.dummyParamName && type.constructor.declarationDescriptor?.classId == ClassId(
            SerializationPackages.packageFqName,
            SerialEntityNames.SERIAL_CTOR_MARKER_NAME
        )
    } == true
}

// finds constructor (KSerializer<T0>, KSerializer<T1>...) on a KSerializer<T<T0, T1...>>
internal fun findSerializerConstructorForTypeArgumentsSerializers(
    serializerDescriptor: ClassDescriptor,
    onlyIfSynthetic: Boolean = false
): ClassConstructorDescriptor? {
    val serializableImplementationTypeArguments = extractKSerializerArgumentFromImplementation(serializerDescriptor)?.arguments
        ?: throw AssertionError("Serializer does not implement KSerializer??")

    val typeParamsCount = serializableImplementationTypeArguments.size
    if (typeParamsCount == 0) return null //don't need it
    val ctor = serializerDescriptor.constructors.find { ctor ->
        ctor.valueParameters.size == typeParamsCount && ctor.valueParameters.all { isKSerializer(it.type) }
    }
    return if (!onlyIfSynthetic) ctor else ctor?.takeIf { it.kind == CallableMemberDescriptor.Kind.SYNTHESIZED }
}

inline fun <reified R> Annotations.findAnnotationConstantValue(annotationFqName: FqName, property: String): R? =
    findAnnotation(annotationFqName)?.let { annotation ->
        annotation.allValueArguments.entries.singleOrNull { it.key.asString() == property }?.value?.value
    } as? R

internal fun Annotations.findAnnotationKotlinTypeValue(
    annotationFqName: FqName,
    moduleForResolve: ModuleDescriptor,
    property: String
): KotlinType? =
    findAnnotation(annotationFqName)?.let { annotation ->
        val maybeKClass = annotation.allValueArguments.entries.singleOrNull { it.key.asString() == property }?.value as? KClassValue
        maybeKClass?.getArgumentType(moduleForResolve)
    }

internal fun ClassDescriptor.getKSerializerConstructorMarker(): ClassDescriptor =
    module.findClassAcrossModuleDependencies(ClassId(SerializationPackages.packageFqName, SerialEntityNames.SERIAL_CTOR_MARKER_NAME))!!

internal fun ModuleDescriptor.getClassFromInternalSerializationPackage(classSimpleName: String) =
    getFromPackage(SerializationPackages.internalPackageFqName, classSimpleName)

internal fun ModuleDescriptor.getClassFromSerializationPackage(classSimpleName: String) =
    getFromPackage(SerializationPackages.packageFqName, classSimpleName)

private fun ModuleDescriptor.getFromPackage(packageFqName: FqName, classSimpleName: String) = requireNotNull(
    findClassAcrossModuleDependencies(
        ClassId(
            packageFqName,
            Name.identifier(classSimpleName)
        )
    )
) { "Can't locate class $classSimpleName from package $packageFqName" }

internal fun ClassDescriptor.getClassFromSerializationPackage(classSimpleName: String) =
    requireNotNull(
        module.findClassAcrossModuleDependencies(
            ClassId(
                SerializationPackages.packageFqName,
                Name.identifier(classSimpleName)
            )
        )
    ) { "Can't locate class $classSimpleName" }

internal fun ClassDescriptor.getClassFromInternalSerializationPackage(classSimpleName: String) =
    module.getClassFromInternalSerializationPackage(classSimpleName)

fun ClassDescriptor.toSimpleType(nullable: Boolean = false) =
    KotlinTypeFactory.simpleType(Annotations.EMPTY, this.typeConstructor, emptyList(), nullable)

internal fun Annotated.annotationsWithArguments(): List<Triple<ClassDescriptor, List<ValueArgument>, List<ValueParameterDescriptor>>> =
    annotations.asSequence()
        .filter { it.type.toClassDescriptor?.isSerialInfoAnnotation == true }
        .filterIsInstance<LazyAnnotationDescriptor>()
        .mapNotNull { annDesc ->
            annDesc.type.toClassDescriptor?.let {
                Triple(it, annDesc.annotationEntry.valueArguments, it.unsubstitutedPrimaryConstructor?.valueParameters.orEmpty())
            }
        }
        .toList()
