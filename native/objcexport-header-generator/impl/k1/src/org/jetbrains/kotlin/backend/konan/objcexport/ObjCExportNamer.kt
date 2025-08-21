/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.common.serialization.findSourceFile
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.descriptors.isArray
import org.jetbrains.kotlin.backend.konan.descriptors.isInterface
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.nativeBinaryOptions.UnitSuspendFunctionObjCExport
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.konan.isNativeStdlib
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.library.metadata.CurrentKlibModuleOrigin
import org.jetbrains.kotlin.library.metadata.DeserializedKlibModuleOrigin
import org.jetbrains.kotlin.library.metadata.SyntheticModulesOrigin
import org.jetbrains.kotlin.library.metadata.klibModuleOrigin
import org.jetbrains.kotlin.library.shortName
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.resolve.descriptorUtil.isSubclassOf
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.descriptorUtil.propertyIfAccessor
import org.jetbrains.kotlin.resolve.source.PsiSourceFile

internal interface ObjCExportNameTranslator {
    fun getFileClassName(file: KtFile): ObjCExportNamer.ClassOrProtocolName

    fun getCategoryName(file: KtFile): String

    fun getClassOrProtocolName(
        ktClassOrObject: KtClassOrObject,
    ): ObjCExportNamer.ClassOrProtocolName

    fun getTypeParameterName(ktTypeParameter: KtTypeParameter): String
}

interface ObjCExportNamer {
    data class ClassOrProtocolName(
        override val swiftName: String,
        override val objCName: String,
        override val binaryName: String = objCName,
    ) : ObjCExportClassOrProtocolName

    data class PropertyName(
        override val swiftName: String,
        override val objCName: String,
    ) : ObjCExportPropertyName

    interface Configuration {
        val topLevelNamePrefix: String
        fun getAdditionalPrefix(module: ModuleDescriptor): String?
        val objcGenerics: Boolean

        val disableSwiftMemberNameMangling: Boolean
            get() = false
        val ignoreInterfaceMethodCollisions: Boolean
            get() = false

        val nameCollisionMode: ObjCExportNameCollisionMode
            get() = ObjCExportNameCollisionMode.NONE

        val explicitMethodFamily: Boolean
            get() = false
    }

    val topLevelNamePrefix: String

    fun getFileClassName(file: SourceFile): ClassOrProtocolName
    fun getClassOrProtocolName(descriptor: ClassDescriptor): ClassOrProtocolName
    fun getSelector(method: FunctionDescriptor): String
    fun getParameterName(parameter: ParameterDescriptor): String
    fun getSwiftName(method: FunctionDescriptor): String
    fun getPropertyName(property: PropertyDescriptor): PropertyName
    fun getObjectInstanceSelector(descriptor: ClassDescriptor): String
    fun getEnumEntrySelector(descriptor: ClassDescriptor): String
    fun getEnumEntrySwiftName(descriptor: ClassDescriptor): String
    fun getEnumStaticMemberSelector(descriptor: CallableMemberDescriptor): String
    fun getTypeParameterName(typeParameterDescriptor: TypeParameterDescriptor): String

    fun numberBoxName(classId: ClassId): ClassOrProtocolName

    val kotlinAnyName: ClassOrProtocolName
    val mutableSetName: ClassOrProtocolName
    val mutableMapName: ClassOrProtocolName
    val kotlinNumberName: ClassOrProtocolName

    fun getObjectPropertySelector(descriptor: ClassDescriptor): String
    fun getCompanionObjectPropertySelector(descriptor: ClassDescriptor): String
    fun needsExplicitMethodFamily(name: String): Boolean

    companion object {
        @InternalKotlinNativeApi
        const val kotlinThrowableAsErrorMethodName: String = "asError"

        @InternalKotlinNativeApi
        const val objectPropertyName: String = "shared"

        @InternalKotlinNativeApi
        const val companionObjectPropertyName: String = "companion"
    }
}

fun createNamer(
    moduleDescriptor: ModuleDescriptor,
    topLevelNamePrefix: String,
): ObjCExportNamer =
    createNamer(moduleDescriptor, emptyList(), topLevelNamePrefix)

fun createNamer(
    moduleDescriptor: ModuleDescriptor,
    exportedDependencies: List<ModuleDescriptor>,
    topLevelNamePrefix: String,
): ObjCExportNamer = ObjCExportNamerImpl(
    (exportedDependencies + moduleDescriptor).toSet(),
    moduleDescriptor.builtIns,
    ObjCExportMapper(local = true, unitSuspendFunctionExport = UnitSuspendFunctionObjCExport.DEFAULT),
    ObjCExportProblemCollector.SILENT,
    topLevelNamePrefix,
    local = true
)

