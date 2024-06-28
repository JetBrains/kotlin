/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.resolve

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.ValueArgument
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyAnnotationDescriptor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.TypeAttributes

fun ClassConstructorDescriptor.isSerializationCtor(): Boolean {
    /*kind == CallableMemberDescriptor.Kind.SYNTHESIZED does not work because DeserializedClassConstructorDescriptor loses its kind*/
    return valueParameters.lastOrNull()?.run {
        name == SerialEntityNames.dummyParamName && type.constructor.declarationDescriptor?.classId == ClassId(
            SerializationPackages.internalPackageFqName,
            SerialEntityNames.SERIAL_CTOR_MARKER_NAME
        )
    } == true
}

// finds constructor (KSerializer<T0>, KSerializer<T1>...) on a KSerializer<T<T0, T1...>>
fun findSerializerConstructorForTypeArgumentsSerializers(
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

fun AnnotationDescriptor.findAnnotationEntry(): KtAnnotationEntry? = (this as? LazyAnnotationDescriptor)?.annotationEntry

inline fun <reified R> Annotations.findAnnotationConstantValue(annotationFqName: FqName, property: String): R? =
    findAnnotation(annotationFqName)?.findConstantValue(property)

inline fun <reified R> AnnotationDescriptor.findConstantValue(property: String): R? =
    allValueArguments.entries.singleOrNull { it.key.asString() == property }?.value?.value as? R

fun Annotations.findAnnotationKotlinTypeValue(
    annotationFqName: FqName,
    moduleForResolve: ModuleDescriptor,
    property: String
): KotlinType? =
    findAnnotation(annotationFqName)?.let { annotation ->
        val maybeKClass = annotation.allValueArguments.entries.singleOrNull { it.key.asString() == property }?.value as? KClassValue
        maybeKClass?.getArgumentType(moduleForResolve)
    }

fun ClassDescriptor.getKSerializerConstructorMarker(): ClassDescriptor =
    module.findClassAcrossModuleDependencies(
        ClassId(
            SerializationPackages.internalPackageFqName,
            SerialEntityNames.SERIAL_CTOR_MARKER_NAME
        )
    )!!

fun ClassDescriptor.getKSerializer(): ClassDescriptor =
    module.findClassAcrossModuleDependencies(
        ClassId(
            SerializationPackages.packageFqName,
            SerialEntityNames.KSERIALIZER_NAME
        )
    )!!

fun ClassDescriptor.findNamedCompanionAnnotation(): ClassDescriptor? =
    module.findClassAcrossModuleDependencies(SerializationAnnotations.namedCompanionClassId)

fun ModuleDescriptor.getJsExportIgnore(): ClassDescriptor? =
    findClassAcrossModuleDependencies(SerializationJsDependenciesClassIds.jsExportIgnore)

fun getInternalPackageFqn(classSimpleName: String): FqName =
    SerializationPackages.internalPackageFqName.child(Name.identifier(classSimpleName))

fun ModuleDescriptor.getClassFromInternalSerializationPackage(classSimpleName: String) =
    requireNotNull(
        findClassAcrossModuleDependencies(
            ClassId(
                SerializationPackages.internalPackageFqName,
                Name.identifier(classSimpleName)
            )
        )
    ) { "Can't locate class $classSimpleName from package ${SerializationPackages.internalPackageFqName}" }

fun ModuleDescriptor.getClassFromSerializationDescriptorsPackage(classSimpleName: String) =
    requireNotNull(
        findClassAcrossModuleDependencies(
            ClassId(
                SerializationPackages.descriptorsPackageFqName,
                Name.identifier(classSimpleName)
            )
        )
    ) { "Can't locate class $classSimpleName from package ${SerializationPackages.descriptorsPackageFqName}" }

fun getSerializationPackageFqn(classSimpleName: String): FqName =
    SerializationPackages.packageFqName.child(Name.identifier(classSimpleName))

fun ModuleDescriptor.getClassFromSerializationPackage(classSimpleName: String) =
    SerializationPackages.allPublicPackages.firstNotNullOfOrNull { pkg ->
        module.findClassAcrossModuleDependencies(ClassId(
            pkg,
            Name.identifier(classSimpleName)
        ))
    } ?: throw IllegalArgumentException("Can't locate class $classSimpleName")

fun ClassDescriptor.getClassFromSerializationPackage(classSimpleName: String) =
    module.getClassFromSerializationPackage(classSimpleName)

fun ClassDescriptor.getClassFromInternalSerializationPackage(classSimpleName: String) =
    module.getClassFromInternalSerializationPackage(classSimpleName)

fun ClassDescriptor.toSimpleType(nullable: Boolean = false) =
    KotlinTypeFactory.simpleType(TypeAttributes.Empty, this.typeConstructor, emptyList(), nullable)

fun Annotated.annotationsWithArguments(): List<Triple<ClassDescriptor, List<ValueArgument>, List<ValueParameterDescriptor>>> =
    annotations.asSequence()
        .filter { it.type.toClassDescriptor?.isSerialInfoAnnotation == true }
        .filterIsInstance<LazyAnnotationDescriptor>()
        .mapNotNull { annDesc ->
            annDesc.type.toClassDescriptor?.let {
                Triple(it, annDesc.annotationEntry.valueArguments, it.unsubstitutedPrimaryConstructor?.valueParameters.orEmpty())
            }
        }
        .toList()
