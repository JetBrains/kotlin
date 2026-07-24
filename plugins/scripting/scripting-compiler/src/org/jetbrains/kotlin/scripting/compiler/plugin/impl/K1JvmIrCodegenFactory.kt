/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContextImpl
import org.jetbrains.kotlin.backend.common.linkage.issues.checkNoUnboundSymbols
import org.jetbrains.kotlin.backend.common.serialization.DescriptorByIdSignatureFinderImpl
import org.jetbrains.kotlin.backend.jvm.*
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory.BackendInput
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory.IdeCodegenSettings
import org.jetbrains.kotlin.backend.jvm.mapping.IrTypeMapper
import org.jetbrains.kotlin.backend.jvm.mapping.mapClass
import org.jetbrains.kotlin.backend.jvm.metadata.MetadataSerializer
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.ClassBuilderMode
import org.jetbrains.kotlin.codegen.createFreeFakeLambdaDescriptor
import org.jetbrains.kotlin.codegen.createFreeFakeLocalPropertyDescriptor
import org.jetbrains.kotlin.codegen.serialization.JvmSignatureSerializer
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapperBase
import org.jetbrains.kotlin.compiler.plugin.getCompilerExtensions
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.idea.MainFunctionDetector
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.load.java.DescriptorsJvmAbiUtil
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.lazy.types.RawTypeImpl
import org.jetbrains.kotlin.load.kotlin.NON_EXISTENT_CLASS_NAME
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.jvm.JvmProtoBuf
import org.jetbrains.kotlin.metadata.jvm.deserialization.ClassMapperLite
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmFlags
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.metadata.jvm.serialization.JvmStringTable
import org.jetbrains.kotlin.metadata.serialization.MutableVersionRequirementTable
import org.jetbrains.kotlin.metadata.serialization.StringTable
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.JvmStandardClassIds.JVM_DEFAULT_WITHOUT_COMPATIBILITY_FQ_NAME
import org.jetbrains.kotlin.name.JvmStandardClassIds.JVM_DEFAULT_WITH_COMPATIBILITY_FQ_NAME
import org.jetbrains.kotlin.protobuf.GeneratedMessageLite
import org.jetbrains.kotlin.protobuf.MessageLite
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.psi2ir.generators.DeclarationStubGeneratorForNotFoundClasses
import org.jetbrains.kotlin.psi2ir.generators.DeclarationStubGeneratorImpl
import org.jetbrains.kotlin.psi2ir.preprocessing.SourceDeclarationsPreprocessor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.CleanableBindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.nonSourceAnnotations
import org.jetbrains.kotlin.resolve.jvm.requiresFunctionNameManglingForParameterTypes
import org.jetbrains.kotlin.resolve.jvm.requiresFunctionNameManglingForReturnType
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.K1JvmSerializationBindings.*
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.JvmGeneratorExtensionsImpl
import org.jetbrains.kotlin.serialization.DescriptorSerializer
import org.jetbrains.kotlin.serialization.SerializableStringTable
import org.jetbrains.kotlin.serialization.SerializerExtension
import org.jetbrains.kotlin.serialization.VersionRequirementUtils
import org.jetbrains.kotlin.types.FlexibleType
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeApproximator
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.Method

