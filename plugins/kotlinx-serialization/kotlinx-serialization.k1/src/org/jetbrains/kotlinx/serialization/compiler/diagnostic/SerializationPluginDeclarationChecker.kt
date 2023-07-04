/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.diagnostic

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.JvmNames.TRANSIENT_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyAnnotationDescriptor
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isEnum
import org.jetbrains.kotlin.types.typeUtil.supertypes
import org.jetbrains.kotlin.util.slicedMap.Slices
import org.jetbrains.kotlin.util.slicedMap.WritableSlice
import org.jetbrains.kotlinx.serialization.compiler.backend.common.*
import org.jetbrains.kotlinx.serialization.compiler.backend.common.bodyPropertiesDescriptorsMap
import org.jetbrains.kotlinx.serialization.compiler.backend.common.primaryConstructorPropertiesDescriptorsMap
import org.jetbrains.kotlinx.serialization.compiler.diagnostic.SerializationErrors.EXTERNAL_SERIALIZER_USELESS
import org.jetbrains.kotlinx.serialization.compiler.resolve.*
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.LOAD_NAME
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.SAVE_NAME
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.SERIAL_DESC_FIELD_NAME

val SERIALIZABLE_PROPERTIES: WritableSlice<ClassDescriptor, SerializableProperties> = Slices.createSimpleSlice()

open class SerializationPluginDeclarationChecker : DeclarationChecker {
    private var useLegacyEnumSerializerCached: Boolean? = null

    final override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (descriptor !is ClassDescriptor) return

