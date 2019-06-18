/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.diagnostic

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.reportFromPlugin
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny
import org.jetbrains.kotlin.resolve.hasBackingField
import org.jetbrains.kotlin.resolve.isInlineClassType
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyAnnotationDescriptor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.supertypes
import org.jetbrains.kotlin.util.slicedMap.Slices
import org.jetbrains.kotlin.util.slicedMap.WritableSlice
import org.jetbrains.kotlinx.serialization.compiler.backend.common.AbstractSerialGenerator
import org.jetbrains.kotlinx.serialization.compiler.backend.common.bodyPropertiesDescriptorsMap
import org.jetbrains.kotlinx.serialization.compiler.backend.common.findTypeSerializerOrContextUnchecked
import org.jetbrains.kotlinx.serialization.compiler.backend.common.primaryConstructorPropertiesDescriptorsMap
import org.jetbrains.kotlinx.serialization.compiler.resolve.*

internal val SERIALIZABLE_PROPERTIES: WritableSlice<ClassDescriptor, SerializableProperties> = Slices.createSimpleSlice()

open class SerializationPluginDeclarationChecker : DeclarationChecker {
    final override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (descriptor !is ClassDescriptor) return

        if (!canBeSerializedInternally(descriptor, context.trace)) return
        if (declaration !is KtPureClassOrObject) return
        val props = buildSerializableProperties(descriptor, context.trace) ?: return
        checkTransients(declaration, context.trace)
        analyzePropertiesSerializers(context.trace, descriptor, props.serializableProperties)
    }

    private fun canBeSerializedInternally(descriptor: ClassDescriptor, trace: BindingTrace): Boolean {
        if (!descriptor.annotations.hasAnnotation(SerializationAnnotations.serializableAnnotationFqName)) return false

        if (!serializationPluginEnabledOn(descriptor)) {
            trace.reportOnSerializableAnnotation(descriptor, SerializationErrors.PLUGIN_IS_NOT_ENABLED)
            return false
        }

        if (descriptor.isInline) {
            trace.reportOnSerializableAnnotation(descriptor, SerializationErrors.INLINE_CLASSES_NOT_SUPPORTED)
            return false
        }
        if (!descriptor.hasSerializableAnnotationWithoutArgs) return false

        if (descriptor.serializableAnnotationIsUseless) {
            trace.reportOnSerializableAnnotation(descriptor, SerializationErrors.SERIALIZABLE_ANNOTATION_IGNORED)
            return false
        }

        // check that we can instantiate supertype
        if (!descriptor.isSerializableEnum()) { // enums are inherited from java.lang.Enum and can't be inherited from other classes
            val superClass = descriptor.getSuperClassOrAny()
            if (!superClass.isInternalSerializable && superClass.constructors.singleOrNull { it.valueParameters.size == 0 } == null) {
                trace.reportOnSerializableAnnotation(descriptor, SerializationErrors.NON_SERIALIZABLE_PARENT_MUST_HAVE_NOARG_CTOR)
                return false
            }
        }
        return true
    }

    open fun serializationPluginEnabledOn(descriptor: ClassDescriptor): Boolean {
        // In the CLI/Gradle compiler, this diagnostic is located in the plugin itself.
        // Therefore, if we are here, plugin is in the compile classpath and enabled.
        // For the IDE case, see SerializationPluginIDEDeclarationChecker
        return true
    }

    private fun buildSerializableProperties(descriptor: ClassDescriptor, trace: BindingTrace): SerializableProperties? {
        if (!descriptor.annotations.hasAnnotation(SerializationAnnotations.serializableAnnotationFqName)) return null
        if (!descriptor.isInternalSerializable) return null
        if (descriptor.hasCompanionObjectAsSerializer) return null // customized by user

        val props = SerializableProperties(descriptor, trace.bindingContext)
        if (!props.isExternallySerializable) trace.reportOnSerializableAnnotation(
            descriptor,
            SerializationErrors.PRIMARY_CONSTRUCTOR_PARAMETER_IS_NOT_A_PROPERTY
        )

        // check that all names are unique
        val namesSet = mutableSetOf<String>()
        props.serializableProperties.forEach {
            if (!namesSet.add(it.name)) {
                descriptor.safeReport { a ->
                    trace.reportFromPlugin(
                        SerializationErrors.DUPLICATE_SERIAL_NAME.on(a, it.name),
                        SerializationPluginErrorsRendering
                    )
                }
            }
        }

        trace.record(SERIALIZABLE_PROPERTIES, descriptor, props)
        return props
    }

    private fun checkTransients(declaration: KtPureClassOrObject, trace: BindingTrace) {
        val propertiesMap: Map<PropertyDescriptor, KtDeclaration> =
            declaration.bodyPropertiesDescriptorsMap(
                trace.bindingContext,
                filterUninitialized = false
            ) + declaration.primaryConstructorPropertiesDescriptorsMap(trace.bindingContext)
        propertiesMap.forEach { (descriptor, declaration) ->
            val isInitialized = declarationHasInitializer(declaration) || descriptor.isLateInit
            val isMarkedTransient = descriptor.annotations.serialTransient
            val hasBackingField = descriptor.hasBackingField(trace.bindingContext)
            if (!hasBackingField && isMarkedTransient) {
                val transientPsi =
                    (descriptor.annotations.findAnnotation(SerializationAnnotations.serialTransientFqName) as? LazyAnnotationDescriptor)?.annotationEntry
                trace.reportFromPlugin(
                    SerializationErrors.TRANSIENT_IS_REDUNDANT.on(transientPsi ?: declaration),
                    SerializationPluginErrorsRendering
                )
            }

            if (isMarkedTransient && !isInitialized && hasBackingField) {
                trace.reportFromPlugin(
                    SerializationErrors.TRANSIENT_MISSING_INITIALIZER.on(declaration),
                    SerializationPluginErrorsRendering
                )
            }
        }
    }

    private fun declarationHasInitializer(declaration: KtDeclaration): Boolean = when (declaration) {
        is KtParameter -> declaration.hasDefaultValue()
        is KtProperty -> declaration.hasDelegateExpressionOrInitializer()
        else -> false
    }

    private fun analyzePropertiesSerializers(trace: BindingTrace, serializableClass: ClassDescriptor, props: List<SerializableProperty>) {
        val generatorContextForAnalysis = object : AbstractSerialGenerator(trace.bindingContext, serializableClass) {}
        props.forEach {
            val serializer = it.serializableWith?.toClassDescriptor
            val propertyPsi = it.descriptor.findPsi() ?: return@forEach
            val ktType = (propertyPsi as? KtCallableDeclaration)?.typeReference
            if (serializer != null) {
                val element = ktType?.typeElement
                checkSerializerNullability(it.type, serializer.defaultType, element, trace, propertyPsi)
                generatorContextForAnalysis.checkTypeArguments(it.module, it.type, element, trace, propertyPsi)
            } else {
                generatorContextForAnalysis.checkType(it.module, it.type, ktType, trace, propertyPsi)
            }
        }
    }

    private fun AbstractSerialGenerator.checkTypeArguments(
        module: ModuleDescriptor,
        type: KotlinType,
        element: KtTypeElement?,
        trace: BindingTrace,
        fallbackElement: PsiElement
    ) {
        type.arguments.forEachIndexed { i, it ->
            checkType(
                module,
                it.type,
                element?.typeArgumentsAsTypes?.getOrNull(i),
                trace,
                fallbackElement
            )
        }
    }

    private fun AbstractSerialGenerator.checkType(
        module: ModuleDescriptor,
        type: KotlinType,
        ktType: KtTypeReference?,
        trace: BindingTrace,
        fallbackElement: PsiElement
    ) {
        if (type.genericIndex != null) return // type arguments always have serializer stored in class' field
        val element = ktType?.typeElement
        if (type.isInlineClassType()) {
            trace.reportFromPlugin(
                SerializationErrors.INLINE_CLASSES_NOT_SUPPORTED.on(element ?: fallbackElement),
                SerializationPluginErrorsRendering
            )
        }
        val serializer = findTypeSerializerOrContextUnchecked(module, type)
        if (serializer != null) {
            checkSerializerNullability(type, serializer.defaultType, element, trace, fallbackElement)
            checkTypeArguments(module, type, element, trace, fallbackElement)
        } else {
            trace.reportFromPlugin(
                SerializationErrors.SERIALIZER_NOT_FOUND.on(element ?: fallbackElement, type),
                SerializationPluginErrorsRendering
            )
        }
    }

    private fun checkSerializerNullability(
        classType: KotlinType,
        serializerType: KotlinType,
        element: KtTypeElement?,
        trace: BindingTrace,
        fallbackElement: PsiElement
    ) {
        // @Serializable annotation has proper signature so this error would be caught in type checker
        val castedToKSerial = serializerType.supertypes().find { isKSerializer(it) } ?: return

        if (!classType.isMarkedNullable && castedToKSerial.arguments.first().type.isMarkedNullable)
            trace.reportFromPlugin(
                SerializationErrors.SERIALIZER_NULLABILITY_INCOMPATIBLE.on(element ?: fallbackElement, serializerType, classType),
                SerializationPluginErrorsRendering
            )
    }

    private inline fun ClassDescriptor.safeReport(report: (KtAnnotationEntry) -> Unit) {
        findSerializableAnnotationDeclaration()?.let(report)
    }

    private fun BindingTrace.reportOnSerializableAnnotation(descriptor: ClassDescriptor, error: DiagnosticFactory0<in KtAnnotationEntry>) {
        descriptor.safeReport { e ->
            reportFromPlugin(
                error.on(e),
                SerializationPluginErrorsRendering
            )
        }
    }
}

internal val ClassDescriptor.serializableAnnotationIsUseless: Boolean
    get() = hasSerializableAnnotationWithoutArgs && !isInternalSerializable && !hasCompanionObjectAsSerializer && !isSerializableEnum()