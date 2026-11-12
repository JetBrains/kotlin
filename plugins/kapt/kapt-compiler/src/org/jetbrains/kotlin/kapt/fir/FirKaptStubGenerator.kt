/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.fir

import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.backend.common.output.OutputFile
import org.jetbrains.kotlin.backend.common.output.OutputFileCollection
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.codegen.ModuleNameUtil
import org.jetbrains.kotlin.codegen.PackagePartRegistry
import org.jetbrains.kotlin.codegen.addCompiledPartsAndSort
import org.jetbrains.kotlin.codegen.serializeModuleMapping
import org.jetbrains.kotlin.codegen.signature.BothSignatureWriter
import org.jetbrains.kotlin.codegen.signature.JvmSignatureWriter
import org.jetbrains.kotlin.codegen.signature.WritingParameterToGenericSignatureMode
import org.jetbrains.kotlin.codegen.state.isMethodWithDeclarationSiteWildcardsFqName
import org.jetbrains.kotlin.codegen.state.extractTypeMappingModeFromAnnotation
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.MavenComparableVersion
import org.jetbrains.kotlin.config.JvmDefaultMode
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.incrementalCompilationComponents
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.jvmDefaultMode
import org.jetbrains.kotlin.config.jvmTarget
import org.jetbrains.kotlin.config.moduleName
import org.jetbrains.kotlin.config.noNewJavaAnnotationTargets
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.java.JavaVisibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.isNewPlaceForBodyGeneration
import org.jetbrains.kotlin.fir.resolve.lookupSuperTypes
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.declarations.comparators.FirCallableDeclarationComparator
import org.jetbrains.kotlin.fir.declarations.comparators.FirMemberDeclarationComparator
import org.jetbrains.kotlin.fir.extensions.declarationGenerators
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.extensions.generatedMembers
import org.jetbrains.kotlin.fir.extensions.generatedNestedClassifiers
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.resultOrNull
import org.jetbrains.kotlin.fir.references.toResolvedEnumEntrySymbol
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.util.getChildren
import org.jetbrains.kotlin.util.metadataVersion
import org.jetbrains.kotlin.fir.backend.FirMetadataSource
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmTypeMapper
import org.jetbrains.kotlin.fir.backend.jvm.FirMetadataSerializer
import org.jetbrains.kotlin.fir.backend.jvm.makeLocalFirMetadataSerializerForMetadataSource
import org.jetbrains.kotlin.fir.backend.utils.startOffsetSkippingComments
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.unwrapFakeOverrides
import org.jetbrains.kotlin.fir.unwrapFakeOverridesOrDelegated
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.FirExpressionEvaluator
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.kapt.base.stubs.KotlinPosition
import org.jetbrains.kotlin.kapt.stubs.KaptStubOrigin
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.load.kotlin.NON_EXISTENT_CLASS_NAME
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.load.kotlin.getOptimalModeForReturnType
import org.jetbrains.kotlin.load.kotlin.getOptimalModeForValueParameter
import org.jetbrains.kotlin.metadata.jvm.JvmModuleProtoBuf
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.serialization.SerializableStringTable
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.codegen.state.InfoForMangling
import org.jetbrains.kotlin.codegen.state.collectFunctionSignatureForManglingSuffix
import org.jetbrains.kotlin.codegen.state.md5base64
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.inlineClassRepresentationInJvm
import org.jetbrains.kotlin.codegen.ClassBuilderMode
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.types.AbstractTypeMapper.writeFormalTypeParameter
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.org.objectweb.asm.ClassWriter
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.Method
import org.jetbrains.org.objectweb.asm.tree.*
import java.io.File

/**
 * Generates, straight from FIR, the class files the JVM backend generates for stubs in the kapt mode, without running Fir2Ir, the
 * lowerings and the code generator.
 *
 * The class files are what makes stub generation incremental. With the module mapping, they are written as the incremental data,
 * the output directory of the incremental compilation: it reads their `@Metadata` to update its caches and find the dirty files,
 * and the next build resolves the declarations of unchanged files against them, found through the module mapping. So they have to
 * match the backend's class files, not only the Java stubs kapt converts their ASM class nodes to.
 */
