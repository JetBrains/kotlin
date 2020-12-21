/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.native.interop.gen.jvm.GenerationMode
import org.jetbrains.kotlin.native.interop.gen.jvm.InteropConfiguration
import org.jetbrains.kotlin.native.interop.gen.jvm.KotlinPlatform
import org.jetbrains.kotlin.native.interop.indexer.*

/**
 * Components that are not passed via StubIr but required for bridge generation.
 */
class BridgeGenerationInfo(val cGlobalName: String, val typeInfo: TypeInfo)

/**
 * Additional components that are required to generate bridges.
 * TODO: Metadata-based interop should not depend on these components.
 */
interface BridgeGenerationComponents {

    val setterToBridgeInfo: Map<PropertyAccessor.Setter, BridgeGenerationInfo>

    val getterToBridgeInfo: Map<PropertyAccessor.Getter, BridgeGenerationInfo>

    val arrayGetterInfo: Map<PropertyAccessor.Getter, BridgeGenerationInfo>

    val enumToTypeMirror: Map<ClassStub.Enum, TypeMirror>

    val wCStringParameters: Set<FunctionParameterStub>

    val cStringParameters: Set<FunctionParameterStub>
}

class BridgeGenerationComponentsBuilder {

    val getterToBridgeInfo = mutableMapOf<PropertyAccessor.Getter, BridgeGenerationInfo>()
    val setterToBridgeInfo = mutableMapOf<PropertyAccessor.Setter, BridgeGenerationInfo>()
    val arrayGetterBridgeInfo = mutableMapOf<PropertyAccessor.Getter, BridgeGenerationInfo>()
    val enumToTypeMirror = mutableMapOf<ClassStub.Enum, TypeMirror>()
    val wCStringParameters = mutableSetOf<FunctionParameterStub>()
    val cStringParameters = mutableSetOf<FunctionParameterStub>()

    fun build(): BridgeGenerationComponents = object : BridgeGenerationComponents {
        override val getterToBridgeInfo =
                this@BridgeGenerationComponentsBuilder.getterToBridgeInfo.toMap()

        override val setterToBridgeInfo =
                this@BridgeGenerationComponentsBuilder.setterToBridgeInfo.toMap()

        override val enumToTypeMirror =
                this@BridgeGenerationComponentsBuilder.enumToTypeMirror.toMap()

        override val wCStringParameters: Set<FunctionParameterStub> =
                this@BridgeGenerationComponentsBuilder.wCStringParameters.toSet()

        override val cStringParameters: Set<FunctionParameterStub> =
                this@BridgeGenerationComponentsBuilder.cStringParameters.toSet()

        override val arrayGetterInfo: Map<PropertyAccessor.Getter, BridgeGenerationInfo> =
                this@BridgeGenerationComponentsBuilder.arrayGetterBridgeInfo.toMap()
    }
}

/**
 * Components that are not passed via StubIr but required for generation of wrappers.
 */
class WrapperGenerationInfo(val global: GlobalDecl, val passViaPointer: Boolean = false)

interface WrapperGenerationComponents {
    val getterToWrapperInfo: Map<PropertyAccessor.Getter.ExternalGetter, WrapperGenerationInfo>
    val setterToWrapperInfo: Map<PropertyAccessor.Setter.ExternalSetter, WrapperGenerationInfo>
}

class WrapperGenerationComponentsBuilder {

    val getterToWrapperInfo = mutableMapOf<PropertyAccessor.Getter.ExternalGetter, WrapperGenerationInfo>()
    val setterToWrapperInfo = mutableMapOf<PropertyAccessor.Setter.ExternalSetter, WrapperGenerationInfo>()

    fun build(): WrapperGenerationComponents = object : WrapperGenerationComponents {
        override val getterToWrapperInfo = this@WrapperGenerationComponentsBuilder.getterToWrapperInfo.toMap()

        override val setterToWrapperInfo = this@WrapperGenerationComponentsBuilder.setterToWrapperInfo.toMap()
    }
}

/**
 * Common part of all [StubIrBuilder] implementations.
 */