// Note: this class duplicates some of ObjCExportNamerImpl logic,
// but operates on different representation.
internal open class ObjCExportNameTranslatorImpl(
    private val configuration: ObjCExportNamer.Configuration,
) : ObjCExportNameTranslator {

    private val helper = ObjCExportNamingHelper(configuration.topLevelNamePrefix, configuration.objcGenerics)

    override fun getFileClassName(file: KtFile): ObjCExportNamer.ClassOrProtocolName =
        helper.getFileClassName(file)

    override fun getCategoryName(file: KtFile): String =
        helper.translateFileName(file)

    override fun getClassOrProtocolName(ktClassOrObject: KtClassOrObject): ObjCExportNamer.ClassOrProtocolName =
        ObjCExportNamer.ClassOrProtocolName(
            swiftName = getClassOrProtocolAsSwiftName(ktClassOrObject, true),
            objCName = buildString {
                getClassOrProtocolAsSwiftName(ktClassOrObject, false).split('.').forEachIndexed { index, part ->
                    append(if (index == 0) part else part.replaceFirstChar(Char::uppercaseChar))
                }
            }
        )

    private fun getClassOrProtocolAsSwiftName(
        ktClassOrObject: KtClassOrObject,
        forSwift: Boolean,
    ): String = buildString {
        val objCName = ktClassOrObject.getObjCName()
        if (objCName.isExact) {
            append(objCName.asIdentifier(forSwift))
        } else {
            val outerClass = ktClassOrObject.getStrictParentOfType<KtClassOrObject>()
            if (outerClass != null) {
                appendNameWithContainer(ktClassOrObject, objCName, outerClass, forSwift)
            } else {
                if (!forSwift) append(configuration.topLevelNamePrefix)
                append(objCName.asIdentifier(forSwift))
            }
        }
    }

    private fun StringBuilder.appendNameWithContainer(
        ktClassOrObject: KtClassOrObject,
        objCName: ObjCName,
        outerClass: KtClassOrObject,
        forSwift: Boolean,
    ) = helper.appendNameWithContainer(
        this,
        ktClassOrObject, objCName.asIdentifier(forSwift),
        outerClass, getClassOrProtocolAsSwiftName(outerClass, forSwift),
        object : ObjCExportNamingHelper.ClassInfoProvider<KtClassOrObject> {
            override fun hasGenerics(clazz: KtClassOrObject): Boolean =
                clazz.typeParametersWithOuter.count() != 0

            override fun isInterface(clazz: KtClassOrObject): Boolean = ktClassOrObject.isInterface
        }
    )

    override fun getTypeParameterName(ktTypeParameter: KtTypeParameter): String = buildString {
        append(ktTypeParameter.name!!.toIdentifier())
        while (helper.isTypeParameterNameReserved(this.toString())) append('_')
    }
}

private class ObjCExportNamingHelper(
    private val topLevelNamePrefix: String,
    private val objcGenerics: Boolean,
) {

    fun translateFileName(fileName: String): String =
        PackagePartClassUtils.getFilePartShortName(fileName).toIdentifier()

    fun translateFileName(file: KtFile): String = translateFileName(file.name)

    fun getFileClassName(fileName: String): ObjCExportNamer.ClassOrProtocolName {
        val baseName = translateFileName(fileName)
        return ObjCExportNamer.ClassOrProtocolName(swiftName = baseName, objCName = "$topLevelNamePrefix$baseName")
    }

    fun getFileClassName(file: KtFile): ObjCExportNamer.ClassOrProtocolName =
        getFileClassName(file.name)

    fun <T> appendNameWithContainer(
        builder: StringBuilder,
        clazz: T,
        ownName: String,
        containingClass: T,
        containerName: String,
        provider: ClassInfoProvider<T>,
    ) = builder.apply {
        if (clazz.canBeSwiftInner(provider)) {
            append(containerName)
            if (!this.contains('.') && containingClass.canBeSwiftOuter(provider)) {
                // AB -> AB.C
                append('.')
                append(mangleSwiftNestedClassName(ownName))
            } else {
                // AB -> ABC
                // A.B -> A.BC
                append(ownName.replaceFirstChar(Char::uppercaseChar))
            }
        } else {
            // AB, A.B -> ABC
            val dotIndex = containerName.indexOf('.')
            if (dotIndex == -1) {
                append(containerName)
            } else {
                append(containerName.substring(0, dotIndex))
                append(containerName.substring(dotIndex + 1).replaceFirstChar(Char::uppercaseChar))
            }
            append(ownName.replaceFirstChar(Char::uppercaseChar))
        }
    }

    interface ClassInfoProvider<T> {
        fun hasGenerics(clazz: T): Boolean
        fun isInterface(clazz: T): Boolean
    }

    private fun <T> T.canBeSwiftOuter(provider: ClassInfoProvider<T>): Boolean = when {
        objcGenerics && provider.hasGenerics(this) -> {
            // Swift nested classes are static but capture outer's generics.
            false
        }

        provider.isInterface(this) -> {
            // Swift doesn't support outer protocols.
            false
        }

        else -> true
    }

    private fun <T> T.canBeSwiftInner(provider: ClassInfoProvider<T>): Boolean = when {
        objcGenerics && provider.hasGenerics(this) -> {
            // Swift compiler doesn't seem to handle this case properly.
            // See https://bugs.swift.org/browse/SR-14607.
            // This behaviour of Kotlin is reported as https://youtrack.jetbrains.com/issue/KT-46518.
            false
        }

        provider.isInterface(this) -> {
            // Swift doesn't support nested protocols.
            false
        }

        else -> true
    }

    fun mangleSwiftNestedClassName(name: String): String = when (name) {
        "Type" -> "${name}_" // See https://github.com/JetBrains/kotlin-native/issues/3167
        else -> name
    }

    fun isTypeParameterNameReserved(name: String): Boolean = name in reservedTypeParameterNames

    private val reservedTypeParameterNames = setOf(
        "id", "NSObject", "NSArray", "NSCopying", "NSNumber", "NSInteger",
        "NSUInteger", "NSString", "NSSet", "NSDictionary", "NSMutableArray", "int", "unsigned", "short",
        "char", "long", "float", "double", "int32_t", "int64_t", "int16_t", "int8_t", "unichar"
    )

    fun isSpecialFamily(name: String): Boolean {
        val trimmed = name.dropWhile { it == '_' }
        return specialFamilyPrefixes.any { startsWithWords(trimmed, it) }
    }

    fun startsWithWords(sentence: String, words: String) = sentence.startsWith(words) &&
        (sentence.length == words.length || !sentence[words.length].isLowerCase())

    private val specialFamilyPrefixes = listOf("alloc", "copy", "mutableCopy", "new", "init")
}

