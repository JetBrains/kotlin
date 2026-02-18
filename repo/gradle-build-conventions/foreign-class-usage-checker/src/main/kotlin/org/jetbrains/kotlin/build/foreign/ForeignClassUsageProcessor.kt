/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.foreign

import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.signature.SignatureReader
import org.jetbrains.org.objectweb.asm.signature.SignatureVisitor
import org.jetbrains.org.objectweb.asm.tree.AnnotationNode
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import org.jetbrains.org.objectweb.asm.tree.FieldNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import java.io.File
import kotlin.metadata.ExperimentalContextReceivers
import kotlin.metadata.KmAnnotation
import kotlin.metadata.KmAnnotationArgument
import kotlin.metadata.KmClass
import kotlin.metadata.KmClassifier
import kotlin.metadata.KmConstructor
import kotlin.metadata.KmFunction
import kotlin.metadata.KmPackage
import kotlin.metadata.KmProperty
import kotlin.metadata.KmType
import kotlin.metadata.KmTypeAlias
import kotlin.metadata.KmTypeParameter
import kotlin.metadata.Visibility
import kotlin.metadata.jvm.JvmFieldSignature
import kotlin.metadata.jvm.JvmMethodSignature
import kotlin.metadata.jvm.KotlinClassMetadata
import kotlin.metadata.jvm.fieldSignature
import kotlin.metadata.jvm.getterSignature
import kotlin.metadata.jvm.setterSignature
import kotlin.metadata.jvm.signature
import kotlin.metadata.jvm.syntheticMethodForAnnotations
import kotlin.metadata.visibility

/**
 * Class name in the JVM "internal" format: "org/foo/bar/Baz$Inner"
 */
internal typealias InternalClassName = String

internal class ForeignClassUsageProcessor(nonPublicMarkers: Set<String>, private val collectUsages: Boolean) {
    private val nonPublicAnnotationDescriptors: Set<String> =
        nonPublicMarkers.mapTo(HashSet()) { "L" + it.replace('.', '/') + ";" }

    private companion object {
        private val KOTLIN_METADATA_DESCRIPTOR: String = Type.getDescriptor(Metadata::class.java)
    }

    private val collectedClassNames = HashSet<InternalClassName>()
    private val visitedClassNames = HashSet<InternalClassName>()

    private lateinit var currentClassName: InternalClassName
    private val collectedUsages: MutableMap<InternalClassName, MutableSet<InternalClassName>> = hashMapOf()

    val foreignClassNames: Set<InternalClassName>
        get() = collectedClassNames - visitedClassNames

    fun usages(className: InternalClassName): Collection<String> {
        return collectedUsages[className].orEmpty()
    }

    fun process(classFile: File) {
        classFile.inputStream().buffered().use { inputStream ->
            val classReader = ClassReader(inputStream)
            val classNode = ClassNode()

            classReader.accept(classNode, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG)

            visitedClassNames.add(classNode.name)
            currentClassName = classNode.name

            val kotlinMetadataAnnotation = classNode.visibleAnnotations.orEmpty().find { it.desc == KOTLIN_METADATA_DESCRIPTOR }
            if (kotlinMetadataAnnotation != null) {
                val kotlinMetadata = readMetadata(kotlinMetadataAnnotation)
                if (kotlinMetadata != null) {
                    processKotlinMetadata(kotlinMetadata, classNode)
                }
            } else {
                processJavaClass(classNode)
            }
        }
    }

    private fun processJavaClass(classNode: ClassNode) {
        if (!hasPublicAccess(classNode.access) || hasNonPublicMarker(classNode.visibleAnnotations, classNode.invisibleAnnotations)) {
            return
        }

        processJavaAnnotations(classNode.visibleAnnotations, classNode.invisibleAnnotations)
        processJavaAnnotations(classNode.visibleTypeAnnotations, classNode.invisibleTypeAnnotations)
        classNode.superName?.let(::processJavaInternalName)
        classNode.interfaces?.forEach(::processJavaInternalName)
        classNode.fields?.forEach(::processJavaField)
        classNode.methods?.forEach(::processJavaMethod)
        classNode.signature?.let(::processJavaGenericSignature)
    }

