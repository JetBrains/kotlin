/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.common.serialization.findSourceFile
import org.jetbrains.kotlin.backend.konan.InternalKotlinNativeApi
import org.jetbrains.kotlin.backend.konan.descriptors.getPackageFragments
import org.jetbrains.kotlin.backend.konan.descriptors.isInterface
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.SourceFile
import org.jetbrains.kotlin.resolve.scopes.MemberScope

abstract class ObjCExportHeaderGenerator @InternalKotlinNativeApi constructor(
    val moduleDescriptors: List<ModuleDescriptor>,
    internal val mapper: ObjCExportMapper,
    val namer: ObjCExportNamer,
    val objcGenerics: Boolean,
    problemCollector: ObjCExportProblemCollector,
) {
    private val stubs = mutableListOf<ObjCExportStub>()

    private val classForwardDeclarations = linkedSetOf<ObjCClassForwardDeclaration>()
    private val protocolForwardDeclarations = linkedSetOf<String>()
    private val extraClassesToTranslate = mutableSetOf<ClassDescriptor>()

    private val translator = ObjCExportTranslatorImpl(this, mapper, namer, problemCollector, objcGenerics)

    private val generatedClasses = mutableSetOf<ClassDescriptor>()
    private val extensions = mutableMapOf<ClassDescriptor, MutableList<CallableMemberDescriptor>>()
    private val topLevel = mutableMapOf<SourceFile, MutableList<CallableMemberDescriptor>>()

    open val shouldExportKDoc = false

    @InternalKotlinNativeApi
    fun buildHeader(): ObjCHeader = ObjCHeader(
        stubs = stubs,
        classForwardDeclarations = classForwardDeclarations,
        protocolForwardDeclarations = protocolForwardDeclarations,
        additionalImports = getAdditionalImports(),
    )

    fun build(): List<String> = buildHeader().render(shouldExportKDoc)

    @InternalKotlinNativeApi
    fun buildInterface(): ObjCExportedInterface {
        val headerLines = build()
        return ObjCExportedInterface(generatedClasses, extensions, topLevel, headerLines, namer, mapper)
    }

    fun getExportStubs(): ObjCExportedStubs =
        ObjCExportedStubs(classForwardDeclarations, protocolForwardDeclarations, stubs)

    protected open fun getAdditionalImports(): List<String> = emptyList()

    fun translateModule() {
        // TODO: make the translation order stable
        // to stabilize name mangling.
        translateBaseDeclarations()
        translateModuleDeclarations()
    }

    fun translateBaseDeclarations() {
        stubs += translator.generateBaseDeclarations()
    }

    fun translateModuleDeclarations() {
        translatePackageFragments()
        translateExtraClasses()
    }

    private fun translateClass(descriptor: ClassDescriptor) {
        if (mapper.shouldBeExposed(descriptor)) {
            if (descriptor.isInterface) {
                generateInterface(descriptor)
            } else {
                generateClass(descriptor)
            }
        } else if (mapper.shouldBeVisible(descriptor)) {
            stubs += if (descriptor.isInterface) {
                translator.translateUnexposedInterfaceAsUnavailableStub(descriptor)
            } else {
                translator.translateUnexposedClassAsUnavailableStub(descriptor)
            }
        }
    }

    /**
     * Recursively collect classes into [collector].
     * We need to do so because we want to make the order of declarations stable.
     */
    private fun MemberScope.collectClasses(collector: MutableCollection<ClassDescriptor>) {
        getContributedDescriptors()
            .asSequence()
            .filterIsInstance<ClassDescriptor>()
            .forEach {
                collector += it
                // Avoid collecting nested declarations from unexposed classes.
                if (mapper.shouldBeExposed(it)) {
                    it.unsubstitutedMemberScope.collectClasses(collector)
                }
            }
    }

    private fun translatePackageFragments() {
        val packageFragments = moduleDescriptors
            .flatMap { it.getPackageFragments() }
            .makePackagesOrderStable()

        packageFragments.forEach { packageFragment ->
            packageFragment.getMemberScope().getContributedDescriptors()
                .asSequence()
                .filterIsInstance<CallableMemberDescriptor>()
                .filter { mapper.shouldBeExposed(it) }
                .forEach {
                    val classDescriptor = mapper.getClassIfCategory(it)
                    if (classDescriptor != null) {
                        // If a class is hidden from Objective-C API then it is meaningless
                        // to export its extensions.
                        if (!classDescriptor.isHiddenFromObjC()) {
                            extensions.getOrPut(classDescriptor, { mutableListOf() }) += it
                        }
                    } else {
                        topLevel.getOrPut(it.findSourceFile(), { mutableListOf() }) += it
                    }
                }
        }

        val classesToTranslate = mutableListOf<ClassDescriptor>()

        packageFragments.forEach { packageFragment ->
            packageFragment.getMemberScope().collectClasses(classesToTranslate)
        }

        classesToTranslate.makeClassesOrderStable().forEach { translateClass(it) }

        extensions.makeCategoriesOrderStable().forEach { (classDescriptor, declarations) ->
            generateExtensions(classDescriptor, declarations)
        }

        topLevel.makeFilesOrderStable().forEach { (sourceFile, declarations) ->
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

    private fun generateFile(sourceFile: SourceFile, declarations: List<CallableMemberDescriptor>) {
        stubs.add(translator.translateFile(sourceFile, declarations))
    }

    private fun generateExtensions(classDescriptor: ClassDescriptor, declarations: List<CallableMemberDescriptor>) {
        stubs.add(translator.translateExtensions(classDescriptor, declarations))
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
        stubs.add(translator.translateClass(descriptor))
    }

    private fun generateInterface(descriptor: ClassDescriptor) {
        if (!generatedClasses.add(descriptor)) return
        val stub = translator.translateInterface(descriptor)
        stubs.add(stub)
    }

    internal fun requireClassOrInterface(descriptor: ClassDescriptor) {
        if (shouldTranslateExtraClass(descriptor) && descriptor !in generatedClasses) {
            extraClassesToTranslate += descriptor
        }
    }

    internal fun referenceClass(forwardDeclaration: ObjCClassForwardDeclaration) {
        classForwardDeclarations += forwardDeclaration
    }

    internal fun referenceProtocol(objCName: String) {
        protocolForwardDeclarations += objCName
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

        private fun MutableList<String>.addImports(imports: Iterable<String>) {
            imports.forEach {
                add("#import <$it>")
            }
        }

        fun createInstance(
            moduleDescriptors: List<ModuleDescriptor>,
            mapper: ObjCExportMapper,
            namer: ObjCExportNamer,
            problemCollector: ObjCExportProblemCollector,
            objcGenerics: Boolean,
            shouldExportKDoc: Boolean,
            additionalImports: List<String>,
        ): ObjCExportHeaderGenerator = ObjCExportHeaderGeneratorImpl(
            moduleDescriptors, mapper, namer, problemCollector, objcGenerics, shouldExportKDoc, additionalImports
        )
    }
}