        checkMetaSerializableApplicable(descriptor, context.trace)
        checkInheritableSerialInfoNotRepeatable(descriptor, context.trace)
        checkEnum(descriptor, declaration, context.trace)
        checkExternalSerializer(descriptor, declaration, context.trace)

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
        checkInheritedAnnotations(descriptor, declaration, context.trace)
    }

    private fun checkMetaSerializableApplicable(descriptor: ClassDescriptor, trace: BindingTrace) {
        if (descriptor.kind != ClassKind.ANNOTATION_CLASS) return
        if (descriptor.classId?.isNestedClass != true) return
        val entry = descriptor.findAnnotationDeclaration(SerializationAnnotations.metaSerializableAnnotationFqName) ?: return
        trace.report(SerializationErrors.META_SERIALIZABLE_NOT_APPLICABLE.on(entry))
    }

    private fun checkInheritableSerialInfoNotRepeatable(descriptor: ClassDescriptor, trace: BindingTrace) {
        if (descriptor.kind != ClassKind.ANNOTATION_CLASS) return
        // both kotlin.Repeatable and java.lang.annotation.Repeatable
        if (!(descriptor.isAnnotatedWithKotlinRepeatable() || descriptor.annotations.hasAnnotation(JvmAnnotationNames.REPEATABLE_ANNOTATION))) return
        val inheritableAnno = descriptor.findAnnotationDeclaration(SerializationAnnotations.inheritableSerialInfoFqName) ?: return
        trace.report(SerializationErrors.INHERITABLE_SERIALINFO_CANT_BE_REPEATABLE.on(inheritableAnno))
    }

    private fun checkExternalSerializer(classDescriptor: ClassDescriptor, declaration: KtDeclaration, trace: BindingTrace) {
        val serializableKType = classDescriptor.serializerForClass ?: return
        val serializableDescriptor = serializableKType.toClassDescriptor ?: return
        val props = SerializableProperties(serializableDescriptor, trace.bindingContext)

        val descriptorOverridden = classDescriptor.unsubstitutedMemberScope
            .getContributedVariables(SERIAL_DESC_FIELD_NAME, NoLookupLocation.FROM_BACKEND).singleOrNull {
                it.kind != CallableMemberDescriptor.Kind.SYNTHESIZED
            } != null

        val serializeOverridden = classDescriptor.unsubstitutedMemberScope
            .getContributedFunctions(SAVE_NAME, NoLookupLocation.FROM_BACKEND).singleOrNull {
                it.valueParameters.size == 2
                        && it.overriddenDescriptors.isNotEmpty()
                        && it.kind != CallableMemberDescriptor.Kind.SYNTHESIZED
            } != null

        val deserializeOverridden = classDescriptor.unsubstitutedMemberScope
            .getContributedFunctions(LOAD_NAME, NoLookupLocation.FROM_BACKEND).singleOrNull {
                it.valueParameters.size == 1
                        && it.overriddenDescriptors.isNotEmpty()
                        && it.kind != CallableMemberDescriptor.Kind.SYNTHESIZED
            } != null

        if (descriptorOverridden && serializeOverridden && deserializeOverridden) {
            val entry = classDescriptor.findAnnotationDeclaration(SerializationAnnotations.serializerAnnotationFqName)
            trace.report(EXTERNAL_SERIALIZER_USELESS.on(entry ?: declaration, classDescriptor.defaultType))
            return
        }

        if (!props.isExternallySerializable) {
            val entry = classDescriptor.findAnnotationDeclaration(SerializationAnnotations.serializerAnnotationFqName)
            val inSameModule =
                trace.bindingContext[BindingContext.FQNAME_TO_CLASS_DESCRIPTOR, serializableDescriptor.fqNameUnsafe] != null
            val diagnostic =
                if (inSameModule) SerializationErrors.EXTERNAL_CLASS_NOT_SERIALIZABLE else SerializationErrors.EXTERNAL_CLASS_IN_ANOTHER_MODULE

            trace.report(diagnostic.on(entry ?: declaration, classDescriptor.defaultType, serializableKType))
        }
    }

    private fun checkInheritedAnnotations(descriptor: ClassDescriptor, declaration: KtDeclaration, trace: BindingTrace) {
        val annotationsFilter: (Annotations) -> List<Pair<FqName, AnnotationDescriptor>> = { an ->
            an.map { it.annotationClass!!.fqNameSafe to it }
                .filter { it.second.annotationClass?.isInheritableSerialInfoAnnotation == true }
        }
        val annotationByFq: MutableMap<FqName, AnnotationDescriptor> = mutableMapOf()
        val reported: MutableSet<FqName> = mutableSetOf()
        // my annotations
        annotationByFq.putAll(annotationsFilter(descriptor.annotations))
        // inherited
        for (clazz in descriptor.getAllSuperClassifiers()) {
            val annotations = annotationsFilter(clazz.annotations)
            annotations.forEach { (fqname, call) ->
                if (fqname in annotationByFq) {
                    val existing = annotationByFq.getValue(fqname)
                    if (existing.allValueArguments != call.allValueArguments) {
                        if (reported.add(fqname)) {
                            val entry = (existing as? LazyAnnotationDescriptor)?.annotationEntry ?: declaration
                            trace.report(
                                SerializationErrors.INCONSISTENT_INHERITABLE_SERIALINFO.on(
                                    entry,
                                    existing.type,
                                    clazz.defaultType
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkMinRuntime(versions: RuntimeVersions, descriptor: ClassDescriptor, trace: BindingTrace) {
        // if RuntimeVersions are present, but implementation version is not,
        // it means that we are reading from jar which does not have this manifest parameter - a pre-1.0 serialization runtime.
        // For non-JAR distributions (klib, js) this method is not invoked, since getVersionsForCurrentModule
        // unable to read from them
        if (!versions.implementationVersionMatchSupported()) {
            descriptor.onSerializableOrMetaAnnotation {
                trace.report(
                    SerializationErrors.PROVIDED_RUNTIME_TOO_LOW.on(
                        it,
                        versions.implementationVersion?.toString() ?: "too low",
                        KotlinCompilerVersion.getVersion() ?: "unknown",
                        RuntimeVersions.MINIMAL_SUPPORTED_VERSION.toString(),
                    )
                )
            }
        }
    }

    private fun checkMinKotlin(versions: RuntimeVersions, descriptor: ClassDescriptor, trace: BindingTrace) {
        if (versions.currentCompilerMatchRequired()) return
        descriptor.onSerializableOrMetaAnnotation {
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

    private fun ClassDescriptor.useLegacyGeneratedEnumSerializer(): Boolean {
        return useLegacyEnumSerializerCached ?: useGeneratedEnumSerializer.also { useLegacyEnumSerializerCached = it }
    }

    private fun canBeSerializedInternally(descriptor: ClassDescriptor, declaration: KtDeclaration, trace: BindingTrace): Boolean {
        // if enum has meta or SerialInfo annotation on a class or entries and used plugin-generated serializer
        if (descriptor.useLegacyGeneratedEnumSerializer() && descriptor.isSerializableEnumWithMissingSerializer()) {
            val declarationToReport = declaration.modifierList ?: declaration
            trace.report(SerializationErrors.EXPLICIT_SERIALIZABLE_IS_REQUIRED.on(declarationToReport))
            return false
        }

        checkCompanionSerializerDependency(descriptor, declaration, trace)

        if (!descriptor.hasSerializableOrMetaAnnotation) return false

        if (!serializationPluginEnabledOn(descriptor)) {
            trace.reportOnSerializableOrMetaAnnotation(descriptor, SerializationErrors.PLUGIN_IS_NOT_ENABLED)
            return false
        }

        if (descriptor.isAnonymousObjectOrContained) {
            trace.reportOnSerializableOrMetaAnnotation(descriptor, SerializationErrors.ANONYMOUS_OBJECTS_NOT_SUPPORTED)
            return false
        }

        if (descriptor.isInner) {
            trace.reportOnSerializableOrMetaAnnotation(descriptor, SerializationErrors.INNER_CLASSES_NOT_SUPPORTED)
            return false
        }

        if (descriptor.isInlineClass() && !canSupportInlineClasses(descriptor.module, trace)) {
            descriptor.onSerializableOrMetaAnnotation {
                trace.report(
                    SerializationErrors.INLINE_CLASSES_NOT_SUPPORTED.on(
                        it,
                        RuntimeVersions.MINIMAL_VERSION_FOR_INLINE_CLASSES.toString(),
                        VersionReader.getVersionsForCurrentModuleFromTrace(descriptor.module, trace)?.implementationVersion.toString()
                    )
                )
            }
            return false
        }

        if (!descriptor.hasSerializableOrMetaAnnotationWithoutArgs) {
            // defined custom serializer
            checkClassWithCustomSerializer(descriptor, declaration, trace)
            return false
        }

        if (descriptor.serializableAnnotationIsUseless) {
            trace.reportOnSerializableOrMetaAnnotation(descriptor, SerializationErrors.SERIALIZABLE_ANNOTATION_IGNORED)
            return false
        }

        // check that we can instantiate supertype
        if (descriptor.kind != ClassKind.ENUM_CLASS) { // enums are inherited from java.lang.Enum and can't be inherited from other classes
            val superClass = descriptor.getSuperClassOrAny()
            if (!superClass.isInternalSerializable && superClass.constructors.singleOrNull { it.valueParameters.size == 0 } == null) {
                trace.reportOnSerializableOrMetaAnnotation(descriptor, SerializationErrors.NON_SERIALIZABLE_PARENT_MUST_HAVE_NOARG_CTOR)
                return false
            }
        }
        return true
    }

    private fun checkCompanionSerializerDependency(descriptor: ClassDescriptor, declaration: KtDeclaration, trace: BindingTrace) {
        val companionObjectDescriptor = descriptor.companionObjectDescriptor ?: return
        val serializerForInCompanion = companionObjectDescriptor.serializerForClass ?: return
        val serializerAnnotationSource =
            companionObjectDescriptor.findAnnotationDeclaration(SerializationAnnotations.serializerAnnotationFqName)
        val serializableWith = descriptor.serializableWith
        if (descriptor.hasSerializableOrMetaAnnotationWithoutArgs) {
            if (serializerForInCompanion == descriptor.defaultType) {
                // @Serializable class Foo / @Serializer(Foo::class) companion object — prohibited due to problems with recursive resolve
                descriptor.onSerializableOrMetaAnnotation {
                    trace.report(SerializationErrors.COMPANION_OBJECT_AS_CUSTOM_SERIALIZER_DEPRECATED.on(it, descriptor))
                }
            } else {
                // @Serializable class Foo / @Serializer(Bar::class) companion object — prohibited as vague and confusing
                trace.report(
                    SerializationErrors.COMPANION_OBJECT_SERIALIZER_INSIDE_OTHER_SERIALIZABLE_CLASS.on(
                        serializerAnnotationSource ?: declaration,
                        descriptor.defaultType,
                        serializerForInCompanion
                    )
                )
            }
        } else if (serializableWith != null) {
            if (serializableWith == companionObjectDescriptor.defaultType && serializerForInCompanion == descriptor.defaultType) {
                // @Serializable(Foo.Companion) class Foo / @Serializer(Foo::class) companion object — the only case that is allowed
            } else {
                // @Serializable(anySer) class Foo / @Serializer(anyOtherClass) companion object — prohibited as vague and confusing
                trace.report(
                    SerializationErrors.COMPANION_OBJECT_SERIALIZER_INSIDE_OTHER_SERIALIZABLE_CLASS.on(
                        serializerAnnotationSource ?: declaration,
                        descriptor.defaultType,
                        serializerForInCompanion
                    )
                )
            }
        } else {
            // (regular) class Foo / @Serializer(something) companion object - not recommended
            trace.report(
                SerializationErrors.COMPANION_OBJECT_SERIALIZER_INSIDE_NON_SERIALIZABLE_CLASS.on(
                    serializerAnnotationSource ?: declaration,
                    descriptor.defaultType,
                    serializerForInCompanion
                )
            )
        }
    }

    private fun checkClassWithCustomSerializer(descriptor: ClassDescriptor, declaration: KtDeclaration, trace: BindingTrace) {
        val annotationPsi = descriptor.findSerializableOrMetaAnnotationDeclaration()
        checkCustomSerializerMatch(descriptor.module, descriptor.defaultType, descriptor, annotationPsi, trace, declaration)
        checkCustomSerializerIsNotLocal(descriptor.module, descriptor, trace, declaration)
        checkCustomSerializerNotAbstract(descriptor.module, descriptor.defaultType, descriptor, annotationPsi, trace, declaration)
    }

    private val ClassDescriptor.isAnonymousObjectOrContained: Boolean
        get() {
            var current: DeclarationDescriptor? = this
            while (current != null) {
                if (DescriptorUtils.isAnonymousObject(current)) {
                    return true
                }
                current = current.containingDeclaration
            }
            return false
        }

    private fun checkEnum(descriptor: ClassDescriptor, declaration: KtDeclaration, trace: BindingTrace) {
        if (descriptor.kind != ClassKind.ENUM_CLASS) return

        val entryBySerialName = mutableMapOf<String, ClassDescriptor?>()
        descriptor.enumEntries().forEach { entryDescriptor ->
            val serialNameAnnotation = entryDescriptor.annotations.serialNameAnnotation
            val serialName = entryDescriptor.annotations.serialNameValue ?: entryDescriptor.name.asString()
            val firstEntry = entryBySerialName[serialName]
            if (firstEntry != null) {
                trace.report(
                    SerializationErrors.DUPLICATE_SERIAL_NAME_ENUM.on(
                        serialNameAnnotation?.findAnnotationEntry() ?: firstEntry.annotations.serialNameAnnotation?.findAnnotationEntry()
                        ?: declaration,
                        descriptor.defaultType,
                        serialName,
                        entryDescriptor.name.asString()
                    )
                )
            } else {
                entryBySerialName[serialName] = entryDescriptor
            }
        }

    }

    private fun ClassDescriptor.isSerializableEnumWithMissingSerializer(): Boolean {
        if (kind != ClassKind.ENUM_CLASS) return false
        if (hasSerializableOrMetaAnnotation) return false
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
        if (!descriptor.hasSerializableOrMetaAnnotation) return null
        if (!descriptor.isInternalSerializable) return null
        if (descriptor.hasCompanionObjectAsSerializer) return null // customized by user

        val props = SerializableProperties(descriptor, trace.bindingContext)
        if (!props.isExternallySerializable) trace.reportOnSerializableOrMetaAnnotation(
            descriptor,
            SerializationErrors.PRIMARY_CONSTRUCTOR_PARAMETER_IS_NOT_A_PROPERTY
        )

        // check that all names are unique
        val namesSet = mutableSetOf<String>()
        props.serializableProperties.forEach {
            if (!namesSet.add(it.name)) {
                descriptor.onSerializableOrMetaAnnotation { a ->
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
                checkCustomSerializerNotAbstract(
                    it.module,
                    it.type,
                    it.descriptor,
                    it.descriptor.findSerializableOrMetaAnnotationDeclaration(),
                    trace,
                    propertyPsi
                )
                checkCustomSerializerIsNotLocal(it.module, it.descriptor, trace, propertyPsi)
                checkSerializerNullability(it.type, serializer.defaultType, element, trace, propertyPsi)
                generatorContextForAnalysis.checkTypeArguments(it.module, it.type, element, trace, propertyPsi)
            } else {
                generatorContextForAnalysis.checkType(it.module, it.type, ktType, trace, propertyPsi)
                checkGenericArrayType(it.type, ktType, trace, propertyPsi)
            }
        }
    }

    private fun checkGenericArrayType(
        type: KotlinType,
        ktType: KtTypeReference?,
        trace: BindingTrace,
        fallbackElement: PsiElement
    ) {
        if (KotlinBuiltIns.isArray(type) && type.arguments.first().type.genericIndex != null) {
            // Array<T> is unsupported, since we can't get T::class from KSerializer<T>
            trace.report(SerializationErrors.GENERIC_ARRAY_ELEMENT_NOT_SUPPORTED.on(ktType ?: fallbackElement))
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

    private fun canSupportInlineClasses(module: ModuleDescriptor, trace: BindingTrace): Boolean {
        if (isIde) return true // do not get version from jar manifest in ide
        return VersionReader.canSupportInlineClasses(module, trace)
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
        if (type.isUnsupportedInlineType() && !canSupportInlineClasses(module, trace)) {
            trace.report(
                SerializationErrors.INLINE_CLASSES_NOT_SUPPORTED.on(
                    element ?: fallbackElement,
                    RuntimeVersions.MINIMAL_VERSION_FOR_INLINE_CLASSES.toString(),
                    VersionReader.getVersionsForCurrentModuleFromTrace(module, trace)?.implementationVersion.toString()
                )
            )
        }
        val serializer = findTypeSerializerOrContextUnchecked(module, type)
        if (serializer != null) {
            checkCustomSerializerMatch(module, type, type, element, trace, fallbackElement)
            checkCustomSerializerIsNotLocal(module, type, trace, fallbackElement)
            checkSerializerNullability(type, serializer.defaultType, element, trace, fallbackElement)
            checkTypeArguments(module, type, element, trace, fallbackElement)
        } else {
            if (!type.isEnum()) {
                // enums are always serializable
                trace.report(SerializationErrors.SERIALIZER_NOT_FOUND.on(element ?: fallbackElement, type))
            }
        }
    }

    private fun checkCustomSerializerNotAbstract(
        module: ModuleDescriptor,
        classType: KotlinType,
        descriptor: Annotated,
        element: KtElement?,
        trace: BindingTrace,
        fallbackElement: PsiElement
    ) {
        val serializerType = descriptor.annotations.serializableWith(module) ?: return
        if (serializerType.toClassDescriptor?.isAbstractOrSealedOrInterface == true) {
            trace.report(
                SerializationErrors.ABSTRACT_SERIALIZER_TYPE.on(
                    element ?: fallbackElement,
                    classType,
                    serializerType
                )
            )
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

    private fun checkCustomSerializerIsNotLocal(
        module: ModuleDescriptor,
        declaration: Annotated,
        trace: BindingTrace,
        declarationElement: PsiElement
    ) {
        val serializerType = declaration.annotations.serializableWith(module) ?: return
        val serializerDescriptor = serializerType.toClassDescriptor ?: return

        if (DescriptorUtils.isLocal(serializerDescriptor)) {
            val element = declaration.findSerializableOrMetaAnnotationDeclaration() ?: declarationElement

            trace.report(
                SerializationErrors.LOCAL_SERIALIZER_USAGE.on(
                    element,
                    serializerType
                )
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

        val serializerForType = castedToKSerial.arguments.first().type
        if (!classType.isMarkedNullable && serializerForType.isMarkedNullable)
            trace.report(
                SerializationErrors.SERIALIZER_NULLABILITY_INCOMPATIBLE.on(element ?: fallbackElement, serializerType, classType),
            )
    }

    private inline fun ClassDescriptor.onSerializableOrMetaAnnotation(report: (KtAnnotationEntry) -> Unit) {
        findSerializableOrMetaAnnotationDeclaration()?.let(report)
    }

    private fun BindingTrace.reportOnSerializableOrMetaAnnotation(
        descriptor: ClassDescriptor,
        error: DiagnosticFactory0<in KtAnnotationEntry>
    ) {
        descriptor.onSerializableOrMetaAnnotation { e ->
            report(error.on(e))
        }
    }
}

val ClassDescriptor.serializableAnnotationIsUseless: Boolean
    get() = hasSerializableOrMetaAnnotationWithoutArgs && !isInternalSerializable && !hasCompanionObjectAsSerializer && kind != ClassKind.ENUM_CLASS && !isSealedSerializableInterface