    private fun processJavaMethod(methodNode: MethodNode) {
        if (!hasPublicAccess(methodNode.access) || hasNonPublicMarker(methodNode.visibleAnnotations, methodNode.invisibleAnnotations)) {
            return
        }

        doProcessJavaMethod(methodNode)
    }

    private fun doProcessJavaMethod(methodNode: MethodNode) {
        processJavaAnnotations(methodNode.visibleAnnotations, methodNode.invisibleAnnotations)
        processJavaAnnotations(methodNode.visibleTypeAnnotations, methodNode.invisibleTypeAnnotations)
        methodNode.visibleParameterAnnotations?.forEach { it?.forEach(::processJavaAnnotation) }
        methodNode.invisibleParameterAnnotations?.forEach { it?.forEach(::processJavaAnnotation) }
        processJavaTypeDescriptor(methodNode.desc)
        methodNode.exceptions?.forEach(::processJavaInternalName)
        methodNode.signature?.let(::processJavaGenericSignature)
    }

    private fun processJavaField(fieldNode: FieldNode) {
        if (!hasPublicAccess(fieldNode.access) || hasNonPublicMarker(fieldNode.visibleAnnotations, fieldNode.invisibleAnnotations)) {
            return
        }

        doProcessJavaField(fieldNode)
    }

    private fun doProcessJavaField(fieldNode: FieldNode) {
        processJavaAnnotations(fieldNode.visibleAnnotations, fieldNode.invisibleAnnotations)
        processJavaAnnotations(fieldNode.visibleTypeAnnotations, fieldNode.invisibleTypeAnnotations)
        processJavaTypeDescriptor(fieldNode.desc)
        fieldNode.signature?.let(::processJavaGenericSignature)
    }

    private fun processJavaAnnotations(visibleAnnotations: List<AnnotationNode>?, invisibleAnnotations: List<AnnotationNode>?) {
        visibleAnnotations?.forEach(::processJavaAnnotation)
        invisibleAnnotations?.forEach(::processJavaAnnotation)
    }

    private fun processJavaAnnotation(annotationNode: AnnotationNode) {
        processJavaTypeDescriptor(annotationNode.desc)

        val values = annotationNode.values ?: return
        var index = 0

        while (index < values.size) {
            val value = values[index + 1]
            processJavaAnnotationValue(value)
            /**
             * Skip both the annotation parameter name and the value.
             * @see [AnnotationNode.values].
             */
            index += 2
        }
    }

    private fun processJavaAnnotationValue(value: Any) {
        when (value) {
            is Type -> processJavaType(value)
            is AnnotationNode -> processJavaAnnotation(value)
            is List<*> -> {
                for (element in value) {
                    if (element != null) {
                        processJavaAnnotationValue(element)
                    }
                }
            }
        }
    }

    private fun processJavaGenericSignature(signature: String) {
        val visitor = object : SignatureVisitor(Opcodes.ASM9) {
            override fun visitClassType(name: String) {
                processJavaInternalName(name)
            }
        }

        SignatureReader(signature).accept(visitor)
    }

    private fun processJavaTypeDescriptor(descriptor: String) {
        val type = Type.getType(descriptor)
        processJavaType(type)
    }

    private fun processJavaType(type: Type) {
        when (type.sort) {
            Type.OBJECT -> processJavaInternalName(type.internalName)
            Type.ARRAY -> processJavaType(type.elementType)
            Type.METHOD -> {
                type.argumentTypes.forEach(::processJavaType)
                processJavaType(type.returnType)
            }
            else -> {}
        }
    }

    private fun processJavaInternalName(internalName: InternalClassName) {
        collectedClassNames.add(internalName)

        if (collectUsages) {
            collectedUsages.getOrPut(internalName) { LinkedHashSet() }.add(currentClassName)
        }
    }

    private fun hasPublicAccess(access: Int): Boolean {
        return access and (Opcodes.ACC_PUBLIC or Opcodes.ACC_PROTECTED) != 0
    }

    private fun hasNonPublicMarker(visibleAnnotations: List<AnnotationNode>?, invisibleAnnotations: List<AnnotationNode>?): Boolean {
        return visibleAnnotations.orEmpty().any { it.desc in nonPublicAnnotationDescriptors }
                || invisibleAnnotations.orEmpty().any { it.desc in nonPublicAnnotationDescriptors }
    }