@InternalKotlinNativeApi
class ObjCExportNamerImpl(
    private val configuration: ObjCExportNamer.Configuration,
    builtIns: KotlinBuiltIns,
    private val mapper: ObjCExportMapper,
    private val problemCollector: ObjCExportProblemCollector,
    private val local: Boolean,
) : ObjCExportNamer {
    constructor(
        moduleDescriptors: Set<ModuleDescriptor>,
        builtIns: KotlinBuiltIns,
        mapper: ObjCExportMapper,
        problemCollector: ObjCExportProblemCollector,
        topLevelNamePrefix: String,
        local: Boolean,
        objcGenerics: Boolean = false,
        disableSwiftMemberNameMangling: Boolean = false,
        ignoreInterfaceMethodCollisions: Boolean = false,
        nameCollisionMode: ObjCExportNameCollisionMode = ObjCExportNameCollisionMode.NONE,
        explicitMethodFamily: Boolean = false,
    ) : this(
        object : ObjCExportNamer.Configuration {
            override val topLevelNamePrefix: String
                get() = topLevelNamePrefix

            override fun getAdditionalPrefix(module: ModuleDescriptor): String? =
                if (module in moduleDescriptors) null else module.objCExportAdditionalNamePrefix

            override val objcGenerics: Boolean
                get() = objcGenerics

            override val disableSwiftMemberNameMangling: Boolean
                get() = disableSwiftMemberNameMangling

            override val ignoreInterfaceMethodCollisions: Boolean
                get() = ignoreInterfaceMethodCollisions

            override val nameCollisionMode: ObjCExportNameCollisionMode
                get() = nameCollisionMode

            override val explicitMethodFamily: Boolean
                get() = explicitMethodFamily
        },
        builtIns,
        mapper,
        problemCollector,
        local
    )

    private val objcGenerics get() = configuration.objcGenerics
    override val topLevelNamePrefix get() = configuration.topLevelNamePrefix
    private val helper = ObjCExportNamingHelper(configuration.topLevelNamePrefix, objcGenerics)

    private fun String.toSpecialStandardClassOrProtocolName() = ObjCExportNamer.ClassOrProtocolName(
        swiftName = "Kotlin$this",
        objCName = "${topLevelNamePrefix}$this"
    )

    override val kotlinAnyName = "Base".toSpecialStandardClassOrProtocolName()

    override val mutableSetName = "MutableSet".toSpecialStandardClassOrProtocolName()
    override val mutableMapName = "MutableDictionary".toSpecialStandardClassOrProtocolName()

    override fun numberBoxName(classId: ClassId): ObjCExportNamer.ClassOrProtocolName =
        classId.shortClassName.asString().toSpecialStandardClassOrProtocolName()

    override val kotlinNumberName = "Number".toSpecialStandardClassOrProtocolName()

    private val methodSelectors = object : Mapping<FunctionDescriptor, String>() {

        // Try to avoid clashing with critical NSObject instance methods:

        private val reserved = setOf(
            "retain", "release", "autorelease",
            "class", "superclass",
            "hash"
        )

        override fun reserved(name: String) = name in reserved

        override fun conflict(first: FunctionDescriptor, second: FunctionDescriptor): Boolean =
            !mapper.canHaveSameSelector(first, second, configuration.ignoreInterfaceMethodCollisions)
    }

    private val methodSwiftNames = object : Mapping<FunctionDescriptor, String>() {
        override fun conflict(first: FunctionDescriptor, second: FunctionDescriptor): Boolean {
            if (configuration.disableSwiftMemberNameMangling) return false // Ignore all conflicts.
            return !mapper.canHaveSameSelector(first, second, configuration.ignoreInterfaceMethodCollisions)
        }
        // Note: this condition is correct but can be too strict.
    }

    private inner class PropertyNameMapping(val forSwift: Boolean) : Mapping<PropertyDescriptor, String>() {
        override fun reserved(name: String) = name in Reserved.propertyNames

        override fun conflict(first: PropertyDescriptor, second: PropertyDescriptor): Boolean {
            if (forSwift && configuration.disableSwiftMemberNameMangling) return false // Ignore all conflicts.
            return !mapper.canHaveSameName(first, second, configuration.ignoreInterfaceMethodCollisions)
        }
    }

    private val objCPropertyNames = PropertyNameMapping(forSwift = false)
    private val swiftPropertyNames = PropertyNameMapping(forSwift = true)

    private open inner class GlobalNameMapping<in T : Any, N> : Mapping<T, N>() {
        final override fun conflict(first: T, second: T): Boolean = true
    }

    private val objCClassNames = GlobalNameMapping<Any, String>()
    private val objCProtocolNames = GlobalNameMapping<ClassDescriptor, String>()

    // Classes and protocols share the same namespace in Swift.
    private val swiftClassAndProtocolNames = GlobalNameMapping<Any, String>()

    private val genericTypeParameterNameMapping = GenericTypeParameterNameMapping()

    private abstract inner class ClassSelectorNameMapping<T : Any> : Mapping<T, String>() {

        // Try to avoid clashing with NSObject class methods:

        private val reserved = setOf(
            "retain", "release", "autorelease",
            "initialize", "load", "alloc", "new", "class", "superclass",
            "classFallbacksForKeyedArchiver", "classForKeyedUnarchiver",
            "description", "debugDescription", "version", "hash",
            "useStoredAccessor"
        )

        override fun reserved(name: String) = (name in reserved) || (name in cKeywords)
    }

    private val objectInstanceSelectors = object : ClassSelectorNameMapping<ClassDescriptor>() {
        override fun conflict(first: ClassDescriptor, second: ClassDescriptor) = false
    }

    private inner class EnumNameMapping : ClassSelectorNameMapping<DeclarationDescriptor>() {
        override fun conflict(first: DeclarationDescriptor, second: DeclarationDescriptor) =
            first.containingDeclaration == second.containingDeclaration
    }

    private val enumClassSelectors = EnumNameMapping()
    private val enumClassSwiftNames = EnumNameMapping()

    override fun getFileClassName(file: SourceFile): ObjCExportNamer.ClassOrProtocolName {
        val candidate by lazy {
            val fileName = when (file) {
                is PsiSourceFile -> {
                    val psiFile = file.psiFile
                    val ktFile = psiFile as? KtFile ?: error("PsiFile '$psiFile' is not KtFile")
                    ktFile.name
                }
                else -> file.name ?: error("$file has no name")
            }
            helper.getFileClassName(fileName)
        }

        val objCName = objCClassNames.getOrPut(file) {
            StringBuilder(candidate.objCName).mangledBySuffixUnderscores()
        }

        val swiftName = swiftClassAndProtocolNames.getOrPut(file) {
            StringBuilder(candidate.swiftName).mangledBySuffixUnderscores()
        }

        return ObjCExportNamer.ClassOrProtocolName(swiftName = swiftName, objCName = objCName)
    }

    override fun getClassOrProtocolName(descriptor: ClassDescriptor): ObjCExportNamer.ClassOrProtocolName =
        ObjCExportNamer.ClassOrProtocolName(
            swiftName = getClassOrProtocolSwiftName(descriptor),
            objCName = getClassOrProtocolObjCName(descriptor)
        )

    private fun getClassOrProtocolSwiftName(
        descriptor: ClassDescriptor,
    ): String = swiftClassAndProtocolNames.getOrPut(descriptor) {
        StringBuilder().apply {
            val objCName = descriptor.getObjCName()
            if (objCName.isExact) {
                append(objCName.asIdentifier(true))
            } else {
                val containingDeclaration = descriptor.containingDeclaration
                if (containingDeclaration is ClassDescriptor) {
                    appendSwiftNameWithContainer(descriptor, objCName, containingDeclaration)
                } else if (containingDeclaration is PackageFragmentDescriptor) {
                    appendTopLevelClassBaseName(descriptor, objCName, true)
                } else {
                    error("unexpected class parent: $containingDeclaration")
                }
            }
        }.mangledBySuffixUnderscores()
    }

    private fun StringBuilder.appendSwiftNameWithContainer(
        clazz: ClassDescriptor,
        objCName: ObjCName,
        containingClass: ClassDescriptor,
    ) = helper.appendNameWithContainer(
        this,
        clazz, objCName.asIdentifier(true),
        containingClass, getClassOrProtocolSwiftName(containingClass),
        object : ObjCExportNamingHelper.ClassInfoProvider<ClassDescriptor> {
            override fun hasGenerics(clazz: ClassDescriptor): Boolean =
                clazz.typeConstructor.parameters.isNotEmpty()

            override fun isInterface(clazz: ClassDescriptor): Boolean = clazz.isInterface
        }
    )

    private fun getClassOrProtocolObjCName(descriptor: ClassDescriptor): String {
        val objCMapping = if (descriptor.isInterface) objCProtocolNames else objCClassNames
        return objCMapping.getOrPut(descriptor) {
            StringBuilder().apply {
                val objCName = descriptor.getObjCName()
                if (objCName.isExact) {
                    append(objCName.asIdentifier(false))
                } else {
                    val containingDeclaration = descriptor.containingDeclaration
                    if (containingDeclaration is ClassDescriptor) {
                        append(getClassOrProtocolObjCName(containingDeclaration))
                            .append(objCName.asIdentifier(false).replaceFirstChar(Char::uppercaseChar))
                    } else if (containingDeclaration is PackageFragmentDescriptor) {
                        append(topLevelNamePrefix).appendTopLevelClassBaseName(descriptor, objCName, false)
                    } else {
                        error("unexpected class parent: $containingDeclaration")
                    }
                }
            }.mangledBySuffixUnderscores()
        }
    }

    private fun StringBuilder.appendTopLevelClassBaseName(descriptor: ClassDescriptor, objCName: ObjCName, forSwift: Boolean) = apply {
        configuration.getAdditionalPrefix(descriptor.module)?.let {
            append(it)
        }
        append(objCName.asIdentifier(forSwift))
    }

    override fun getParameterName(parameter: ParameterDescriptor): String = parameter.getObjCName().asString(forSwift = false)

    override fun getSelector(method: FunctionDescriptor): String = methodSelectors.getOrPut(method) {
        assert(mapper.isBaseMethod(method))

        getPredefined(method, Predefined.anyMethodSelectors)?.let { return it }

        val parameters = mapper.bridgeMethod(method).valueParametersAssociated(method)

        StringBuilder().apply {
            append(method.getMangledName(forSwift = false))

            parameters.forEachIndexed { index, (bridge, it) ->
                val name = when (bridge) {
                    is MethodBridgeValueParameter.Mapped -> when {
                        it is ReceiverParameterDescriptor -> it.getObjCName().asIdentifier(false) { "" }
                        method is PropertySetterDescriptor -> when (parameters.size) {
                            1 -> ""
                            else -> "value"
                        }
                        else -> it!!.getObjCName().asIdentifier(false)
                    }
                    MethodBridgeValueParameter.ErrorOutParameter -> "error"
                    is MethodBridgeValueParameter.SuspendCompletion -> "completionHandler"
                }

                if (index == 0) {
                    append(
                        when {
                            bridge is MethodBridgeValueParameter.ErrorOutParameter -> "AndReturn"
                            bridge is MethodBridgeValueParameter.SuspendCompletion -> "With"
                            method is ConstructorDescriptor -> "With"
                            else -> ""
                        }
                    )
                    append(name.replaceFirstChar(Char::uppercaseChar))
                } else {
                    append(name)
                }

                append(':')
            }
        }.mangledSequence {
            if (parameters.isNotEmpty()) {
                // "foo:" -> "foo_:"
                insert(lastIndex, '_')
            } else {
                // "foo" -> "foo_"
                append("_")
            }
        }
    }

    override fun getSwiftName(method: FunctionDescriptor): String = methodSwiftNames.getOrPut(method) {
        assert(mapper.isBaseMethod(method))

        getPredefined(method, Predefined.anyMethodSwiftNames)?.let { return it }

        val parameters = mapper.bridgeMethod(method).valueParametersAssociated(method)

        StringBuilder().apply {
            append(method.getMangledName(forSwift = true))
            append("(")

            parameters@ for ((bridge, it) in parameters) {
                val label = when (bridge) {
                    is MethodBridgeValueParameter.Mapped -> when {
                        it is ReceiverParameterDescriptor -> it.getObjCName().asIdentifier(true) { "_" }
                        method is PropertySetterDescriptor -> when (parameters.size) {
                            1 -> "_"
                            else -> "value"
                        }
                        else -> it!!.getObjCName().asIdentifier(true)
                    }
                    MethodBridgeValueParameter.ErrorOutParameter -> continue@parameters
                    is MethodBridgeValueParameter.SuspendCompletion -> "completionHandler"
                }

                append(label)
                append(":")
            }

            append(")")
        }.mangledSequence {
            // "foo(label:)" -> "foo(label_:)"
            // "foo()" -> "foo_()"
            insert(lastIndex - 1, '_')
        }
    }

    private fun <T : Any> getPredefined(method: FunctionDescriptor, predefinedForAny: Map<Name, T>): T? {
        return if (method.containingDeclaration.let { it is ClassDescriptor && KotlinBuiltIns.isAny(it) }) {
            predefinedForAny.getValue(method.name)
        } else {
            null
        }
    }

    override fun getPropertyName(property: PropertyDescriptor): ObjCExportNamer.PropertyName {
        assert(mapper.isBaseProperty(property))
        assert(isObjCProperty(property))
        val objCName = property.getObjCName()
        fun PropertyNameMapping.getOrPut(forSwift: Boolean) = getOrPut(property) {
            StringBuilder().apply {
                append(objCName.asIdentifier(forSwift))
            }.mangledSequence {
                append('_')
            }
        }
        return ObjCExportNamer.PropertyName(
            swiftName = swiftPropertyNames.getOrPut(true),
            objCName = objCPropertyNames.getOrPut(false)
        )
    }

    override fun getObjectInstanceSelector(descriptor: ClassDescriptor): String {
        assert(descriptor.kind == ClassKind.OBJECT)

        return objectInstanceSelectors.getOrPut(descriptor) {
            val name = descriptor.getObjCName().asString(false)
                .replaceFirstChar(Char::lowercaseChar).toIdentifier().mangleIfSpecialFamily("get")
            StringBuilder(name).mangledBySuffixUnderscores()
        }
    }

    private fun ClassDescriptor.getEnumEntryName(forSwift: Boolean): Sequence<String> {
        val name = getObjCName().asIdentifier(forSwift) {
            // FOO_BAR_BAZ -> fooBarBaz:
            it.split('_').mapIndexed { index, s ->
                val lower = s.lowercase()
                if (index == 0) lower else lower.replaceFirstChar(Char::uppercaseChar)
            }.joinToString("").toIdentifier()
        }.mangleIfSpecialFamily("the")
        return StringBuilder(name).mangledBySuffixUnderscores()
    }

    override fun getEnumEntrySelector(descriptor: ClassDescriptor): String {
        assert(descriptor.kind == ClassKind.ENUM_ENTRY)

        return enumClassSelectors.getOrPut(descriptor) {
            descriptor.getEnumEntryName(false)
        }
    }

    override fun getEnumEntrySwiftName(descriptor: ClassDescriptor): String {
        assert(descriptor.kind == ClassKind.ENUM_ENTRY)

        return enumClassSwiftNames.getOrPut(descriptor) {
            descriptor.getEnumEntryName(true)
        }
    }

    override fun getEnumStaticMemberSelector(descriptor: CallableMemberDescriptor): String {
        val containingDeclaration = descriptor.containingDeclaration
        require(containingDeclaration is ClassDescriptor && containingDeclaration.kind == ClassKind.ENUM_CLASS)
        require(descriptor.dispatchReceiverParameter == null) { "must be static" }
        require(descriptor.extensionReceiverParameter == null) { "must be static" }

        return enumClassSelectors.getOrPut(descriptor) {
            StringBuilder(descriptor.name.asString()).mangledBySuffixUnderscores()
        }
    }

    override fun getTypeParameterName(typeParameterDescriptor: TypeParameterDescriptor): String {
        return genericTypeParameterNameMapping.getOrPut(typeParameterDescriptor) {
            StringBuilder().apply {
                append(typeParameterDescriptor.name.asString().toIdentifier())
            }.mangledSequence {
                append('_')
            }
        }
    }


    override fun getObjectPropertySelector(descriptor: ClassDescriptor): String {
        val collides = ObjCExportNamer.objectPropertyName == getObjectInstanceSelector(descriptor)
        return ObjCExportNamer.objectPropertyName + (if (collides) "_" else "")
    }

    override fun getCompanionObjectPropertySelector(descriptor: ClassDescriptor): String {
        return ObjCExportNamer.companionObjectPropertyName
    }

    override fun needsExplicitMethodFamily(name: String): Boolean {
        return configuration.explicitMethodFamily && helper.isSpecialFamily(name)
    }

    init {
        if (!local) {
            forceAssignPredefined(builtIns)
        }
    }

    private fun forceAssignPredefined(builtIns: KotlinBuiltIns) {
        val any = builtIns.any

        val predefinedClassNames = mapOf(
            builtIns.any to kotlinAnyName,
            builtIns.mutableSet to mutableSetName,
            builtIns.mutableMap to mutableMapName
        )

        predefinedClassNames.forEach { (descriptor, name) ->
            objCClassNames.forceAssign(descriptor, name.objCName)
            swiftClassAndProtocolNames.forceAssign(descriptor, name.swiftName)
        }

        fun ClassDescriptor.method(name: Name) =
            this.unsubstitutedMemberScope.getContributedFunctions(
                name,
                NoLookupLocation.FROM_BACKEND
            ).single()

        Predefined.anyMethodSelectors.forEach { (name, selector) ->
            methodSelectors.forceAssign(any.method(name), selector)
        }

        Predefined.anyMethodSwiftNames.forEach { (name, swiftName) ->
            methodSwiftNames.forceAssign(any.method(name), swiftName)
        }
    }

    private object Predefined {
        val anyMethodSelectors = mapOf(
            "hashCode" to "hash",
            "toString" to "description",
            "equals" to "isEqual:"
        ).mapKeys { Name.identifier(it.key) }

        val anyMethodSwiftNames = mapOf(
            "hashCode" to "hash()",
            "toString" to "description()",
            "equals" to "isEqual(_:)"
        ).mapKeys { Name.identifier(it.key) }
    }

    private object Reserved {
        val propertyNames = cKeywords +
            setOf("description") // https://youtrack.jetbrains.com/issue/KT-38641
    }

    private fun FunctionDescriptor.getMangledName(forSwift: Boolean): String {
        if (this is ConstructorDescriptor) {
            return if (this.constructedClass.isArray && !forSwift) "array" else "init"
        }

        val candidate = when (this) {
            is PropertyGetterDescriptor -> this.correspondingProperty.getObjCName().asIdentifier(forSwift)
            is PropertySetterDescriptor -> "set${
                this.correspondingProperty.getObjCName().asString(forSwift).replaceFirstChar(kotlin.Char::uppercaseChar)
            }".toIdentifier()
            else -> this.getObjCName().asIdentifier(forSwift)
        }

        return candidate.mangleIfSpecialFamily("do")
    }

    private fun String.mangleIfSpecialFamily(prefix: String): String =
        if (!configuration.explicitMethodFamily && helper.isSpecialFamily(this) ||
            this == "init" || this.startsWith("initWith")
        ) {
            // Method can be detected as having special family by Objective-C compiler, or might clash with a generated constructor.
            // Mangle the name:
            prefix + this.replaceFirstChar(Char::uppercaseChar)
        } else {
            // TODO: handle clashes with NSObject methods etc.
            this
        }

    private inner class GenericTypeParameterNameMapping {
        private val elementToName = mutableMapOf<TypeParameterDescriptor, String>()
        private val typeParameterNameClassOverrides = mutableMapOf<ClassDescriptor, MutableSet<String>>()

        fun getOrPut(element: TypeParameterDescriptor, nameCandidates: () -> Sequence<String>): String {
            getIfAssigned(element)?.let { return it }

            nameCandidates().forEach {
                if (tryAssign(element, it)) {
                    return it
                }
            }

            error("name candidates run out")
        }

        private fun tryAssign(element: TypeParameterDescriptor, name: String): Boolean {
            if (element in elementToName) error(element)

            if (helper.isTypeParameterNameReserved(name)) return false

            if (!validName(element, name)) return false

            assignName(element, name)

            return true
        }

        private fun assignName(element: TypeParameterDescriptor, name: String) {
            if (!local) {
                elementToName[element] = name
                classNameSet(element).add(name)
            }
        }

        private fun validName(element: TypeParameterDescriptor, name: String): Boolean {
            assert(element.containingDeclaration is ClassDescriptor)

            return !objCClassNames.nameExists(name) && !objCProtocolNames.nameExists(name) &&
                (local || name !in classNameSet(element))
        }

        private fun classNameSet(element: TypeParameterDescriptor): MutableSet<String> {
            require(!local)
            return typeParameterNameClassOverrides.getOrPut(element.containingDeclaration as ClassDescriptor) {
                mutableSetOf()
            }
        }

        private fun getIfAssigned(element: TypeParameterDescriptor): String? = elementToName[element]
    }

    private sealed class AssignResult {
        data object Success : AssignResult()
        data object Reserved : AssignResult()
        data class Conflict(val conflictingElement: Any) : AssignResult()
    }

    private abstract inner class Mapping<in T : Any, N>() {
        private val elementToName = mutableMapOf<T, N>()
        private val nameToElements = mutableMapOf<N, MutableList<T>>()

        abstract fun conflict(first: T, second: T): Boolean
        open fun reserved(name: N) = false
        inline fun getOrPut(element: T, nameCandidates: () -> Sequence<N>): N {
            getIfAssigned(element)?.let { return it }

            var reportedCollision = false

            nameCandidates().forEach {
                val res = tryAssign(element, it)
                if (res is AssignResult.Success) {
                    return it
                }

                if (!reportedCollision) {
                    reportedCollision = true
                    val conflict = when (res) {
                        is AssignResult.Conflict if res.conflictingElement is DeclarationDescriptor ->
                            DescriptorRenderer.COMPACT_WITH_SHORT_TYPES.render(res.conflictingElement)
                        is AssignResult.Conflict -> "${res.conflictingElement}"
                        AssignResult.Reserved -> "a keyword or a reserved name"
                    }
                    when (configuration.nameCollisionMode) {
                        ObjCExportNameCollisionMode.ERROR -> {
                            if (element is DeclarationDescriptor) {
                                problemCollector.reportError(element, "name is mangled when generating Objective-C header because it conflicts with $conflict")
                            } else {
                                problemCollector.reportError("name \"$it\" is mangled when generating Objective-C header because it conflicts with $conflict")
                            }
                        }
                        ObjCExportNameCollisionMode.WARNING -> {
                            if (element is DeclarationDescriptor) {
                                problemCollector.reportWarning(element, "name is mangled when generating Objective-C header because it conflicts with $conflict")
                            } else {
                                problemCollector.reportWarning("name \"$it\" is mangled when generating Objective-C header because it conflicts with $conflict")
                            }
                        }
                        ObjCExportNameCollisionMode.NONE -> Unit
                    }
                }
            }

            error("name candidates run out")
        }

        fun nameExists(name: N) = nameToElements.containsKey(name)

        private fun getIfAssigned(element: T): N? = elementToName[element]

        private fun tryAssign(element: T, name: N): AssignResult {
            if (element in elementToName) error(element)

            if (reserved(name)) return AssignResult.Reserved

            nameToElements[name].orEmpty().firstOrNull { conflict(element, it) }?.let {
                return AssignResult.Conflict(it)
            }

            if (!local) {
                nameToElements.getOrPut(name) { mutableListOf() } += element

                elementToName[element] = name
            }

            return AssignResult.Success
        }

        fun forceAssign(element: T, name: N) {
            if (name in nameToElements || element in elementToName) error(element)

            nameToElements[name] = mutableListOf(element)
            elementToName[element] = name
        }
    }

}

