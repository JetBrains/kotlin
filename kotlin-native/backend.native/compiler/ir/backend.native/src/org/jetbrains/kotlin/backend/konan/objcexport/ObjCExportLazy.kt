/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.descriptors.konan.isNativeStdlib
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.hasExpectModifier
import org.jetbrains.kotlin.psi.psiUtil.modalityModifier
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifierTypeOrDefault
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.DescriptorResolver
import org.jetbrains.kotlin.resolve.TypeResolver
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.lazy.FileScopeProvider
import org.jetbrains.kotlin.resolve.lazy.KotlinCodeAnalyzer
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScopeKind
import org.jetbrains.kotlin.resolve.scopes.LexicalWritableScope
import org.jetbrains.kotlin.resolve.scopes.LocalRedeclarationChecker
import org.jetbrains.kotlin.resolve.source.PsiSourceFile
import org.jetbrains.kotlin.types.ErrorUtils

interface ObjCExportLazy {
    interface Configuration {
        val frameworkName: String
        fun isIncluded(moduleInfo: ModuleInfo): Boolean
        fun getCompilerModuleName(moduleInfo: ModuleInfo): String
        val objcGenerics: Boolean
    }

    fun generateBase(): List<ObjCTopLevel<*>>

    fun translate(file: KtFile): List<ObjCTopLevel<*>>
}

@JvmOverloads
fun createObjCExportLazy(
        configuration: ObjCExportLazy.Configuration,
        warningCollector: ObjCExportWarningCollector,
        codeAnalyzer: KotlinCodeAnalyzer,
        typeResolver: TypeResolver,
        descriptorResolver: DescriptorResolver,
        fileScopeProvider: FileScopeProvider,
        builtIns: KotlinBuiltIns,
        deprecationResolver: DeprecationResolver? = null
): ObjCExportLazy = ObjCExportLazyImpl(
        configuration,
        warningCollector,
        codeAnalyzer,
        typeResolver,
        descriptorResolver,
        fileScopeProvider,
        builtIns,
        deprecationResolver
)

