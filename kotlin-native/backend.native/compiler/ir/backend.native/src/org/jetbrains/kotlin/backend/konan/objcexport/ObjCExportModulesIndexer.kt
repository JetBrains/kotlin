/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.common.serialization.findSourceFile
import org.jetbrains.kotlin.backend.konan.descriptors.enumEntries
import org.jetbrains.kotlin.backend.konan.descriptors.getPackageFragments
import org.jetbrains.kotlin.backend.konan.descriptors.isInterface
import org.jetbrains.kotlin.backend.konan.objcexport.sx.ModuleBuilderWithStdlib
import org.jetbrains.kotlin.backend.konan.objcexport.sx.SXClangModuleBuilder
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.konan.isNativeStdlib
import org.jetbrains.kotlin.resolve.scopes.MemberScope

/**
 * Handles inter-module requests.
 */
interface CrossModuleResolver {
    fun findModuleBuilder(declaration: DeclarationDescriptor): SXClangModuleBuilder

    fun findNamer(declaration: DeclarationDescriptor): ObjCExportNamer
}

// TODO: Rename child classes
/**
 * Emits to [eventQueue] information about what declarations from
 * [moduleDescriptors] should be translated.
 */
abstract class ObjCExportModulesIndexer internal constructor(
        val moduleDescriptors: List<ModuleDescriptor>,
        private val mapper: ObjCExportMapper,
        private val eventQueue: EventQueue,
) {

    private val generatedClasses = mutableSetOf<ClassDescriptor>()

    open val shouldExportKDoc = false

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
                            // TODO: Avoid adding if it is already added
                            eventQueue.add(Event.TranslateClass(classDescriptor))
                            eventQueue.add(Event.TranslateExtension(classDescriptor, it))
                        } else {
                            eventQueue.add(Event.TranslateTopLevel(it.findSourceFile(), it))
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
    }

    protected open fun shouldTranslateExtraClass(descriptor: ClassDescriptor): Boolean = true

    private fun generateClass(descriptor: ClassDescriptor) {
        if (!generatedClasses.add(descriptor)) return
        eventQueue.add(Event.TranslateClass(descriptor))
    }

    private fun generateInterface(descriptor: ClassDescriptor) {
        if (!generatedClasses.add(descriptor)) return
        eventQueue.add(Event.TranslateInterface(descriptor))
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