/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.common.serialization.findSourceFile
import org.jetbrains.kotlin.backend.konan.descriptors.enumEntries
import org.jetbrains.kotlin.backend.konan.descriptors.getPackageFragments
import org.jetbrains.kotlin.backend.konan.descriptors.isInterface
import org.jetbrains.kotlin.backend.konan.objcexport.sx.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.konan.isNativeStdlib
import org.jetbrains.kotlin.resolve.scopes.MemberScope

/**
 * Handles inter-module requests.
 */
interface CrossModuleResolver {
    fun findExportGenerator(declaration: DeclarationDescriptor): ObjCExportHeaderGenerator

    fun findNamer(declaration: DeclarationDescriptor): ObjCExportNamer
}

interface EventQueue {
    fun add(event: Event)
}

internal class ObjCExportModuleTranslator(
        val moduleBuilder: SXClangModuleBuilder,
        val objcGenerics: Boolean,
        val problemCollector: ObjCExportProblemCollector,
        internal val mapper: ObjCExportMapper,
        val namer: ObjCExportNamer,
        val resolver: CrossModuleResolver,
        val eventQueue: EventQueue
) {

    fun processEvent(event: Event) {
        when (event) {
            is Event.TranslateClass -> {
                (event.declaration).ifHeaderFound {
                    addTopLevelDeclaration(getTranslator().translateClass(event.declaration))
                }
            }

            is Event.TranslateInterface -> {
                (event.declaration).ifHeaderFound {
                    addTopLevelDeclaration(getTranslator().translateInterface(event.declaration))
                }
            }

            is Event.TranslateExtensions -> {
                (event.classDescriptor).ifHeaderFound {
                    addTopLevelDeclaration(getTranslator().translateExtensions(event.classDescriptor, event.declarations))
                }
            }

            is Event.TranslateUnexposedClass -> {
                (event.classDescriptor).ifHeaderFound {
                    val stub = getTranslator().translateUnexposedClassAsUnavailableStub(event.classDescriptor)
                    addTopLevelDeclaration(stub)
                }
            }

            is Event.TranslateUnexposedInterface -> {
                (event.classDescriptor).ifHeaderFound {
                    val stub = getTranslator().translateUnexposedInterfaceAsUnavailableStub(event.classDescriptor)
                    addTopLevelDeclaration(stub)
                }
            }

            is Event.TranslateFile -> {
                (event.declarations.first()).ifHeaderFound {
                    addTopLevelDeclaration(getTranslator().translateFile(event.sourceFile, event.declarations))
                }
            }

            is Event.TranslateClassForwardDeclaration -> {
                (event.classDescriptor).ifHeaderFound {
                    addClassForwardDeclaration(getTranslator().translateClassForwardDeclaration(event.classDescriptor))
                }
            }

            is Event.TranslateInterfaceForwardDeclaration -> {
                event.classDescriptor.ifHeaderFound {
                    addProtocolForwardDeclaration(getTranslator().translateProtocolForwardDeclaration(event.classDescriptor))
                }
            }
        }
    }

    private inline fun DeclarationDescriptor.ifHeaderFound(action: SXObjCHeader.() -> Unit) {
        moduleBuilder.findHeaderForDeclaration(this)?.action()
    }

    private fun SXObjCHeader.getTranslator(): ObjCExportTranslatorImpl {
        return ObjCExportTranslatorImpl(
                mapper,
                namer,
                problemCollector,
                objcGenerics,
                SXHeaderImportReferenceTracker(this, resolver, mapper, eventQueue),
                eventQueue,
        )
    }

    fun build(): SXClangModule {
        return moduleBuilder.build()
    }
}

