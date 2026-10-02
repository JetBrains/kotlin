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
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SourceFile
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.MemberScope

abstract class ObjCExportHeaderGenerator @InternalKotlinNativeApi constructor(
    val moduleDescriptors: List<ModuleDescriptor>,
    internal val mapper: ObjCExportMapper,
    val namer: ObjCExportNamer,
    val objcGenerics: Boolean,
    val objcExportBlockExplicitParameterNames: Boolean,
    problemCollector: ObjCExportProblemCollector,
) {
    private val stubs = mutableListOf<ObjCExportStub>()
    private val baseDeclarations = mutableListOf<ObjCExportStub>()

    private val classForwardDeclarations = linkedSetOf<ObjCClassForwardDeclaration>()
    private val protocolForwardDeclarations = linkedSetOf<String>()
    private val extraClassesToTranslate = mutableSetOf<ClassDescriptor>()

    private val translator =
        ObjCExportTranslatorImpl(this, mapper, namer, problemCollector, objcGenerics, objcExportBlockExplicitParameterNames)

    private val generatedClasses = mutableSetOf<ClassDescriptor>()
    private val extensions = mutableMapOf<ClassDescriptor, MutableList<CallableMemberDescriptor>>()
    private val topLevel = mutableMapOf<SourceFile, MutableList<CallableMemberDescriptor>>()

    private var recordedEvents: MutableList<ObjCExportUnitEvent>? = null
    private var liveAssembler: ObjCExportHeaderAssembler? = null
    private val referencedClasses = mutableMapOf<String, ClassDescriptor>()
    private val insertedLines = mutableListOf<Pair<Int, List<String>>>()
    private var protocolStubs: Pair<List<String>, Set<ClassDescriptor>>? = null
    private var isExtraModule: (ModuleDescriptor) -> Boolean = { false }

    protected fun isTranslatedAsExtraModule(module: ModuleDescriptor): Boolean = isExtraModule(module)

    open val shouldExportKDoc = false

    @InternalKotlinNativeApi
    fun buildHeader(): ObjCHeader = ObjCHeader(
        stubs = stubs,
        classForwardDeclarations = classForwardDeclarations.sortedBy { it.className }.toSet(),
        protocolForwardDeclarations = protocolForwardDeclarations.sortedBy { it }.toSet(),
        additionalImports = getAdditionalImports(),
    )

    fun build(): List<String> = buildHeader().render(shouldExportKDoc, insertedLines)

    @InternalKotlinNativeApi
    fun buildInterface(objCCacheUnits: ObjCExportUnits? = null): ObjCExportedInterface {
        val header = buildHeader()
        val [protocolStubHeaderLines, protocolStubClasses] = protocolStubs
            ?: Pair(if (insertedLines.isEmpty()) emptyList() else header.renderForProtocolStubs(baseDeclarations), generatedClasses)
        return ObjCExportedInterface(
            generatedClasses, extensions, topLevel, header.render(shouldExportKDoc, insertedLines), namer, mapper,
            protocolStubHeaderLines, protocolStubClasses, objCCacheUnits,
        )
    }

    fun getExportStubs(): ObjCExportedStubs =
        ObjCExportedStubs(
            classForwardDeclarations.sortedBy { it.className }.toSet(),
            protocolForwardDeclarations.sortedBy { it }.toSet(),
            stubs
        )

    protected open fun getAdditionalImports(): List<String> = emptyList()

    fun translateModule() {
        // TODO: make the translation order stable
        // to stabilize name mangling.
        translateBaseDeclarations()
        translateModuleDeclarations()
    }

    fun translateBaseDeclarations() {
        baseDeclarations += translator.generateBaseDeclarations()
        stubs += baseDeclarations
    }

    fun translateModuleDeclarations() {
        translatePackageFragments()
        translateExtraClasses()
    }

    @InternalKotlinNativeApi
    fun translateUnits(): List<ObjCExportUnits> = moduleDescriptors.map { translateUnits(it) }

    @InternalKotlinNativeApi
    fun assembleHeader(
        modules: List<ObjCExportModuleUnits>,
        liveModuleIndices: Map<ModuleDescriptor, Int> = emptyMap(),
        resolveClass: (String) -> ClassDescriptor? = { null },
    ) {
        modules.forEach {
            classForwardDeclarations += it.units.classForwardDeclarations
            protocolForwardDeclarations += it.units.protocolForwardDeclarations
        }
        val isLive = liveModuleIndices.isNotEmpty()
        val assembler = ObjCExportHeaderAssembler(
            modules,
            liveModuleIndices.map { [module, index] -> liveEntries(module, index) },
            translateOnDemand = { classId ->
                val descriptor = referencedClasses[classId] ?: resolveClass(classId)
                when {
                    descriptor == null -> null
                    isLive -> {
                        if (shouldTranslateExtraClass(descriptor)) generateClassOrInterface(descriptor)
                        null
                    }
                    else -> translateExtraClassUnit(descriptor)
                }
            },
            emitContent = ::emit,
            emitCategory = ::emitCategory,
        )
        liveAssembler = assembler.takeIf { isLive }
        try {
            assembler.assemble()
        } finally {
            liveAssembler = null
        }
    }

    private fun liveEntries(module: ModuleDescriptor, moduleIndex: Int): ObjCExportHeaderAssembler.Entries {
        val structure = collectModuleStructure(module)
        val classes = structure.classes.mapIndexed { index, descriptor ->
            ObjCExportHeaderAssembler.ClassEntry(descriptor.classId.toString(), moduleIndex, index) {
                translateClass(descriptor)
                descriptor in generatedClasses
            }
        }
        val categoryMembers = structure.extensions.flatMap { [classDescriptor, declarations] ->
            val classId = classDescriptor.classId.toString()
            val className by lazy { translator.translateExtendedClassName(classDescriptor) }
            declarations.flatMap { [packageName, index, declaration] ->
                translator.translatePlainMemberEntries(listOf(declaration)).map { entry ->
                    ObjCExportHeaderAssembler.CategoryMemberEntry(
                        classId, packageName, moduleIndex, index, entry.sortKey,
                        className = { className },
                        exportThrown = { entry.exportThrown() },
                        content = { ObjCExportUnitContent.Stubs(entry.translate()) },
                    )
                }
            }
        }
        val files = structure.topLevel.map { [sourceFile, entry] ->
            ObjCExportHeaderAssembler.FileEntry(sourceFile.name, entry.first, moduleIndex, entry.second) {
                generateFile(sourceFile, entry.third)
            }
        }
        return ObjCExportHeaderAssembler.Entries(classes, categoryMembers, files)
    }

    @InternalKotlinNativeApi
    fun translateReferencedClassesOfModules(units: ObjCExportUnits, modules: (ModuleDescriptor) -> Boolean) {
        protocolStubs = buildHeader().renderForProtocolStubs(baseDeclarations) to generatedClasses.toSet()
        isExtraModule = modules
        units.events.mapNotNull { referencedClasses[it.classId] }.filter { modules(it.module) }.forEach { requireClassOrInterface(it) }
        translateExtraClasses()
    }

    private class ModuleStructure(
        val classes: List<ClassDescriptor>,
        val extensions: Map<ClassDescriptor, List<Triple<String, Int, CallableMemberDescriptor>>>,
        val topLevel: Map<SourceFile, Triple<String, Int, List<CallableMemberDescriptor>>>,
    )

    private fun collectModuleStructure(module: ModuleDescriptor): ModuleStructure {
        val packageFragments = module.getPackageFragments().makePackagesOrderStable()
        val moduleExtensions = mutableMapOf<ClassDescriptor, MutableList<Triple<String, Int, CallableMemberDescriptor>>>()
        val moduleTopLevel = mutableMapOf<SourceFile, Triple<String, Int, MutableList<CallableMemberDescriptor>>>()
        var position = 0

        packageFragments.forEach { packageFragment ->
            val packageName = packageFragment.fqName.asString()
            packageFragment.getMemberScope().getContributedDescriptors()
                .asSequence()
                .filterIsInstance<CallableMemberDescriptor>()
                .filter { mapper.shouldBeExposed(it) }
                .forEach {
                    val index = position++
                    val classDescriptor = getClassIfCategory(it)
                    if (classDescriptor == null) {
                        val sourceFile = it.findSourceFile()
                        moduleTopLevel.getOrPut(sourceFile) { Triple(packageName, index, mutableListOf()) }.third += it
                        topLevel.getOrPut(sourceFile, { mutableListOf() }) += it
                    } else if (!classDescriptor.isHiddenFromObjC()) {
                        moduleExtensions.getOrPut(classDescriptor) { mutableListOf() } += Triple(packageName, index, it)
                        extensions.getOrPut(classDescriptor, { mutableListOf() }) += it
                    }
                }
        }

        val classesToTranslate = mutableListOf<ClassDescriptor>()
        packageFragments.forEach { packageFragment ->
            packageFragment.getMemberScope().collectClasses(classesToTranslate)
        }
        return ModuleStructure(classesToTranslate.makeClassesOrderStable(), moduleExtensions, moduleTopLevel)
    }

    private fun translateUnits(module: ModuleDescriptor): ObjCExportUnits {
        val structure = collectModuleStructure(module)
        val classes = structure.classes.withIndex().mapNotNullTo(mutableListOf()) { [index, descriptor] ->
            recordClassUnit(descriptor, index, isInMainPass = true) { translateClass(descriptor) }
        }
        val categoryMembers = structure.extensions.flatMap { [classDescriptor, declarations] ->
            recordCategoryMembers(classDescriptor, declarations)
        }
        val files = structure.topLevel.map { [sourceFile, entry] ->
            recordFileUnit(sourceFile, entry.first, entry.second, entry.third)
        }

        val unitClassIds = classes.mapTo(mutableSetOf()) { it.classId }
        var events = ObjCExportUnits(classes, categoryMembers, files, emptySet(), emptySet()).events.toList()
        while (true) {
            val descriptors = events.mapNotNull { referencedClasses[it.classId] }
                .filter { it.module == module && unitClassIds.add(it.classId.toString()) }
            if (descriptors.isEmpty()) break
            val extraUnits = descriptors.mapNotNull { descriptor ->
                recordClassUnit(descriptor, index = -1, isInMainPass = false) { generateClassOrInterface(descriptor) }
            }
            classes += extraUnits
            events = extraUnits.flatMap { it.events }
        }

        return ObjCExportUnits(classes, categoryMembers, files, classForwardDeclarations.toSet(), protocolForwardDeclarations.toSet())
    }

    private fun <T> recording(block: () -> T): Pair<T, List<ObjCExportUnitEvent>> {
        val events = mutableListOf<ObjCExportUnitEvent>()
        val previous = recordedEvents
        recordedEvents = events
        try {
            return block() to events
        } finally {
            recordedEvents = previous
        }
    }

    private fun takeStubsFrom(start: Int): ObjCExportUnitContent.Stubs {
        val unitStubs = stubs.subList(start, stubs.size)
        return ObjCExportUnitContent.Stubs(unitStubs.toList()).also { unitStubs.clear() }
    }

    private fun recordClassUnit(
        descriptor: ClassDescriptor,
        index: Int,
        isInMainPass: Boolean,
        translate: () -> Unit,
    ): ObjCExportClassUnit? {
        val start = stubs.size
        val wasGenerated = descriptor in generatedClasses
        val events = recording(translate).second
        if (stubs.size == start) return null
        val isGenerated = !wasGenerated && descriptor in generatedClasses
        return ObjCExportClassUnit(descriptor.classId.toString(), index, isInMainPass, isGenerated, events, takeStubsFrom(start))
    }

    private fun recordCategoryMembers(
        classDescriptor: ClassDescriptor,
        declarations: List<Triple<String, Int, CallableMemberDescriptor>>,
    ): List<ObjCExportCategoryMember> {
        val classId = classDescriptor.classId.toString()
        val className = recording { translator.translateExtendedClassName(classDescriptor) }.first
        return declarations.flatMap { [packageName, index, declaration] ->
            translator.translatePlainMemberEntries(listOf(declaration)).map { entry ->
                val thrownEvents = recording { entry.exportThrown() }.second
                val [memberStubs, events] = recording { entry.translate() }
                ObjCExportCategoryMember(
                    classId, className, entry.sortKey, packageName, index, thrownEvents, events, ObjCExportUnitContent.Stubs(memberStubs),
                )
            }
        }
    }

    private fun recordFileUnit(
        sourceFile: SourceFile,
        packageName: String,
        index: Int,
        declarations: List<CallableMemberDescriptor>,
    ): ObjCExportFileUnit {
        val start = stubs.size
        val events = recording { generateFile(sourceFile, declarations) }.second
        return ObjCExportFileUnit(sourceFile.name, packageName, index, events, takeStubsFrom(start))
    }

    private fun translateExtraClassUnit(descriptor: ClassDescriptor): ObjCExportClassUnit? {
        if (!shouldTranslateExtraClass(descriptor)) return null
        return recordClassUnit(descriptor, index = -1, isInMainPass = false) { generateClassOrInterface(descriptor) }
    }

    private fun emit(content: ObjCExportUnitContent) {
        when (content) {
            is ObjCExportUnitContent.Stubs -> stubs += content.stubs
            is ObjCExportUnitContent.Lines -> insertedLines += stubs.size to content.lines
        }
    }

    private fun emitCategory(className: String, contents: List<ObjCExportUnitContent>) {
        if (contents.all { it is ObjCExportUnitContent.Stubs }) {
            stubs += translator.buildExtensionsCategory(className, contents.flatMap { (it as ObjCExportUnitContent.Stubs).stubs })
        } else {
            val shell = StubRenderer.render(translator.buildExtensionsCategory(className, emptyList()), shouldExportKDoc)
            val memberLines = contents.flatMap { it.render(shouldExportKDoc, isTopLevel = false) }
            insertedLines += stubs.size to (shell.dropLast(1) + memberLines + shell.last() + "")
        }
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
                    val classDescriptor = getClassIfCategory(it)
                    if (classDescriptor == null) {
                        topLevel.getOrPut(it.findSourceFile(), { mutableListOf() }) += it
                    } else {
                        // If a class is hidden from Objective-C API then it is meaningless
                        // to export its extensions.
                        if (!classDescriptor.isHiddenFromObjC()) {
                            extensions.getOrPut(classDescriptor, { mutableListOf() }) += it
                        }
                    }
                }
        }

        val classesToTranslate = mutableListOf<ClassDescriptor>()

        packageFragments.forEach { packageFragment ->
            packageFragment.getMemberScope().collectClasses(classesToTranslate)
        }

        classesToTranslate.makeClassesOrderStable().forEach { translateClass(it) }

        extensions.makeCategoriesOrderStable().forEach { [classDescriptor, declarations] ->
            generateExtensions(classDescriptor, declarations)
        }

        topLevel.makeFilesOrderStable().forEach { [sourceFile, declarations] ->
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

    private fun record(isEarly: Boolean, descriptor: ClassDescriptor): Boolean {
        val events = recordedEvents
        val assembler = liveAssembler
        if (events == null && assembler == null) return false
        val classId = descriptor.classId.toString()
        referencedClasses.putIfAbsent(classId, descriptor)
        when {
            events != null -> events += ObjCExportUnitEvent(isEarly, classId)
            isEarly -> assembler!!.emit(classId)
            else -> assembler!!.reference(classId)
        }
        return true
    }

    internal fun generateExtraClassEarly(descriptor: ClassDescriptor) {
        if (record(isEarly = true, descriptor)) return
        if (shouldTranslateExtraClass(descriptor)) generateClass(descriptor)
    }

    internal fun generateExtraInterfaceEarly(descriptor: ClassDescriptor) {
        if (record(isEarly = true, descriptor)) return
        if (shouldTranslateExtraClass(descriptor)) generateInterface(descriptor)
    }

    private fun generateClassOrInterface(descriptor: ClassDescriptor) {
        if (descriptor.isInterface) generateInterface(descriptor) else generateClass(descriptor)
    }

    private fun generateClass(descriptor: ClassDescriptor) {
        if (!generatedClasses.add(descriptor)) return
        val translatedClass = translator.translateClass(descriptor)
        stubs.addAll(translatedClass.auxiliaryDeclarations)
        stubs.add(translatedClass.objCInterface)
    }

    private fun generateInterface(descriptor: ClassDescriptor) {
        if (!generatedClasses.add(descriptor)) return
        val stub = translator.translateInterface(descriptor)
        stubs.add(stub)
    }

    internal fun requireClassOrInterface(descriptor: ClassDescriptor) {
        if (record(isEarly = false, descriptor)) return
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
            objcExportBlockExplicitParameterNames: Boolean,
            shouldExportKDoc: Boolean,
            additionalImports: List<String>,
            restrictToLocalModules: Boolean = false,
            cachedModules: Set<ModuleDescriptor> = emptySet(),
        ): ObjCExportHeaderGenerator = ObjCExportHeaderGeneratorImpl(
            moduleDescriptors,
            mapper,
            namer,
            problemCollector,
            objcGenerics,
            objcExportBlockExplicitParameterNames,
            shouldExportKDoc,
            additionalImports,
            restrictToLocalModules,
            cachedModules,
        )
    }
}