private inline fun StringBuilder.mangledSequence(crossinline mangle: StringBuilder.() -> Unit) =
    generateSequence(this.toString()) {
        this@mangledSequence.mangle()
        this@mangledSequence.toString()
    }

private fun StringBuilder.mangledBySuffixUnderscores() = this.mangledSequence { append("_") }

private fun ObjCExportMapper.canHaveCommonSubtype(
    first: ClassDescriptor,
    second: ClassDescriptor,
    ignoreInterfaceMethodCollisions: Boolean,
): Boolean {
    if (first.isSubclassOf(second) || second.isSubclassOf(first)) {
        return true
    }

    if (first.isFinalClass || second.isFinalClass) {
        return false
    }

    return (first.isInterface || second.isInterface) && !ignoreInterfaceMethodCollisions
}

private fun ObjCExportMapper.canBeInheritedBySameClass(
    first: CallableMemberDescriptor,
    second: CallableMemberDescriptor,
    ignoreInterfaceMethodCollisions: Boolean,
): Boolean {
    if (isTopLevel(first) || isTopLevel(second)) {
        return isTopLevel(first) && isTopLevel(second) &&
            first.propertyIfAccessor.findSourceFile() == second.propertyIfAccessor.findSourceFile()
    }

    val firstClass = getClassIfCategory(first) ?: first.containingDeclaration as ClassDescriptor
    val secondClass = getClassIfCategory(second) ?: second.containingDeclaration as ClassDescriptor

    if (first is ConstructorDescriptor) {
        return firstClass == secondClass || second !is ConstructorDescriptor && firstClass.isSubclassOf(secondClass)
    }

    if (second is ConstructorDescriptor) {
        return secondClass == firstClass || first !is ConstructorDescriptor && secondClass.isSubclassOf(firstClass)
    }

    return canHaveCommonSubtype(firstClass, secondClass, ignoreInterfaceMethodCollisions)
}