    fun processKotlinMetadata(metadata: KotlinClassMetadata, classNode: ClassNode) {
        when (metadata) {
            is KotlinClassMetadata.Class -> processKotlinClass(metadata.kmClass, classNode)
            is KotlinClassMetadata.FileFacade -> processKotlinPackage(metadata.kmPackage, classNode)
            is KotlinClassMetadata.MultiFileClassPart -> processKotlinPackage(metadata.kmPackage, classNode)
            is KotlinClassMetadata.SyntheticClass, is KotlinClassMetadata.MultiFileClassFacade -> {}
            is KotlinClassMetadata.Unknown -> error("Unknown Kotlin metadata kind: $metadata")
        }
    }

    private fun processKotlinClass(kmClass: KmClass, classNode: ClassNode) {
        if (!kmClass.visibility.isPublicApi || hasNonPublicMarker(classNode.visibleAnnotations, classNode.invisibleAnnotations)) {
            return
        }

        processJavaAnnotations(classNode.visibleAnnotations, classNode.invisibleAnnotations)
        kmClass.supertypes.forEach(::processKotlinType)
        kmClass.typeParameters.forEach(::processKotlinTypeParameter)
        kmClass.typeAliases.forEach { processKotlinTypeAlias(it, packageName = null) }
        kmClass.constructors.forEach { processKotlinConstructor(it, classNode) }
        kmClass.functions.forEach { processKotlinFunction(it, classNode) }
        kmClass.properties.forEach { processKotlinProperty(it, classNode) }
    }

    private fun processKotlinPackage(kmPackage: KmPackage, classNode: ClassNode) {
        val packageName = classNode.name.substringBeforeLast('/')

        kmPackage.functions.forEach { processKotlinFunction(it, classNode) }
        kmPackage.properties.forEach { processKotlinProperty(it, classNode) }
        kmPackage.typeAliases.forEach { processKotlinTypeAlias(it, packageName) }
    }

    private fun processKotlinTypeAlias(kmTypeAlias: KmTypeAlias, packageName: String?) {
        if (packageName != null) {
            visitedClassNames.add(packageName + "/" + kmTypeAlias.name)
        }

        if (!kmTypeAlias.visibility.isPublicApi || hasNonPublicMarker(kmTypeAlias.annotations)) {
            return
        }

        kmTypeAlias.annotations.forEach(::processKotlinAnnotation)
        kmTypeAlias.typeParameters.forEach(::processKotlinTypeParameter)
        kmTypeAlias.underlyingType.let(::processKotlinType)
        kmTypeAlias.expandedType.let(::processKotlinType)
    }

    private fun processKotlinConstructor(kmConstructor: KmConstructor, classNode: ClassNode) {
        if (!kmConstructor.visibility.isPublicApi || (classNode.access and Opcodes.ACC_ANNOTATION) != 0) {
            return
        }

        val signature = kmConstructor.signature ?: error("No signature for constructor of ${classNode.name}")
        val methodNode = classNode.findMethod(signature) ?: error("No method node for constructor $signature")
        if (hasNonPublicMarker(methodNode.visibleAnnotations, methodNode.invisibleAnnotations)) {
            return
        }

        doProcessJavaMethod(methodNode)
    }

    @OptIn(ExperimentalContextReceivers::class)
    private fun processKotlinFunction(kmFunction: KmFunction, classNode: ClassNode) {
        if (!kmFunction.visibility.isPublicApi) {
            return
        }

        val signature = kmFunction.signature ?: error("No signature for function ${kmFunction.name}")
        val methodNode = classNode.findMethod(signature) ?: error("No method node for function $signature")
        if (hasNonPublicMarker(methodNode.visibleAnnotations, methodNode.invisibleAnnotations)) {
            return
        }

        doProcessJavaMethod(methodNode)

        kmFunction.receiverParameterType?.let(::processKotlinType)
        kmFunction.contextReceiverTypes.forEach(::processKotlinType)
        processKotlinType(kmFunction.returnType)
    }