@OptIn(SymbolInternals::class, DirectDeclarationsAccess::class, ScopeFunctionRequiresPrewarm::class)
internal class FirKaptStubGenerator(
    private val session: FirSession,
    private val scopeSession: ScopeSession,
    private val configuration: CompilerConfiguration,
    /** The file facades and multifile class parts for the module mapping, shared by the generators of all modules. */
    private val packagePartRegistry: PackagePartRegistry,
) {
    val compiledClasses = mutableListOf<ClassNode>()
    val origins = HashMap<Any, KaptStubOrigin>()

    private val typeMapper = FirJvmTypeMapper(session)
    private val typeContext = typeMapper.typeContext
    private val annotations = FirKaptAnnotations(session, typeMapper)
    private val languageVersionSettings = session.languageVersionSettings
    private val jvmDefaultMode = languageVersionSettings.jvmDefaultMode
    private val moduleName = configuration.moduleName ?: JvmProtoBufUtil.DEFAULT_MODULE_NAME
    private val classFileVersion = (configuration.jvmTarget ?: JvmTarget.DEFAULT).majorVersion
    private val targetId = TargetId(moduleName, "java-production")

    private val stubClasses = mutableListOf<StubClass>()
    private val multifileParts = LinkedHashMap<String, MutableList<StubClass>>()

    /** Fields of properties, bound to the metadata after [renameClashingFields]. */
    private val fieldBindings = mutableListOf<Triple<FirMetadataSerializer, FirProperty, FieldNode>>()
    private val publicApiFields = HashSet<FieldNode>()

    private inner class StubClass(
        val node: ClassNode,
        val firFile: FirFile,
        val metadata: FirMetadataSource?,
        val serializer: FirMetadataSerializer?,
        val kind: KotlinClassHeader.Kind,
        val sourceFile: File?,
        /** For a multifile class part, the facade; for a facade, the parts. */
        val multifileFacade: String? = null,
        val multifileParts: List<String>? = null,
        val facadeSourceFiles: List<File> = emptyList(),
    )

    fun generate(files: List<FirFile>): OutputFileCollection {
        for (file in files) generateFile(file)
        for ([facadeName, parts] in multifileParts) generateMultifileFacade(facadeName, parts.sortedBy { it.node.name })
        for ([serializer, property, field] in fieldBindings) {
            serializer.bindFieldMetadata(FirMetadataSource.Property(property), Type.getType(field.desc) to field.name)
        }
        for (stubClass in stubClasses) writeMetadata(stubClass)
        return FirKaptOutputFiles(stubClasses.map { ClassOutputFile(it) })
    }

    // ---------------------------------------------------------------- files and classes

    @OptIn(DirectDeclarationsAccess::class)
    private fun generateFile(file: FirFile) {
        // Expect declarations have no JVM classes or members, the actual ones do.
        for (declaration in file.declarations) {
            if (declaration is FirRegularClass && !declaration.isExpect) generateClass(declaration, file, outer = null)
        }

        val callables = file.declarations.filterIsInstance<FirMemberDeclaration>().filter {
            (it is FirNamedFunction || it is FirProperty || it is FirTypeAlias) && !it.isExpect
        }
        if (callables.isEmpty()) return

        val facadeName = file.facadeClassName()
        val isMultifilePart = file.hasAnnotation(JVM_MULTIFILE_CLASS)
        val node = ClassNode()
        node.version = classFileVersion
        node.access = if (isMultifilePart) Opcodes.ACC_FINAL or Opcodes.ACC_SUPER or Opcodes.ACC_SYNTHETIC
        else Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL or Opcodes.ACC_SUPER
        node.name = if (isMultifilePart) JvmFileClassUtil.manglePartName(facadeName, file.name) else facadeName
        node.superName = "java/lang/Object"
        node.interfaces = mutableListOf()
        val metadata = FirMetadataSource.File(file)
        val stubClass = StubClass(
            node, file, metadata, serializer(metadata, parent = null),
            if (isMultifilePart) KotlinClassHeader.Kind.MULTIFILE_CLASS_PART else KotlinClassHeader.Kind.FILE_FACADE,
            file.sourceFile?.path?.let(::File), multifileFacade = facadeName.takeIf { isMultifilePart },
        )
        addClass(stubClass)
        if (isMultifilePart) multifileParts.getOrPut(facadeName) { mutableListOf() } += stubClass
        // Same as FileClassLowering: incremental compilation finds the top-level declarations of unchanged files in the module mapping.
        packagePartRegistry.addPart(file.packageDirective.packageFqName, node.name, facadeName.takeIf { isMultifilePart })
        // Multifile parts do not keep @JvmName.
        val written = annotations.write(
            if (isMultifilePart) file.annotations.filterNot { it.annotationTypeRef.coneTypeOrNull?.classId == JvmStandardClassIds.Annotations.JvmName }
            else file.annotations
        )
        node.visibleAnnotations = written.visible.toMutableList()
        node.invisibleAnnotations = written.invisible.toMutableList()
        origins[node] = FirKaptClassOrigin(
            file, KotlinPosition(file.path, isRelativePath = false, 0), firSource = file.source, sourceAnnotations = written.sources,
            packageFqName = file.packageDirective.packageFqName, simpleName = node.name.substringAfterLast('/'),
            isNested = false, isInner = false, isEnumClass = false, isNestedInInterface = false, firClass = null,
        )
        val owner = Owner.Facade(stubClass)
        for (declaration in callables) {
            when (declaration) {
                is FirNamedFunction -> generateFunction(declaration, owner)
                is FirProperty -> generateProperty(declaration, owner)
                is FirTypeAlias -> generateTypeAliasAnnotationsMethod(declaration, owner)
                else -> {}
            }
        }
    }

    /** TypeAliasAnnotationMethodsLowering: an annotated type alias keeps its annotations on an empty `T$annotations` method. */
    private fun generateTypeAliasAnnotationsMethod(alias: FirTypeAlias, owner: Owner.Facade) {
        if (alias.annotations.isEmpty()) return
        val access = visibilityFlag(alias.visibility) or Opcodes.ACC_STATIC or Opcodes.ACC_SYNTHETIC or Opcodes.ACC_DEPRECATED
        val name = JvmAbi.getSyntheticMethodNameForAnnotatedTypeAlias(alias.name)
        val node = MethodNode(Opcodes.API_VERSION, access, name, "()V", null, null)
        val written = annotations.write(alias.annotations)
        node.visibleAnnotations = written.visible.toMutableList()
        node.invisibleAnnotations = written.invisible.toMutableList()
        owner.stubClass.node.methods.add(node)
        origins[node] = FirKaptMethodOrigin(
            alias, owner.firFile, KotlinPosition(owner.path, isRelativePath = false, Int.MAX_VALUE), null, isConstructor = false,
            sourceAnnotations = written.sources, isJvmOverride = false, parameterNames = emptyList(), superConstructorArguments = { null },
            returnTypeSource = typeSource(false, null, null), parameterTypeSources = emptyList(),
        )
    }

    /** Same as GenerateMultifileFacades: the facade delegates to the parts' non-private functions and has their constants. */
    private fun generateMultifileFacade(facadeName: String, parts: List<StubClass>) {
        val node = ClassNode()
        node.version = classFileVersion
        node.access = Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL or Opcodes.ACC_SUPER
        node.name = facadeName
        node.superName = "java/lang/Object"
        node.interfaces = mutableListOf()
        val firstFile = parts.first().firFile
        val stubClass = StubClass(
            node, firstFile, null, null, KotlinClassHeader.Kind.MULTIFILE_CLASS, null, multifileParts = parts.map { it.node.name },
            facadeSourceFiles = parts.mapNotNull { it.sourceFile },
        )
        addClass(stubClass)
        // Like in the JVM backend, the facade has no source file, so imports from the root package are not checked (KT-72659), and
        // its members keep their offsets in the parts, all compared as if they were in one file.
        val facadePath = "<multi-file facade $facadeName>"
        origins[node] = FirKaptClassOrigin(
            firFile = null, position = KotlinPosition(facadePath, isRelativePath = false, Int.MAX_VALUE), firSource = null,
            sourceAnnotations = emptyList(),
            packageFqName = firstFile.packageDirective.packageFqName, simpleName = facadeName.substringAfterLast('/'),
            isNested = false, isInner = false, isEnumClass = false, isNestedInInterface = false, firClass = null,
        )
        for (part in parts) {
            for (field in part.node.fields.toList()) {
                if (field.value == null || field.access and Opcodes.ACC_PRIVATE != 0) continue
                part.node.fields.remove(field)
                node.fields.add(field)
            }
            for (method in part.node.methods) {
                if (method.access and Opcodes.ACC_PRIVATE != 0 || method.name.endsWith(JvmAbi.ANNOTATED_PROPERTY_METHOD_NAME_SUFFIX)) continue
                val delegate = MethodNode(Opcodes.API_VERSION, method.access, method.name, method.desc, method.signature, method.exceptions?.toTypedArray())
                delegate.visibleAnnotations = method.visibleAnnotations
                delegate.invisibleAnnotations = method.invisibleAnnotations
                delegate.visibleParameterAnnotations = method.visibleParameterAnnotations
                delegate.invisibleParameterAnnotations = method.invisibleParameterAnnotations
                delegate.annotationDefault = method.annotationDefault
                node.methods.add(delegate)
                (origins[method] as? FirKaptMethodOrigin)?.let {
                    origins[delegate] = it.withPosition(it.position?.let { p -> KotlinPosition(facadePath, isRelativePath = false, p.pos) })
                }
            }
        }
    }

    private fun FirFile.facadeClassName(): String {
        val jvmName = annotations.firstOrNull { it.annotationTypeRef.coneTypeOrNull?.classId == JvmStandardClassIds.Annotations.JvmName }
            ?.let { (it.argumentMapping.mapping[StandardNames.NAME] as? FirLiteralExpression)?.value as? String }
        val simpleName = jvmName ?: PackagePartClassUtils.getFilePartShortName(name)
        val packagePrefix = packageDirective.packageFqName.asString().replace('.', '/')
        return if (packagePrefix.isEmpty()) simpleName else "$packagePrefix/$simpleName"
    }

    private fun addClass(stubClass: StubClass) {
        stubClasses += stubClass
        compiledClasses += stubClass.node
    }

    private fun serializer(metadata: FirMetadataSource, parent: FirMetadataSerializer?): FirMetadataSerializer =
        makeLocalFirMetadataSerializerForMetadataSource(
            metadata, session, scopeSession, parent, targetId, configuration, null, ClassBuilderMode.KAPT3,
        )

    /** The class that declares a member, and where its static parts go. */
    private sealed class Owner {
        abstract val stubClass: StubClass?

        class Facade(override val stubClass: StubClass) : Owner()
        class Class(override val stubClass: StubClass, val klass: FirRegularClass, val outer: Class?) : Owner()
        class DefaultImpls(override val stubClass: StubClass, val iface: Class) : Owner()
    }

    private val defaultImpls = HashMap<FirRegularClass, Owner.DefaultImpls>()

    private fun FirRegularClass.isJvmInterface(): Boolean = classKind == ClassKind.INTERFACE || classKind == ClassKind.ANNOTATION_CLASS

    /** Whether non-abstract interface members get compatibility delegates in `DefaultImpls`. */
    private fun FirRegularClass.isInCompatibilityMode(): Boolean = when (jvmDefaultMode) {
        JvmDefaultMode.ENABLE -> !hasAnnotation(JVM_DEFAULT_WITHOUT_COMPATIBILITY)
        JvmDefaultMode.NO_COMPATIBILITY -> hasAnnotation(JVM_DEFAULT_WITH_COMPATIBILITY)
        JvmDefaultMode.DISABLE -> false
    }

    private fun defaultImplsOf(iface: Owner.Class): Owner.DefaultImpls = defaultImpls.getOrPut(iface.klass) {
        val outerNode = iface.stubClass.node
        val name = outerNode.name + "$" + JvmAbi.DEFAULT_IMPLS_CLASS_NAME
        val node = ClassNode()
        node.version = classFileVersion
        node.name = name
        node.access = Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL or Opcodes.ACC_SUPER
        node.superName = "java/lang/Object"
        node.interfaces = mutableListOf()
        val innerClass = InnerClassNode(
            name, outerNode.name, JvmAbi.DEFAULT_IMPLS_CLASS_NAME, Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC or Opcodes.ACC_FINAL,
        )
        node.innerClasses = mutableListOf(innerClass)
        outerNode.innerClasses.add(innerClass)
        val stubClass = StubClass(node, iface.stubClass.firFile, null, null, KotlinClassHeader.Kind.SYNTHETIC_CLASS, iface.stubClass.sourceFile)
        addClass(stubClass)
        // The backend finds the interface as the source of DefaultImpls, so the class copies the interface's KDoc.
        origins[node] = FirKaptClassOrigin(
            iface.stubClass.firFile, position(iface.stubClass.firFile, iface.klass.source, null), iface.klass.source, emptyList(),
            iface.klass.classId.packageFqName, JvmAbi.DEFAULT_IMPLS_CLASS_NAME, isNested = true, isInner = false, isEnumClass = false,
            isNestedInInterface = true, firClass = null,
        )
        Owner.DefaultImpls(stubClass, iface)
    }

    /**
     * Places a non-abstract interface member like InterfaceLowering: a default method, possibly with a compatibility delegate in
     * `DefaultImpls`, or, without JVM default methods, an abstract method and the implementation in `DefaultImpls`.
     */
    private fun placeInterfaceMember(
        declaration: FirCallableDeclaration, owner: Owner.Class, node: MethodNode, parameters: List<JvmParameter>,
        returnType: ConeKotlinType?, typeParameters: List<FirTypeParameterRef>, isGetter: Boolean, propertyForTypes: FirProperty?,
        positionSource: FirDeclaration,
    ) {
        val isPrivate = declaration.visibility == Visibilities.Private
        val isDisabled = jvmDefaultMode == JvmDefaultMode.DISABLE
        if (isDisabled && isPrivate) {
            owner.stubClass.node.methods.remove(node)
            origins.remove(node)
        } else if (isDisabled) {
            node.access = node.access or Opcodes.ACC_ABSTRACT
            node.localVariables = mutableListOf()
        } else if (isPrivate || !owner.klass.isInCompatibilityMode()) {
            return
        }
        val implsOwner = defaultImplsOf(owner)
        val access = Opcodes.ACC_STATIC or (if (isPrivate) Opcodes.ACC_PRIVATE else Opcodes.ACC_PUBLIC) or
                (if (isDisabled) 0 else Opcodes.ACC_DEPRECATED) or (node.access and Opcodes.ACC_VARARGS)
        val thisParameter = JvmParameter(
            owner.klass.defaultType(), null, WritingParameterToGenericSignatureMode.REGULAR, "\$this", emptyList(), null,
        )
        val movedParameters = parameters.map { it.asMovedReceiver() }
        addMethod(
            declaration, implsOwner, node.name, access, listOf(thisParameter) + movedParameters, returnType,
            owner.klass.typeParameters + typeParameters, isGetter = isGetter, bindMetadata = false, positionSource = positionSource,
            propertyForTypes = propertyForTypes, jvmOverride = false,
        )
    }

    /**
     * Members generated for inherited declarations: delegating members of `by` delegation, and the bridges to default methods of
     * interfaces (InheritedDefaultMethodsOnClassesLowering).
     */
    private fun generateInheritedMembers(owner: Owner.Class) {
        val klass = owner.klass
        val lookupTag = klass.symbol.toLookupTag()
        val scope = klass.unsubstitutedScope(session, scopeSession, withForcedTypeCalculator = true, memberRequiredPhase = null)
        for (name in scope.getCallableNames()) {
            scope.processFunctionsByName(name) { symbol ->
                val function = symbol.fir
                when {
                    function.origin == FirDeclarationOrigin.Delegated && symbol.containingClassLookupTag() == lookupTag -> {
                        // JDK default methods of collections, like `List.getFirst`, are not delegated.
                        val original = symbol.unwrapFakeOverridesOrDelegated()
                        val isJdkDefault = !original.isAbstract &&
                                (original.origin is FirDeclarationOrigin.Java || original.origin == FirDeclarationOrigin.Enhancement)
                        if (!isJdkDefault) generateInheritedFunction(function, owner, bridge = false)
                    }
                    isInheritedInterfaceDefault(symbol, owner) -> generateInheritedFunction(function, owner, bridge = true)
                }
            }
            scope.processPropertiesByName(name) { symbol ->
                val property = (symbol as? FirPropertySymbol)?.fir ?: return@processPropertiesByName
                when {
                    property.origin == FirDeclarationOrigin.Delegated && symbol.containingClassLookupTag() == lookupTag ->
                        generateInheritedAccessors(property, owner, bridge = false)
                    isInheritedInterfaceDefault(symbol, owner) -> generateInheritedAccessors(property, owner, bridge = true)
                }
            }
        }
    }

    /**
     * Interface members inherited from other interfaces get delegates in `DefaultImpls`, like the own ones: deprecated compatibility
     * bridges to JVM default methods, and plain redirections to implementations that only exist in `DefaultImpls`.
     */
    private fun generateInheritedDefaultImpls(owner: Owner.Class) {
        val klass = owner.klass
        val scope = klass.unsubstitutedScope(session, scopeSession, withForcedTypeCalculator = true, memberRequiredPhase = null)
        val withCompatibility = jvmDefaultMode == JvmDefaultMode.DISABLE || klass.isInCompatibilityMode()
        fun implsAccess(symbol: FirCallableSymbol<*>): Int? {
            val isJvmDefault = symbol.unwrapFakeOverrides().isCompiledToJvmDefault()
            if (isJvmDefault && !withCompatibility) return null
            return Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC or (if (isJvmDefault) Opcodes.ACC_DEPRECATED else 0)
        }
        val thisParameter = JvmParameter(klass.defaultType(), null, WritingParameterToGenericSignatureMode.REGULAR, "\$this", emptyList(), null)
        for (name in scope.getCallableNames()) {
            scope.processFunctionsByName(name) { symbol ->
                if (!isInheritedInterfaceDefault(symbol, owner)) return@processFunctionsByName
                val implsAccess = implsAccess(symbol) ?: return@processFunctionsByName
                val function = symbol.fir
                val parameters = callableParameters(function).map { it.asMovedReceiver() } +
                        function.valueParameters.map { it.toJvmParameter(function) } + listOfNotNull(continuationParameter(function))
                val returnType = if (function.isSuspend) session.builtinTypes.nullableAnyType.coneType else function.returnTypeRef.coneType
                addMethod(
                    function, defaultImplsOf(owner), functionJvmName(function, owner), implsAccess or varargsFlag(function),
                    listOf(thisParameter) + parameters, returnType.takeUnless { function.returnsVoid() },
                    klass.typeParameters + function.typeParameters, isGetter = false, bindMetadata = false,
                    jvmOverride = false, firSource = null, withExceptions = false,
                    position = KotlinPosition(owner.path, isRelativePath = false, SYNTHETIC_OFFSET),
                )
            }
            scope.processPropertiesByName(name) { symbol ->
                val property = (symbol as? FirPropertySymbol)?.fir ?: return@processPropertiesByName
                if (!isInheritedInterfaceDefault(symbol, owner)) return@processPropertiesByName
                val implsAccess = implsAccess(symbol) ?: return@processPropertiesByName
                for (accessor in listOfNotNull(property.getter, property.setter.takeIf { property.isVar })) {
                    val parameters = callableParameters(property).map { it.asMovedReceiver() }.toMutableList()
                    if (accessor.isSetter) {
                        parameters += JvmParameter(
                            property.returnTypeRef.coneType, property.returnTypeRef, WritingParameterToGenericSignatureMode.REGULAR, null,
                            emptyList(), null,
                        )
                    }
                    addMethod(
                        accessor, defaultImplsOf(owner), accessorJvmName(property, accessor, owner), implsAccess, listOf(thisParameter) + parameters,
                        if (accessor.isGetter) property.returnTypeRef.coneType else null, klass.typeParameters + property.typeParameters,
                        isGetter = accessor.isGetter, bindMetadata = false, jvmOverride = false, firSource = null,
                        propertyForTypes = property, withExceptions = false,
                        position = KotlinPosition(owner.path, isRelativePath = false, SYNTHETIC_OFFSET),
                    )
                }
            }
        }
    }

    private fun JvmParameter.asMovedReceiver(): JvmParameter =
        if (source is FirReceiverParameter) JvmParameter(type, typeRef, mode, "\$receiver", annotations, source) else this

    /** Like `findDefaultImplsRedirection`: implementations only in `DefaultImpls` always need one, JVM default methods by the mode. */
    private fun isInheritedInterfaceDefault(symbol: FirCallableSymbol<*>, owner: Owner.Class): Boolean {
        val klass = owner.klass
        if (symbol.containingClassLookupTag() == klass.symbol.toLookupTag() && symbol.origin !is FirDeclarationOrigin.SubstitutionOverride) {
            return false
        }
        val implementation = symbol.unwrapFakeOverrides()
        // JDK methods of mapped collection interfaces too, like `Iterable.forEach`, and the hidden ones of newer JDKs like `List.getFirst`.
        if (implementation.origin is FirDeclarationOrigin.Java || implementation.origin == FirDeclarationOrigin.Enhancement ||
            implementation.origin == FirDeclarationOrigin.Synthetic.FakeHiddenInPreparationForNewJdk
        ) return false
        val implementationClass = implementation.containingClassLookupTag()?.toRegularClassSymbol(session) ?: return false
        if (implementationClass.classKind != ClassKind.INTERFACE || implementationClass.origin is FirDeclarationOrigin.Java) return false
        if (implementation.isAbstract || implementation.visibility == Visibilities.Private) return false
        if (implementation.hasAnnotationWithClassId(PLATFORM_DEPENDENT, session)) return false
        if (klass.classKind != ClassKind.INTERFACE && implementation.isCompiledToJvmDefault()) {
            val redirects = when (jvmDefaultMode) {
                JvmDefaultMode.ENABLE -> !klass.hasAnnotation(JVM_DEFAULT_WITHOUT_COMPATIBILITY)
                JvmDefaultMode.NO_COMPATIBILITY -> klass.hasAnnotation(JVM_DEFAULT_WITH_COMPATIBILITY)
                JvmDefaultMode.DISABLE -> true
            }
            if (!redirects) return false
        }
        val hasBody = when (val fir = implementation.fir) {
            is FirFunction -> fir.body != null || fir.hasBodyInSource() || fir.origin !is FirDeclarationOrigin.Source
            is FirProperty -> fir.getter?.let { it.body != null || it.hasBodyInSource() || fir.origin !is FirDeclarationOrigin.Source } == true
            else -> false
        }
        if (!hasBody) return false
        // Not if a superclass already implements it.
        val superClass = klass.superConeTypes.firstNotNullOfOrNull { type ->
            type.fullyExpandedType(session).toRegularClassSymbol(session)?.takeIf { it.classKind != ClassKind.INTERFACE && !it.classId.isAny() }
        } ?: return true
        val superScope = superClass.unsubstitutedScope(session, scopeSession, withForcedTypeCalculator = true, memberRequiredPhase = null)
        var inherited = false
        val process: (FirCallableSymbol<*>) -> Unit = { if (it.unwrapFakeOverrides() == implementation) inherited = true }
        when (symbol) {
            is FirNamedFunctionSymbol -> superScope.processFunctionsByName(symbol.name) { process(it) }
            is FirPropertySymbol -> superScope.processPropertiesByName(symbol.name) { process(it) }
            else -> {}
        }
        return !inherited
    }

    private fun ClassId.isAny(): Boolean = this == StandardClassIds.Any

    // ---------------------------------------------------------------- collections

    /** A member of a builtin collection whose JVM parameters are `Object` or `Collection<?>` (SpecialGenericSignatures). */
    private class SpecialMethod(
        val name: String, val owner: ClassId, val parameterNames: List<String>,
        val objectParameters: Set<Int> = emptySet(), val wildcardParameters: Set<Int> = emptySet(),
    )

    /**
     * What BridgeLowering, CollectionStubMethodLowering and ToArrayLowering add to collection classes: `size()` and the like for
     * the special properties, `Object` bridges of members like `contains`, `toArray`, and throwing stubs of mutating members in
     * read-only collections.
     */
    private fun generateCollectionMembers(owner: Owner.Class) {
        val klass = owner.klass
        val superTypes = lookupSuperTypes(klass, lookupInterfaces = true, deep = true, useSiteSession = session, substituteTypes = true)
        val superClassIds = superTypes.mapTo(HashSet()) { it.lookupTag.classId }
        if (superClassIds.none { it.isSpecialBuiltinOwner() }) return
        val scope = klass.unsubstitutedScope(session, scopeSession, withForcedTypeCalculator = true, memberRequiredPhase = null)
        val superClass = klass.superConeTypes.firstNotNullOfOrNull { type ->
            type.fullyExpandedType(session).toRegularClassSymbol(session)?.takeIf { it.classKind == ClassKind.CLASS }
        }
        val superClassIsCollection = superClass != null &&
                lookupSuperTypes(superClass.fir, lookupInterfaces = true, deep = true, useSiteSession = session, substituteTypes = false)
                    .any { it.lookupTag.classId.isSpecialBuiltinOwner() }
        val javaSuperClass = superClass?.takeIf { superClassIsCollection && it.origin is FirDeclarationOrigin.Java }
        generateSpecialPropertyBridges(owner, scope, javaSuperClass != null, superClassIsCollection)
        generateRenamedBuiltinBridges(owner, scope, superClassIds, superClassIsCollection, javaSuperClass != null)
        if (superClassIsCollection) generateInheritedSpecialMethodBridges(owner, scope, superClassIds)
        if (StandardClassIds.Collection in superClassIds && !superClassIsCollection && !scope.declaresFunction(TO_ARRAY, klass)) {
            generateToArray(owner)
        }
        if (!superClassIsCollection) generateCollectionStubs(owner, scope, superTypes)
    }

    private fun classPosition(owner: Owner.Class): KotlinPosition? = origins[owner.stubClass.node]?.position

    private fun ClassId.isSpecialBuiltinOwner(): Boolean =
        packageFqName == StandardClassIds.BASE_COLLECTIONS_PACKAGE || this == StandardClassIds.CharSequence

    /** Whether [implementation] is an abstract member of a builtin, which a class that does not implement it gets as abstract. */
    private fun isAbstractBuiltin(implementation: FirCallableSymbol<*>, implementationClassId: ClassId): Boolean =
        implementation.isAbstract && implementation.origin == FirDeclarationOrigin.Library && implementationClassId.isSpecialBuiltinOwner()

    /** A builtin member with another JVM name, like `CharSequence.get` and `charAt`, and whether its bridge is an override. */
    private class RenamedBuiltin(val owner: ClassId, val name: Name, val jvmName: String, val bridgeOverrides: Boolean)

    /** A final bridge with the JVM name calls the Kotlin member, which is abstract if the class does not implement it. */
    private fun generateRenamedBuiltinBridges(
        owner: Owner.Class, scope: FirTypeScope, superClassIds: Set<ClassId>, superClassIsCollection: Boolean, fromJavaSuperClass: Boolean,
    ) {
        if (superClassIsCollection && !fromJavaSuperClass) return
        for (renamed in RENAMED_BUILTINS) {
            if (renamed.owner in superClassIds) generateRenamedBuiltinBridge(owner, scope, renamed, fromJavaSuperClass)
        }
    }

    private fun generateRenamedBuiltinBridge(owner: Owner.Class, scope: FirTypeScope, renamed: RenamedBuiltin, fromJavaSuperClass: Boolean) {
        var found: FirNamedFunctionSymbol? = null
        scope.processFunctionsByName(renamed.name) { if (found == null && it.fir.valueParameters.size == 1) found = it }
        val symbol = found ?: return
        val implementation = symbol.unwrapFakeOverrides()
        val implementationClassId = implementation.containingClassLookupTag()?.classId ?: return
        val declared = implementationClassId == owner.klass.symbol.classId
        val fromJava = !declared && fromJavaSuperClass && implementation.origin !is FirDeclarationOrigin.Source
        val abstract = !declared && !fromJava && isAbstractBuiltin(implementation, implementationClassId)
        if (!declared && !abstract && !fromJava) return
        val function = symbol.fir
        val parameters = function.valueParameters.map { it.toJvmParameter(function) }
        if (fromJava) {
            // Like the getters of a Java superclass's special properties, with erased types.
            addMethod(
                function, owner, renamed.name.asString(), Opcodes.ACC_PUBLIC, parameters,
                function.returnTypeRef.coneType.eraseClassTypeParameters(owner.klass), emptyList(), isGetter = false, bindMetadata = false,
                annotationsFrom = null, nullability = false, jvmOverride = true, firSource = owner.klass.source, withExceptions = false,
                position = classPosition(owner),
            )
        }
        if (abstract) {
            addMethod(
                function, owner, renamed.name.asString(), Opcodes.ACC_PUBLIC or Opcodes.ACC_ABSTRACT, parameters,
                function.returnTypeRef.coneType, emptyList(), isGetter = false, bindMetadata = false, nullability = false, jvmOverride = true,
                firSource = null, withExceptions = false, position = KotlinPosition(owner.path, isRelativePath = false, -1),
            )
        }
        addMethod(
            function, owner, renamed.jvmName, Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL, parameters, function.returnTypeRef.coneType,
            emptyList(), isGetter = false, bindMetadata = false, annotationsFrom = function.takeUnless { fromJava }, nullability = false,
            jvmOverride = renamed.bridgeOverrides,
            firSource = owner.klass.source, withExceptions = false, position = classPosition(owner),
        )
    }

    /** `getSize()` and `size()`: the getter of the Kotlin property, and the final JVM method that calls it. */
    private fun generateSpecialPropertyBridges(owner: Owner.Class, scope: FirTypeScope, fromJavaSuperClass: Boolean, superClassIsCollection: Boolean) {
        val klass = owner.klass
        val lookupTag = klass.symbol.toLookupTag()
        for ([name, jvmName] in SPECIAL_PROPERTIES) {
            // This class's override if there is one: for a Java superclass, there are also its fields, like `HashMap.size`.
            var found: FirPropertySymbol? = null
            scope.processPropertiesByName(name) { symbol ->
                if (symbol is FirPropertySymbol && (found == null || symbol.containingClassLookupTag() == lookupTag && found?.containingClassLookupTag() != lookupTag)) {
                    found = symbol
                }
            }
            val symbol = found ?: continue
            val implementation = symbol.unwrapFakeOverrides()
            val implementationClassId = implementation.containingClassLookupTag()?.classId ?: continue
            // A Kotlin collection superclass already has the final `size()` calling the overridable `getSize()`.
            val declared = implementationClassId == klass.symbol.classId && (!superClassIsCollection || fromJavaSuperClass)
            val fromJava = implementationClassId != klass.symbol.classId && fromJavaSuperClass && implementation.origin !is FirDeclarationOrigin.Source
            val abstract = !declared && !superClassIsCollection && isAbstractBuiltin(implementation, implementationClassId)
            if (!declared && !fromJava && !abstract) continue
            val property = symbol.fir
            val type = symbol.resolvedReturnType
            if (fromJava || abstract) {
                // The backend gives the getters of a Java superclass's properties erased types.
                val getterType = if (fromJava) type.eraseClassTypeParameters(klass) else type
                addMethod(
                    property, owner, JvmAbi.getterName(name.asString()),
                    Opcodes.ACC_PUBLIC or (if (abstract) Opcodes.ACC_ABSTRACT else 0), emptyList(), getterType, emptyList(), isGetter = true,
                    bindMetadata = false, annotationsFrom = null, nullability = false, jvmOverride = true,
                    firSource = owner.klass.source.takeUnless { abstract }, withExceptions = false,
                    position = if (abstract) KotlinPosition(owner.path, isRelativePath = false, -1) else classPosition(owner),
                )
            }
            addMethod(
                property, owner, jvmName, Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL, emptyList(), type, emptyList(), isGetter = true,
                bindMetadata = false, annotationsFrom = null, nullability = false, jvmOverride = true, firSource = owner.klass.source, withExceptions = false,
                position = classPosition(owner),
            )
        }
    }

    private fun ConeKotlinType.eraseClassTypeParameters(klass: FirRegularClass): ConeKotlinType =
        substitutorByMap(klass.typeParameters.associate { it.symbol to session.builtinTypes.nullableAnyType.coneType }, session)
            .substituteOrSelf(this)

    private fun FirTypeScope.declaresFunction(name: Name, klass: FirRegularClass): Boolean {
        var declared = false
        processFunctionsByName(name) { if (it.containingClassLookupTag() == klass.symbol.toLookupTag() && it.origin == FirDeclarationOrigin.Source) declared = true }
        return declared
    }

    private fun specialOverride(function: FirNamedFunction, owner: Owner, delegated: Boolean = false): SpecialMethod? {
        val klass = (owner as? Owner.Class)?.klass ?: return null
        if (!(function.isOverride || delegated) || klass.isJvmInterface() || SPECIAL_METHODS.none { it.name == function.name.asString() }) {
            return null
        }
        val superClassIds = lookupSuperTypes(klass, lookupInterfaces = true, deep = true, useSiteSession = session, substituteTypes = false)
            .mapTo(HashSet()) { it.lookupTag.classId }
        return specialMethod(function.name, function.valueParameters.size, superClassIds)
    }

    private fun specialMethod(name: Name, parameterCount: Int, superClassIds: Set<ClassId>): SpecialMethod? =
        SPECIAL_METHODS.firstOrNull { it.name == name.asString() && it.parameterNames.size == parameterCount && it.owner in superClassIds }

    /**
     * The parameters of a special method as the JVM sees them, `Object` and `Collection<?>`. An `Object` parameter keeps a nullable
     * declared type's nullability, a non-null one needs a type check, which takes `Any?`.
     */
    private fun List<JvmParameter>.toSpecial(method: SpecialMethod, offset: Int): List<JvmParameter> =
        mapIndexed { index, parameter ->
            val [type, nullabilityType] = when (index - offset) {
                in method.objectParameters -> session.builtinTypes.nullableAnyType.coneType.let {
                    it to (if (parameter.nullabilityType.isNullableInIr()) parameter.nullabilityType else it)
                }
                in method.wildcardParameters -> StandardClassIds.Collection.constructClassLikeType(arrayOf(ConeStarProjection)).let { it to it }
                else -> return@mapIndexed parameter
            }
            JvmParameter(type, null, parameter.mode, parameter.name, parameter.annotations, parameter.source, nullabilityType = nullabilityType)
        }

    private fun descriptorOf(parameters: List<JvmParameter>): List<String> = parameters.map { mapType(it.type).descriptor }

    /**
     * For members of a collection superclass specialized by this class, like `HashMap<File, V>.containsKey(File)` or
     * `AbstractList<String>.contains(String)`: a typed delegate, and the final `Object` bridge.
     */
    private fun generateInheritedSpecialMethodBridges(owner: Owner.Class, scope: FirTypeScope, superClassIds: Set<ClassId>) {
        val lookupTag = owner.klass.symbol.toLookupTag()
        for (name in scope.getCallableNames()) {
            scope.processFunctionsByName(name) { symbol ->
                val isOverride = symbol.origin is FirDeclarationOrigin.SubstitutionOverride || symbol.origin is FirDeclarationOrigin.IntersectionOverride
                if (symbol.containingClassLookupTag() != lookupTag || !isOverride || symbol.isAbstract) return@processFunctionsByName
                val implementation = symbol.unwrapFakeOverrides()
                val fromJava = implementation.origin is FirDeclarationOrigin.Java || implementation.origin == FirDeclarationOrigin.Enhancement
                val function = symbol.fir
                val method = specialMethod(name, function.valueParameters.size, superClassIds) ?: return@processFunctionsByName
                val parameters = function.valueParameters.map {
                    JvmParameter(
                        it.returnTypeRef.coneType, null, WritingParameterToGenericSignatureMode.REGULAR,
                        it.name.asString().takeUnless { fromJava }, emptyList(), null,
                    )
                }
                val special = parameters.toSpecial(method, 0)
                if (descriptorOf(parameters) == descriptorOf(special)) return@processFunctionsByName
                val returnType = function.returnTypeRef.coneType.takeUnless { function.returnsVoid() }
                addMethod(
                    function, owner, name.asString(), Opcodes.ACC_PUBLIC, parameters, returnType, function.typeParameters,
                    isGetter = false, bindMetadata = false, annotationsFrom = null, nullability = false, jvmOverride = true,
                    firSource = owner.klass.source, withExceptions = false, position = classPosition(owner),
                )
                addMethod(
                    function, owner, name.asString(), Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL,
                    special.mapIndexed { index, it ->
                        JvmParameter(it.type, it.typeRef, it.mode, method.parameterNames[index], it.annotations, it.source)
                    },
                    returnType, function.typeParameters, isGetter = false, bindMetadata = false, annotationsFrom = null, nullability = false,
                    jvmOverride = true, firSource = owner.klass.source, withExceptions = false, position = classPosition(owner),
                )
            }
        }
    }

    /** ToArrayLowering: the `toArray` methods of `java.util.Collection`, which Kotlin collections do not have. */
    private fun generateToArray(owner: Owner.Class) {
        val position = KotlinPosition(owner.path, isRelativePath = false, Int.MAX_VALUE)
        for ([descriptor, signature] in listOf<Pair<String, String?>>(
            "()[Ljava/lang/Object;" to null, "([Ljava/lang/Object;)[Ljava/lang/Object;" to "<T:Ljava/lang/Object;>([TT;)[TT;",
        )) {
            val node = MethodNode(Opcodes.API_VERSION, Opcodes.ACC_PUBLIC, "toArray", descriptor, signature, null)
            owner.stubClass.node.methods.add(node)
            val parameterCount = Type.getArgumentTypes(descriptor).size
            origins[node] = FirKaptMethodOrigin(
                null, owner.firFile, position, null, isConstructor = false, sourceAnnotations = emptyList(), isJvmOverride = false,
                parameterNames = List(parameterCount) { "array" }, superConstructorArguments = { null },
                returnTypeSource = typeSource(false, null, null), parameterTypeSources = List(parameterCount) { null },
            )
        }
    }

    /**
     * CollectionStubComputer: a class implementing a read-only collection, but not the mutable one, gets throwing stubs of the
     * members declared in the mutable one that it does not implement.
     */
    private fun generateCollectionStubs(owner: Owner.Class, scope: FirTypeScope, superTypes: List<ConeClassLikeType>) {
        val klass = owner.klass
        val superClassIds = superTypes.mapTo(HashSet()) { it.lookupTag.classId }
        // Only the most specific read-only collections: a list gets the stubs of `MutableList`, not of `MutableCollection`.
        val pairs = COLLECTION_PAIRS.filter { [readOnly, mutable] -> readOnly in superClassIds && mutable !in superClassIds }
        for ([readOnly, mutable] in pairs) {
            if (pairs.any { [other, _] -> readOnly in COLLECTION_SUPERTYPES[other].orEmpty() }) continue
            val readOnlyType = superTypes.first { it.lookupTag.classId == readOnly }
            val mutableClass = session.symbolProvider.getClassLikeSymbolByClassId(mutable) as? FirRegularClassSymbol ?: continue
            val substitutor = substitutorByMap(
                mutableClass.typeParameterSymbols.zip(readOnlyType.typeArguments).associate { [parameter, argument] ->
                    parameter to (argument.type ?: session.builtinTypes.nullableAnyType.coneType)
                }, session,
            )
            val mutableScope = mutableClass.unsubstitutedScope(session, scopeSession, withForcedTypeCalculator = true, memberRequiredPhase = null)
            for (name in mutableScope.getCallableNames()) {
                mutableScope.processFunctionsByName(name) { symbol ->
                    if (symbol.containingClassLookupTag()?.classId != mutable || symbol.fir.dispatchReceiverType == null) return@processFunctionsByName
                    // Members declared in the mutable class, and abstract fake overrides of nothing from the read-only one.
                    if (symbol.origin is FirDeclarationOrigin.SubstitutionOverride || symbol.origin is FirDeclarationOrigin.IntersectionOverride) {
                        var overridesReadOnly = false
                        mutableScope.processOverriddenFunctions(symbol) {
                            if (it.containingClassLookupTag()?.classId == readOnly) overridesReadOnly = true
                            if (overridesReadOnly) ProcessorAction.STOP else ProcessorAction.NEXT
                        }
                        if (!symbol.isAbstract || overridesReadOnly) return@processFunctionsByName
                    }
                    val function = symbol.fir
                    var implemented = false
                    scope.processFunctionsByName(name) {
                        if (!it.isAbstract && it.fir.valueParameters.size == function.valueParameters.size) implemented = true
                    }
                    if (implemented) return@processFunctionsByName
                    val jvmName = if (name == REMOVE_AT) "remove" else name.asString()
                    val typedParameters = function.valueParameters.map {
                        JvmParameter(
                            substitutor.substituteOrSelf(it.returnTypeRef.coneType), null, WritingParameterToGenericSignatureMode.REGULAR,
                            it.name.asString(), emptyList(), null,
                        )
                    }
                    val parameters = specialMethod(name, typedParameters.size, superClassIds)?.let { typedParameters.toSpecial(it, 0) }
                        ?: typedParameters
                    val returnType = substitutor.substituteOrSelf(function.returnTypeRef.coneType).takeUnless { function.returnsVoid() }
                    // A stub of both `MutableCollection` and `MutableList`, or a member the class already has, is generated once.
                    val descriptor = Type.getMethodDescriptor(returnType?.let { mapType(it) } ?: Type.VOID_TYPE, *parameters.map { mapType(it.type) }.toTypedArray())
                    if (owner.stubClass.node.methods.any { it.name == jvmName && it.desc == descriptor }) return@processFunctionsByName
                    // `remove(int)` for `removeAt` overrides nothing in Kotlin.
                    addMethod(
                        function, owner, jvmName, Opcodes.ACC_PUBLIC, parameters, returnType, function.typeParameters, isGetter = false,
                        bindMetadata = false, annotationsFrom = null, nullability = false, jvmOverride = name != REMOVE_AT, firSource = null,
                        withExceptions = false, position = KotlinPosition(owner.path, isRelativePath = false, Int.MAX_VALUE),
                    )
                }
            }
        }
    }

    /** Like `isCompiledToJvmDefault`: a source interface follows this module's mode, a library one its metadata. */
    private fun FirCallableSymbol<*>.isCompiledToJvmDefault(): Boolean {
        val container = containingClassLookupTag()?.toRegularClassSymbol(session) ?: return jvmDefaultMode.isEnabled
        if (container.origin.fromSource) return jvmDefaultMode.isEnabled
        return container.fir.isNewPlaceForBodyGeneration == true
    }

    /** The backend copies the annotations of the inherited declaration, `@Deprecated` included, and `throws` only for redirections. */
    private fun inheritedMemberAccess(declaration: FirCallableDeclaration, bridge: Boolean): Int =
        visibilityFlag(declaration.visibility) or varargsFlag(declaration) or deprecatedFlag(declaration) or
                (if (bridge && jvmDefaultMode != JvmDefaultMode.DISABLE) Opcodes.ACC_BRIDGE else 0)

    private fun generateInheritedFunction(function: FirNamedFunction, owner: Owner.Class, bridge: Boolean) {
        val receivers = callableParameters(function, hasBody = true)
        val typedParameters = receivers + function.valueParameters.map { it.toJvmParameter(function) } + listOfNotNull(continuationParameter(function))
        val returnType = (if (function.isSuspend) session.builtinTypes.nullableAnyType.coneType else function.returnTypeRef.coneType)
            .takeUnless { function.returnsVoid() }
        // Value classes delegate in a static `-impl` function, the box only gets the overriding method.
        val inValueClass = isValueClassStaticMember(function, owner)
        if (inValueClass && !bridge) {
            addMethod(
                function, owner, functionJvmName(function, owner), Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC or varargsFlag(function),
                listOf(valueClassThisParameter(owner.klass)) + typedParameters, returnType, owner.klass.typeParameters + function.typeParameters,
                isGetter = false, synthesizedPosition = true, jvmOverride = false, withExceptions = false,
            )
        }
        // Delegates of members like `contains(E)` take `Object`, or keep the typed method and get a final `Object` bridge.
        val name = functionJvmName(function, owner, inValueClass = false)
        val specialParameters = specialOverride(function, owner, delegated = !bridge)
            ?.let { typedParameters.toSpecial(it, receivers.size) }
        // A bridge is needed if the JVM method differs: its parameters, or its name, mangled for value classes.
        val parameters = specialParameters
            ?.takeIf { name == function.name.asString() && descriptorOf(it) == descriptorOf(typedParameters) } ?: typedParameters
        addMethod(
            function, owner, name, inheritedMemberAccess(function, bridge), parameters,
            returnType, function.typeParameters, isGetter = false, bindMetadata = !bridge && !inValueClass,
            forceBoxedReturn = forceBoxedReturnType(function, owner), synthesizedPosition = true, jvmOverride = true,
            firSource = function.source.takeUnless { bridge }, withExceptions = bridge,
        )
        if (specialParameters != null && parameters !== specialParameters) {
            addMethod(
                function, owner, function.name.asString(), Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL, specialParameters, returnType,
                function.typeParameters, isGetter = false, bindMetadata = false, nullability = false,
                jvmOverride = true, firSource = owner.klass.source, withExceptions = false, position = classPosition(owner),
            )
        }
    }

    private fun generateInheritedAccessors(property: FirProperty, owner: Owner.Class, bridge: Boolean) {
        for (accessor in listOfNotNull(property.getter, property.setter.takeIf { property.isVar })) {
            val parameters = callableParameters(property, hasBody = true).toMutableList()
            if (accessor.isSetter) {
                parameters += JvmParameter(
                    property.returnTypeRef.coneType, property.returnTypeRef, WritingParameterToGenericSignatureMode.REGULAR, null,
                    accessor.valueParameters.firstOrNull()?.annotations.orEmpty(), accessor.valueParameters.firstOrNull(),
                )
            }
            val returnType = if (accessor.isGetter) property.returnTypeRef.coneType else null
            val inValueClass = isValueClassStaticMember(accessor, owner)
            if (inValueClass && !bridge) {
                addMethod(
                    accessor, owner, accessorJvmName(property, accessor, owner), Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC,
                    listOf(valueClassThisParameter(owner.klass)) + parameters, returnType, owner.klass.typeParameters + property.typeParameters,
                    isGetter = accessor.isGetter, synthesizedPosition = true, jvmOverride = false, propertyForTypes = property,
                    withExceptions = false,
                )
            }
            addMethod(
                accessor, owner, accessorJvmName(property, accessor, owner, inValueClass = false), inheritedMemberAccess(property, bridge),
                parameters, returnType, property.typeParameters, isGetter = accessor.isGetter,
                bindMetadata = !bridge && !inValueClass, synthesizedPosition = true, jvmOverride = true,
                firSource = property.source.takeUnless { bridge }, propertyForTypes = property, withExceptions = false,
            )
        }
    }

    /** `foo$default` is generated for each function with default arguments, `DefaultImpls` exists even if it only has those. */
    private fun requireDefaultImplsForDefaultArguments(function: FirFunction, owner: Owner.Class) {
        if (function.valueParameters.none { it.defaultValue != null }) return
        if (jvmDefaultMode == JvmDefaultMode.DISABLE || owner.klass.isInCompatibilityMode()) defaultImplsOf(owner)
    }

    @OptIn(DirectDeclarationsAccess::class)
    private fun generateClass(klass: FirRegularClass, file: FirFile, outer: Owner.Class?): StubClass {
        val classId = klass.classId
        val node = ClassNode()
        node.version = classFileVersion
        node.name = JvmClassName.internalNameByClassId(classId)
        node.access = classAccessFlags(klass)
        writeClassSignature(klass, node)

        val metadata = FirMetadataSource.Class(klass)
        val stubClass = StubClass(
            node, file, metadata, serializer(metadata, outer?.stubClass?.serializer), KotlinClassHeader.Kind.CLASS,
            file.sourceFile?.path?.let(::File),
        )
        addClass(stubClass)

        val written = annotations.write(klass.annotations)
        node.visibleAnnotations = written.visible.toMutableList()
        node.invisibleAnnotations = written.invisible.toMutableList()
        if (klass.isValueClassOnJvm() && !klass.hasAnnotation(JVM_INLINE)) {
            node.visibleAnnotations.add(AnnotationNode("Lkotlin/jvm/JvmInline;"))
        }
        if (klass.classKind == ClassKind.ANNOTATION_CLASS) {
            node.visibleAnnotations.addAll(javaMetaAnnotations(klass.annotations, repeatableContainer = "${node.name}$${JvmAbi.REPEATABLE_ANNOTATION_CONTAINER_NAME}"))
        }

        val outerClass = outer?.klass
        origins[node] = FirKaptClassOrigin(
            file, position(file, klass.source, keywordTokens = null), klass.source, written.sources,
            packageFqName = classId.packageFqName, simpleName = classId.shortClassName.asString(),
            isNested = outer != null, isInner = klass.isInner, isEnumClass = klass.classKind == ClassKind.ENUM_CLASS,
            isNestedInInterface = outerClass?.classKind == ClassKind.INTERFACE, firClass = klass,
        )

        node.innerClasses = mutableListOf()
        if (outer != null) {
            node.innerClasses.add(innerClassNode(klass, outer.stubClass.node.name))
        }

        val owner = Owner.Class(stubClass, klass, outer)

        if (klass.classKind == ClassKind.ENUM_CLASS) {
            for (entry in klass.declarations.filterIsInstance<FirEnumEntry>()) generateEnumEntry(entry, owner)
        }

        val nestedClasses = mutableListOf<FirRegularClass>()
        // Like Fir2Ir, declarations generated by FIR plugins follow the declared ones.
        val allDeclarations = buildList {
            addAll(klass.declarations)
            if (session.extensionService.declarationGenerators.isNotEmpty()) {
                addAll(klass.generatedMembers(session).sortedWith(FirCallableDeclarationComparator))
                addAll(klass.generatedNestedClassifiers(session).sortedWith(FirMemberDeclarationComparator))
            }
        }
        for (declaration in allDeclarations) {
            when (declaration) {
                is FirRegularClass -> nestedClasses += declaration
                is FirConstructor -> generateConstructor(declaration, owner)
                is FirNamedFunction -> generateFunction(declaration, owner)
                is FirProperty -> generateProperty(declaration, owner)
                else -> {}
            }
        }

        if (klass.isData && !klass.isValueClassOnJvm()) generateDataClassMembers(klass, owner)
        if (klass.isValueClassOnJvm()) generateValueClassAnyMembers(owner)
        if (!klass.isJvmInterface()) {
            generateInheritedMembers(owner)
            generateCollectionMembers(owner)
        }
        if (klass.classKind == ClassKind.INTERFACE) generateInheritedDefaultImpls(owner)
        if (klass.classKind == ClassKind.OBJECT && !klass.isCompanion) generateInstanceField(klass, owner)
        klass.companionObjectSymbol?.let { generateCompanionField(it.fir, owner) }

        for (nested in nestedClasses) {
            node.innerClasses.add(innerClassNode(nested, node.name))
            generateClass(nested, file, owner)
        }
        if (klass.classKind == ClassKind.ANNOTATION_CLASS && klass.hasAnnotation(StandardClassIds.Annotations.Repeatable) &&
            !klass.hasAnnotation(JvmStandardClassIds.Annotations.Java.Repeatable)
        ) {
            generateRepeatableContainer(klass, owner)
        }
        node.innerClasses.sortBy { it.name.replace('/', '.').replace('$', '.') }
        renameClashingFields(node)
        if (klass.hasAnnotation(JvmStandardClassIds.Annotations.JvmRecord)) {
            node.access = node.access or Opcodes.ACC_RECORD
            node.recordComponents = node.fields.filter { it.access and Opcodes.ACC_STATIC == 0 }.map {
                RecordComponentNode(it.name, it.desc, it.signature)
            }
        }

        return stubClass
    }

    /** Same as RenameFieldsLowering: public ABI fields keep their names, then static ones. */
    private fun renameClashingFields(node: ClassNode) {
        val fields = node.fields.sortedBy {
            when {
                it in publicApiFields -> 0
                it.access and Opcodes.ACC_STATIC != 0 -> 1
                else -> 2
            }
        }
        val count = HashMap<String, Int>()
        for (field in fields) {
            val index = count[field.name] ?: 0
            val oldName = field.name
            if (index != 0 && field !in publicApiFields) field.name = "$oldName$$index"
            count[oldName] = index + 1
        }
    }

    private fun javaMetaAnnotations(annotations: List<FirAnnotation>, repeatableContainer: String?): List<AnnotationNode> {
        val result = mutableListOf<AnnotationNode>()
        fun has(classId: ClassId) = annotations.any { it.annotationTypeRef.coneTypeOrNull?.fullyExpandedType(session)?.classId == classId }
        fun find(classId: ClassId) = annotations.firstOrNull { it.annotationTypeRef.coneTypeOrNull?.fullyExpandedType(session)?.classId == classId }

        if (has(StandardClassIds.Annotations.MustBeDocumented) && !has(JvmStandardClassIds.Annotations.Java.Documented)) {
            result += AnnotationNode("Ljava/lang/annotation/Documented;")
        }
        if (!has(JvmStandardClassIds.Annotations.Java.Retention)) {
            val retention = find(StandardClassIds.Annotations.Retention)?.let { annotation ->
                annotation.argumentMapping.mapping.values.firstOrNull()?.enumEntryName()
            } ?: AnnotationRetention.RUNTIME.name
            val policy = if (retention == AnnotationRetention.BINARY.name) "CLASS" else retention
            result += AnnotationNode("Ljava/lang/annotation/Retention;").apply {
                values = mutableListOf("value", arrayOf("Ljava/lang/annotation/RetentionPolicy;", policy))
            }
        }
        val target = find(StandardClassIds.Annotations.Target)
        if (target != null && !has(JvmStandardClassIds.Annotations.Java.Target)) {
            val kotlinTargets = target.argumentMapping.mapping.values.flatMap { it.enumEntryNames() }.toSet()
            val javaTargets = kotlinTargets.mapNotNull { javaElementType(it) }.toSet().sortedBy { JAVA_ELEMENT_TYPES.indexOf(it) }
            result += AnnotationNode("Ljava/lang/annotation/Target;").apply {
                values = mutableListOf("value", javaTargets.map { arrayOf("Ljava/lang/annotation/ElementType;", it) })
            }
        }
        if (repeatableContainer != null && has(StandardClassIds.Annotations.Repeatable) && !has(JvmStandardClassIds.Annotations.Java.Repeatable)) {
            result += AnnotationNode("Ljava/lang/annotation/Repeatable;").apply {
                values = mutableListOf("value", Type.getObjectType(repeatableContainer))
            }
        }
        return result
    }

    private val supportsNewJavaTargets: Boolean by lazy {
        session.symbolProvider.getClassLikeSymbolByClassId(ClassId.fromString("java/lang/invoke/LambdaMetafactory")) != null
    }

    private fun javaElementType(kotlinTarget: String): String? = when (kotlinTarget) {
        "CLASS" -> "TYPE"
        "ANNOTATION_CLASS" -> "ANNOTATION_TYPE"
        "CONSTRUCTOR" -> "CONSTRUCTOR"
        "LOCAL_VARIABLE" -> "LOCAL_VARIABLE"
        "FUNCTION", "PROPERTY_GETTER", "PROPERTY_SETTER" -> "METHOD"
        "FIELD" -> "FIELD"
        "VALUE_PARAMETER" -> "PARAMETER"
        "TYPE_PARAMETER" -> "TYPE_PARAMETER".takeIf { supportsNewJavaTargets && !configuration.noNewJavaAnnotationTargets }
        "TYPE" -> "TYPE_USE".takeIf { supportsNewJavaTargets && !configuration.noNewJavaAnnotationTargets }
        else -> null
    }

    private fun FirExpression.enumEntryName(): String? =
        (this as? FirQualifiedAccessExpression)?.calleeReference?.toResolvedEnumEntrySymbol()?.name?.asString()

    private fun FirExpression.enumEntryNames(): List<String> = when (this) {
        is org.jetbrains.kotlin.fir.expressions.FirVarargArgumentsExpression -> arguments.flatMap { it.enumEntryNames() }
        is org.jetbrains.kotlin.fir.expressions.FirCollectionLiteral -> argumentList.arguments.flatMap { it.enumEntryNames() }
        is org.jetbrains.kotlin.fir.expressions.FirSpreadArgumentExpression -> expression.enumEntryNames()
        else -> listOfNotNull(enumEntryName())
    }

    private fun generateRepeatableContainer(klass: FirRegularClass, owner: Owner.Class) {
        val outerNode = owner.stubClass.node
        val name = outerNode.name + "$" + JvmAbi.REPEATABLE_ANNOTATION_CONTAINER_NAME
        val node = ClassNode()
        node.version = classFileVersion
        node.name = name
        node.access = Opcodes.ACC_PUBLIC or Opcodes.ACC_ANNOTATION or Opcodes.ACC_INTERFACE or Opcodes.ACC_ABSTRACT
        node.superName = "java/lang/Object"
        node.interfaces = mutableListOf("java/lang/annotation/Annotation")
        val copied = klass.annotations.filter {
            it.annotationTypeRef.coneTypeOrNull?.fullyExpandedType(session)?.classId in REPEATABLE_CONTAINER_META_ANNOTATIONS
        }
        val written = annotations.write(copied)
        node.visibleAnnotations = (written.visible + AnnotationNode("Lkotlin/jvm/internal/RepeatableContainer;") +
                javaMetaAnnotations(copied, repeatableContainer = null)).toMutableList()
        node.invisibleAnnotations = written.invisible.toMutableList()
        val innerClass = InnerClassNode(
            name, outerNode.name, JvmAbi.REPEATABLE_ANNOTATION_CONTAINER_NAME,
            Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC or Opcodes.ACC_ANNOTATION or Opcodes.ACC_INTERFACE or Opcodes.ACC_ABSTRACT,
        )
        node.innerClasses = mutableListOf(innerClass)
        outerNode.innerClasses.add(innerClass)
        val method = MethodNode(
            Opcodes.API_VERSION, Opcodes.ACC_PUBLIC or Opcodes.ACC_ABSTRACT, "value", "()[L${outerNode.name};", null, null,
        )
        method.localVariables = mutableListOf()
        node.methods.add(method)
        val stubClass = StubClass(node, owner.firFile, null, null, KotlinClassHeader.Kind.SYNTHETIC_CLASS, owner.stubClass.sourceFile)
        addClass(stubClass)
        val position = KotlinPosition(owner.path, isRelativePath = false, Int.MAX_VALUE)
        origins[node] = FirKaptClassOrigin(
            owner.firFile, position, null, written.sources, klass.classId.packageFqName,
            JvmAbi.REPEATABLE_ANNOTATION_CONTAINER_NAME, isNested = true, isInner = false, isEnumClass = false,
            isNestedInInterface = true, firClass = null,
        )
        origins[method] = FirKaptMethodOrigin(
            null, owner.firFile, position, null, isConstructor = false, sourceAnnotations = emptyList(), isJvmOverride = false,
            parameterNames = emptyList(), superConstructorArguments = { null },
            returnTypeSource = typeSource(false, null, null), parameterTypeSources = emptyList(),
        )
    }

    private fun innerClassNode(klass: FirRegularClass, outerName: String): InnerClassNode =
        InnerClassNode(
            JvmClassName.internalNameByClassId(klass.classId), outerName, klass.classId.shortClassName.asString(),
            innerClassAccessFlags(klass),
        )

    private fun classAccessFlags(klass: FirRegularClass): Int {
        val visibility = if (klass.visibility.isPublicApiLike()) Opcodes.ACC_PUBLIC else 0
        val kind = when (klass.classKind) {
            ClassKind.ANNOTATION_CLASS -> Opcodes.ACC_ANNOTATION or Opcodes.ACC_INTERFACE or Opcodes.ACC_ABSTRACT
            ClassKind.INTERFACE -> Opcodes.ACC_INTERFACE or Opcodes.ACC_ABSTRACT
            ClassKind.ENUM_CLASS -> Opcodes.ACC_ENUM or Opcodes.ACC_SUPER or modalityFlags(klass.enumModality())
            else -> Opcodes.ACC_SUPER or modalityFlags(klass.modality ?: Modality.FINAL)
        }
        return visibility or kind or deprecatedFlag(klass) or (if (klass.hasAnnotation(JVM_SYNTHETIC)) Opcodes.ACC_SYNTHETIC else 0)
    }

    @OptIn(DirectDeclarationsAccess::class)
    private fun FirRegularClass.enumModality(): Modality {
        val hasEntryBodies = declarations.any { it is FirEnumEntry && it.requiresSubclass() }
        return when {
            modality == Modality.ABSTRACT || modality == Modality.SEALED -> Modality.ABSTRACT
            hasEntryBodies -> Modality.OPEN
            else -> Modality.FINAL
        }
    }

    private fun innerClassAccessFlags(klass: FirRegularClass): Int {
        var flags = when (klass.visibility) {
            Visibilities.Public, Visibilities.Internal -> Opcodes.ACC_PUBLIC
            Visibilities.Protected -> Opcodes.ACC_PROTECTED
            Visibilities.Private -> Opcodes.ACC_PRIVATE
            else -> 0
        }
        if (!klass.isInner) flags = flags or Opcodes.ACC_STATIC
        flags = flags or when (klass.classKind) {
            ClassKind.ANNOTATION_CLASS -> Opcodes.ACC_ANNOTATION or Opcodes.ACC_INTERFACE or Opcodes.ACC_ABSTRACT
            ClassKind.INTERFACE -> Opcodes.ACC_INTERFACE or Opcodes.ACC_ABSTRACT
            ClassKind.ENUM_CLASS -> Opcodes.ACC_ENUM or modalityFlags(klass.enumModality())
            else -> modalityFlags(klass.modality ?: Modality.FINAL)
        }
        return flags
    }

    private fun Visibility.isPublicApiLike(): Boolean =
        this == Visibilities.Public || this == Visibilities.Protected || this == Visibilities.Internal || this == Visibilities.Local

    private fun modalityFlags(modality: Modality): Int = when (modality) {
        Modality.ABSTRACT, Modality.SEALED -> Opcodes.ACC_ABSTRACT
        Modality.FINAL -> Opcodes.ACC_FINAL
        Modality.OPEN -> 0
    }

    private fun writeClassSignature(klass: FirRegularClass, node: ClassNode) {
        val sw = BothSignatureWriter(BothSignatureWriter.Mode.CLASS)
        writeFormalTypeParameters(klass.typeParameters.filterIsInstance<FirTypeParameter>(), sw)

        val superTypes = klass.superConeTypes.map { it.fullyExpandedType(session) }
        val superClassType = superTypes.firstOrNull { type ->
            val symbol = type.toRegularClassSymbol(session)
            symbol != null && symbol.classKind != ClassKind.INTERFACE && symbol.classKind != ClassKind.ANNOTATION_CLASS
        }

        sw.writeSuperclass()
        val superName = when {
            klass.classKind == ClassKind.ENUM_CLASS -> writeSuperType(StandardClassIds.Enum.constructClassLikeType(arrayOf(klass.defaultType())), sw)
            klass.classKind == ClassKind.ANNOTATION_CLASS || klass.classKind == ClassKind.INTERFACE || superClassType == null -> {
                sw.writeClassBegin(OBJECT_TYPE)
                sw.writeClassEnd()
                OBJECT_TYPE
            }
            else -> writeSuperType(superClassType, sw)
        }
        sw.writeSuperclassEnd()

        val interfaces = LinkedHashSet<String>()
        val markers = LinkedHashSet<String>()
        for (type in superTypes) {
            val symbol = type.toRegularClassSymbol(session)
            val isInterface = symbol?.classKind == ClassKind.INTERFACE || symbol?.classKind == ClassKind.ANNOTATION_CLASS
            if (!isInterface) continue
            if (type === superClassType) continue
            sw.writeInterface()
            interfaces += writeSuperType(type, sw).internalName
            sw.writeInterfaceEnd()
            KOTLIN_MARKER_INTERFACES[type.classId.asSingleFqName()]?.let(markers::add)
        }
        for (marker in markers) {
            sw.writeInterface()
            sw.writeAsmType(Type.getObjectType(marker))
            sw.writeInterfaceEnd()
        }
        interfaces += markers

        node.superName = superName.internalName
        node.interfaces = interfaces.toMutableList()
        node.signature = sw.genericSignature()
    }

    private fun writeSuperType(type: ConeKotlinType, sw: JvmSignatureWriter): Type =
        mapType(type, TypeMappingMode.SUPER_TYPE, sw)

    // ---------------------------------------------------------------- members

    private fun generateEnumEntry(entry: FirEnumEntry, owner: Owner.Class) {
        val enumType = Type.getObjectType(owner.stubClass.node.name)
        val field = FieldNode(
            Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC or Opcodes.ACC_FINAL or Opcodes.ACC_ENUM or deprecatedFlag(entry),
            entry.name.asString(), enumType.descriptor, null, null,
        )
        val written = annotations.write(entry.annotations)
        field.visibleAnnotations = written.visible.toMutableList()
        field.invisibleAnnotations = written.invisible.toMutableList()
        owner.stubClass.node.fields.add(field)
        origins[field] = FirKaptFieldOrigin(
            entry, owner.stubClass.firFile, position(owner.stubClass.firFile, entry.source, null), entry.source, written.sources,
            typeSource(false, entry.returnTypeRef, null), firInitializer = null, firProperty = null, isTypeEnumInCompanion = false,
        )
        if (entry.requiresSubclass()) generateEnumEntryClass(entry, owner)
    }

    /** As in Fir2Ir: arguments alone also make an anonymous object, but only declared members make a class. */
    private fun FirEnumEntry.requiresSubclass(): Boolean {
        val initializer = initializer
        return initializer is org.jetbrains.kotlin.fir.expressions.FirAnonymousObjectExpression &&
                initializer.anonymousObject.declarations.any { it !is FirConstructor }
    }

    /** Without bodies, the class of an enum entry with a body has no members. */
    private fun generateEnumEntryClass(entry: FirEnumEntry, owner: Owner.Class) {
        val enumNode = owner.stubClass.node
        val node = ClassNode()
        node.version = classFileVersion
        node.name = enumNode.name + "$" + entry.name.asString()
        node.access = Opcodes.ACC_SUPER or Opcodes.ACC_FINAL
        node.superName = enumNode.name
        node.interfaces = mutableListOf()
        val innerClass = InnerClassNode(node.name, enumNode.name, entry.name.asString(), Opcodes.ACC_STATIC or Opcodes.ACC_FINAL)
        node.innerClasses = mutableListOf(innerClass)
        enumNode.innerClasses.add(innerClass)
        addClass(StubClass(node, owner.firFile, null, null, KotlinClassHeader.Kind.SYNTHETIC_CLASS, owner.stubClass.sourceFile))
        origins[node] = FirKaptClassOrigin(
            owner.firFile, position(owner.firFile, entry.source, null), null, emptyList(), owner.klass.classId.packageFqName,
            entry.name.asString(), isNested = true, isInner = false, isEnumClass = false, isNestedInInterface = false, firClass = null,
        )
    }

    private fun generateInstanceField(klass: FirRegularClass, owner: Owner.Class) {
        val type = Type.getObjectType(owner.stubClass.node.name)
        val field = FieldNode(
            Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC or Opcodes.ACC_FINAL, JvmAbi.INSTANCE_FIELD, type.descriptor, null, null,
        )
        field.invisibleAnnotations = mutableListOf(AnnotationNode(NOT_NULL_DESC))
        // ObjectClassLowering adds it as the first declaration.
        owner.stubClass.node.fields.add(0, field)
        origins[field] = syntheticFieldOrigin(klass, owner)
    }

    private fun generateCompanionField(companion: FirRegularClass, owner: Owner.Class) {
        val type = Type.getObjectType(JvmClassName.internalNameByClassId(companion.classId))
        val isInterface = owner.klass.classKind == ClassKind.INTERFACE
        val visibility = if (isInterface) Opcodes.ACC_PUBLIC else visibilityFlag(companion.visibility)
        val synthetic = if (isInterface && companion.visibility == Visibilities.Private) Opcodes.ACC_SYNTHETIC else 0
        val field = FieldNode(
            visibility or synthetic or Opcodes.ACC_STATIC or Opcodes.ACC_FINAL or deprecatedFlag(companion),
            companion.classId.shortClassName.asString(), type.descriptor, null, null,
        )
        field.invisibleAnnotations = mutableListOf(AnnotationNode(NOT_NULL_DESC))
        owner.stubClass.node.fields.add(field)
        origins[field] = syntheticFieldOrigin(companion, owner)
    }

    private fun syntheticFieldOrigin(declaration: FirDeclaration, owner: Owner): FirKaptFieldOrigin =
        FirKaptFieldOrigin(
            declaration, owner.firFile, KotlinPosition(owner.path, isRelativePath = false, Int.MAX_VALUE), null, emptyList(),
            typeSource(false, null, null), firInitializer = null, firProperty = null, isTypeEnumInCompanion = false,
        )

    private val Owner.firFile: FirFile get() = stubClass!!.firFile
    private val Owner.path: String get() = firFile.path

    private val FirFile.path: String get() = sourceFile?.path ?: name

    private fun generateConstructor(constructor: FirConstructor, owner: Owner.Class) {
        val klass = owner.klass
        if (klass.classKind == ClassKind.INTERFACE || klass.classKind == ClassKind.ANNOTATION_CLASS) return
        if (klass.isValueClassOnJvm()) return generateValueClassConstructor(constructor, owner)

        val parameters = mutableListOf<JvmParameter>()
        if (klass.isInner) {
            val outerType = owner.outer!!.klass.defaultType()
            parameters += JvmParameter(outerType, null, WritingParameterToGenericSignatureMode.OUTER_THIS, "this$0", emptyList(), null)
        }
        if (klass.classKind == ClassKind.ENUM_CLASS) {
            parameters += JvmParameter(
                session.builtinTypes.stringType.coneType, null, WritingParameterToGenericSignatureMode.ENUM_CONSTRUCTOR_SYNTHETIC_PARAMETER,
                "\$enum\$name", emptyList(), null,
            )
            parameters += JvmParameter(
                session.builtinTypes.intType.coneType, null, WritingParameterToGenericSignatureMode.ENUM_CONSTRUCTOR_SYNTHETIC_PARAMETER,
                "\$enum\$ordinal", emptyList(), null,
            )
        }
        parameters += constructor.valueParameters.map { it.toJvmParameter(constructor) }

        val visibility = when {
            klass.classKind == ClassKind.ENUM_CLASS -> Opcodes.ACC_PRIVATE
            klass.classKind == ClassKind.OBJECT -> Opcodes.ACC_PRIVATE
            klass.modality == Modality.SEALED -> Opcodes.ACC_PRIVATE
            else -> visibilityFlag(constructor.visibility)
        }
        val access = visibility or deprecatedFlag(constructor) or varargsFlag(constructor)
        // Constructors taking value classes are hidden behind a synthetic one with a marker parameter.
        val isHidden = visibility != Opcodes.ACC_PRIVATE && constructor.valueParameters.any { it.returnTypeRef.coneType.requiresMangling() }
        addMethod(
            constructor, owner, "<init>", if (isHidden) (access and VISIBILITY_FLAGS.inv()) or Opcodes.ACC_PRIVATE else access,
            parameters, returnType = null, typeParameters = emptyList(), isGetter = false, bindMetadata = !isHidden,
        )
        if (isHidden) {
            val marker = JvmParameter(
                DEFAULT_CONSTRUCTOR_MARKER.constructClassLikeType(isMarkedNullable = true), null,
                WritingParameterToGenericSignatureMode.REGULAR, null, emptyList(), null,
            )
            addMethod(
                constructor, owner, "<init>", access or Opcodes.ACC_SYNTHETIC, parameters + marker, returnType = null,
                typeParameters = emptyList(), isGetter = false, annotationsFrom = null,
            )
        }

        if (constructor.isPrimary && constructor.valueParameters.isNotEmpty() && constructor.valueParameters.all { it.defaultValue != null } &&
            klass.classKind == ClassKind.CLASS && !klass.isInner && !klass.isValue && !klass.isLocal && klass.modality != Modality.SEALED &&
            constructor.visibility != Visibilities.Private && !constructor.hasAnnotation(JVM_OVERLOADS) &&
            klass.declarations.none { it is FirConstructor && it.valueParameters.isEmpty() }
        ) {
            addMethod(
                constructor, owner, "<init>", visibilityFlag(constructor.visibility) or deprecatedFlag(constructor),
                emptyList(), returnType = null, typeParameters = emptyList(), isGetter = false, annotationsFrom = constructor,
                bindMetadata = false, synthesizedPosition = true,
            )
        }
        generateVersionOverloads(
            constructor, owner, "<init>", access, parameters.dropLast(constructor.valueParameters.size), null, emptyList(),
        )
        if (constructor.hasAnnotation(JVM_OVERLOADS)) {
            generateJvmOverloads(constructor, owner, "<init>", access and Opcodes.ACC_VARARGS.inv(), parameters.dropLast(constructor.valueParameters.size), null, emptyList(), isStatic = false)
        }
    }

    private fun FirValueParameter.toJvmParameter(function: FirFunction): JvmParameter {
        val type = returnTypeRef.coneType.let { if (isVararg) it else it }
        return JvmParameter(type, returnTypeRef, WritingParameterToGenericSignatureMode.REGULAR, name.identifierOrNullIfSpecial, annotations, this)
    }

    private class JvmParameter(
        val type: ConeKotlinType,
        val typeRef: FirTypeRef?,
        val mode: WritingParameterToGenericSignatureMode,
        val name: String?,
        val annotations: List<FirAnnotation>,
        val source: FirDeclaration?,
        val isContinuation: Boolean = false,
        /** The type that decides the nullability annotation, for parameters whose JVM type is not their Kotlin one. */
        val nullabilityType: ConeKotlinType = type,
    )

    private fun generateFunction(function: FirNamedFunction, owner: Owner) {
        if (function.isLocal) return
        if (isValueClassStaticMember(function, owner)) return generateValueClassFunction(function, owner as Owner.Class)
        val klass = (owner as? Owner.Class)?.klass
        val isJvmStatic = function.hasAnnotation(JVM_STATIC)

        val name = functionJvmName(function, owner)
        val declaredParameters = callableParameters(function) + function.valueParameters.map { it.toJvmParameter(function) } +
                listOfNotNull(continuationParameter(function))
        // Overrides of members like `contains(E)` take `Object`, or keep the typed method and get a final `Object` bridge.
        val specialParameters = specialOverride(function, owner)?.let { declaredParameters.toSpecial(it, callableParameters(function).size) }
        val parameters = specialParameters
            ?.takeIf { name == function.name.asString() && descriptorOf(it) == descriptorOf(declaredParameters) } ?: declaredParameters
        val returnType = if (function.isSuspend) session.builtinTypes.nullableAnyType.coneType else function.returnTypeRef.coneType

        val isStatic = owner is Owner.Facade || (isJvmStatic && klass?.classKind == ClassKind.OBJECT && !klass.isCompanion) ||
                function.isEnumGenerated() || function.isCompanionBlockMember
        val access = methodAccessFlags(function, owner, isStatic)
        val node = addMethod(
            function, owner, name, access, parameters, returnType.takeUnless { function.returnsVoid() }, function.typeParameters,
            isGetter = false, forceBoxedReturn = forceBoxedReturnType(function, owner), nullability = !function.isEnumGenerated(),
        )
        if (specialParameters != null && parameters !== specialParameters) {
            addMethod(
                function, owner, function.name.asString(), Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL, specialParameters,
                returnType.takeUnless { function.returnsVoid() },
                function.typeParameters, isGetter = false, bindMetadata = false, nullability = false,
                jvmOverride = true, firSource = (owner as Owner.Class).klass.source, withExceptions = false, position = classPosition(owner),
            )
        }
        // AddContinuationLowering keeps a private template without the state machine for the inliner.
        if (function.isSuspend && function.isInline && function.hasBody() && function.visibility != Visibilities.Private &&
            !function.isReifiable() && !function.hasAnnotation(INLINE_ONLY)
        ) {
            addMethod(
                function, owner, name + FOR_INLINE_SUFFIX, access and VISIBILITY_FLAGS.inv() or Opcodes.ACC_PRIVATE, parameters,
                returnType, function.typeParameters, isGetter = false, bindMetadata = false, synthesizedPosition = true, firSource = null,
            )
        }
        val wrappers = generateVersionOverloads(
            function, owner, name, node.access, callableParameters(function), returnType.takeUnless { function.returnsVoid() },
            function.typeParameters, suffix = listOfNotNull(continuationParameter(function)),
        ).toMutableList()
        if (function.hasAnnotation(JVM_OVERLOADS)) {
            wrappers += generateJvmOverloads(
                function, owner, name, node.access and Opcodes.ACC_VARARGS.inv(), callableParameters(function),
                returnType.takeUnless { function.returnsVoid() }, function.typeParameters, isStatic,
                suffix = listOfNotNull(continuationParameter(function)),
            )
        }
        if (klass?.classKind == ClassKind.INTERFACE && !isStatic) {
            requireDefaultImplsForDefaultArguments(function, owner)
            if (function.hasBody()) {
                placeInterfaceMember(
                    function, owner, node, parameters, returnType.takeUnless { function.returnsVoid() }, function.typeParameters,
                    isGetter = false, propertyForTypes = null, positionSource = function,
                )
            }
        }

        if (isJvmStatic && klass != null && klass.isCompanion) {
            val outer = owner.outer ?: return
            val staticAccess = staticProxyAccess(access, outer)
            addMethod(
                function, outer, name, staticAccess, parameters, returnType.takeUnless { function.returnsVoid() }, function.typeParameters,
                isGetter = false, bindMetadata = false, jvmOverride = false,
            )
            for (wrapper in wrappers) addStaticProxy(wrapper, outer)
        }
    }

    private fun addStaticProxy(method: MethodNode, outer: Owner.Class) {
        val proxy = MethodNode(
            Opcodes.API_VERSION, staticProxyAccess(method.access, outer), method.name, method.desc, method.signature,
            method.exceptions?.toTypedArray(),
        )
        proxy.visibleAnnotations = method.visibleAnnotations
        proxy.invisibleAnnotations = method.invisibleAnnotations
        proxy.visibleParameterAnnotations = method.visibleParameterAnnotations
        proxy.invisibleParameterAnnotations = method.invisibleParameterAnnotations
        outer.stubClass.node.methods.add(proxy)
        origins[method]?.let { origins[proxy] = it }
    }

    private fun generateValueClassFunction(function: FirNamedFunction, owner: Owner.Class) {
        val klass = owner.klass
        val parameters = callableParameters(function) + function.valueParameters.map { it.toJvmParameter(function) } +
                listOfNotNull(continuationParameter(function))
        val returnType = (if (function.isSuspend) session.builtinTypes.nullableAnyType.coneType else function.returnTypeRef.coneType)
            .takeUnless { function.returnsVoid() }
        addMethod(
            function, owner, functionJvmName(function, owner),
            visibilityFlag(function.visibility) or Opcodes.ACC_STATIC or Opcodes.ACC_FINAL or varargsFlag(function) or deprecatedFlag(function),
            listOf(valueClassThisParameter(klass)) + parameters, returnType, klass.typeParameters + function.typeParameters,
            isGetter = false, jvmOverride = false,
        )
        if (function.isOverride) {
            addMethod(
                function, owner, function.jvmNameFromAnnotation() ?: function.name.asString(), visibilityFlag(function.visibility) or varargsFlag(function),
                parameters, returnType, function.typeParameters, isGetter = false, bindMetadata = false,
            )
        }
    }

    private fun staticProxyAccess(access: Int, outer: Owner.Class): Int {
        val isInterface = outer.klass.classKind == ClassKind.INTERFACE || outer.klass.classKind == ClassKind.ANNOTATION_CLASS
        return (access and Opcodes.ACC_ABSTRACT.inv() and (if (isInterface) Opcodes.ACC_FINAL.inv() else -1)) or Opcodes.ACC_STATIC
    }

    private fun FirDeclaration.isEnumGenerated(): Boolean = source?.kind is KtFakeSourceElementKind.EnumGeneratedDeclaration

    private fun FirNamedFunction.returnsVoid(): Boolean =
        !isSuspend && returnTypeRef.coneType.fullyExpandedType(session).let { it.isUnit && !it.isMarkedNullable }

    /** [hasBody] is for the generated method: delegates have bodies even if what they delegate to has none here. */
    private fun callableParameters(callable: FirCallableDeclaration, hasBody: Boolean? = null): List<JvmParameter> {
        val result = mutableListOf<JvmParameter>()
        for (parameter in callable.contextParameters) {
            result += JvmParameter(
                parameter.returnTypeRef.coneType, parameter.returnTypeRef, WritingParameterToGenericSignatureMode.REGULAR,
                parameter.name.identifierOrNullIfSpecial, parameter.annotations, parameter,
            )
        }
        callable.receiverParameter?.takeUnless { callable.isCompanionExtension }?.let { receiver ->
            // Only receivers of functions with bodies are renamed, abstract ones keep a special name.
            val name = when (callable) {
                is FirProperty -> callable.name.takeIf { hasBody ?: !callable.isAbstract }
                is FirNamedFunction -> callable.name.takeIf { hasBody ?: callable.hasBody() }
                else -> null
            }
            result += JvmParameter(
                receiver.typeRef.coneType, receiver.typeRef, WritingParameterToGenericSignatureMode.REGULAR,
                name?.let { AsmUtil.LABELED_THIS_PARAMETER + it.asString() }, receiver.annotations, receiver,
            )
        }
        return result
    }

    private fun continuationParameter(function: FirNamedFunction): JvmParameter? {
        if (!function.isSuspend) return null
        val continuationType = StandardClassIds.Continuation.constructClassLikeType(arrayOf(function.returnTypeRef.coneType))
        return JvmParameter(
            continuationType, function.returnTypeRef, WritingParameterToGenericSignatureMode.REGULAR, "\$completion", emptyList(), null,
            isContinuation = true,
        )
    }

    private fun generateJvmOverloads(
        function: FirFunction, owner: Owner, name: String, access: Int, prefix: List<JvmParameter>, returnType: ConeKotlinType?,
        typeParameters: List<FirTypeParameterRef>, isStatic: Boolean, suffix: List<JvmParameter> = emptyList(),
    ): List<MethodNode> {
        val result = mutableListOf<MethodNode>()
        val valueParameters = function.valueParameters
        val defaults = valueParameters.count { it.defaultValue != null }
        val hasIntroducedAt = valueParameters.any { it.hasAnnotation(StandardClassIds.Annotations.IntroducedAt) }
        for (skip in 1..defaults) {
            var toSkip = skip
            val kept = valueParameters.asReversed().filter { parameter ->
                if (toSkip > 0 && parameter.defaultValue != null) {
                    toSkip--
                    false
                } else true
            }.asReversed()
            val isInterface = (owner as? Owner.Class)?.klass?.classKind == ClassKind.INTERFACE
            val modality = if (function is FirConstructor || isStatic || isInterface) 0 else Opcodes.ACC_FINAL
            val wrapperAccess = (access and (Opcodes.ACC_ABSTRACT or Opcodes.ACC_STRICT or Opcodes.ACC_SYNCHRONIZED).inv()) or modality or
                    (if (kept.lastOrNull()?.isVararg == true) Opcodes.ACC_VARARGS else 0)
            val wrapper = addMethod(
                function, owner, wrapperName(function, owner, name, kept), wrapperAccess,
                prefix + kept.map { it.toJvmParameter(function) } + suffix, returnType, typeParameters, isGetter = false, bindMetadata = false,
            )
            val methods = owner.stubClass!!.node.methods
            if (hasIntroducedAt && methods.any { it !== wrapper && it.name == wrapper.name && it.desc == wrapper.desc }) {
                methods.remove(wrapper)
                origins.remove(wrapper)
            } else {
                result += wrapper
            }
        }
        return result
    }

    /**
     * Same as VersionOverloadsLowering: a deprecated overload for each earlier version of a function whose parameters with default
     * values are annotated with `@IntroducedAt`.
     */
    private fun generateVersionOverloads(
        function: FirFunction, owner: Owner, name: String, access: Int, prefix: List<JvmParameter>, returnType: ConeKotlinType?,
        typeParameters: List<FirTypeParameterRef>, suffix: List<JvmParameter> = emptyList(),
    ): List<MethodNode> {
        val versionSource = if (function is FirNamedFunction && function.source?.kind is KtFakeSourceElementKind.DataClassGeneratedMembers &&
            function.name == StandardNames.DATA_CLASS_COPY
        ) (owner as? Owner.Class)?.klass?.primaryConstructorIfAny(session)?.fir ?: return emptyList() else function
        val versions = versionSource.valueParameters.map { parameter ->
            if (parameter.defaultValue == null) null
            else parameter.annotations.firstOrNull { it.annotationTypeRef.coneTypeOrNull?.classId == StandardClassIds.Annotations.IntroducedAt }
                ?.let { (it.argumentMapping.mapping[Name.identifier("version")] as? FirLiteralExpression)?.value as? String }
        }
        if (versions.all { it == null }) return emptyList()
        val result = mutableListOf<MethodNode>()
        val groups = versions.withIndex().groupBy({ it.value?.let(::MavenComparableVersion) }, { it.index })
            .toSortedMap(nullsLast(compareByDescending { it }))
        val included = BooleanArray(versions.size) { true }
        var first = true
        for ([_, indexes] in groups) {
            if (!first) {
                val parameters = function.valueParameters.filterIndexed { index, _ -> included[index] }
                result += addMethod(
                    function, owner, wrapperName(function, owner, name, parameters),
                    (access and (Opcodes.ACC_VARARGS or Opcodes.ACC_SYNTHETIC).inv()) or Opcodes.ACC_DEPRECATED or
                            (if (parameters.lastOrNull()?.isVararg == true) Opcodes.ACC_VARARGS else 0),
                    prefix + parameters.map { it.toJvmParameter(function) } + suffix, returnType, typeParameters, isGetter = false,
                    bindMetadata = false, explicitAnnotations = function.annotations.filterNot {
                        it.annotationTypeRef.coneTypeOrNull?.classId == JVM_OVERLOADS
                    },
                )
            }
            first = false
            for (index in indexes) included[index] = false
        }
        return result
    }

    /** Overloads are mangled for their own parameters. */
    private fun wrapperName(function: FirFunction, owner: Owner, name: String, parameters: List<FirValueParameter>): String =
        if (function is FirNamedFunction) functionJvmName(function, owner, parameters) else name

    private fun functionJvmName(
        function: FirNamedFunction, owner: Owner, valueParameters: List<FirValueParameter> = function.valueParameters,
        inValueClass: Boolean = isValueClassStaticMember(function, owner),
    ): String {
        function.jvmNameFromAnnotation()?.let { return it }
        val parameterTypes = (function.contextParameters + listOfNotNull(function.receiverParameter.takeUnless { function.isCompanionExtension }) +
                valueParameters)
            .map { it.typeForMangling() }
        val name = inlineClassMangledName(
            function.name.asString(), parameterTypes, function.returnTypeRef.coneType, function.isSuspend, owner,
            inValueClass = inValueClass,
        )
        return mangleIfInternal(name, function, owner)
    }

    private fun FirElement.typeForMangling(): ConeKotlinType = when (this) {
        is FirReceiverParameter -> typeRef.coneType
        is FirValueParameter -> returnTypeRef.coneType
        else -> error("Unexpected parameter: $this")
    }

    /** Same as `InlineClassAbi.mangledNameFor`, for functions outside of value classes. */
    private fun inlineClassMangledName(
        name: String, parameterTypes: List<ConeKotlinType>, returnType: ConeKotlinType?, isSuspend: Boolean, owner: Owner,
        inValueClass: Boolean = false,
    ): String {
        val mangledParameters = parameterTypes.any { it.requiresMangling() }
        val mangledReturnType = returnType != null && owner !is Owner.Facade && returnType.inlineClassOrNull() != null
        if (!mangledParameters && !mangledReturnType) return if (inValueClass) "$name-impl" else name
        val parameterInfos = parameterTypes.map { it.infoForMangling() } +
                listOfNotNull(InfoForMangling(FqNameUnsafe("kotlin.coroutines.Continuation"), isValue = false, isNullable = false).takeIf { isSuspend })
        val signature = collectFunctionSignatureForManglingSuffix(
            useOldManglingRules = false, mangledParameters, parameterInfos, returnType?.takeIf { mangledReturnType }?.infoForMangling(),
        ) ?: return name
        return "$name-${md5base64(signature)}"
    }

    private fun FirRegularClass.isValueClassOnJvm(): Boolean = symbol.inlineClassRepresentationInJvm() != null

    /** A member of a value class compiled to a static `-impl` method taking the unboxed value. */
    private fun isValueClassStaticMember(declaration: FirCallableDeclaration, owner: Owner): Boolean {
        val klass = (owner as? Owner.Class)?.klass ?: return false
        if (!klass.isValueClassOnJvm() || declaration.isCompanionBlockMember) return false
        val property = (declaration as? FirPropertyAccessor)?.propertySymbol?.fir ?: declaration as? FirProperty
        return property?.name != klass.symbol.inlineClassRepresentationInJvm()?.underlyingPropertyName
    }

    private fun valueClassThisParameter(klass: FirRegularClass): JvmParameter =
        JvmParameter(klass.defaultType(), null, WritingParameterToGenericSignatureMode.REGULAR, "arg0", emptyList(), null)

    /** Same as JvmInlineClassLowering: a private synthetic constructor and static `constructor-impl` functions. */
    private fun generateValueClassConstructor(constructor: FirConstructor, owner: Owner.Class) {
        val klass = owner.klass
        val parameters = constructor.valueParameters.map { it.toJvmParameter(constructor) }
        if (constructor.isPrimary) {
            addMethod(
                constructor, owner, "<init>", Opcodes.ACC_PRIVATE or Opcodes.ACC_SYNTHETIC, parameters, returnType = null,
                typeParameters = emptyList(), isGetter = false, bindMetadata = false,
            )
        }
        val underlyingType = klass.symbol.inlineClassRepresentationInJvm()!!.underlyingType
        addMethod(
            constructor, owner, "constructor-impl", visibilityFlag(constructor.visibility) or Opcodes.ACC_STATIC or varargsFlag(constructor),
            parameters, underlyingType, klass.typeParameters, isGetter = false,
        )
    }

    /** Synthesized `equals`, `hashCode` and `toString` of a value class: static implementations and the overriding methods. */
    private fun generateValueClassAnyMembers(owner: Owner.Class) {
        val klass = owner.klass
        val scope = klass.unsubstitutedScope(session, scopeSession, withForcedTypeCalculator = true, memberRequiredPhase = null)
        val thisParameter = valueClassThisParameter(klass)
        for (name in listOf(OperatorNameConventions.EQUALS, OperatorNameConventions.HASH_CODE, OperatorNameConventions.TO_STRING)) {
            scope.processFunctionsByName(name) { symbol ->
                val function = symbol.fir
                if (function.origin !is FirDeclarationOrigin.Synthetic || symbol.containingClassLookupTag() != klass.symbol.toLookupTag()) {
                    return@processFunctionsByName
                }
                val parameters = function.valueParameters.map { it.toJvmParameter(function) }
                val returnType = function.returnTypeRef.coneType
                addMethod(
                    function, owner, "${name.asString()}-impl", Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC, listOf(thisParameter) + parameters,
                    returnType, klass.typeParameters, isGetter = false, synthesizedPosition = true, firSource = null,
                )
                addMethod(
                    function, owner, name.asString(), Opcodes.ACC_PUBLIC, parameters, returnType, emptyList(), isGetter = false,
                    bindMetadata = false, synthesizedPosition = true, jvmOverride = true, firSource = null, nullability = false,
                )
            }
        }
    }

    private fun ConeKotlinType.inlineClassOrNull(): FirRegularClassSymbol? =
        fullyExpandedType(session).toRegularClassSymbol(session)?.takeIf { it.inlineClassRepresentationInJvm() != null }

    private fun ConeKotlinType.erasedUpperBoundClass(): FirRegularClassSymbol? {
        val expanded = fullyExpandedType(session).lowerBoundIfFlexible()
        if (expanded is ConeTypeParameterType) {
            val bound = (expanded.lookupTag as? org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTagImpl)
                ?.typeParameterSymbol?.resolvedBounds?.firstOrNull()?.coneType ?: return null
            return bound.erasedUpperBoundClass()
        }
        return expanded.toRegularClassSymbol(session)
    }

    private fun ConeKotlinType.requiresMangling(): Boolean {
        val klass = erasedUpperBoundClass() ?: return false
        return klass.inlineClassRepresentationInJvm() != null && klass.classId != StandardClassIds.Result
    }

    private fun ConeKotlinType.infoForMangling(): InfoForMangling =
        InfoForMangling(
            (erasedUpperBoundClass()?.classId ?: StandardClassIds.Any).asSingleFqName().toUnsafe(),
            isValue = inlineClassOrNull() != null,
            isNullable = fullyExpandedType(session).isMarkedOrFlexiblyNullable,
        )

    private fun mangleIfInternal(name: String, declaration: FirCallableDeclaration, owner: Owner): String {
        if (owner is Owner.Facade) return name
        if (declaration.visibility != Visibilities.Internal || declaration.hasAnnotation(PUBLISHED_API)) return name
        return name + "$" + NameUtils.sanitizeAsJavaIdentifier(moduleName)
    }

    private fun FirDeclaration.jvmNameFromAnnotation(): String? =
        annotations.firstOrNull { it.annotationTypeRef.coneTypeOrNull?.classId == JvmStandardClassIds.Annotations.JvmName }
            ?.let { (it.argumentMapping.mapping[StandardNames.NAME] as? FirLiteralExpression)?.value as? String }

    private fun methodAccessFlags(function: FirCallableDeclaration, owner: Owner, isStatic: Boolean, modalitySource: FirCallableDeclaration = function): Int {
        val klass = (owner as? Owner.Class)?.klass
        val isInterface = klass?.classKind == ClassKind.INTERFACE || klass?.classKind == ClassKind.ANNOTATION_CLASS
        val hasBody = when (function) {
            is FirFunction -> function.body != null || function.hasBodyInSource()
            else -> false
        }
        val modality = when {
            klass?.classKind == ClassKind.ANNOTATION_CLASS -> if (isStatic) 0 else Opcodes.ACC_ABSTRACT
            function.isEnumGenerated() || modalitySource.isEnumGenerated() -> 0
            isStatic -> if (owner is Owner.Facade) Opcodes.ACC_FINAL else modalityFlags(modalitySource.modality ?: Modality.FINAL)
            else -> when (modalitySource.modality ?: Modality.FINAL) {
                Modality.FINAL -> if (isInterface && hasBody) 0 else Opcodes.ACC_FINAL
                Modality.ABSTRACT -> Opcodes.ACC_ABSTRACT
                else -> if (isInterface && !hasBody) Opcodes.ACC_ABSTRACT else 0
            }
        }
        val isPublicPrivateInterfaceMethod = klass?.classKind == ClassKind.INTERFACE && !isStatic && hasBody &&
                function.visibility == Visibilities.Private && jvmDefaultMode != JvmDefaultMode.DISABLE &&
                (configuration.jvmTarget ?: JvmTarget.DEFAULT) == JvmTarget.JVM_1_8
        val visibility = if (isPublicPrivateInterfaceMethod) Opcodes.ACC_PUBLIC else visibilityFlag(function.visibility)
        val synthetic = if (function.hasAnnotation(JVM_SYNTHETIC) || function.isReifiable() || function.isDeprecatedHidden()) Opcodes.ACC_SYNTHETIC else 0
        return visibility or modality or deprecatedFlag(function) or (if (isStatic) Opcodes.ACC_STATIC else 0) or
                varargsFlag(function) or (if (function.isExternal) Opcodes.ACC_NATIVE else 0) or synthetic or
                (if (function.hasAnnotation(STRICTFP)) Opcodes.ACC_STRICT else 0) or
                (if (function.hasAnnotation(SYNCHRONIZED)) Opcodes.ACC_SYNCHRONIZED else 0)
    }

    private fun FirFunction.hasBody(): Boolean = body != null || hasBodyInSource()

    private fun FirFunction.hasBodyInSource(): Boolean =
        source?.let { source ->
            source.lighterASTNode.getChildren(source.treeStructure).any { it.tokenType == org.jetbrains.kotlin.KtNodeTypes.BLOCK || it.tokenType == KtTokens.EQ }
        } == true

    private fun FirCallableDeclaration.isReifiable(): Boolean =
        this is FirNamedFunction && isInline && typeParameters.any { it.symbol.isReified }

    private fun FirCallableDeclaration.isDeprecatedHidden(): Boolean {
        val deprecated = annotations.firstOrNull { it.annotationTypeRef.coneTypeOrNull?.classId == StandardClassIds.Annotations.Deprecated }
            ?: return false
        val level = deprecated.argumentMapping.mapping[StandardClassIds.Annotations.ParameterNames.deprecatedLevel] ?: return false
        return (level as? FirQualifiedAccessExpression)?.calleeReference?.toResolvedEnumEntrySymbol()?.name?.asString() ==
                DeprecationLevel.HIDDEN.name
    }

    private fun visibilityFlag(visibility: Visibility): Int = when (visibility) {
        Visibilities.Public, Visibilities.Internal -> Opcodes.ACC_PUBLIC
        Visibilities.Protected -> Opcodes.ACC_PROTECTED
        Visibilities.Private, Visibilities.PrivateToThis -> Opcodes.ACC_PRIVATE
        JavaVisibilities.PackageVisibility -> 0
        else -> Opcodes.ACC_PUBLIC
    }

    private fun varargsFlag(function: FirCallableDeclaration): Int =
        if (function is FirFunction && function.valueParameters.lastOrNull()?.isVararg == true) Opcodes.ACC_VARARGS else 0

    private fun deprecatedFlag(declaration: FirDeclaration): Int =
        if (declaration.annotations.any {
                val classId = it.annotationTypeRef.coneTypeOrNull?.classId
                classId == StandardClassIds.Annotations.Deprecated || classId == JvmStandardClassIds.Annotations.Java.Deprecated
            }
        ) Opcodes.ACC_DEPRECATED else 0

    private fun FirDeclaration.hasAnnotation(classId: ClassId): Boolean =
        annotations.any { it.annotationTypeRef.coneTypeOrNull?.fullyExpandedType(session)?.classId == classId }

    private fun forceBoxedReturnType(property: FirProperty, owner: Owner): Boolean {
        val klass = (owner as? Owner.Class)?.klass ?: return false
        if (!property.isOverride) return false
        val returnType = property.returnTypeRef.coneType.fullyExpandedType(session)
        if (!returnType.isPrimitive || returnType.isMarkedNullable) return false
        val scope = klass.unsubstitutedScope(session, scopeSession, withForcedTypeCalculator = true, memberRequiredPhase = null)
        scope.processPropertiesByName(property.name) {}
        var result = false
        scope.processOverriddenProperties(property.symbol) { overridden ->
            val type = overridden.resolvedReturnType.fullyExpandedType(session)
            if (!type.isPrimitive || type.isMarkedNullable) {
                result = true
                ProcessorAction.STOP
            } else ProcessorAction.NEXT
        }
        return result
    }

    private fun forceBoxedReturnType(function: FirNamedFunction, owner: Owner): Boolean {
        val klass = (owner as? Owner.Class)?.klass ?: return false
        if (!function.isOverride) return false
        val returnType = function.returnTypeRef.coneType.fullyExpandedType(session)
        if (!returnType.isPrimitive || returnType.isMarkedNullable) return false
        val scope = klass.unsubstitutedScope(session, scopeSession, withForcedTypeCalculator = true, memberRequiredPhase = null)
        scope.processFunctionsByName(function.name) {}
        var result = false
        scope.processOverriddenFunctions(function.symbol) { overridden ->
            val type = overridden.resolvedReturnType.fullyExpandedType(session)
            if (!type.isPrimitive || type.isMarkedNullable) {
                result = true
                ProcessorAction.STOP
            } else ProcessorAction.NEXT
        }
        return result
    }

    /** `equals`, `hashCode` and `toString` that FIR synthesizes for a data class, in the order of DataClassMembersGenerator. */
    private fun generateDataClassMembers(klass: FirRegularClass, owner: Owner.Class) {
        val scope = klass.unsubstitutedScope(session, scopeSession, withForcedTypeCalculator = true, memberRequiredPhase = null)
        for (name in listOf(OperatorNameConventions.TO_STRING, OperatorNameConventions.HASH_CODE, OperatorNameConventions.EQUALS)) {
            scope.processFunctionsByName(name) { symbol ->
                val function = symbol.fir
                if (function.origin !is FirDeclarationOrigin.Synthetic || symbol.containingClassLookupTag() != klass.symbol.toLookupTag()) {
                    return@processFunctionsByName
                }
                // Their source is the class, so the backend copies the class KDoc to them.
                addMethod(
                    function, owner, name.asString(), Opcodes.ACC_PUBLIC, function.valueParameters.map { it.toJvmParameter(function) },
                    function.returnTypeRef.coneType, emptyList(), isGetter = false, bindMetadata = false, synthesizedPosition = true,
                    jvmOverride = true,
                )
            }
        }
    }

    // ---------------------------------------------------------------- properties

    private fun generateProperty(property: FirProperty, owner: Owner) {
        if (property.isLocal) return
        val klass = (owner as? Owner.Class)?.klass
        val isJvmField = property.hasJvmField()
        val isConst = property.isConst
        val isCompanionMember = klass?.isCompanion == true
        val isObjectMember = klass?.classKind == ClassKind.OBJECT && !klass.isCompanion
        val isJvmStatic = property.hasAnnotation(JVM_STATIC)

        val hasField = klass?.classKind != ClassKind.ANNOTATION_CLASS &&
                (property.delegate != null || (property.hasBackingField && !property.isExpect))
        if (hasField) generateField(property, owner, isJvmField, isConst, isCompanionMember, isObjectMember)

        // The line mapping of `$annotations` needs the getter or the field in the same class, a companion field moves out.
        val outerKind = (owner as? Owner.Class)?.outer?.klass?.classKind
        val fieldMovedOut = isCompanionMember && hasField && (outerKind != ClassKind.INTERFACE || isJvmField)
        if (isJvmField || isConst) {
            generateAnnotationsMethod(property, owner, withoutPosition = fieldMovedOut)
            return
        }

        val getter = property.getter
        val hasGetter = getter != null && !(getter.isDefaultAccessor() && getter.visibility == Visibilities.Private)
        if (hasGetter) generateAccessor(property, getter, owner, isJvmStatic)
        val setter = property.setter
        if (property.isVar && setter != null && !(setter.isDefaultAccessor() && setter.visibility == Visibilities.Private)) {
            generateAccessor(property, setter, owner, isJvmStatic)
        }
        generateAnnotationsMethod(property, owner, withoutPosition = !hasGetter && fieldMovedOut)
    }

    /** An accessor written in source, even without a body like `protected set`, has its own position. */
    private fun FirPropertyAccessor.positionSource(property: FirProperty): FirDeclaration =
        if (source?.kind is KtRealSourceElementKind) this else property

    private fun FirPropertyAccessor.isDefaultAccessor(): Boolean = source?.kind is org.jetbrains.kotlin.KtFakeSourceElementKind.DefaultAccessor || body == null && !hasBodyInSource()

    private fun FirProperty.hasJvmField(): Boolean =
        hasAnnotation(JvmStandardClassIds.Annotations.JvmField) || backingField?.hasAnnotation(JvmStandardClassIds.Annotations.JvmField) == true

    private fun generateField(
        property: FirProperty, owner: Owner, isJvmField: Boolean, isConst: Boolean, isCompanionMember: Boolean, isObjectMember: Boolean,
    ) {
        val klass = (owner as? Owner.Class)?.klass
        val isDelegate = property.delegate != null
        val name = if (isDelegate) property.name.asString() + JvmAbi.DELEGATED_PROPERTY_NAME_SUFFIX else property.name.asString()
        // Like Fir2Ir, the field of `field: T` has the type and the offsets of the explicit backing field.
        val explicitBackingField = property.getExplicitBackingField()
        val fieldTypeRef = explicitBackingField?.returnTypeRef ?: property.returnTypeRef
        val type = if (isDelegate) property.delegate!!.resolvedType else fieldTypeRef.coneType
        val typeRef = if (isDelegate) property.backingField?.returnTypeRef else fieldTypeRef

        val visibility = when {
            isConst || isJvmField -> property.visibility
            property.isLateInit -> property.setter?.visibility ?: property.visibility
            else -> Visibilities.Private
        }
        // Fields of companion object properties move to the class. An interface only gets the @JvmField fields, and copies of
        // the constants.
        val outerOfCompanion = if (isCompanionMember) (owner as Owner.Class).outer else null
        val isInterfaceCompanion = outerOfCompanion?.klass?.classKind == ClassKind.INTERFACE
        val target: Owner = when {
            outerOfCompanion == null -> owner
            !isInterfaceCompanion || isJvmField -> outerOfCompanion
            else -> owner
        }
        val isStatic = owner is Owner.Facade || isCompanionMember || isObjectMember || property.isCompanionBlockMember
        val isFinal = !property.isVar || isDelegate
        val access = visibilityFlag(visibility) or (if (isStatic) Opcodes.ACC_STATIC else 0) or (if (isFinal) Opcodes.ACC_FINAL else 0) or
                deprecatedFlag(property) or
                (if (property.backingField?.hasAnnotation(VOLATILE) == true || property.hasAnnotation(VOLATILE)) Opcodes.ACC_VOLATILE else 0) or
                (if (property.backingField?.hasAnnotation(TRANSIENT) == true || property.hasAnnotation(TRANSIENT)) Opcodes.ACC_TRANSIENT else 0) or
                (if (property.hasAnnotation(JVM_SYNTHETIC) || property.backingField?.hasAnnotation(JVM_SYNTHETIC) == true) Opcodes.ACC_SYNTHETIC else 0)

        val mode = if (property.isVar && !isDelegate) parameterMode(property, type) else returnMode(property, type)
        val sw = BothSignatureWriter(BothSignatureWriter.Mode.TYPE)
        val asmType = mapType(type, mode, sw)
        val value = if (isConst) constantValue(property) else null

        fun addTo(target: Owner) {
            // Moved out of a private companion, a non-private field becomes visible, so the backend deprecates it.
            val deprecated = target !== owner && (owner as Owner.Class).klass.visibility == Visibilities.Private &&
                    visibility != Visibilities.Private
            // Delegate fields have no generic signature, unless they are companion fields, which the backend re-creates as static ones.
            val node = FieldNode(
                access or (if (deprecated) Opcodes.ACC_DEPRECATED else 0), name, asmType.descriptor,
                sw.genericSignature().takeUnless { isDelegate && !isCompanionMember }, value,
            )
            if (visibility == Visibilities.Public || visibility == Visibilities.Protected) publicApiFields += node
            if (target === owner || !(isConst && isInterfaceCompanion)) {
                owner.stubClass?.serializer?.let { fieldBindings += Triple(it, property, node) }
            }
            val written = annotations.write(property.backingField?.annotations.orEmpty())
            node.visibleAnnotations = written.visible.toMutableList()
            node.invisibleAnnotations =
                written.invisible.plusNullability(nullabilityAnnotation(type, asmType).takeUnless { property.isLateInit }).toMutableList()
            target.stubClass!!.node.fields.add(node)
            // Like Fir2Ir, a delegate field has the offsets of the delegate expression.
            val fieldPosition = if (isDelegate) position(owner.firFile, property.delegate?.source, null)
            else position(owner.firFile, (explicitBackingField ?: property).source, VAL_VAR)
            origins[node] = FirKaptFieldOrigin(
                property, owner.firFile, fieldPosition, property.source, written.sources,
                typeSource(type.containsErrorTypes(), typeRef, mode),
                firInitializer = if (isDelegate) null else property.initializer, firProperty = property,
                isTypeEnumInCompanion = type.isEnumInCompanion(),
            )
        }

        addTo(target)
        if (isConst && isInterfaceCompanion && target === owner &&
            property.visibility != Visibilities.Private && property.visibility != Visibilities.Protected
        ) {
            addTo(outerOfCompanion)
        }
    }

    private fun ConeKotlinType.isEnumInCompanion(): Boolean {
        val symbol = fullyExpandedType(session).toRegularClassSymbol(session) ?: return false
        if (symbol.classKind != ClassKind.ENUM_CLASS) return false
        var outerId = symbol.classId.outerClassId
        while (outerId != null) {
            val outer = session.symbolProvider.getClassLikeSymbolByClassId(outerId) as? FirRegularClassSymbol ?: return false
            if (outer.isCompanion) return true
            outerId = outerId.outerClassId
        }
        return false
    }

    private fun constantValue(property: FirProperty): Any? {
        val result = FirExpressionEvaluator.evaluatePropertyInitializer(property, session)
        val literal = result?.resultOrNull<FirLiteralExpression>() ?: return null
        return literal.toAsmConstant()
    }

    private fun accessorJvmName(
        property: FirProperty, accessor: FirPropertyAccessor, owner: Owner, inValueClass: Boolean = isValueClassStaticMember(accessor, owner),
    ): String {
        accessor.jvmNameFromAnnotation()?.let { return it }
        val klass = (owner as? Owner.Class)?.klass
        val propertyName = property.name.asString()
        if (klass?.classKind == ClassKind.ANNOTATION_CLASS) return propertyName
        if (klass != null && klass.hasAnnotation(JvmStandardClassIds.Annotations.JvmRecord)) return propertyName
        val name = if (accessor.isGetter) JvmAbi.getterName(propertyName) else JvmAbi.setterName(propertyName)
        val receiverTypes = (property.contextParameters + listOfNotNull(property.receiverParameter.takeUnless { property.isCompanionExtension }))
            .map { it.typeForMangling() }
        val propertyType = property.returnTypeRef.coneType
        val mangled = if (accessor.isGetter) {
            inlineClassMangledName(name, receiverTypes, propertyType, isSuspend = false, owner, inValueClass)
        } else {
            inlineClassMangledName(name, receiverTypes + propertyType, null, isSuspend = false, owner, inValueClass)
        }
        return if (property.hasAnnotation(PUBLISHED_API)) mangled else mangleIfInternal(mangled, accessor, owner)
    }

    private fun generateAccessor(property: FirProperty, accessor: FirPropertyAccessor, owner: Owner, isJvmStatic: Boolean) {
        val klass = (owner as? Owner.Class)?.klass
        if (klass != null && isValueClassStaticMember(accessor, owner)) {
            val parameters = callableParameters(property)
            addMethod(
                accessor, owner, accessorJvmName(property, accessor, owner),
                visibilityFlag(accessor.visibility) or Opcodes.ACC_STATIC or Opcodes.ACC_FINAL or deprecatedFlag(property),
                listOf(valueClassThisParameter(klass)) + parameters, property.returnTypeRef.coneType.takeIf { accessor.isGetter },
                klass.typeParameters + property.typeParameters, isGetter = accessor.isGetter, jvmOverride = false,
                positionSource = accessor.positionSource(property), propertyForTypes = property,
            )
            return
        }
        val name = accessorJvmName(property, accessor, owner)
        val parameters = callableParameters(property).toMutableList()
        if (accessor.isSetter) {
            val parameter = accessor.valueParameters.single()
            parameters += JvmParameter(
                property.returnTypeRef.coneType, property.returnTypeRef, WritingParameterToGenericSignatureMode.REGULAR,
                parameter.name.identifierOrNullIfSpecial.takeUnless { accessor.isDefaultAccessor() }, parameter.annotations, parameter,
            )
        }
        val returnType = if (accessor.isGetter) property.returnTypeRef.coneType else null
        val isStatic = owner is Owner.Facade || (isJvmStatic && klass?.classKind == ClassKind.OBJECT && !klass.isCompanion) ||
                property.isEnumGenerated() || property.isCompanionBlockMember
        val access = methodAccessFlags(accessor, owner, isStatic, modalitySource = property) or deprecatedFlag(property)
        val node = addMethod(
            accessor, owner, name, access, parameters, returnType, property.typeParameters, isGetter = accessor.isGetter,
            positionSource = accessor.positionSource(property), propertyForTypes = property,
            forceBoxedReturn = accessor.isGetter && forceBoxedReturnType(property, owner),
        )
        if (klass?.classKind == ClassKind.INTERFACE && !isStatic && accessor.hasBody()) {
            placeInterfaceMember(
                accessor, owner, node, parameters, returnType, property.typeParameters, isGetter = accessor.isGetter,
                propertyForTypes = property, positionSource = accessor.positionSource(property),
            )
        }
        if (isJvmStatic && klass != null && klass.isCompanion) {
            val outer = owner.outer ?: return
            val staticAccess = staticProxyAccess(access, outer)
            addMethod(
                accessor, outer, name, staticAccess, parameters, returnType, property.typeParameters, isGetter = accessor.isGetter,
                bindMetadata = false, positionSource = accessor.positionSource(property), propertyForTypes = property, jvmOverride = false,
            )
        }
    }

    private fun generateAnnotationsMethod(property: FirProperty, owner: Owner, withoutPosition: Boolean) {
        val propertyAnnotations = property.annotations.filter {
            it.useSiteTarget == null || it.useSiteTarget == org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget.PROPERTY
        }
        if (propertyAnnotations.isEmpty()) return
        val getter = property.getter
        val accessorName = if (getter != null) accessorJvmName(property, getter, owner) else JvmAbi.getterName(property.name.asString())
        val name = JvmAbi.getSyntheticMethodNameForAnnotatedProperty(accessorName)
        val parameters = callableParameters(property).map { JvmParameter(it.type, it.typeRef, it.mode, null, emptyList(), it.source) }
        val visibility = visibilityFlag(property.visibility)
        val access = visibility or Opcodes.ACC_STATIC or Opcodes.ACC_SYNTHETIC or Opcodes.ACC_DEPRECATED
        val klass = (owner as? Owner.Class)?.klass
        val target = if (owner is Owner.Class && klass != null && klass.isJvmInterface() && !property.isCompanionBlockMember &&
            (klass.classKind == ClassKind.ANNOTATION_CLASS || jvmDefaultMode == JvmDefaultMode.DISABLE || klass.isInCompatibilityMode())
        ) defaultImplsOf(owner) else owner
        val node = addMethod(
            property, target, name, access, parameters, null, emptyList(), isGetter = false, bindMetadata = false,
            positionSource = getter?.takeUnless { it.isDefaultAccessor() } ?: property, annotationsFrom = null,
            explicitAnnotations = propertyAnnotations, nullability = false,
            // The line mapping only finds the getter of `$annotations` named after `getX` in the same class, and not for member
            // extensions.
            synthesizedPosition = withoutPosition ||
                    name != JvmAbi.getSyntheticMethodNameForAnnotatedProperty(JvmAbi.getterName(property.name.asString())) ||
                    (owner is Owner.Class && property.receiverParameter != null) ||
                    (target is Owner.DefaultImpls && getter?.hasBody() != true),
            jvmOverride = false,
        )
        owner.stubClass?.serializer?.bindPropertyMetadata(
            FirMetadataSource.Property(property), Method(node.name, node.desc),
            JvmLoweredDeclarationOrigin.SYNTHETIC_METHOD_FOR_PROPERTY_OR_TYPEALIAS_ANNOTATIONS,
        )
    }

    // ---------------------------------------------------------------- methods

    private fun addMethod(
        declaration: FirCallableDeclaration,
        owner: Owner,
        name: String,
        access: Int,
        parameters: List<JvmParameter>,
        returnType: ConeKotlinType?,
        typeParameters: List<FirTypeParameterRef>,
        isGetter: Boolean,
        forceBoxedReturn: Boolean = false,
        bindMetadata: Boolean = true,
        positionSource: FirDeclaration = declaration,
        annotationsFrom: FirDeclaration? = declaration,
        explicitAnnotations: List<FirAnnotation>? = null,
        extraInvisibleAnnotations: List<AnnotationNode> = emptyList(),
        nullability: Boolean = true,
        propertyForTypes: FirProperty? = null,
        synthesizedPosition: Boolean = false,
        jvmOverride: Boolean? = null,
        firSource: KtSourceElement? = declaration.source,
        withExceptions: Boolean = true,
        position: KotlinPosition? = null,
    ): MethodNode {
        val explicitPosition = position
        val stubClass = owner.stubClass!!
        val isAnnotationMethod = (owner as? Owner.Class)?.klass?.classKind == ClassKind.ANNOTATION_CLASS
        val modeSource = propertyForTypes ?: declaration

        val sw = BothSignatureWriter(BothSignatureWriter.Mode.METHOD)
        writeFormalTypeParameters(typeParameters.filterIsInstance<FirTypeParameter>(), sw)
        sw.writeParametersStart()
        val parameterModes = parameters.map { parameter ->
            val mode = if (parameter.mode == WritingParameterToGenericSignatureMode.REGULAR) parameterMode(modeSource, parameter.type) else TypeMappingMode.DEFAULT
            sw.writeParameterType(parameter.mode)
            mapType(parameter.type, mode, sw)
            sw.writeParameterTypeEnd()
            mode
        }
        sw.writeReturnType()
        val returnMode = returnType?.let {
            when {
                forceBoxedReturn -> TypeMappingMode.RETURN_TYPE_BOXED
                else -> returnMode(modeSource, it, isAnnotationMethod)
            }
        }
        val returnAsmType = if (returnType == null) {
            sw.writeAsmType(Type.VOID_TYPE)
            Type.VOID_TYPE
        } else {
            mapType(returnType, returnMode!!, sw)
        }
        sw.writeReturnTypeEnd()
        val signature = sw.makeJvmMethodSignature(name)

        val isSynthetic = access and Opcodes.ACC_SYNTHETIC != 0
        val genericSignature = signature.genericsSignature.withoutErrorWildcards().takeIf {
            !isSynthetic || (declaration as? FirNamedFunction)?.isInline == true
        }
        val node = MethodNode(
            Opcodes.API_VERSION, access, name, signature.asmMethod.descriptor, genericSignature,
            if (withExceptions) exceptions(declaration) else null,
        )

        val written = annotations.write(explicitAnnotations ?: annotationsFrom?.annotations.orEmpty())
        node.visibleAnnotations = written.visible.toMutableList()
        val generateNullability = nullability && access and Opcodes.ACC_PRIVATE == 0 && !isSynthetic &&
                !(isAnnotationMethod && access and Opcodes.ACC_STATIC == 0)
        node.invisibleAnnotations = (written.invisible + extraInvisibleAnnotations)
            .plusNullability(returnType?.takeIf { generateNullability }?.let { nullabilityAnnotation(it, returnAsmType) })
            .toMutableList()

        val parameterAsmTypes = signature.asmMethod.argumentTypes
        val visibleParameterAnnotations = mutableListOf<List<AnnotationNode>>()
        val invisibleParameterAnnotations = mutableListOf<List<AnnotationNode>>()
        val parameterSources = mutableListOf<FirKaptSourceAnnotation>()
        for ([index, parameter] in parameters.withIndex()) {
            if (parameter.mode != WritingParameterToGenericSignatureMode.REGULAR) continue
            val writtenParameter = annotations.write(parameter.annotations)
            parameterSources += writtenParameter.sources
            visibleParameterAnnotations.add(writtenParameter.visible)
            invisibleParameterAnnotations.add(
                writtenParameter.invisible.plusNullability(
                    runIf(generateNullability) { nullabilityAnnotation(parameter.nullabilityType, parameterAsmTypes[index]) }
                )
            )
        }
        if (visibleParameterAnnotations.any { it.isNotEmpty() }) {
            @Suppress("UNCHECKED_CAST")
            node.visibleParameterAnnotations = visibleParameterAnnotations.map { it.toMutableList() }.toTypedArray() as Array<List<AnnotationNode>>
        }
        if (invisibleParameterAnnotations.any { it.isNotEmpty() }) {
            @Suppress("UNCHECKED_CAST")
            node.invisibleParameterAnnotations = invisibleParameterAnnotations.map { it.toMutableList() }.toTypedArray() as Array<List<AnnotationNode>>
        }
        if (access and Opcodes.ACC_ABSTRACT != 0) node.localVariables = mutableListOf()

        if (isAnnotationMethod && declaration is FirPropertyAccessor) {
            annotationDefault(declaration.propertySymbol.fir, owner)?.let { node.annotationDefault = it }
        }

        stubClass.node.methods.add(node)

        val returnTypeRef = when (declaration) {
            is FirPropertyAccessor -> if (isGetter) declaration.propertySymbol.fir.returnTypeRef else null
            is FirFunction -> declaration.returnTypeRef
            else -> null
        }
        val position = explicitPosition ?: if (synthesizedPosition) KotlinPosition(owner.path, isRelativePath = false, Int.MAX_VALUE)
        else position(owner.firFile, positionSource.source, positionSource.keywordTokens())
        origins[node] = FirKaptMethodOrigin(
            declaration, owner.firFile, position, firSource,
            isConstructor = declaration is FirConstructor, sourceAnnotations = written.sources,
            isJvmOverride = jvmOverride ?: isJvmOverride(declaration, owner),
            parameterNames = parameters.map { it.name },
            superConstructorArguments = { if (declaration is FirConstructor) superConstructorArguments(owner as Owner.Class) else null },
            returnTypeSource = typeSource(returnType?.containsErrorTypes() == true, returnTypeRef, returnMode),
            parameterTypeSources = parameters.mapIndexed { index, parameter ->
                if (parameter.mode != WritingParameterToGenericSignatureMode.REGULAR) null
                else typeSource(parameter.type.containsErrorTypes(), parameter.typeRef, parameterModes[index], parameter.isContinuation)
            },
        )

        if (bindMetadata && declaration is FirFunction) {
            stubClass.serializer?.bindMethodMetadata(FirMetadataSource.Function(declaration), signature.asmMethod)
        }
        return node
    }

    private fun isJvmOverride(declaration: FirCallableDeclaration, owner: Owner): Boolean {
        if (declaration !is FirPropertyAccessor) return declaration.isOverride
        val property = declaration.propertySymbol.fir
        if (!property.isOverride) return false
        if (declaration.isGetter) return true
        val klass = (owner as? Owner.Class)?.klass ?: return false
        val scope = klass.unsubstitutedScope(session, scopeSession, withForcedTypeCalculator = true, memberRequiredPhase = null)
        scope.processPropertiesByName(property.name) {}
        return scope.getDirectOverriddenProperties(property.symbol, unwrapIntersectionAndSubstitutionOverride = true).any { it.isVar }
    }

    private fun FirDeclaration.keywordTokens(): TokenSet? = when (this) {
        is FirNamedFunction -> FUN_KEYWORD
        is FirConstructor -> CONSTRUCTOR_KEYWORD
        is FirVariable, is FirPropertyAccessor -> VAL_VAR
        else -> null
    }

    private fun exceptions(declaration: FirCallableDeclaration): Array<String>? {
        val throws = declaration.annotations.firstOrNull {
            val classId = it.annotationTypeRef.coneTypeOrNull?.fullyExpandedType(session)?.classId
            classId == JvmStandardClassIds.Annotations.Throws || classId == JvmStandardClassIds.Annotations.ThrowsAlias
        } ?: return null
        val value = annotations.annotationNode(throws)?.values?.getOrNull(1) as? List<*> ?: return null
        return value.filterIsInstance<Type>().map { it.internalName }.toTypedArray()
    }

    private fun annotationDefault(property: FirProperty, owner: Owner.Class): Any? {
        val constructor = owner.klass.primaryConstructorIfAny(session)?.fir ?: return null
        val parameter = constructor.valueParameters.firstOrNull { it.name == property.name } ?: return null
        val defaultValue = parameter.defaultValue ?: return null
        return annotations.annotationValue(defaultValue, parameter.returnTypeRef.coneType)
    }

    private fun superConstructorArguments(owner: Owner.Class): List<Any?>? {
        val superClass = owner.klass.superConeTypes.firstNotNullOfOrNull { type ->
            if (type is ConeErrorType || type.isAny) return@firstNotNullOfOrNull null
            type.fullyExpandedType(session).toRegularClassSymbol(session)
                ?.takeIf { it.classKind == ClassKind.CLASS || it.classKind == ClassKind.ENUM_CLASS }
        } ?: return listOf()
        var result: List<Any?>? = null
        superClass.unsubstitutedScope(session, scopeSession, withForcedTypeCalculator = false, memberRequiredPhase = null)
            .processDeclaredConstructors { constructor ->
                val isAccessible = constructor.visibility == Visibilities.Public || constructor.visibility == Visibilities.Protected
                val isHidden = constructor.valueParameterSymbols.any { it.resolvedReturnType.requiresMangling() }
                if (result == null && isAccessible && !isHidden && superClass.modality != Modality.SEALED) {
                    result = constructor.valueParameterSymbols.map { defaultValueForType(it.resolvedReturnType) }
                }
            }
        return result
    }

    private fun defaultValueForType(type: ConeKotlinType): Any? {
        val expanded = type.fullyExpandedType(session)
        if (expanded.isMarkedNullable) return null
        return when (expanded.classId) {
            StandardClassIds.Boolean -> false
            StandardClassIds.Char -> '\u0000'
            StandardClassIds.Byte -> 0.toByte()
            StandardClassIds.Short -> 0.toShort()
            StandardClassIds.Int -> 0
            StandardClassIds.Long -> 0L
            StandardClassIds.Float -> 0.0f
            StandardClassIds.Double -> 0.0
            else -> null
        }
    }

    // ---------------------------------------------------------------- types

    private fun BothSignatureWriter.genericSignature(): String? = makeJavaGenericSignature().withoutErrorWildcards()

    /** The JVM backend's error class is final, so it never gets a wildcard. */
    private fun String?.withoutErrorWildcards(): String? = this?.replace("+L$NON_EXISTENT_CLASS_NAME;", "L$NON_EXISTENT_CLASS_NAME;")

    private fun mapType(type: ConeKotlinType, mode: TypeMappingMode = TypeMappingMode.DEFAULT, sw: JvmSignatureWriter? = null): Type =
        typeMapper.mapStubType(type, session, mode, sw)

    /** The backend sees a local class as its first supertype, so the mapping mode depends on that type. */
    @OptIn(ClassIdBasedLocality::class)
    private fun ConeKotlinType.forMappingMode(): ConeKotlinType =
        if ((fullyExpandedType(session) as? ConeClassLikeType)?.lookupTag?.classId?.isLocal == true) forJvmSignature(session) else this

    private fun writeFormalTypeParameters(typeParameters: List<FirTypeParameter>, sw: JvmSignatureWriter) {
        if (sw.skipGenericSignature()) return
        for (typeParameter in typeParameters) {
            typeContext.writeFormalTypeParameter(typeParameter.symbol.toLookupTag(), sw) { type, mode ->
                mapType(type as ConeKotlinType, mode, sw)
            }
        }
    }

    private fun returnMode(declaration: FirCallableDeclaration, type: ConeKotlinType, isAnnotationMethod: Boolean = false): TypeMappingMode =
        with(typeContext) {
            val mappedType = type.forMappingMode()
            extractTypeMappingModeFromAnnotation(suppressWildcardsMode(declaration), mappedType, isAnnotationMethod, mapTypeAliases = false)
                ?: getOptimalModeForReturnType(mappedType, isAnnotationMethod)
        }

    private fun parameterMode(declaration: FirCallableDeclaration, type: ConeKotlinType): TypeMappingMode = with(typeContext) {
        val mappedType = type.forMappingMode()
        extractTypeMappingModeFromAnnotation(suppressWildcardsMode(declaration), mappedType, isForAnnotationParameter = false, mapTypeAliases = false)
            ?: if (mappedType.typeArguments.isNotEmpty() && declaration.isMethodWithDeclarationSiteWildcards()) TypeMappingMode.GENERIC_ARGUMENT
            else getOptimalModeForValueParameter(mappedType)
    }

    /** `addAll` and `putAll` of mutable collections, and their overrides, have all wildcards, like `Collection<? extends String>`. */
    private fun FirCallableDeclaration.isMethodWithDeclarationSiteWildcards(): Boolean {
        if (this !is FirNamedFunction || name.asString() != "addAll" && name.asString() != "putAll") return false
        fun FirNamedFunctionSymbol.matches() = callableId.asSingleFqName().isMethodWithDeclarationSiteWildcardsFqName
        if (symbol.matches()) return true
        val klass = symbol.containingClassLookupTag()?.toRegularClassSymbol(session) ?: return false
        var found = false
        klass.unsubstitutedScope(session, scopeSession, withForcedTypeCalculator = true, memberRequiredPhase = null).run {
            processFunctionsByName(name) {}
            processOverriddenFunctions(symbol) { if (it.matches()) found = true; if (found) ProcessorAction.STOP else ProcessorAction.NEXT }
        }
        return found
    }

    private fun suppressWildcardsMode(declaration: FirCallableDeclaration): Boolean? {
        suppressWildcards(declaration.annotations)?.let { return it }
        if (declaration is FirPropertyAccessor) suppressWildcards(declaration.propertySymbol.fir.annotations)?.let { return it }
        var classId = declaration.containingClassLookupTag()?.classId
        while (classId != null) {
            val klass = session.symbolProvider.getClassLikeSymbolByClassId(classId) as? FirRegularClassSymbol ?: break
            suppressWildcards(klass.resolvedAnnotationsWithArguments)?.let { return it }
            classId = classId.outerClassId
        }
        return null
    }

    private fun suppressWildcards(annotations: List<FirAnnotation>): Boolean? {
        val annotation = annotations.firstOrNull {
            it.annotationTypeRef.coneTypeOrNull?.classId == JvmStandardClassIds.Annotations.JvmSuppressWildcards
        } ?: return null
        val argument = annotation.argumentMapping.mapping[Name.identifier("suppress")] ?: return true
        return ((argument as? FirLiteralExpression)?.value as? Boolean) ?: true
    }

    /** Like AnnotationCodegen: nothing for flexible nullability and for bare type parameters with a nullable bound. */
    private fun nullabilityAnnotation(type: ConeKotlinType, asmType: Type): AnnotationNode? {
        if (AsmUtil.isPrimitive(asmType)) return null
        val expanded = type.fullyExpandedType(session)
        if (expanded is ConeFlexibleType && expanded.lowerBound.isMarkedNullable != expanded.upperBound.isMarkedNullable) return null
        val nullable = expanded.isNullableInIr()
        if (expanded is ConeTypeParameterType && !expanded.isMarkedNullable && nullable) return null
        return AnnotationNode(if (nullable) NULLABLE_DESC else NOT_NULL_DESC)
    }

    /** `IrType.isNullable()`: a type parameter is nullable if any of its bounds is. */
    private fun ConeKotlinType.isNullableInIr(): Boolean = when (val type = fullyExpandedType(session)) {
        is ConeFlexibleType -> type.upperBound.isNullableInIr()
        is ConeDefinitelyNotNullType -> false
        is ConeTypeParameterType -> type.isMarkedNullable ||
                (type.lookupTag as? org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTagImpl)?.typeParameterSymbol?.resolvedBounds
                    .orEmpty().any { it.coneType.isNullableInIr() }
        else -> type.isMarkedNullable
    }

    /** The backend does not add a nullability annotation that the declaration already has. */
    private fun List<AnnotationNode>.plusNullability(annotation: AnnotationNode?): List<AnnotationNode> =
        if (annotation == null || any { it.desc == annotation.desc }) this else this + annotation

    // ---------------------------------------------------------------- positions and metadata

    private fun position(file: FirFile, source: KtSourceElement?, keywordTokens: TokenSet?): KotlinPosition {
        val kind = source?.kind
        val offset = if (kind is KtFakeSourceElementKind.DataClassGeneratedMembers || kind is KtFakeSourceElementKind.EnumGeneratedDeclaration) {
            Int.MAX_VALUE
        } else {
            source?.startOffsetSkippingComments(keywordTokens) ?: source?.startOffset ?: Int.MAX_VALUE
        }
        return KotlinPosition(file.path, isRelativePath = false, offset)
    }

    private fun writeMetadata(stubClass: StubClass) {
        val node = stubClass.node
        val annotation = AnnotationNode(JvmAnnotationNames.METADATA_DESC)
        val values = mutableListOf<Any>()
        values.add(JvmAnnotationNames.METADATA_VERSION_FIELD_NAME)
        values.add(configuration.metadataVersion(languageVersionSettings.languageVersion).toArray().toList())
        values.add(JvmAnnotationNames.KIND_FIELD_NAME)
        values.add(stubClass.kind.id)
        val syntheticClassVisibility = if (stubClass.kind == KotlinClassHeader.Kind.SYNTHETIC_CLASS) {
            // DefaultImpls and repeatable annotation containers are public.
            ProtoBuf.Visibility.PUBLIC.number shl JvmAnnotationNames.METADATA_SYNTHETIC_CLASS_VISIBILITY_BIT_FIRST
        } else 0
        val flags = JvmAnnotationNames.METADATA_JVM_IR_FLAG or JvmAnnotationNames.METADATA_JVM_IR_STABLE_ABI_FLAG or
                syntheticClassVisibility or (if (languageVersionSettings.isPreRelease()) JvmAnnotationNames.METADATA_PRE_RELEASE_FLAG else 0)
        values.add(JvmAnnotationNames.METADATA_EXTRA_INT_FIELD_NAME)
        values.add(flags)
        stubClass.multifileParts?.let {
            values.add(JvmAnnotationNames.METADATA_DATA_FIELD_NAME)
            values.add(it)
        }
        stubClass.multifileFacade?.let {
            values.add(JvmAnnotationNames.METADATA_MULTIFILE_CLASS_NAME_FIELD_NAME)
            values.add(it)
        }
        val serialized = stubClass.metadata?.let { stubClass.serializer?.serialize(it) }
        if (serialized != null) {
            val [proto, stringTable] = serialized
            values.add(JvmAnnotationNames.METADATA_DATA_FIELD_NAME)
            values.add(JvmProtoBufUtil.writeData(proto, stringTable).toList())
            values.add(JvmAnnotationNames.METADATA_STRINGS_FIELD_NAME)
            values.add(stringTable.strings.toList())
        }
        annotation.values = values
        node.visibleAnnotations = (node.visibleAnnotations.orEmpty() + annotation).toMutableList()
    }

    private inner class ClassOutputFile(private val stubClass: StubClass) : OutputFile {
        override val relativePath: String get() = stubClass.node.name + ".class"
        override val sourceFiles: List<File> get() = listOfNotNull(stubClass.sourceFile) + stubClass.facadeSourceFiles
        override fun asByteArray(): ByteArray {
            val writer = ClassWriter(0)
            stubClass.node.accept(writer)
            return writer.toByteArray()
        }

        override fun asText(): String = throw UnsupportedOperationException()
    }

    companion object {
        private val OBJECT_TYPE = Type.getObjectType("java/lang/Object")
        private const val NOT_NULL_DESC = "Lorg/jetbrains/annotations/NotNull;"
        private const val NULLABLE_DESC = "Lorg/jetbrains/annotations/Nullable;"

        private val JVM_STATIC = JvmStandardClassIds.Annotations.JvmStatic
        private val JVM_INLINE = jvmAnnotation("JvmInline")
        private val DEFAULT_CONSTRUCTOR_MARKER = ClassId.fromString("kotlin/jvm/internal/DefaultConstructorMarker")
        private const val VISIBILITY_FLAGS = Opcodes.ACC_PUBLIC or Opcodes.ACC_PROTECTED or Opcodes.ACC_PRIVATE
        private val JVM_MULTIFILE_CLASS = jvmAnnotation("JvmMultifileClass")
        private val JVM_DEFAULT_WITH_COMPATIBILITY = jvmAnnotation("JvmDefaultWithCompatibility")
        private val JVM_DEFAULT_WITHOUT_COMPATIBILITY = jvmAnnotation("JvmDefaultWithoutCompatibility")
        private val JVM_OVERLOADS = jvmAnnotation("JvmOverloads")
        private val JVM_SYNTHETIC = jvmAnnotation("JvmSynthetic")
        private val STRICTFP = jvmAnnotation("Strictfp")
        private val SYNCHRONIZED = jvmAnnotation("Synchronized")
        private val VOLATILE = JvmStandardClassIds.Annotations.JvmVolatile
        private val TRANSIENT = jvmAnnotation("Transient")

        private fun jvmAnnotation(name: String) = ClassId(JvmStandardClassIds.BASE_JVM_PACKAGE, Name.identifier(name))
        private val PUBLISHED_API = StandardClassIds.Annotations.PublishedApi
        private val INLINE_ONLY = ClassId.fromString("kotlin/internal/InlineOnly")
        private val PLATFORM_DEPENDENT = ClassId.fromString("kotlin/internal/PlatformDependent")

        private val TO_ARRAY = Name.identifier("toArray")
        private val REMOVE_AT = Name.identifier("removeAt")
        private val SPECIAL_PROPERTIES = listOf(
            Name.identifier("size") to "size", Name.identifier("keys") to "keySet", Name.identifier("values") to "values",
            Name.identifier("entries") to "entrySet", Name.identifier("length") to "length",
        )
        private val SPECIAL_METHODS = listOf(
            SpecialMethod("contains", StandardClassIds.Collection, listOf("element"), objectParameters = setOf(0)),
            SpecialMethod("remove", StandardClassIds.Collection, listOf("element"), objectParameters = setOf(0)),
            SpecialMethod("containsAll", StandardClassIds.Collection, listOf("elements"), wildcardParameters = setOf(0)),
            SpecialMethod("removeAll", StandardClassIds.Collection, listOf("elements"), wildcardParameters = setOf(0)),
            SpecialMethod("retainAll", StandardClassIds.Collection, listOf("elements"), wildcardParameters = setOf(0)),
            SpecialMethod("indexOf", StandardClassIds.List, listOf("element"), objectParameters = setOf(0)),
            SpecialMethod("lastIndexOf", StandardClassIds.List, listOf("element"), objectParameters = setOf(0)),
            SpecialMethod("containsKey", StandardClassIds.Map, listOf("key"), objectParameters = setOf(0)),
            SpecialMethod("containsValue", StandardClassIds.Map, listOf("value"), objectParameters = setOf(0)),
            SpecialMethod("get", StandardClassIds.Map, listOf("key"), objectParameters = setOf(0)),
            SpecialMethod("getOrDefault", StandardClassIds.Map, listOf("key", "defaultValue"), objectParameters = setOf(0)),
            SpecialMethod("remove", StandardClassIds.Map, listOf("key"), objectParameters = setOf(0)),
            SpecialMethod("remove", StandardClassIds.Map, listOf("key", "value"), objectParameters = setOf(0, 1)),
        )
        private val RENAMED_BUILTINS = listOf(
            RenamedBuiltin(StandardClassIds.CharSequence, OperatorNameConventions.GET, "charAt", bridgeOverrides = true),
            RenamedBuiltin(StandardClassIds.MutableList, REMOVE_AT, "remove", bridgeOverrides = false),
        )
        private val COLLECTION_SUPERTYPES = mapOf(
            StandardClassIds.Collection to setOf(StandardClassIds.Iterable),
            StandardClassIds.List to setOf(StandardClassIds.Collection, StandardClassIds.Iterable),
            StandardClassIds.Set to setOf(StandardClassIds.Collection, StandardClassIds.Iterable),
            StandardClassIds.ListIterator to setOf(StandardClassIds.Iterator),
        )
        private val COLLECTION_PAIRS = listOf(
            StandardClassIds.Collection to StandardClassIds.MutableCollection, StandardClassIds.Set to StandardClassIds.MutableSet,
            StandardClassIds.List to StandardClassIds.MutableList, StandardClassIds.Map to StandardClassIds.MutableMap,
            StandardClassIds.MapEntry to StandardClassIds.MutableMapEntry, StandardClassIds.Iterable to StandardClassIds.MutableIterable,
            StandardClassIds.Iterator to StandardClassIds.MutableIterator,
            StandardClassIds.ListIterator to StandardClassIds.MutableListIterator,
        )
        private const val FOR_INLINE_SUFFIX = "\$\$forInline"

        /**
         * The annotations JvmCachedDeclarations.getRepeatedAnnotationSyntheticContainer copies to the container: javac rejects a
         * container that is not @Inherited when the repeatable annotation is. Like there, @MustBeDocumented is not copied.
         */
        private val REPEATABLE_CONTAINER_META_ANNOTATIONS = setOf(
            StandardClassIds.Annotations.Retention, StandardClassIds.Annotations.Target, JvmStandardClassIds.Annotations.Java.Inherited,
        )

        private val JAVA_ELEMENT_TYPES = listOf(
            "TYPE", "FIELD", "METHOD", "PARAMETER", "CONSTRUCTOR", "LOCAL_VARIABLE", "ANNOTATION_TYPE", "PACKAGE", "TYPE_PARAMETER",
            "TYPE_USE", "MODULE", "RECORD_COMPONENT",
        )

        /** The offset of declarations the JVM backend synthesizes from inherited ones, like delegates in `DefaultImpls`. */
        private const val SYNTHETIC_OFFSET = -2

        private val FUN_KEYWORD = TokenSet.create(KtTokens.FUN_KEYWORD)
        private val CONSTRUCTOR_KEYWORD = TokenSet.create(KtTokens.CONSTRUCTOR_KEYWORD)
        private val VAL_VAR = TokenSet.create(KtTokens.VAL_KEYWORD, KtTokens.VAR_KEYWORD)

        private val KOTLIN_MARKER_INTERFACES: Map<FqName, String> = buildMap {
            for (mapping in JavaToKotlinClassMap.mutabilityMappings) {
                put(mapping.kotlinReadOnly.asSingleFqName(), "kotlin/jvm/internal/markers/KMappedMarker")
                val mutableClassId = mapping.kotlinMutable
                put(
                    mutableClassId.asSingleFqName(),
                    "kotlin/jvm/internal/markers/K" + mutableClassId.relativeClassName.asString()
                        .replace("MutableEntry", "Entry")
                        .replace(".", "$")
                )
            }
        }
    }
}