private fun ObjCExportMapper.canHaveSameSelector(
    first: FunctionDescriptor,
    second: FunctionDescriptor,
    ignoreInterfaceMethodCollisions: Boolean,
): Boolean {
    assert(isBaseMethod(first))
    assert(isBaseMethod(second))

    if (!canBeInheritedBySameClass(first, second, ignoreInterfaceMethodCollisions)) {
        return true
    }

    if (first.dispatchReceiverParameter == null || second.dispatchReceiverParameter == null) {
        // I.e. any is category method.
        return false
    }

    if (first.name != second.name) {
        return false
    }
    if (first.extensionReceiverParameter?.type != second.extensionReceiverParameter?.type) {
        return false
    }

    if (first is PropertySetterDescriptor && second is PropertySetterDescriptor) {
        // Methods should merge in any common subclass as it can't have two properties with same name.
    } else if (first.valueParameters.map { it.type } == second.valueParameters.map { it.type }) {
        // Methods should merge in any common subclasses since they have the same signature.
    } else {
        return false
    }

    // Check if methods have the same bridge (and thus the same ABI):
    return bridgeMethod(first) == bridgeMethod(second)
}

private fun ObjCExportMapper.canHaveSameName(
    first: PropertyDescriptor,
    second: PropertyDescriptor,
    ignoreInterfaceMethodCollisions: Boolean,
): Boolean {
    assert(isBaseProperty(first))
    assert(isObjCProperty(first))
    assert(isBaseProperty(second))
    assert(isObjCProperty(second))

    if (!canBeInheritedBySameClass(first, second, ignoreInterfaceMethodCollisions)) {
        return true
    }

    if (first.dispatchReceiverParameter == null || second.dispatchReceiverParameter == null) {
        // I.e. any is category property.
        return false
    }

    if (first.name != second.name) {
        return false
    }

    return bridgePropertyType(first) == bridgePropertyType(second)
}

