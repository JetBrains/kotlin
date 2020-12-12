/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.diagnostic

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperInterfaces
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.hasBackingField
import org.jetbrains.kotlin.resolve.isInlineClass
import org.jetbrains.kotlin.resolve.isInlineClassType
import org.jetbrains.kotlin.resolve.jvm.annotations.TRANSIENT_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyAnnotationDescriptor
import org.jetbrains.kotlin.resolve.source.getPsi
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

        if (!canBeSerializedInternally(descriptor, declaration, context.trace)) return
        if (declaration !is KtPureClassOrObject) return
        if (!isIde) {
            // In IDE, BindingTrace is recreated each time code is modified, effectively resulting in JAR manifest read every time user types
            // something, which may be very slow. So we perform this check only during CLI/Gradle compilation.
            VersionReader.getVersionsForCurrentModuleFromTrace(descriptor.module, context.trace)?.let {
                checkMinKotlin(it, descriptor, context.trace)
                checkMinRuntime(it, descriptor, context.trace)
            }
        }
        val props = buildSerializableProperties(descriptor, context.trace) ?: return
        checkCorrectTransientAnnotationIsUsed(descriptor, props.serializableProperties, context.trace)
        checkTransients(declaration, context.trace)
        analyzePropertiesSerializers(context.trace, descriptor, props.serializableProperties)
    }

    private fun checkMinRuntime(versions: VersionReader.RuntimeVersions, descriptor: ClassDescriptor, trace: BindingTrace) {
        // if RuntimeVersions are present, but implementation version is not,
        // it means that we are reading from jar which does not have this manifest parameter - a pre-1.0 serialization runtime.
        // For non-JAR distributions (klib, js) this method is not invoked, since getVersionsForCurrentModule
        // unable to read from them
        if (!versions.implementationVersionMatchSupported()) {
            descriptor.onSerializableAnnotation {
                trace.report(
                    SerializationErrors.PROVIDED_RUNTIME_TOO_LOW.on(
                        it,
                        versions.implementationVersion?.toString() ?: "too low",
                        KotlinCompilerVersion.getVersion() ?: "unknown",
                        VersionReader.MINIMAL_SUPPORTED_VERSION.toString(),
                    )
                )
            }
        }
    }

    private fun checkMinKotlin(versions: VersionReader.RuntimeVersions, descriptor: ClassDescriptor, trace: BindingTrace) {
        if (versions.currentCompilerMatchRequired()) return
        descriptor.onSerializableAnnotation {
            trace.report(
                SerializationErrors.REQUIRED_KOTLIN_TOO_HIGH.on(
                    it,
                    KotlinCompilerVersion.getVersion() ?: "too low",
                    versions.implementationVersion?.toString() ?: "unknown",
                    versions.requireKotlinVersion?.toString() ?: "N/A",
                )
            )
        }
    }

    protected open val isIde: Boolean get() = false

    private fun checkCorrectTransientAnnotationIsUsed(
        descriptor: ClassDescriptor,
        properties: List<SerializableProperty>,
        trace: BindingTrace
    ) {
        if (descriptor.getSuperInterfaces().any { it.fqNameSafe.asString() == "java.io.Serializable" }) return // do not check
        for (prop in properties) {
            if (prop.transient) continue // correct annotation is used
            val incorrectTransient = prop.descriptor.backingField?.annotations?.findAnnotation(TRANSIENT_ANNOTATION_FQ_NAME)
            if (incorrectTransient != null) {
                val elementToReport = incorrectTransient.source.getPsi() ?: prop.descriptor.findPsi() ?: continue
                trace.report(SerializationErrors.INCORRECT_TRANSIENT.on(elementToReport))
            }
        }
    }

    private fun canBeSerializedInternally(descriptor: ClassDescriptor, declaration: KtDeclaration, trace: BindingTrace): Boolean {
        if (descriptor.isSerializableEnumWithMissingSerializer()) {
            val declarationToReport = declaration.modifierList ?: declaration
            trace.report(SerializationErrors.EXPLICIT_SERIALIZABLE_IS_REQUIRED.on(declarationToReport))
            return false
        }

        if (!descriptor.annotations.hasAnnotation(SerializationAnnotations.serializableAnnotationFqName)) return false

        if (!serializationPluginEnabledOn(descriptor)) {
            trace.reportOnSerializableAnnotation(descriptor, SerializationErrors.PLUGIN_IS_NOT_ENABLED)
            return false
        }

        if (descriptor.isInlineClass()) {
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

    private fun ClassDescriptor.isSerializableEnumWithMissingSerializer(): Boolean {
        if (kind != ClassKind.ENUM_CLASS) return false
        if (annotations.hasAnnotation(SerializationAnnotations.serializableAnnotationFqName)) return false
        if (annotations.hasAnySerialAnnotation) return true
        return enumEntries().any { (it.annotations.hasAnySerialAnnotation) }
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
                descriptor.onSerializableAnnotation { a ->
                    trace.report(SerializationErrors.DUPLICATE_SERIAL_NAME.on(a, it.name))
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
                trace.report(SerializationErrors.TRANSIENT_IS_REDUNDANT.on(transientPsi ?: declaration))
            }

            if (isMarkedTransient && !isInitialized && hasBackingField) {
                trace.report(SerializationErrors.TRANSIENT_MISSING_INITIALIZER.on(declaration))
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
                checkCustomSerializerMatch(it.module, it.type, it.descriptor, element, trace, propertyPsi)
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

    private fun KotlinType.isUnsupportedInlineType() = isInlineClassType() && !KotlinBuiltIns.isPrimitiveTypeOrNullablePrimitiveType(this)

    private fun AbstractSerialGenerator.checkType(
        module: ModuleDescriptor,
        type: KotlinType,
        ktType: KtTypeReference?,
        trace: BindingTrace,
        fallbackElement: PsiElement
    ) {
        if (type.genericIndex != null) return // type arguments always have serializer stored in class' field
        val element = ktType?.typeElement
        if (type.isUnsupportedInlineType()) {
            trace.report(SerializationErrors.INLINE_CLASSES_NOT_SUPPORTED.on(element ?: fallbackElement))
        }
        val serializer = findTypeSerializerOrContextUnchecked(module, type)
        if (serializer != null) {
            checkCustomSerializerMatch(module, type, type, element, trace, fallbackElement)
            checkSerializerNullability(type, serializer.defaultType, element, trace, fallbackElement)
            checkTypeArguments(module, type, element, trace, fallbackElement)
        } else {
            trace.report(SerializationErrors.SERIALIZER_NOT_FOUND.on(element ?: fallbackElement, type))
        }
    }

    private fun checkCustomSerializerMatch(
        module: ModuleDescriptor,
        classType: KotlinType,
        descriptor: Annotated,
        element: KtElement?,
        trace: BindingTrace,
        fallbackElement: PsiElement
    ) {
        val serializerType = descriptor.annotations.serializableWith(module) ?: return
        val serializerForType = serializerType.supertypes().find { isKSerializer(it) }?.arguments?.first()?.type ?: return
        // Compare constructors because we do not care about generic arguments and nullability
        if (classType.constructor != serializerForType.constructor)
            trace.report(
                SerializationErrors.SERIALIZER_TYPE_INCOMPATIBLE.on(
                    element ?: fallbackElement,
                    classType,
                    serializerType,
                    serializerForType
                )
            )
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

        val serializerForType = castedToKSerial.arguments.first().type
        if (!classType.isMarkedNullable && serializerForType.isMarkedNullable)
            trace.report(
                SerializationErrors.SERIALIZER_NULLABILITY_INCOMPATIBLE.on(element ?: fallbackElement, serializerType, classType),
            )
    }

    private inline fun ClassDescriptor.onSerializableAnnotation(report: (KtAnnotationEntry) -> Unit) {
        findSerializableAnnotationDeclaration()?.let(report)
    }

    private fun BindingTrace.reportOnSerializableAnnotation(descriptor: ClassDescriptor, error: DiagnosticFactory0<in KtAnnotationEntry>) {
        descriptor.onSerializableAnnotation { e ->
            report(error.on(e))
        }
    }
}

internal val ClassDescriptor.serializableAnnotationIsUseless: Boolean
    get() = hasSerializableAnnotationWithoutArgs && !isInternalSerializable && !hasCompanionObjectAsSerializer && !isSerializableEnum()