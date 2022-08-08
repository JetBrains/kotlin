/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.common.serialization.findSourceFile
import org.jetbrains.kotlin.backend.konan.descriptors.enumEntries
import org.jetbrains.kotlin.backend.konan.descriptors.getPackageFragments
import org.jetbrains.kotlin.backend.konan.descriptors.isInterface
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.scopes.MemberScope

class SXHeaderImportReferenceTracker(
        val header: SXObjCHeader,
        val sxBuilder: SXClangModuleBuilder,
) : ReferenceTracker {
    override fun trackReference(declaration: DeclarationDescriptor) {
        val declarationHeader = sxBuilder.findHeaderForDeclaration(declaration)
        header.addImport(declarationHeader)
        println("$header depends on $declarationHeader because of ${declaration.name}")
    }

    override fun trackClassForwardDeclaration(forwardDeclaration: ObjCClassForwardDeclaration) {
        header.addClassForwardDeclaration(forwardDeclaration)
    }

    override fun trackProtocolForwardDeclaration(objCName: String) {
        header.addProtocolForwardDeclaration(objCName)
    }
}

abstract class ObjCExportHeaderGenerator internal constructor(
        val moduleDescriptors: List<ModuleDescriptor>,
        internal val mapper: ObjCExportMapper,
        val namer: ObjCExportNamer,
        val objcGenerics: Boolean,
        problemCollector: ObjCExportProblemCollector,
        frameworkName: String
) {
    private val extraClassesToTranslate = mutableSetOf<ClassDescriptor>()

    private val generatedClasses = mutableSetOf<ClassDescriptor>()
    private val extensions = mutableMapOf<ClassDescriptor, MutableList<CallableMemberDescriptor>>()
    private val topLevel = mutableMapOf<SourceFile, MutableList<CallableMemberDescriptor>>()

    private val trackersCache = mutableMapOf<SXObjCHeader, SXHeaderImportReferenceTracker>()

    private fun getTracker(header: SXObjCHeader) = trackersCache.getOrPut(header) {
        SXHeaderImportReferenceTracker(header, sxBuilder)
    }

    open val shouldExportKDoc = false

    private val sxBuilder = SXClangModuleBuilder(moduleDescriptors, namer, true, "$frameworkName.h")

    private val translator = ObjCExportTranslatorImpl(this, mapper, namer, problemCollector, objcGenerics)

    internal fun sxBuild() {
        foundationImports.forEach {
            sxBuilder.findHeaderForStdlib().addImport(it)
        }
        getAdditionalImports().forEach {
            sxBuilder.findHeaderForStdlib().addImport(it)
        }

    }

    internal fun buildInterface(): ObjCExportedInterface {
        sxBuild()
        return ObjCExportedInterface(generatedClasses, extensions, topLevel, namer, mapper, sxBuilder.build())
    }

    protected open fun getAdditionalImports(): List<String> = emptyList()

    fun translateModule() {
        // TODO: make the translation order stable
        // to stabilize name mangling.
        translateBaseDeclarations()
        translateModuleDeclarations()
    }

    fun translateBaseDeclarations() {
        val toplevels = translator.generateBaseDeclarations()
        toplevels.forEach {
            sxBuilder.findHeaderForStdlib().addTopLevelDeclaration(it)
        }
    }

    fun translateModuleDeclarations() {
        translatePackageFragments()
        translateExtraClasses()
    }

    private fun translatePackageFragments() {
        val packageFragments = moduleDescriptors.flatMap { it.getPackageFragments() }

        packageFragments.forEach { packageFragment ->
            packageFragment.getMemberScope().getContributedDescriptors()
                    .asSequence()
                    .filterIsInstance<CallableMemberDescriptor>()
                    .filter { mapper.shouldBeExposed(it) }
                    .forEach {
                        val classDescriptor = mapper.getClassIfCategory(it)
                        if (classDescriptor != null) {
                            extensions.getOrPut(classDescriptor, { mutableListOf() }) += it
                        } else {
                            topLevel.getOrPut(it.findSourceFile(), { mutableListOf() }) += it
                        }
                    }

        }

        fun MemberScope.translateClasses() {
            getContributedDescriptors()
                    .asSequence()
                    .filterIsInstance<ClassDescriptor>()
                    .forEach {
                        if (mapper.shouldBeExposed(it)) {
                            if (it.isInterface) {
                                generateInterface(it)
                            } else {
                                generateClass(it)
                            }

                            it.unsubstitutedMemberScope.translateClasses()
                        } else if (mapper.shouldBeVisible(it)) {
                            inHeader(it) {
                                addTopLevelDeclaration(if (it.isInterface) {
                                    translator.translateUnexposedInterfaceAsUnavailableStub(it)
                                } else {
                                    translator.translateUnexposedClassAsUnavailableStub(it)
                                })
                            }
                        }
                    }
        }

        packageFragments.forEach { packageFragment ->
            packageFragment.getMemberScope().translateClasses()
        }

        extensions.forEach { classDescriptor, declarations ->
            generateExtensions(classDescriptor, declarations)
        }

        topLevel.forEach { sourceFile, declarations ->
            generateFile(sourceFile, declarations)
        }
    }

    /**
     * Translates additional classes referenced from the module's declarations, such as parameter types, return types,
     * thrown exception types, and underlying enum types.
     *
     * This is required for classes from dependencies to be exported correctly. However, we also currently rely on this
     * for a few edge cases, such as some inner classes. Sub classes may reject certain descriptors to be translated.
     * Some referenced descriptors may be translated early for ordering reasons.
     * @see shouldTranslateExtraClass
     * @see generateExtraClassEarly
     * @see generateExtraInterfaceEarly
     */
    private fun translateExtraClasses() {
        while (extraClassesToTranslate.isNotEmpty()) {
            val descriptor = extraClassesToTranslate.first()
            extraClassesToTranslate -= descriptor

            assert(shouldTranslateExtraClass(descriptor)) { "Shouldn't be queued for translation: $descriptor" }
            if (descriptor.isInterface) {
                generateInterface(descriptor)
            } else {
                generateClass(descriptor)
            }
        }
    }

    private inline fun inHeader(declaration: DeclarationDescriptor, action: SXObjCHeader.() -> Unit) {
        val header = sxBuilder.findHeaderForDeclaration(declaration)
        val oldTracker = translator.tracker
        translator.tracker = getTracker(header)
        header.action()
        translator.tracker = oldTracker
    }

    private fun generateFile(sourceFile: SourceFile, declarations: List<CallableMemberDescriptor>) {
        inHeader(declarations.first()) { addTopLevelDeclaration(translator.translateFile(sourceFile, declarations)) }
    }

    private fun generateExtensions(classDescriptor: ClassDescriptor, declarations: List<CallableMemberDescriptor>) {
        inHeader(classDescriptor) { addTopLevelDeclaration(translator.translateExtensions(classDescriptor, declarations)) }
    }

    protected open fun shouldTranslateExtraClass(descriptor: ClassDescriptor): Boolean = true

    internal fun generateExtraClassEarly(descriptor: ClassDescriptor) {
        if (shouldTranslateExtraClass(descriptor)) generateClass(descriptor)
    }

    internal fun generateExtraInterfaceEarly(descriptor: ClassDescriptor) {
        if (shouldTranslateExtraClass(descriptor)) generateInterface(descriptor)
    }

    private fun generateClass(descriptor: ClassDescriptor) {
        if (!generatedClasses.add(descriptor)) return
        inHeader(descriptor) { addTopLevelDeclaration(translator.translateClass(descriptor)) }
    }

    private fun generateInterface(descriptor: ClassDescriptor) {
        if (!generatedClasses.add(descriptor)) return
        inHeader(descriptor) { addTopLevelDeclaration(translator.translateInterface(descriptor)) }
    }

    internal fun requireClassOrInterface(descriptor: ClassDescriptor) {
        if (shouldTranslateExtraClass(descriptor) && descriptor !in generatedClasses) {
            extraClassesToTranslate += descriptor
        }
    }

    internal fun referenceClass(forwardDeclaration: ObjCClassForwardDeclaration) {
        translator.tracker.trackClassForwardDeclaration(forwardDeclaration)
    }

    internal fun referenceProtocol(objCName: String) {
        translator.tracker.trackProtocolForwardDeclaration(objCName)
    }

    companion object {
        val foundationImports = listOf(
            "Foundation/NSArray.h",
            "Foundation/NSDictionary.h",
            "Foundation/NSError.h",
            "Foundation/NSObject.h",
            "Foundation/NSSet.h",
            "Foundation/NSString.h",
            "Foundation/NSValue.h"
        )
    }
}