private class ObjCName(
    private val kotlinName: String,
    private val objCName: String?,
    private val swiftName: String?,
    val isExact: Boolean,
) {
    // TODO: Prevent mangling when objCName or swiftName is provided

    fun asString(forSwift: Boolean): String = swiftName.takeIf { forSwift } ?: objCName ?: kotlinName

    fun asIdentifier(forSwift: Boolean, default: (String) -> String = { it.toIdentifier() }): String =
        swiftName.takeIf { forSwift } ?: objCName ?: default(kotlinName)
}

private fun DeclarationDescriptor.getObjCName(): ObjCName {
    var objCName: String? = null
    var swiftName: String? = null
    var isExact = false
    annotations.findAnnotation(KonanFqNames.objCName)?.let { annotation ->
        objCName = annotation.argumentValue("name")?.value as String?
        swiftName = annotation.argumentValue("swiftName")?.value as String?
        isExact = annotation.argumentValue("exact")?.value as Boolean? ?: false
    }
    return ObjCName(name.asString(), objCName, swiftName, isExact)
}

private fun <T> T.upcast(): T = this

private fun CallableDescriptor.getObjCName(): ObjCName =
    overriddenDescriptors.firstOrNull()?.getObjCName() ?: upcast<DeclarationDescriptor>().getObjCName()