internal class ObjCExportLazyImpl(
        private val configuration: ObjCExportLazy.Configuration,
        warningCollector: ObjCExportWarningCollector,
        private val codeAnalyzer: KotlinCodeAnalyzer,
        private val typeResolver: TypeResolver,
        private val descriptorResolver: DescriptorResolver,
        private val fileScopeProvider: FileScopeProvider,
        builtIns: KotlinBuiltIns,
        deprecationResolver: DeprecationResolver?
) : ObjCExportLazy {

    private val namerConfiguration = createNamerConfiguration(configuration)

    private val nameTranslator: ObjCExportNameTranslator = ObjCExportNameTranslatorImpl(namerConfiguration)

    private val mapper = ObjCExportMapper(deprecationResolver, local = true)

    private val namer = ObjCExportNamerImpl(namerConfiguration, builtIns, mapper, local = true)

    private val translator: ObjCExportTranslator = ObjCExportTranslatorImpl(
            null,
            mapper,
            namer,
            warningCollector,
            objcGenerics = configuration.objcGenerics
    )

    private val isValid: Boolean
        get() = codeAnalyzer.moduleDescriptor.isValid

    override fun generateBase() = translator.generateBaseDeclarations()

    override fun translate(file: KtFile): List<ObjCTopLevel<*>> =
            translateClasses(file) + translateTopLevels(file)

    private fun translateClasses(container: KtDeclarationContainer): List<ObjCClass<*>> {
        val result = mutableListOf<ObjCClass<*>>()
        container.declarations.forEach { declaration ->
            // Supposed to be true if ObjCExportMapper.shouldBeVisible is true.
            if (declaration is KtClassOrObject && declaration.isPublic && declaration !is KtEnumEntry
                    && !declaration.hasExpectModifier()) {

                if (!declaration.isAnnotation() && !declaration.hasModifier(KtTokens.INLINE_KEYWORD)) {
                    result += translateClass(declaration)
                }

                declaration.body?.let {
                    result += translateClasses(it)
                }
            }
        }
        return result
    }

    private fun translateClass(ktClassOrObject: KtClassOrObject): ObjCClass<*> {
        val name = nameTranslator.getClassOrProtocolName(ktClassOrObject)

        // Note: some attributes may be missing (e.g. "unavailable" for unexposed classes).

        return if (ktClassOrObject.isInterface) {
            LazyObjCProtocolImpl(name, ktClassOrObject, this)
        } else {
            val isFinal = ktClassOrObject.modalityModifier() == null ||
                    ktClassOrObject.hasModifier(KtTokens.FINAL_KEYWORD)

            val attributes = if (isFinal) {
                listOf(OBJC_SUBCLASSING_RESTRICTED)
            } else {
                emptyList()
            }

            LazyObjCInterfaceImpl(name,
                                  attributes,
                                  generics = translateGenerics(ktClassOrObject),
                                  psi = ktClassOrObject,
                                  lazy = this)
        }
    }

    private fun translateGenerics(ktClassOrObject: KtClassOrObject): List<String> = if (configuration.objcGenerics) {
        ktClassOrObject.typeParametersWithOuter
                .map { nameTranslator.getTypeParameterName(it) }
                .toList()
    } else {
        emptyList()
    }

    private fun translateTopLevels(file: KtFile): List<ObjCInterface> {
        val extensions =
                mutableMapOf<ClassDescriptor, MutableList<KtCallableDeclaration>>()

        val topLevel = mutableListOf<KtCallableDeclaration>()

        file.children.filterIsInstance<KtCallableDeclaration>().forEach {
            // Supposed to be similar to ObjCExportMapper.shouldBeVisible.
            if ((it is KtFunction || it is KtProperty) && it.isPublic && !it.hasExpectModifier()) {
                val classDescriptor = getClassIfExtension(it)
                if (classDescriptor != null) {
                    extensions.getOrPut(classDescriptor, { mutableListOf() }) += it
                } else {
                    topLevel += it
                }
            }
        }

        val result = mutableListOf<ObjCInterface>()

        extensions.mapTo(result) { (classDescriptor, declarations) ->
            translateExtensions(file, classDescriptor, declarations)
        }

        if (topLevel.isNotEmpty()) result += translateFileClass(file, topLevel)

        return result
    }

    private fun translateFileClass(file: KtFile, declarations: List<KtCallableDeclaration>): ObjCInterface {
        val name = nameTranslator.getFileClassName(file)
        return LazyObjCFileInterface(name, file, declarations, this)
    }

    private fun translateExtensions(
            file: KtFile,
            classDescriptor: ClassDescriptor,
            declarations: List<KtCallableDeclaration>
    ): ObjCInterface {
        // TODO: consider using file-based categories in compiler too.

        val name = if (ErrorUtils.isError(classDescriptor)) {
            ObjCExportNamer.ClassOrProtocolName("ERROR", "ERROR")
        } else {
            namer.getClassOrProtocolName(classDescriptor)
        }

        return LazyObjCExtensionInterface(name, nameTranslator.getCategoryName(file), classDescriptor, declarations, this)
    }

    private fun resolveDeclaration(ktDeclaration: KtDeclaration): DeclarationDescriptor =
            codeAnalyzer.resolveToDescriptor(ktDeclaration)

    private fun resolve(ktClassOrObject: KtClassOrObject) =
            resolveDeclaration(ktClassOrObject) as ClassDescriptor

    private fun resolve(ktCallableDeclaration: KtCallableDeclaration) =
            resolveDeclaration(ktCallableDeclaration) as CallableMemberDescriptor

    private fun getClassIfExtension(topLevelDeclaration: KtCallableDeclaration): ClassDescriptor? {
        val receiverType = topLevelDeclaration.receiverTypeReference ?: return null
        val fileScope = fileScopeProvider.getFileResolutionScope(topLevelDeclaration.containingKtFile)

        val trace = BindingTraceContext() // TODO: revise.

        val kotlinReceiverType = typeResolver.resolveType(
                createHeaderScope(topLevelDeclaration, fileScope, trace),
                receiverType,
                trace,
                checkBounds = false
        )

        return translator.getClassIfExtension(kotlinReceiverType)
    }

    private fun createHeaderScope(
            declaration: KtCallableDeclaration,
            parent: LexicalScope,
            trace: BindingTrace
    ): LexicalScope {
        if (declaration.typeParameters.isEmpty()) return parent

        val fakeName = Name.special("<fake>")
        val sourceElement = SourceElement.NO_SOURCE

        val descriptor: CallableMemberDescriptor
        val scopeKind: LexicalScopeKind

        when (declaration) {
            is KtFunction -> {
                descriptor = SimpleFunctionDescriptorImpl.create(
                        parent.ownerDescriptor,
                        Annotations.EMPTY,
                        fakeName,
                        CallableMemberDescriptor.Kind.DECLARATION,
                        sourceElement
                )
                scopeKind = LexicalScopeKind.FUNCTION_HEADER
            }
            is KtProperty -> {
                descriptor = PropertyDescriptorImpl.create(
                        parent.ownerDescriptor,
                        Annotations.EMPTY,
                        Modality.FINAL,
                        DescriptorVisibilities.PUBLIC,
                        declaration.isVar,
                        fakeName,
                        CallableMemberDescriptor.Kind.DECLARATION,
                        sourceElement,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false
                )
                scopeKind = LexicalScopeKind.PROPERTY_HEADER
            }
            else -> TODO("${declaration::class}")
        }

        val result = LexicalWritableScope(
                parent,
                descriptor,
                false,
                LocalRedeclarationChecker.DO_NOTHING,
                scopeKind
        )

        val typeParameters = descriptorResolver.resolveTypeParametersForDescriptor(
                descriptor,
                result,
                result,
                declaration.typeParameters,
                trace
        )

        descriptorResolver.resolveGenericBounds(declaration, descriptor, result, typeParameters, trace)

        return result
    }

    private class LazyObjCProtocolImpl(
        name: ObjCExportNamer.ClassOrProtocolName,
        override val psi: KtClassOrObject,
        private val lazy: ObjCExportLazyImpl
    ) : LazyObjCProtocol(name) {
        override val descriptor: ClassDescriptor by lazy { lazy.resolve(psi) }

        override val isValid: Boolean
            get() = lazy.isValid

        override fun computeRealStub(): ObjCProtocol = lazy.translator.translateInterface(descriptor)
    }

    private class LazyObjCInterfaceImpl(
        name: ObjCExportNamer.ClassOrProtocolName,
        attributes: List<String>,
        generics: List<String>,
        override val psi: KtClassOrObject,
        private val lazy: ObjCExportLazyImpl
    ) : LazyObjCInterface(name = name, generics = generics, categoryName = null, attributes = attributes) {
        override val descriptor: ClassDescriptor by lazy { lazy.resolve(psi) }

        override val isValid: Boolean
            get() = lazy.isValid

        override fun computeRealStub(): ObjCInterface = lazy.translator.translateClass(descriptor)
    }

    private class LazyObjCFileInterface(
        name: ObjCExportNamer.ClassOrProtocolName,
        private val file: KtFile,
        private val declarations: List<KtCallableDeclaration>,
        private val lazy: ObjCExportLazyImpl
    ) : LazyObjCInterface(name = name, generics = emptyList(), categoryName = null, attributes = listOf(OBJC_SUBCLASSING_RESTRICTED)) {
        override val descriptor: ClassDescriptor?
            get() = null

        override val isValid: Boolean
            get() = lazy.isValid

        override val psi: PsiElement?
            get() = null

        override fun computeRealStub(): ObjCInterface = lazy.translator.translateFile(
            PsiSourceFile(file),
            declarations.mapNotNull { declaration ->
                lazy.resolve(declaration).takeIf { descriptor ->
                    lazy.mapper.shouldBeExposed(descriptor)
                }
            }
        )
    }

    private class LazyObjCExtensionInterface(
        name: ObjCExportNamer.ClassOrProtocolName,
        categoryName: String,
        private val classDescriptor: ClassDescriptor,
        private val declarations: List<KtCallableDeclaration>,
        private val lazy: ObjCExportLazyImpl
    ) : LazyObjCInterface(name = name.objCName, generics = emptyList(), categoryName = categoryName, attributes = emptyList()) {
        override val descriptor: ClassDescriptor?
            get() = null

        override val isValid: Boolean
            get() = lazy.isValid

        override val psi: PsiElement?
            get() = null

        override fun computeRealStub(): ObjCInterface = lazy.translator.translateExtensions(
            classDescriptor,
            declarations.mapNotNull { declaration ->
                lazy.resolve(declaration).takeIf { lazy.mapper.shouldBeExposed(it) }
            }
        )
    }
}