interface StubsBuildingContext {
    val configuration: InteropConfiguration

    fun mirror(type: Type): TypeMirror

    val declarationMapper: DeclarationMapper

    fun generateNextUniqueId(prefix: String): String

    val generatedObjCCategoriesMembers: MutableMap<ObjCClass, GeneratedObjCCategoriesMembers>

    val platform: KotlinPlatform

    /**
     * In some cases StubIr should be different for metadata and sourcecode modes.
     * For example, it is impossible to represent call to superclass constructor in
     * metadata directly and arguments should be passed via annotations instead.
     */
    val generationMode: GenerationMode

    fun isStrictEnum(enumDef: EnumDef): Boolean

    val macroConstantsByName: Map<String, MacroDef>

    fun tryCreateIntegralStub(type: Type, value: Long): IntegralConstantStub?

    fun tryCreateDoubleStub(type: Type, value: Double): DoubleConstantStub?

    val bridgeComponentsBuilder: BridgeGenerationComponentsBuilder

    val wrapperComponentsBuilder: WrapperGenerationComponentsBuilder

    fun getKotlinClassFor(objCClassOrProtocol: ObjCClassOrProtocol, isMeta: Boolean = false): Classifier

    fun getKotlinClassForPointed(structDecl: StructDecl): Classifier

    fun isOverloading(name: String, types: List<StubType>): Boolean
}

/**
 *
 */
internal interface StubElementBuilder {
    val context: StubsBuildingContext

    fun build(): List<StubIrElement>
}

open class StubsBuildingContextImpl(
        private val stubIrContext: StubIrContext
) : StubsBuildingContext {

    override val configuration: InteropConfiguration = stubIrContext.configuration
    override val platform: KotlinPlatform = stubIrContext.platform
    override val generationMode: GenerationMode = stubIrContext.generationMode
    val imports: Imports = stubIrContext.imports
    protected val nativeIndex: NativeIndex = stubIrContext.nativeIndex

    private var theCounter = 0

    private val uniqFunctions = mutableSetOf<String>()

    override fun isOverloading(name: String, types: List<StubType>):Boolean  {
        return if (configuration.library.language == Language.CPP) {
            val signature = "${name}( ${types.map { it.toString() }.joinToString(", ")}  )"
            !uniqFunctions.add(signature)
        } else {
            !uniqFunctions.add(name)
        }
    }

    override fun generateNextUniqueId(prefix: String) =
            prefix + pkgName.replace('.', '_') + theCounter++

    override fun mirror(type: Type): TypeMirror = mirror(declarationMapper, type)

    /**
     * Indicates whether this enum should be represented as Kotlin enum.
     */

    override fun isStrictEnum(enumDef: EnumDef): Boolean = with(enumDef) {
        if (this.isAnonymous) {
            return false
        }

        val name = this.kotlinName

        if (name in configuration.strictEnums) {
            return true
        }

        if (name in configuration.nonStrictEnums) {
            return false
        }

        // Let the simple heuristic decide:
        return !this.constants.any { it.isExplicitlyDefined }
    }

    override val generatedObjCCategoriesMembers = mutableMapOf<ObjCClass, GeneratedObjCCategoriesMembers>()

    override val declarationMapper = DeclarationMapperImpl()

    override val macroConstantsByName: Map<String, MacroDef> =
            (nativeIndex.macroConstants + nativeIndex.wrappedMacros).associateBy { it.name }

    /**
     * The name to be used for this enum in Kotlin
     */
    val EnumDef.kotlinName: String
        get() = if (spelling.startsWith("enum ")) {
            spelling.substringAfter(' ')
        } else {
            assert (!isAnonymous)
            spelling
        }


    private val pkgName: String
        get() = configuration.pkgName

    /**
     * The name to be used for this struct in Kotlin
     */
    val StructDecl.kotlinName: String
        get() = stubIrContext.getKotlinName(this)

    override fun tryCreateIntegralStub(type: Type, value: Long): IntegralConstantStub? {
        val integerType = when (val unwrappedType = type.unwrapTypedefs()) {
            is IntegerType -> unwrappedType
            CharType -> IntegerType(1, true, "char")
            else -> return null
        }
        val size = integerType.size
        if (size != 1 && size != 2 && size != 4 && size != 8) return null
        return IntegralConstantStub(value, size, declarationMapper.isMappedToSigned(integerType))
    }

    override fun tryCreateDoubleStub(type: Type, value: Double): DoubleConstantStub? {
        val unwrappedType = type.unwrapTypedefs() as? FloatingType ?: return null
        val size = unwrappedType.size
        if (size != 4 && size != 8) return null
        return DoubleConstantStub(value, size)
    }

    override val bridgeComponentsBuilder = BridgeGenerationComponentsBuilder()

    override val wrapperComponentsBuilder = WrapperGenerationComponentsBuilder()

    override fun getKotlinClassFor(objCClassOrProtocol: ObjCClassOrProtocol, isMeta: Boolean): Classifier {
        return declarationMapper.getKotlinClassFor(objCClassOrProtocol, isMeta)
    }

    override fun getKotlinClassForPointed(structDecl: StructDecl): Classifier {
        val classifier = declarationMapper.getKotlinClassForPointed(structDecl)
        return classifier
    }

    open inner class DeclarationMapperImpl : DeclarationMapper {
        override fun getKotlinClassForPointed(structDecl: StructDecl): Classifier {
            val baseName = structDecl.kotlinName
            val pkg = when (platform) {
                KotlinPlatform.JVM -> pkgName
                KotlinPlatform.NATIVE -> if (structDecl.def == null) {
                    cnamesStructsPackageName // to be imported as forward declaration.
                } else {
                    getPackageFor(structDecl)
                }
            }
            return Classifier.topLevel(pkg, baseName)
        }

        override fun getKotlinClassForManaged(structDecl: StructDecl): Classifier =
                error("ManagedType requires a plugin")

        override fun isMappedToStrict(enumDef: EnumDef): Boolean = isStrictEnum(enumDef)

        override fun getKotlinNameForValue(enumDef: EnumDef): String = enumDef.kotlinName

        override fun getPackageFor(declaration: TypeDeclaration): String {
            return imports.getPackage(declaration.location) ?: pkgName
        }

        override val useUnsignedTypes: Boolean
            get() = when (platform) {
                KotlinPlatform.JVM -> false
                KotlinPlatform.NATIVE -> true
            }
    }

}