private fun ParameterDescriptor.getObjCName(): ObjCName {
    val callableDescriptor = containingDeclaration as? CallableDescriptor ?: return upcast<CallableDescriptor>().getObjCName()
    fun CallableDescriptor.getBase(): CallableDescriptor = overriddenDescriptors.firstOrNull()?.getBase() ?: this
    val baseCallableDescriptor = callableDescriptor.getBase()
    if (callableDescriptor.extensionReceiverParameter == this) {
        return baseCallableDescriptor.extensionReceiverParameter!!.upcast<CallableDescriptor>().getObjCName()
    }
    val parameterIndex = callableDescriptor.valueParameters.indexOf(this)
    if (parameterIndex != -1) {
        return baseCallableDescriptor.valueParameters[parameterIndex].upcast<CallableDescriptor>().getObjCName()
    }
    error("Unexpected parameter: $this")
}

private val objCNameShortName = KonanFqNames.objCName.shortName().asString()

private fun KtClassOrObject.getObjCName(): ObjCName {
    var objCName: String? = null
    var swiftName: String? = null
    var isExact = false
    annotationEntries.firstOrNull {
        it.calleeExpression?.constructorReferenceExpression?.getReferencedName() == objCNameShortName
    }?.let { annotation ->
        fun ValueArgument.getStringValue(): String? {
            val stringTemplateExpression = when (this) {
                is KtValueArgument -> stringTemplateExpression
                else -> getArgumentExpression() as? KtStringTemplateExpression
            } ?: return null
            return (stringTemplateExpression.entries.singleOrNull() as? KtLiteralStringTemplateEntry)?.text
        }

        fun ValueArgument.getBooleanValue(): Boolean =
            (getArgumentExpression() as? KtConstantExpression)?.text?.toBooleanStrictOrNull() ?: false

        val argNames = setOf("name", "swiftName", "exact")
        val processedArgs = mutableSetOf<String>()
        for (argument in annotation.valueArguments) {
            val argName = argument.getArgumentName()?.asName?.asString() ?: (argNames - processedArgs).firstOrNull() ?: break
            when (argName) {
                "name" -> objCName = argument.getStringValue()
                "swiftName" -> swiftName = argument.getStringValue()
                "exact" -> isExact = argument.getBooleanValue()
            }
            processedArgs.add(argName)
        }
    }
    return ObjCName(name!!, objCName, swiftName, isExact)
}