internal class FirKaptOutputFiles(private val files: List<OutputFile>) : OutputFileCollection {
    private val byPath = files.associateBy { it.relativePath }
    override fun get(relativePath: String): OutputFile? = byPath[relativePath]
    override fun asList(): List<OutputFile> = files
}

/**
 * The module mapping, `META-INF/<module>.kotlin_module`, as JvmIrCodegenFactory.generateModuleMetadata writes it, without optional
 * annotation classes. Incremental compilation finds the top-level declarations of unchanged files through it, so the parts of the files
 * compiled before are kept.
 */
internal class FirKaptModuleMappingFile(
    private val configuration: CompilerConfiguration,
    packagePartRegistry: PackagePartRegistry,
    override val sourceFiles: List<File>,
) : OutputFile {
    private val moduleName = configuration.moduleName ?: JvmProtoBufUtil.DEFAULT_MODULE_NAME
    override val relativePath: String = ModuleNameUtil.getMappingFileName(moduleName)

    private val moduleProto: JvmModuleProtoBuf.Module = JvmModuleProtoBuf.Module.newBuilder().apply {
        val incrementalCache = configuration.incrementalCompilationComponents?.getIncrementalCache(TargetId(moduleName, "java-production"))
        for (part in packagePartRegistry.parts.values.addCompiledPartsAndSort(incrementalCache, configuration.languageVersionSettings)) {
            part.addTo(this)
        }
        val [stringTableProto, qualifiedNameTableProto] = SerializableStringTable().buildProto()
        setStringTable(stringTableProto)
        setQualifiedNameTable(qualifiedNameTableProto)
    }.build()

    override fun asByteArray(): ByteArray =
        moduleProto.serializeModuleMapping(configuration.metadataVersion(), configuration.languageVersionSettings)

    override fun asText(): String = String(asByteArray(), Charsets.UTF_8)
}