    @OptIn(ExperimentalContextReceivers::class)
    private fun processKotlinProperty(kmProperty: KmProperty, classNode: ClassNode) {
        if (!kmProperty.visibility.isPublicApi) {
            return
        }

        val annotationMethodSignature = kmProperty.syntheticMethodForAnnotations
        if (annotationMethodSignature != null) {
            val methodNode = classNode.findMethod(annotationMethodSignature)
                ?: error("No annotation method node for property ${kmProperty.name}")

            if (hasNonPublicMarker(methodNode.visibleAnnotations, methodNode.invisibleAnnotations)) {
                return
            }
        }

        kmProperty.fieldSignature?.let { classNode.findField(it) }?.let(::processJavaField)
        kmProperty.getterSignature?.let { classNode.findMethod(it) }?.let(::doProcessJavaMethod)
        kmProperty.setterSignature?.let { classNode.findMethod(it) }?.let(::doProcessJavaMethod)

        kmProperty.contextReceiverTypes.forEach(::processKotlinType)
        kmProperty.receiverParameterType?.let(::processKotlinType)
        processKotlinType(kmProperty.returnType)
    }

    private fun processKotlinTypeParameter(kmTypeParameter: KmTypeParameter) {
        kmTypeParameter.upperBounds.forEach(::processKotlinType)
    }

    private fun processKotlinAnnotation(kmAnnotation: KmAnnotation) {
        processClassName(kmAnnotation.className)
        kmAnnotation.arguments.values.forEach(::processKotlinAnnotationArgument)
    }

    private fun processKotlinAnnotationArgument(kmArgument: KmAnnotationArgument) {
        when (kmArgument) {
            is KmAnnotationArgument.AnnotationValue -> processKotlinAnnotation(kmArgument.annotation)
            is KmAnnotationArgument.ArrayKClassValue -> processClassName(kmArgument.className)
            is KmAnnotationArgument.ArrayValue -> kmArgument.elements.forEach(::processKotlinAnnotationArgument)
            is KmAnnotationArgument.EnumValue -> processClassName(kmArgument.enumClassName)
            is KmAnnotationArgument.KClassValue -> processClassName(kmArgument.className)
            is KmAnnotationArgument.BooleanValue,
            is KmAnnotationArgument.ByteValue,
            is KmAnnotationArgument.CharValue,
            is KmAnnotationArgument.DoubleValue,
            is KmAnnotationArgument.FloatValue,
            is KmAnnotationArgument.IntValue,
            is KmAnnotationArgument.LongValue,
            is KmAnnotationArgument.ShortValue,
            is KmAnnotationArgument.StringValue,
            is KmAnnotationArgument.UByteValue,
            is KmAnnotationArgument.UIntValue,
            is KmAnnotationArgument.ULongValue,
            is KmAnnotationArgument.UShortValue,
                -> {
            }
        }
    }

    private fun processKotlinType(kmType: KmType) {
        kmType.abbreviatedType?.let(::processKotlinType)
        kmType.outerType?.let(::processKotlinType)

        when (val kmClassifier = kmType.classifier) {
            is KmClassifier.Class -> processClassName(kmClassifier.name)
            is KmClassifier.TypeAlias -> processClassName(kmClassifier.name)
            is KmClassifier.TypeParameter -> {}
        }

        for (kmTypeProjection in kmType.arguments) {
            kmTypeProjection.type?.let(::processKotlinType)
        }
    }

    private fun hasNonPublicMarker(annotations: List<KmAnnotation>): Boolean {
        for (annotation in annotations) {
            val descriptor = "L" + annotation.className.replace('.', '/') + ";"
            if (descriptor in nonPublicAnnotationDescriptors) {
                return true
            }
        }

        return false
    }

    private fun processClassName(name: String) {
        val internalName = name.replace('.', '$')
        processJavaInternalName(internalName)
    }
}

private val Visibility.isPublicApi: Boolean
    get() = this == Visibility.PUBLIC || this == Visibility.PROTECTED

private fun ClassNode.findMethod(signature: JvmMethodSignature): MethodNode? {
    return methods.find { it.name == signature.name && it.desc == signature.descriptor }
}

private fun ClassNode.findField(signature: JvmFieldSignature): FieldNode? {
    return fields.find { it.name == signature.name && it.desc == signature.descriptor }
}