internal val ModuleDescriptor.objCExportAdditionalNamePrefix: String
    get() {
        if (this.isNativeStdlib()) return "Kotlin"

        val fullPrefix = when (val module = this.klibModuleOrigin) {
            CurrentKlibModuleOrigin ->
                error("expected deserialized module, got $this (origin = $module)")
            SyntheticModulesOrigin ->
                this.name.asString().let { it.substring(1, it.lastIndex) }
            is DeserializedKlibModuleOrigin ->
                module.library.let { it.shortName ?: it.uniqueName }
        }

        return abbreviate(fullPrefix)
    }


fun abbreviate(name: String): String {
    val normalizedName = name
        .replaceFirstChar(Char::uppercaseChar)
        .replace("-|\\.".toRegex(), "_")

    val uppers = normalizedName.filterIndexed { index, character -> index == 0 || character.isUpperCase() }
    if (uppers.length >= 3) return uppers

    return normalizedName
}

// Note: most usages of this method rely on the fact that concatenation of valid identifiers is valid identifier.
// This may sometimes be a bit conservative (since it requires mangling non-first character as if it was first);
// ignore this for simplicity as having Kotlin identifiers starting from digits is supposed to be rare case.
internal fun String.toValidObjCSwiftIdentifier(): String {
    if (this.isEmpty()) return "__"

    return this.replace('$', '_') // TODO: handle more special characters.
        .let { if (it.first().isDigit()) "_$it" else it }
        .let { if (it == "_") "__" else it }
}

// Private shortcut.
private fun String.toIdentifier(): String = this.toValidObjCSwiftIdentifier()