data class StubIrBuilderResult(
        val stubs: SimpleStubContainer,
        val declarationMapper: DeclarationMapper,
        val bridgeGenerationComponents: BridgeGenerationComponents,
        val wrapperGenerationComponents: WrapperGenerationComponents
)

/**
 * Produces [StubIrBuilderResult] for given [KotlinPlatform] using [InteropConfiguration].
 */
class StubIrBuilder(private val context: StubIrContext) {

    private val configuration = context.configuration
    private val nativeIndex: NativeIndex = context.nativeIndex

    private val classes = mutableListOf<ClassStub>()
    private val functions = mutableListOf<FunctionStub>()
    private val globals = mutableListOf<PropertyStub>()
    private val typealiases = mutableListOf<TypealiasStub>()
    private val containers = mutableListOf<SimpleStubContainer>()

    private fun addStubs(stubs: List<StubIrElement>) = stubs.forEach(this::addStub)

    private fun addStub(stub: StubIrElement) {
        when(stub) {
            is ClassStub -> classes += stub
            is FunctionStub -> functions += stub
            is PropertyStub -> globals += stub
            is TypealiasStub -> typealiases += stub
            is SimpleStubContainer -> containers += stub
            else -> error("Unexpected stub: $stub")
        }
    }

    private val excludedFunctions: Set<String>
        get() = configuration.excludedFunctions

    private val excludedMacros: Set<String>
        get() = configuration.excludedMacros

    private val buildingContext = context.plugin.stubsBuildingContext(context)