internal class ObjCExportStdlibHeaderTranslator(
        val moduleBuilder: ModuleBuilderWithStdlib,
        val translator: ObjCExportTranslatorImpl,
) {
    private fun translateBaseDeclarations() {
        val headerForStdlib = moduleBuilder.getStdlibHeader()
        translator.generateBaseDeclarations().forEach {
            headerForStdlib.addTopLevelDeclaration(it)
        }
    }

    private fun buildImports() {
        val headerForStdlib = moduleBuilder.getStdlibHeader()
        foundationImports.forEach {
            headerForStdlib.addImport(it)
        }
//            getAdditionalImports().forEach {
//                headerForStdlib.addImport(it)
//            }
    }

    fun build() {
        buildImports()
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

abstract class ObjCExportHeaderGenerator internal constructor(
        val moduleDescriptors: List<ModuleDescriptor>,
        internal val mapper: ObjCExportMapper,
        val namer: ObjCExportNamer,
        private val frameworkName: String,
        val moduleBuilder: SXClangModuleBuilder,
        val eventQueue: EventQueue,
        val index: SXIndex,
) {

    private val generatedClasses = mutableSetOf<ClassDescriptor>()
    private val extensions = mutableMapOf<ClassDescriptor, MutableList<CallableMemberDescriptor>>()
    private val topLevel = mutableMapOf<SourceFile, MutableList<CallableMemberDescriptor>>()


    open val shouldExportKDoc = false

    internal fun buildInterface(): ObjCExportedInterface {
        return ObjCExportedInterface(
                generatedClasses,
                extensions,
                topLevel,
                namer,
                mapper,
                moduleBuilder.build(),
                frameworkName
        )
    }

    protected open fun getAdditionalImports(): List<String> = emptyList()

    fun indexModule() {
        // TODO: make the translation order stable
        // to stabilize name mangling.
        indexPackageFragments()
    }

    private fun indexPackageFragments() {
        val packageFragments: List<PackageFragmentDescriptor> = moduleDescriptors
                // standard library is not exported. We will translate only
                // transtively used parts.
                .filter { !it.isNativeStdlib() }
                .flatMap { it.getPackageFragments() }

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
                    .forEach { classDescriptor ->
                        if (mapper.shouldBeExposed(classDescriptor)) {
                            if (classDescriptor.isInterface) {
                                generateInterface(classDescriptor)
                            } else {
                                generateClass(classDescriptor)
                            }
                            classDescriptor.unsubstitutedMemberScope.translateClasses()
                        } else if (mapper.shouldBeVisible(classDescriptor)) {
                            if (classDescriptor.isInterface) {
                                eventQueue.add(Event.TranslateUnexposedInterface(classDescriptor))

                            } else {
                                eventQueue.add(Event.TranslateUnexposedClass(classDescriptor))
                            }
                        }
                    }
        }

        packageFragments.forEach { packageFragment ->
            packageFragment.getMemberScope().translateClasses()
        }

        extensions.forEach { (classDescriptor, declarations) ->
            generateExtensions(classDescriptor, declarations)
        }

        topLevel.forEach { (sourceFile, declarations) ->
            generateFile(sourceFile, declarations)
        }
    }

    private fun generateFile(sourceFile: SourceFile, declarations: List<CallableMemberDescriptor>) {
        eventQueue.add(Event.TranslateFile(sourceFile, declarations))
    }

    private fun generateExtensions(classDescriptor: ClassDescriptor, declarations: List<CallableMemberDescriptor>) {
        eventQueue.add(Event.TranslateExtensions(classDescriptor, declarations))
    }

    protected open fun shouldTranslateExtraClass(descriptor: ClassDescriptor): Boolean = true

    private fun generateClass(descriptor: ClassDescriptor) {
        if (!generatedClasses.add(descriptor)) return
        eventQueue.add(Event.TranslateClass(descriptor))
    }

    private fun generateInterface(descriptor: ClassDescriptor) {
        if (!generatedClasses.add(descriptor)) return
        eventQueue.add(Event.TranslateInterface(descriptor))
        index.storage.add(SXIndex.Item(descriptor, moduleBuilder))
    }
}


internal fun ObjCExportNamer.ClassOrProtocolName.toNameAttributes(): List<String> = listOfNotNull(
        binaryName.takeIf { it != objCName }?.let { objcRuntimeNameAttribute(it) },
        swiftName.takeIf { it != objCName }?.let { swiftNameAttribute(it) }
)

internal fun swiftNameAttribute(swiftName: String) = "swift_name(\"$swiftName\")"
internal fun objcRuntimeNameAttribute(name: String) = "objc_runtime_name(\"$name\")"

interface ObjCExportScope {
    fun getGenericTypeUsage(typeParameterDescriptor: TypeParameterDescriptor?): ObjCGenericTypeUsage?
}

internal class ObjCClassExportScope constructor(container: DeclarationDescriptor, val namer: ObjCExportNamer) : ObjCExportScope {
    private val typeNames = if (container is ClassDescriptor && !container.isInterface) {
        container.typeConstructor.parameters
    } else {
        emptyList<TypeParameterDescriptor>()
    }

    override fun getGenericTypeUsage(typeParameterDescriptor: TypeParameterDescriptor?): ObjCGenericTypeUsage? {
        val localTypeParam = typeNames.firstOrNull {
            typeParameterDescriptor != null &&
                    (it == typeParameterDescriptor || (it.isCapturedFromOuterDeclaration && it.original == typeParameterDescriptor))
        }

        return if (localTypeParam == null) {
            null
        } else {
            ObjCGenericTypeParameterUsage(localTypeParam, namer)
        }
    }
}

internal object ObjCNoneExportScope : ObjCExportScope {
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