class K1JvmIrCodegenFactory(
    private val configuration: CompilerConfiguration,
    private val externalMangler: K1JvmDescriptorMangler? = null,
    private val externalSymbolTable: SymbolTable? = null,
    private val jvmGeneratorExtensions: JvmGeneratorExtensionsImpl = JvmGeneratorExtensionsImpl(configuration),
    private val ideCodegenSettings: IdeCodegenSettings = IdeCodegenSettings(),
) {
    val normalFactory: JvmIrCodegenFactory = JvmIrCodegenFactory(
        configuration,
        ideCodegenSettings
    )

    @K1Deprecation
    fun convertToIr(state: GenerationState, files: Collection<KtFile>, bindingContext: BindingContext): BackendInput = with(state) {
        convertToIr(
            files, configuration, module, diagnosticReporter, bindingContext, config.languageVersionSettings,
            skipBodies = !classBuilderMode.generateBodies
        )
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    @K1Deprecation
    fun convertToIr(
        files: Collection<KtFile>,
        configuration: CompilerConfiguration,
        module: ModuleDescriptor,
        diagnosticReporter: DiagnosticReporter,
        bindingContext: BindingContext,
        languageVersionSettings: LanguageVersionSettings,
        skipBodies: Boolean,
    ): BackendInput {
        val [mangler, symbolTable] =
            if (externalSymbolTable != null) externalMangler!! to externalSymbolTable
            else {
                val mangler = K1JvmDescriptorMangler(MainFunctionDetector(bindingContext, languageVersionSettings))
                val signaturer = DisabledIdSignatureDescriptor
                val symbolTable = SymbolTable(signaturer, IrFactoryImpl)
                mangler to symbolTable
            }
        val psi2ir = Psi2IrTranslator(
            languageVersionSettings,
            Psi2IrConfiguration(ignoreErrors = false, skipBodies),
            configuration::checkNoUnboundSymbols
        )
        val psi2irContext = psi2ir.createGeneratorContext(
            module,
            bindingContext,
            configuration,
            symbolTable,
            jvmGeneratorExtensions,
            fragmentContext = null,
        )

        // Built-ins deduplication must be enabled immediately so that there is no chance for duplicate built-in symbols to occur. For
        // example, the creation of `IrPluginContextImpl` might already lead to duplicate built-in symbols via `BuiltinSymbolsBase`.
        if (symbolTable is K1SymbolTableWithBuiltInsDeduplication) {
            symbolTable.bindBuiltIns(psi2irContext.moduleDescriptor.builtIns)
        }

        val stubGenerator =
            DeclarationStubGeneratorImpl(
                psi2irContext.moduleDescriptor, symbolTable, psi2irContext.irBuiltIns,
                DescriptorByIdSignatureFinderImpl(psi2irContext.moduleDescriptor, mangler),
                jvmGeneratorExtensions
            )

        SourceDeclarationsPreprocessor(psi2irContext).run(files)

        // The plugin context contains unbound symbols right after construction and has to be
        // instantiated before we resolve unbound symbols and invoke any postprocessing steps.
        val pluginContext = IrPluginContextImpl(
            psi2irContext.moduleDescriptor,
            psi2irContext.languageVersionSettings,
            symbolTable,
            psi2irContext.irBuiltIns,
            stubGenerator,
            diagnosticReporter
        )
        for (extension in configuration.filteredExtensions) {
            if (!psi2irContext.configuration.generateBodies &&
                !@OptIn(FirIncompatiblePluginAPI::class) extension.shouldAlsoBeAppliedInKaptStubGenerationMode
            ) continue

            psi2ir.addPostprocessingStep { module ->
                val old = stubGenerator.unboundSymbolGeneration
                try {
                    stubGenerator.unboundSymbolGeneration = true
                    extension.generate(module, pluginContext)
                } finally {
                    stubGenerator.unboundSymbolGeneration = old
                }
            }
        }

        val irProviders = if (ideCodegenSettings.shouldStubAndNotLinkUnboundSymbols) {
            listOf(stubGenerator)
        } else {
            val stubGeneratorForMissingClasses = DeclarationStubGeneratorForNotFoundClasses(stubGenerator)
            listOf(stubGenerator, stubGeneratorForMissingClasses)
        }

        val irModuleFragment = psi2ir.generateModuleFragment(psi2irContext, files, irProviders)

        stubGenerator.unboundSymbolGeneration = true

        if (!configuration.getBoolean(JVMConfigurationKeys.DO_NOT_CLEAR_BINDING_CONTEXT) && files.none { it.isScript() }) {
            if (bindingContext !is CleanableBindingContext) {
                error("BindingContext should be cleanable in JVM IR to avoid leaking memory: $bindingContext")
            }
            bindingContext.clear()
        }
        return BackendInput(
            irModuleFragment,
            psi2irContext.irBuiltIns,
            symbolTable,
            irProviders,
            debuggerExtensions = null,
            K1JvmBackendExtension(),
            pluginContext,
        )
    }

    private val CompilerConfiguration.filteredExtensions: List<IrGenerationExtension>
        get() = this.getCompilerExtensions(IrGenerationExtension)
}

private class K1JvmBackendExtension : JvmBackendExtension {
    private val globalSerializationBindings = K1JvmSerializationBindings()
    private val serializationBindings = HashMap<ClassBuilder, K1JvmSerializationBindings>()

    override fun createSerializer(
        context: JvmBackendContext, klass: IrClass, type: Type, classBuilder: ClassBuilder, parentSerializer: MetadataSerializer?,
    ): MetadataSerializer = DescriptorMetadataSerializer(
        context, klass, type, serializationBindings.getOrPut(classBuilder) { K1JvmSerializationBindings() },
        globalSerializationBindings, parentSerializer,
    )

    override fun createModuleMetadataSerializer(context: JvmBackendContext): ModuleMetadataSerializer = object : ModuleMetadataSerializer {
        override fun serializeOptionalAnnotationClass(
            metadata: MetadataSource.Class,
            stringTable: SerializableStringTable,
        ): ProtoBuf.Class {
            error("K1 mode is no longer supported")
        }
    }

    override fun createBuiltinsSerializer() = error("JVM backend builtins serialization is not supported in K1")
}

private class DescriptorMetadataSerializer(
    private val context: JvmBackendContext,
    private val irClass: IrClass,
    private val type: Type,
    private val serializationBindings: K1JvmSerializationBindings,
    private val globalSerializationBindings: K1JvmSerializationBindings,
    parent: MetadataSerializer?
) : MetadataSerializer {
    @OptIn(K1Deprecation::class)
    private val typeApproximator =
        TypeApproximator(context.state.module.builtIns, context.state.config.languageVersionSettings)

    private val serializerExtension = JvmSerializerExtension(
        serializationBindings, globalSerializationBindings, context.state, context.defaultTypeMapper, typeApproximator,
    )

    @OptIn(K1Deprecation::class)
    private val serializer: DescriptorSerializer? = run {
        val languageVersionSettings = context.config.languageVersionSettings
        when (val metadata = irClass.metadata) {
            is DescriptorMetadataSource.Class -> DescriptorSerializer.create(
                metadata.descriptor, serializerExtension, (parent as? DescriptorMetadataSerializer)?.serializer,
                languageVersionSettings, context.state.project,
            )
            is DescriptorMetadataSource.Script -> DescriptorSerializer.create(
                metadata.descriptor, serializerExtension, (parent as? DescriptorMetadataSerializer)?.serializer,
                languageVersionSettings, context.state.project,
            )
            is DescriptorMetadataSource.File -> DescriptorSerializer.createTopLevel(
                serializerExtension, languageVersionSettings,
                context.state.project
            )
            is DescriptorMetadataSource.Function -> DescriptorSerializer.createForLambda(serializerExtension, languageVersionSettings)
            else -> null
        }
    }

    override fun serialize(metadata: MetadataSource, containingFile: MetadataSource.File?): Pair<MessageLite, JvmStringTable>? {
        val localDelegatedProperties = irClass.localDelegatedProperties
        if (localDelegatedProperties != null && localDelegatedProperties.isNotEmpty()) {
            serializerExtension.localDelegatedProperties.put(
                // key for local delegated properties metadata in interfaces depends on jvmDefaultMode
                if (irClass.isInterface && !context.config.jvmDefaultMode.isEnabled) context.defaultTypeMapper.mapClass(
                    context.cachedDeclarations.getDefaultImplsClass(irClass)
                ) else type,
                @OptIn(UnsafeDuringIrConstructionAPI::class)
                localDelegatedProperties.mapNotNull { (it.owner.metadata as? DescriptorMetadataSource.LocalDelegatedProperty)?.descriptor }
            )
        }

        @OptIn(K1Deprecation::class)
        val message = when (metadata) {
            is DescriptorMetadataSource.Class -> serializer!!.classProto(metadata.descriptor).build()
            is DescriptorMetadataSource.Script -> serializer!!.classProto(metadata.descriptor).build()
            is DescriptorMetadataSource.File ->
                serializer!!.packagePartProto(irClass.getPackageFragment().packageFqName, metadata.descriptors).apply {
                    serializerExtension.serializeJvmPackage(this, type)
                }.build()
            is DescriptorMetadataSource.Function -> {
                val withTypeParameters = createFreeFakeLambdaDescriptor(metadata.descriptor, typeApproximator)
                serializationBindings.get(METHOD_FOR_FUNCTION, metadata.descriptor)?.let {
                    serializationBindings.put(METHOD_FOR_FUNCTION, withTypeParameters, it)
                }
                serializer!!.functionProto(withTypeParameters)?.build()
            }
            else -> null
        } ?: return null
        @OptIn(K1Deprecation::class)
        return message to serializer!!.stringTable as JvmStringTable
    }

    override fun bindPropertyMetadata(metadata: MetadataSource.Property, signature: Method, origin: IrDeclarationOrigin) {
        val descriptor = (metadata as DescriptorMetadataSource.Property).descriptor
        val slice = when (origin) {
            JvmLoweredDeclarationOrigin.SYNTHETIC_METHOD_FOR_PROPERTY_OR_TYPEALIAS_ANNOTATIONS ->
                SYNTHETIC_METHOD_FOR_PROPERTY
            IrDeclarationOrigin.PROPERTY_DELEGATE ->
                DELEGATE_METHOD_FOR_PROPERTY
            else -> throw IllegalStateException("invalid origin $origin for property-related method $signature")
        }
        globalSerializationBindings.put(slice, descriptor, signature)
    }

    override fun bindMethodMetadata(metadata: MetadataSource.Function, signature: Method) {
        val descriptor = (metadata as DescriptorMetadataSource.Function).descriptor
        serializationBindings.put(METHOD_FOR_FUNCTION, descriptor, signature)
    }

    override fun bindFieldMetadata(metadata: MetadataSource.Property, signature: Pair<Type, String>) {
        val descriptor = (metadata as DescriptorMetadataSource.Property).descriptor
        globalSerializationBindings.put(FIELD_FOR_PROPERTY, descriptor, signature)
    }
}

private class JvmSerializerExtension(
    private val bindings: K1JvmSerializationBindings,
    private val globalBindings: K1JvmSerializationBindings,
    state: GenerationState,
    private val typeMapper: KotlinTypeMapperBase,
    private val approximator: TypeApproximator,
) : SerializerExtension() {
    override val stringTable = K1JvmCodegenStringTable(typeMapper)
    private val useTypeTable = state.config.useTypeTableInSerializer
    private val moduleName = state.moduleName
    private val classBuilderMode = state.classBuilderMode
    private val languageVersionSettings = state.config.languageVersionSettings
    private val isParamAssertionsDisabled = state.config.isParamAssertionsDisabled
    private val unifiedNullChecks = state.config.unifiedNullChecks
    private val functionsWithInlineClassReturnTypesMangled = state.config.functionsWithInlineClassReturnTypesMangled
    override val metadataVersion = state.config.metadataVersion
    private val jvmDefaultMode = state.config.jvmDefaultMode
    private val useOldManglingScheme = state.config.useOldManglingSchemeForFunctionsWithInlineClassesInSignatures
    private val signatureSerializer = JvmSignatureSerializerImpl(stringTable)
    internal val localDelegatedProperties: MutableMap<Type, List<VariableDescriptorWithAccessors>> = mutableMapOf()

    override fun shouldUseTypeTable(): Boolean = useTypeTable

    override fun serializeClass(
        descriptor: ClassDescriptor,
        proto: ProtoBuf.Class.Builder,
        versionRequirementTable: MutableVersionRequirementTable,
        childSerializer: DescriptorSerializer
    ) {
        if (moduleName != JvmProtoBufUtil.DEFAULT_MODULE_NAME) {
            proto.setExtension(JvmProtoBuf.classModuleName, stringTable.getStringIndex(moduleName))
        }
        //TODO: support local delegated properties in new defaults scheme
        val containerAsmType =
            if (DescriptorUtils.isInterface(descriptor) && !jvmDefaultMode.isEnabled)
                Type.getObjectType(typeMapper.mapClass(descriptor).internalName + JvmAbi.DEFAULT_IMPLS_SUFFIX)
            else typeMapper.mapClass(descriptor)
        writeLocalProperties(proto, containerAsmType, JvmProtoBuf.classLocalVariable)
        writeVersionRequirementForJvmDefaultIfNeeded(descriptor, proto, versionRequirementTable)

        if (jvmDefaultMode.isEnabled && DescriptorUtils.isInterface(descriptor)) {
            proto.setExtension(JvmProtoBuf.jvmClassFlags, JvmFlags.getClassFlags(true, isInCompatibilityMode(descriptor)))
        }
    }

    private fun isInCompatibilityMode(descriptor: ClassDescriptor): Boolean {
        val annotations = descriptor.annotations
        return (jvmDefaultMode == JvmDefaultMode.ENABLE && !annotations.hasAnnotation(JVM_DEFAULT_WITHOUT_COMPATIBILITY_FQ_NAME)) ||
                (jvmDefaultMode == JvmDefaultMode.NO_COMPATIBILITY && annotations.hasAnnotation(JVM_DEFAULT_WITH_COMPATIBILITY_FQ_NAME))
    }

    // Interfaces which have @JvmDefault members somewhere in the hierarchy need the compiler 1.2.40+
    // so that the generated bridges in subclasses would call the super members correctly
    private fun writeVersionRequirementForJvmDefaultIfNeeded(
        classDescriptor: ClassDescriptor,
        builder: ProtoBuf.Class.Builder,
        versionRequirementTable: MutableVersionRequirementTable
    ) {
        if (DescriptorUtils.isInterface(classDescriptor)) {
            if (jvmDefaultMode == JvmDefaultMode.NO_COMPATIBILITY) {
                builder.addVersionRequirement(
                    VersionRequirementUtils.writeVersionRequirement(
                        major = 1, minor = 4, patch = 0,
                        ProtoBuf.VersionRequirement.VersionKind.COMPILER_VERSION, versionRequirementTable
                    )
                )
            }
        }
    }

    override fun serializePackage(packageFqName: FqName, proto: ProtoBuf.Package.Builder) {
        if (moduleName != JvmProtoBufUtil.DEFAULT_MODULE_NAME) {
            proto.setExtension(JvmProtoBuf.packageModuleName, stringTable.getStringIndex(moduleName))
        }
    }

    fun serializeJvmPackage(proto: ProtoBuf.Package.Builder, partAsmType: Type) {
        writeLocalProperties(proto, partAsmType, JvmProtoBuf.packageLocalVariable)
    }

    private fun <MessageType : GeneratedMessageLite.ExtendableMessage<MessageType>, BuilderType : GeneratedMessageLite.ExtendableBuilder<MessageType, BuilderType>> writeLocalProperties(
        proto: BuilderType,
        classAsmType: Type,
        extension: GeneratedMessageLite.GeneratedExtension<MessageType, List<ProtoBuf.Property>>
    ) {
        for (localVariable in localDelegatedProperties[classAsmType].orEmpty()) {
            if (localVariable !is LocalVariableDescriptor) continue
            val propertyDescriptor = createFreeFakeLocalPropertyDescriptor(localVariable, approximator)
            val serializer = DescriptorSerializer.createForLambda(this, languageVersionSettings)
            proto.addExtension(extension, serializer.propertyProto(propertyDescriptor)?.build() ?: continue)
        }
    }

    override fun serializeFlexibleType(
        flexibleType: FlexibleType,
        lowerProto: ProtoBuf.Type.Builder,
        upperProto: ProtoBuf.Type.Builder
    ) {
        lowerProto.flexibleTypeCapabilitiesId = stringTable.getStringIndex(JvmProtoBufUtil.PLATFORM_TYPE_ID)

        if (flexibleType is RawTypeImpl) {
            lowerProto.setExtension(JvmProtoBuf.isRaw, true)

            // we write this Extension for compatibility with old compiler
            upperProto.setExtension(JvmProtoBuf.isRaw, true)
        }
    }

    override fun serializeType(type: KotlinType, proto: ProtoBuf.Type.Builder) {
        // TODO: don't store type annotations in our binary metadata on Java 8, use *TypeAnnotations attributes instead
        for (annotation in type.nonSourceAnnotations) {
            proto.addAnnotation(annotationSerializer.serializeAnnotation(annotation))
        }
    }

    override fun serializeTypeParameter(typeParameter: TypeParameterDescriptor, proto: ProtoBuf.TypeParameter.Builder) {
        for (annotation in typeParameter.nonSourceAnnotations) {
            proto.addAnnotation(annotationSerializer.serializeAnnotation(annotation))
        }
    }

    override fun serializeConstructor(
        descriptor: ConstructorDescriptor, proto: ProtoBuf.Constructor.Builder, childSerializer: DescriptorSerializer
    ) {
        val method = getBinding(METHOD_FOR_FUNCTION, descriptor)
        if (method != null) {
            val signature = signatureSerializer.methodSignature(descriptor, descriptor.name, method)
            if (signature != null) {
                proto.setExtension(JvmProtoBuf.constructorSignature, signature)
            }
        }
    }

    override fun serializeFunction(
        descriptor: FunctionDescriptor,
        proto: ProtoBuf.Function.Builder,
        versionRequirementTable: MutableVersionRequirementTable?,
        childSerializer: DescriptorSerializer
    ) {
        val method = getBinding(METHOD_FOR_FUNCTION, descriptor)
        if (method != null) {
            val signature = signatureSerializer.methodSignature(descriptor, descriptor.name, method)
            if (signature != null) {
                proto.setExtension(JvmProtoBuf.methodSignature, signature)
            }
        }

        if (descriptor.needsInlineParameterNullCheckRequirement()) {
            versionRequirementTable?.writeInlineParameterNullCheckRequirement(proto::addVersionRequirement)
        }

        if (requiresFunctionNameManglingForReturnType(descriptor) &&
            !DescriptorUtils.hasJvmNameAnnotation(descriptor) &&
            !requiresFunctionNameManglingForParameterTypes(descriptor)
        ) {
            versionRequirementTable?.writeFunctionNameManglingForReturnTypeRequirement(proto::addVersionRequirement)
        }

        if ((requiresFunctionNameManglingForReturnType(descriptor) ||
                    requiresFunctionNameManglingForParameterTypes(descriptor)) &&
            !DescriptorUtils.hasJvmNameAnnotation(descriptor) && !useOldManglingScheme
        ) {
            versionRequirementTable?.writeNewFunctionNameManglingRequirement(proto::addVersionRequirement)
        }
    }

    private fun MutableVersionRequirementTable.writeInlineParameterNullCheckRequirement(add: (Int) -> Unit) {
        if (unifiedNullChecks) {
            // Since Kotlin 1.4, we generate a call to Intrinsics.checkNotNullParameter in inline functions which causes older compilers
            // (earlier than 1.3.50) to crash because a functional parameter in this position can't be inlined
            add(
                VersionRequirementUtils.writeVersionRequirement(
                    1, 3, 50, ProtoBuf.VersionRequirement.VersionKind.COMPILER_VERSION, this
                )
            )
        }
    }

    private fun MutableVersionRequirementTable.writeFunctionNameManglingForReturnTypeRequirement(add: (Int) -> Unit) {
        if (functionsWithInlineClassReturnTypesMangled) {
            add(
                VersionRequirementUtils.writeVersionRequirement(
                    1, 4, 0, ProtoBuf.VersionRequirement.VersionKind.LANGUAGE_VERSION, this
                )
            )
        }
    }

    private fun MutableVersionRequirementTable.writeNewFunctionNameManglingRequirement(add: (Int) -> Unit) {
        add(
            VersionRequirementUtils.writeVersionRequirement(
                1, 4, 30, ProtoBuf.VersionRequirement.VersionKind.COMPILER_VERSION, this
            )
        )
    }

    private fun FunctionDescriptor.needsInlineParameterNullCheckRequirement(): Boolean =
        isInline && !isSuspend && !isParamAssertionsDisabled &&
                !DescriptorVisibilities.isPrivate(visibility) &&
                (valueParameters.any { it.type.isFunctionType } || extensionReceiverParameter?.type?.isFunctionType == true)

    override fun serializeProperty(
        descriptor: PropertyDescriptor,
        proto: ProtoBuf.Property.Builder,
        versionRequirementTable: MutableVersionRequirementTable?,
        childSerializer: DescriptorSerializer
    ) {
        val getter = descriptor.getter
        val setter = descriptor.setter
        val getterMethod = if (getter == null) null else getBinding(METHOD_FOR_FUNCTION, getter)
        val setterMethod = if (setter == null) null else getBinding(METHOD_FOR_FUNCTION, setter)

        val field = getBinding(FIELD_FOR_PROPERTY, descriptor)
        val syntheticMethod = getBinding(SYNTHETIC_METHOD_FOR_PROPERTY, descriptor)
        val delegateMethod = getBinding(DELEGATE_METHOD_FOR_PROPERTY, descriptor)
        assert(descriptor.isDelegated || delegateMethod == null) { "non-delegated property $descriptor has delegate method" }

        val signature = signatureSerializer.propertySignature(
            descriptor.name,
            field?.second,
            field?.first?.descriptor,
            if (syntheticMethod != null) signatureSerializer.methodSignature(null, null, syntheticMethod) else null,
            if (delegateMethod != null) signatureSerializer.methodSignature(null, null, delegateMethod) else null,
            if (getterMethod != null) signatureSerializer.methodSignature(null, null, getterMethod) else null,
            if (setterMethod != null) signatureSerializer.methodSignature(null, null, setterMethod) else null,
            field?.first?.descriptor?.let { signatureSerializer.requiresPropertySignature(descriptor, it) } ?: false,
        )

        if (signature != null) {
            proto.setExtension(JvmProtoBuf.propertySignature, signature)
        }

        if (descriptor.isJvmFieldPropertyInInterfaceCompanion() && versionRequirementTable != null) {
            proto.setExtension(JvmProtoBuf.flags, JvmFlags.getPropertyFlags(true))
        }

        if (getter?.needsInlineParameterNullCheckRequirement() == true || setter?.needsInlineParameterNullCheckRequirement() == true) {
            versionRequirementTable?.writeInlineParameterNullCheckRequirement(proto::addVersionRequirement)
        }

        if (!DescriptorUtils.hasJvmNameAnnotation(descriptor) && requiresFunctionNameManglingForReturnType(descriptor)) {
            if (!useOldManglingScheme) {
                versionRequirementTable?.writeNewFunctionNameManglingRequirement(proto::addVersionRequirement)
            }
            versionRequirementTable?.writeFunctionNameManglingForReturnTypeRequirement(proto::addVersionRequirement)
        }
    }

    private fun PropertyDescriptor.isJvmFieldPropertyInInterfaceCompanion(): Boolean {
        if (!DescriptorsJvmAbiUtil.hasJvmFieldAnnotation(this)) return false

        val container = containingDeclaration
        if (!DescriptorUtils.isCompanionObject(container)) return false

        val grandParent = (container as ClassDescriptor).containingDeclaration
        return DescriptorUtils.isInterface(grandParent) || DescriptorUtils.isAnnotationClass(grandParent)
    }

    override fun serializeErrorType(type: KotlinType, builder: ProtoBuf.Type.Builder) {
        if (classBuilderMode === ClassBuilderMode.KAPT3) {
            builder.className = stringTable.getStringIndex(NON_EXISTENT_CLASS_NAME)
            return
        }

        super.serializeErrorType(type, builder)
    }

    private fun <K : Any, V> getBinding(slice: SerializationMappingSlice<K, V>, key: K): V? =
        bindings.get(slice, key) ?: globalBindings.get(slice, key)
}

private class JvmSignatureSerializerImpl(stringTable: StringTable) : JvmSignatureSerializer<FunctionDescriptor, PropertyDescriptor>(stringTable) {
    // We don't write those signatures which can be trivially reconstructed from already serialized data
    // TODO: make JvmStringTable implement NameResolver and use JvmProtoBufUtil#getJvmMethodSignature instead
    override fun requiresFunctionSignature(descriptor: FunctionDescriptor, desc: String): Boolean {
        val sb = StringBuilder()
        sb.append("(")
        val receiverParameter = descriptor.extensionReceiverParameter
        if (receiverParameter != null) {
            val receiverDesc = mapTypeDefault(receiverParameter.value.type) ?: return true
            sb.append(receiverDesc)
        }

        for (valueParameter in descriptor.valueParameters) {
            val paramDesc = mapTypeDefault(valueParameter.type) ?: return true
            sb.append(paramDesc)
        }

        sb.append(")")

        val returnType = descriptor.returnType
        val returnTypeDesc = (if (returnType == null) "V" else mapTypeDefault(returnType)) ?: return true
        sb.append(returnTypeDesc)

        return sb.toString() != desc
    }

    override fun requiresPropertySignature(descriptor: PropertyDescriptor, desc: String): Boolean {
        return desc != mapTypeDefault(descriptor.type)
    }

    private fun mapTypeDefault(type: KotlinType): String? {
        val classifier = type.constructor.declarationDescriptor as? ClassDescriptor ?: return null
        val classId = classifier.classId
        return if (classId == null) null else ClassMapperLite.mapClass(classId.asString())
    }
}

/**
 * [ExpectSymbolTransformer] replaces `expect` symbols in expressions with `actual` symbols. An `actual` symbol must be provided by
 * overriding [getActualClass], [getActualProperty], [getActualConstructor], and [getActualFunction].
 */
@OptIn(ObsoleteDescriptorBasedAPI::class, UnsafeDuringIrConstructionAPI::class)
abstract class ExpectSymbolTransformer : IrVisitorVoid() {

    protected abstract fun getActualClass(descriptor: ClassDescriptor): IrClassSymbol?

    protected data class ActualPropertyResult(
        val propertySymbol: IrPropertySymbol,
        val getterSymbol: IrSimpleFunctionSymbol?,
        val setterSymbol: IrSimpleFunctionSymbol?,
    )

    protected abstract fun getActualProperty(descriptor: PropertyDescriptor): ActualPropertyResult?

    protected abstract fun getActualConstructor(descriptor: ClassConstructorDescriptor): IrConstructorSymbol?

    protected abstract fun getActualFunction(descriptor: FunctionDescriptor): IrSimpleFunctionSymbol?

    /**
     * [isTargetDeclaration] can be overridden to customize if an element referring to [declaration] should be transformed. This check
     * precedes [getActualClass], [getActualProperty], and so on.
     */
    protected open fun isTargetDeclaration(declaration: IrDeclaration): Boolean = declaration.isExpect

    override fun visitElement(element: IrElement) {
        element.acceptChildren(this, null)
    }

    override fun visitConstructorCall(expression: IrConstructorCall) {
        super.visitConstructorCall(expression)
        if (!isTargetDeclaration(expression.symbol.owner)) return

        expression.symbol = getActualConstructor(expression.symbol.descriptor) ?: return
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall) {
        super.visitDelegatingConstructorCall(expression)
        if (!isTargetDeclaration(expression.symbol.owner)) return

        expression.symbol = getActualConstructor(expression.symbol.descriptor) ?: return
    }

    override fun visitEnumConstructorCall(expression: IrEnumConstructorCall) {
        super.visitEnumConstructorCall(expression)
        if (!isTargetDeclaration(expression.symbol.owner)) return

        expression.symbol = getActualConstructor(expression.symbol.descriptor) ?: return
    }

    override fun visitCall(expression: IrCall) {
        super.visitCall(expression)
        if (!isTargetDeclaration(expression.symbol.owner)) return

        expression.symbol = getActualFunction(expression.symbol.descriptor) ?: return
    }

    override fun visitPropertyReference(expression: IrPropertyReference) {
        super.visitPropertyReference(expression)
        if (!isTargetDeclaration(expression.symbol.owner)) return

        (val newSymbol = propertySymbol, val newGetter = getterSymbol, val newSetter = setterSymbol) = getActualProperty(expression.symbol.descriptor)
            ?: return
        expression.symbol = newSymbol
        expression.getter = newGetter
        expression.setter = newSetter
    }

    override fun visitFunctionReference(expression: IrFunctionReference) {
        super.visitFunctionReference(expression)
        if (!isTargetDeclaration(expression.symbol.owner)) return

        expression.symbol = getActualFunction(expression.symbol.descriptor) ?: return
    }

    override fun visitClassReference(expression: IrClassReference) {
        super.visitClassReference(expression)
        val oldSymbol = expression.symbol as? IrClassSymbol ?: return
        if (!isTargetDeclaration(oldSymbol.owner)) return

        expression.symbol = getActualClass(oldSymbol.descriptor) ?: return
    }
}

internal fun KotlinTypeMapperBase.mapClass(classifier: ClassifierDescriptor): Type {
    this as IrTypeMapper
    return when (classifier) {
        is ClassDescriptor -> mapClass(context.referenceClass(classifier).owner)
        is TypeParameterDescriptor -> mapType(context.referenceTypeParameter(classifier).defaultType)
        else -> error("Unknown descriptor: $classifier")
    }
}