    fun build(): StubIrBuilderResult {
        nativeIndex.objCProtocols.filter { !it.isForwardDeclaration }.forEach { generateStubsForObjCProtocol(it) }
        nativeIndex.objCClasses.filter { !it.isForwardDeclaration && !it.isNSStringSubclass()} .forEach { generateStubsForObjCClass(it) }
        nativeIndex.objCCategories.filter { !it.clazz.isNSStringSubclass() }.forEach { generateStubsForObjCCategory(it) }
        nativeIndex.structs.forEach { generateStubsForStruct(it) }
        nativeIndex.enums.forEach { generateStubsForEnum(it) }
        nativeIndex.functions.filter { it.name !in excludedFunctions }.forEach { generateStubsForFunction(it) }
        nativeIndex.typedefs.forEach { generateStubsForTypedef(it) }
        nativeIndex.globals.filter { it.name !in excludedFunctions }.forEach { generateStubsForGlobal(it) }
        nativeIndex.macroConstants.filter { it.name !in excludedMacros }.forEach { generateStubsForMacroConstant(it) }
        nativeIndex.wrappedMacros.filter { it.name !in excludedMacros }.forEach { generateStubsForWrappedMacro(it) }

        val meta = StubContainerMeta()
        val stubs = SimpleStubContainer(
                meta,
                classes.toList(),
                functions.toList(),
                globals.toList(),
                typealiases.toList(),
                containers.toList()
        )
        return StubIrBuilderResult(
                stubs,
                buildingContext.declarationMapper,
                buildingContext.bridgeComponentsBuilder.build(),
                buildingContext.wrapperComponentsBuilder.build()
        )
    }

    private fun generateStubsForWrappedMacro(macro: WrappedMacroDef) {
        try {
            generateStubsForGlobal(GlobalDecl(macro.name, macro.type, isConst = true))
        } catch (e: Throwable) {
            context.log("Warning: cannot generate stubs for macro ${macro.name}")
        }
    }

    private fun generateStubsForMacroConstant(constant: ConstantDef) {
        try {
            addStubs(MacroConstantStubBuilder(buildingContext, constant).build())
        } catch (e: Throwable) {
            context.log("Warning: cannot generate stubs for constant ${constant.name}")
        }
    }

    private fun generateStubsForEnum(enumDef: EnumDef) {
        try {
            addStubs(EnumStubBuilder(buildingContext, enumDef).build())
        } catch (e: Throwable) {
            context.log("Warning: cannot generate definition for enum ${enumDef.spelling}")
        }
    }

    private fun generateStubsForFunction(func: FunctionDecl) {
        try {
            addStubs(FunctionStubBuilder(buildingContext, func, skipOverloads = true).build())
        } catch (e: Throwable) {
            context.log("Warning: cannot generate stubs for function ${func.name}")
        }
    }

    private fun generateStubsForStruct(decl: StructDecl) {
        try {
            addStubs(StructStubBuilder(buildingContext, decl).build())
        } catch (e: Throwable) {
            context.log("Warning: cannot generate definition for struct ${decl.spelling}")
        }
    }

    private fun generateStubsForTypedef(typedefDef: TypedefDef) {
        try {
            addStubs(TypedefStubBuilder(buildingContext, typedefDef).build())
        } catch (e: Throwable) {
            context.log("Warning: cannot generate typedef ${typedefDef.name}")
        }
    }

    private fun generateStubsForGlobal(global: GlobalDecl) {
        try {
            addStubs(GlobalStubBuilder(buildingContext, global).build())
        } catch (e: Throwable) {
            context.log("Warning: cannot generate stubs for global ${global.name}")
        }
    }

    private fun generateStubsForObjCProtocol(objCProtocol: ObjCProtocol) {
        addStubs(ObjCProtocolStubBuilder(buildingContext, objCProtocol).build())
    }

    private fun generateStubsForObjCClass(objCClass: ObjCClass) {
        addStubs(ObjCClassStubBuilder(buildingContext, objCClass).build())
    }

    private fun generateStubsForObjCCategory(objCCategory: ObjCCategory) {
        addStubs(ObjCCategoryStubBuilder(buildingContext, objCCategory).build())
    }
}