internal fun ObjCExportNamer.ClassOrProtocolName.toNameAttributes(): List<String> = listOfNotNull(
        binaryName.takeIf { it != objCName }?.let { objcRuntimeNameAttribute(it) },
        swiftName.takeIf { it != objCName }?.let { swiftNameAttribute(it) }
)

internal fun swiftNameAttribute(swiftName: String) = "swift_name(\"$swiftName\")"
internal fun objcRuntimeNameAttribute(name: String) = "objc_runtime_name(\"$name\")"

interface ObjCExportScope{
    fun getGenericTypeUsage(typeParameterDescriptor: TypeParameterDescriptor?): ObjCGenericTypeUsage?
}

internal class ObjCClassExportScope constructor(container:DeclarationDescriptor, val namer: ObjCExportNamer): ObjCExportScope {
    private val typeNames = if(container is ClassDescriptor && !container.isInterface) {
        container.typeConstructor.parameters
    } else {
        emptyList<TypeParameterDescriptor>()
    }

    override fun getGenericTypeUsage(typeParameterDescriptor: TypeParameterDescriptor?): ObjCGenericTypeUsage? {
        val localTypeParam = typeNames.firstOrNull {
            typeParameterDescriptor != null &&
                    (it == typeParameterDescriptor || (it.isCapturedFromOuterDeclaration && it.original == typeParameterDescriptor))
        }

        return if(localTypeParam == null) {
            null
        } else {
            ObjCGenericTypeParameterUsage(localTypeParam, namer)
        }
    }
}

internal object ObjCNoneExportScope: ObjCExportScope{
    override fun getGenericTypeUsage(typeParameterDescriptor: TypeParameterDescriptor?): ObjCGenericTypeUsage? = null
}

internal const val OBJC_SUBCLASSING_RESTRICTED = "objc_subclassing_restricted"

internal fun ClassDescriptor.needCompanionObjectProperty(namer: ObjCExportNamer, mapper: ObjCExportMapper): Boolean {
    val companionObject = companionObjectDescriptor
    if (companionObject == null || !mapper.shouldBeExposed(companionObject)) return false

    if (kind == ClassKind.ENUM_CLASS && enumEntries.any {
                namer.getEnumEntrySelector(it) == ObjCExportNamer.companionObjectPropertyName ||
                        namer.getEnumEntrySwiftName(it) == ObjCExportNamer.companionObjectPropertyName
            }
    ) return false // 'companion' property would clash with enum entry, don't generate it.

    return true
}