private abstract class LazyObjCInterface : ObjCInterface {

    constructor(
            name: ObjCExportNamer.ClassOrProtocolName,
            generics: List<String>,
            categoryName: String?,
            attributes: List<String>
    ) : super(name.objCName, generics, categoryName, attributes + name.toNameAttributes())

    constructor(
            name: String,
            generics: List<String>,
            categoryName: String,
            attributes: List<String>
    ) : super(name, generics, categoryName, attributes)

    protected abstract fun computeRealStub(): ObjCInterface

    private val realStub by lazy { computeRealStub() }

    override val members: List<Stub<*>>
        get() = realStub.members

    override val superProtocols: List<String>
        get() = realStub.superProtocols

    override val superClass: String?
        get() = realStub.superClass

    override val superClassGenerics: List<ObjCNonNullReferenceType>
        get() = realStub.superClassGenerics
}

private abstract class LazyObjCProtocol(
        name: ObjCExportNamer.ClassOrProtocolName
) : ObjCProtocol(name.objCName, name.toNameAttributes()) {

    protected abstract fun computeRealStub(): ObjCProtocol

    private val realStub by lazy { computeRealStub() }

    override val members: List<Stub<*>>
        get() = realStub.members

    override val superProtocols: List<String>
        get() = realStub.superProtocols
}

