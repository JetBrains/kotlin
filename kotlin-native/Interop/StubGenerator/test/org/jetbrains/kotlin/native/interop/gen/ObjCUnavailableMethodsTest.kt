/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.native.interop.gen.jvm.CCallMode
import org.jetbrains.kotlin.native.interop.gen.jvm.GenerationMode
import org.jetbrains.kotlin.native.interop.gen.jvm.InteropConfiguration
import org.jetbrains.kotlin.native.interop.gen.jvm.KotlinPlatform
import org.jetbrains.kotlin.native.interop.indexer.CompilationImpl
import org.jetbrains.kotlin.native.interop.indexer.EnumDef
import org.jetbrains.kotlin.native.interop.indexer.HeaderId
import org.jetbrains.kotlin.native.interop.indexer.Language
import org.jetbrains.kotlin.native.interop.indexer.Location
import org.jetbrains.kotlin.native.interop.indexer.MacroDef
import org.jetbrains.kotlin.native.interop.indexer.ObjCCategory
import org.jetbrains.kotlin.native.interop.indexer.ObjCClass
import org.jetbrains.kotlin.native.interop.indexer.ObjCClassOrProtocol
import org.jetbrains.kotlin.native.interop.indexer.ObjCMethod
import org.jetbrains.kotlin.native.interop.indexer.ObjCProperty
import org.jetbrains.kotlin.native.interop.indexer.ObjCProtocol
import org.jetbrains.kotlin.native.interop.indexer.Parameter
import org.jetbrains.kotlin.native.interop.indexer.StructDecl
import org.jetbrains.kotlin.native.interop.indexer.Type
import org.jetbrains.kotlin.native.interop.indexer.TypeDeclaration
import org.jetbrains.kotlin.native.interop.indexer.UnavailableObjCMethod
import org.jetbrains.kotlin.native.interop.indexer.VoidType
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ObjCUnavailableMethodsTest : IndexerTestsBase() {

    @BeforeEach
    fun onlyOnMac() {
        Assumptions.assumeTrue(HostManager.hostIsMac)
    }

    @Test
    fun `unavailable method is not imported`() {
        val clazz = indexObjCClass("""
            @interface Foo
            - (void)availableMethod;
            - (void)unavailableMethod __attribute__((unavailable));
            @end
        """.trimIndent())

        assertEquals(listOf("availableMethod"), clazz.methods.map { it.selector })
        assertEquals(listOf("unavailableMethod"), clazz.unavailableMethods.map { it.selector })
    }

    @Test
    fun `unavailable superclass method is not inherited by subclasses`() {
        val base = TestObjCClass("Base", methods = listOf(objCMethod("displayLayer")))
        val middle = TestObjCClass(
                "Middle",
                baseClass = base,
                unavailableMethods = listOf(unavailableObjCMethod("displayLayer"))
        )
        val derived = TestObjCClass("Derived", baseClass = middle)

        assertEquals(emptyList(), derived.inheritedMethods(isClass = false).map { it.selector }.toList())
    }

    @Test
    fun `unavailable method declared in class blocks required protocol method with the same selector`() {
        val protocol = TestObjCProtocol("LayerDelegate", methods = listOf(objCMethod("displayLayer")))
        val availableOwner = TestObjCClass("AvailableLayerOwner", protocols = listOf(protocol))
        val unavailableOwner = TestObjCClass(
                "UnavailableLayerOwner",
                protocols = listOf(protocol),
                unavailableMethods = listOf(unavailableObjCMethod("displayLayer"))
        )

        assertEquals(listOf("displayLayer"), classStub(availableOwner).methods.map { it.name })
        assertEquals(emptyList(), classStub(unavailableOwner).methods.map { it.name })
    }

    @Test
    fun `unavailable default init is not imported as inherited constructor`() {
        val nsObject = TestObjCClass("NSObject", methods = listOf(objCMethod("init", isInit = true)))
        val regularSubclass = TestObjCClass("RegularSubclass", baseClass = nsObject)
        val unavailableInitSubclass = TestObjCClass(
                "UnavailableInitSubclass",
                baseClass = nsObject,
                unavailableMethods = listOf(
                        unavailableObjCMethod("init"),
                        unavailableObjCMethod("new", isClass = true)
                )
        )

        assertEquals(listOf("init"), classStub(regularSubclass).objCConstructorSelectors())

        val unavailableInitSubclassStub = classStub(unavailableInitSubclass)
        assertEquals(emptyList(), unavailableInitSubclassStub.objCConstructorSelectors())
        assertEquals(
                listOf(StubOrigin.Synthetic.DefaultConstructor),
                unavailableInitSubclassStub.constructors.map { it.origin }
        )
    }

    private fun indexObjCClass(headerContents: String, className: String = "Foo"): ObjCClass =
            index(headerContents, language = Language.OBJECTIVE_C).index.objCClasses.single { it.name == className }

    private fun classStub(clazz: ObjCClass): ClassStub.Simple =
            ObjCClassStubBuilder(TestStubsBuildingContext(), clazz).build()
                    .filterIsInstance<ClassStub.Simple>()
                    .single { it.classifier.topLevelName == clazz.name }

    private fun ClassStub.Simple.objCConstructorSelectors(): List<String> =
            constructors.mapNotNull { (it.origin as? StubOrigin.ObjCMethod)?.method?.selector }

    private fun objCMethod(selector: String, isClass: Boolean = false, isInit: Boolean = false): ObjCMethod =
            ObjCMethod(
                    selector = selector,
                    encoding = "",
                    parameters = emptyList<Parameter>(),
                    returnType = VoidType,
                    isVariadic = false,
                    isClass = isClass,
                    nsConsumesSelf = false,
                    nsReturnsRetained = false,
                    isOptional = false,
                    isInit = isInit,
                    isExplicitlyDesignatedInitializer = false,
                    isDirect = false,
                    swiftName = null,
            )

    private fun unavailableObjCMethod(selector: String, isClass: Boolean = false): UnavailableObjCMethod =
            UnavailableObjCMethod(selector, isClass)

    private class TestObjCClass(
            name: String,
            override val baseClass: ObjCClass? = null,
            override val protocols: List<ObjCProtocol> = emptyList(),
            override val methods: List<ObjCMethod> = emptyList(),
            override val unavailableMethods: List<UnavailableObjCMethod> = emptyList(),
            override val properties: List<ObjCProperty> = emptyList(),
            override val includedCategories: List<ObjCCategory> = emptyList(),
            override val location: Location = testLocation,
            override val isForwardDeclaration: Boolean = false,
            override val binaryName: String? = name,
    ) : ObjCClass(name)

    private class TestObjCProtocol(
            name: String,
            override val protocols: List<ObjCProtocol> = emptyList(),
            override val methods: List<ObjCMethod> = emptyList(),
            override val unavailableMethods: List<UnavailableObjCMethod> = emptyList(),
            override val properties: List<ObjCProperty> = emptyList(),
            override val location: Location = testLocation,
            override val isForwardDeclaration: Boolean = false,
            override val binaryName: String? = name,
    ) : ObjCProtocol(name)

    private class TestStubsBuildingContext : StubsBuildingContext {
        override val configuration: InteropConfiguration = InteropConfiguration(
                library = CompilationImpl(
                        includes = emptyList(),
                        additionalPreambleLines = emptyList(),
                        compilerArgs = emptyList(),
                        language = Language.OBJECTIVE_C
                ),
                pkgName = "test",
                excludedFunctions = emptySet(),
                excludedMacros = emptySet(),
                strictEnums = emptySet(),
                nonStrictEnums = emptySet(),
                noStringConversion = emptySet(),
                exportForwardDeclarations = emptyList(),
                allowedOverloadsForCFunctions = emptySet(),
                disableDesignatedInitializerChecks = false,
                disableExperimentalAnnotation = true,
                target = KonanTarget.MACOS_X64,
                cCallMode = CCallMode.INDIRECT,
        )
        override val declarationMapper: DeclarationMapper = object : DeclarationMapper {
            override val useUnsignedTypes: Boolean = true

            override fun getKotlinClassForPointed(structDecl: StructDecl): Classifier =
                    Classifier.topLevel(cnamesStructsPackageName, structDecl.spelling)

            override fun isMappedToStrict(enumDef: EnumDef): Boolean = false

            override fun getKotlinNameForValue(enumDef: EnumDef): String = enumDef.spelling

            override fun getPackageFor(declaration: TypeDeclaration): String = "test"
        }
        override val generatedObjCCategoriesMembers: MutableMap<ObjCClass, GeneratedObjCCategoriesMembers> = mutableMapOf()
        override val platform: KotlinPlatform = KotlinPlatform.NATIVE
        override val generationMode: GenerationMode = GenerationMode.METADATA
        override val macroConstantsByName: Map<String, MacroDef> = emptyMap()
        override val bridgeComponentsBuilder: BridgeGenerationComponentsBuilder = BridgeGenerationComponentsBuilder()
        override val wrapperComponentsBuilder: WrapperGenerationComponentsBuilder = WrapperGenerationComponentsBuilder()

        override fun mirror(type: Type): TypeMirror =
                error("Unexpected type mirror request for $type")

        override fun generateNextUniqueId(prefix: String): String = "${prefix}0"

        override fun isStrictEnum(enumDef: EnumDef): Boolean = false

        override fun tryCreateIntegralStub(type: Type, value: Long): IntegralConstantStub? = null

        override fun tryCreateDoubleStub(type: Type, value: Double): DoubleConstantStub? = null

        override fun getKotlinClassFor(objCClassOrProtocol: ObjCClassOrProtocol, isMeta: Boolean): Classifier =
                declarationMapper.getKotlinClassFor(objCClassOrProtocol, isMeta)

        override fun getKotlinClassForPointed(structDecl: StructDecl): Classifier =
                declarationMapper.getKotlinClassForPointed(structDecl)

        override fun isOverloading(name: String, types: List<StubType>): Boolean = false
    }

    private companion object {
        val testLocation: Location = Location(HeaderId("test.h"))
    }
}