internal fun createNamerConfiguration(configuration: ObjCExportLazy.Configuration): ObjCExportNamer.Configuration {
    return object : ObjCExportNamer.Configuration {
        override val topLevelNamePrefix = abbreviate(configuration.frameworkName)

        override fun getAdditionalPrefix(module: ModuleDescriptor): String? {
            if (module.isStdlib()) return "Kotlin"

            // Note: incorrect for compiler since it doesn't store ModuleInfo to ModuleDescriptor.
            val moduleInfo = module.getCapability(ModuleInfo.Capability) ?: return null
            if (configuration.isIncluded(moduleInfo)) return null
            return abbreviate(configuration.getCompilerModuleName(moduleInfo))
        }

        override val objcGenerics = configuration.objcGenerics
    }
}

// TODO: find proper solution.
private fun ModuleDescriptor.isStdlib(): Boolean =
        this.builtIns == this || this.isCommonStdlib() || this.isNativeStdlib()

private val kotlinSequenceClassId = ClassId.topLevel(FqName("kotlin.sequences.Sequence"))

private fun ModuleDescriptor.isCommonStdlib() =
        this.findClassAcrossModuleDependencies(kotlinSequenceClassId)?.module == this


private val KtModifierListOwner.isPublic: Boolean
    get() = this.visibilityModifierTypeOrDefault() == KtTokens.PUBLIC_KEYWORD

internal val KtPureClassOrObject.isInterface: Boolean
    get() = this is KtClass && this.isInterface()

internal val KtClassOrObject.typeParametersWithOuter
    get() = generateSequence(this, { if (it is KtClass && it.isInner()) it.containingClassOrObject else null })
            .flatMap { it.typeParameters.asSequence() }
