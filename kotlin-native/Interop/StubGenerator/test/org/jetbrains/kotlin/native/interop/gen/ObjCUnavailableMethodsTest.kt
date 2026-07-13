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
import org.jetbrains.kotlin.native.interop.indexer.ObjCInstanceType
import org.jetbrains.kotlin.native.interop.indexer.ObjCMethod
import org.jetbrains.kotlin.native.interop.indexer.ObjCPointer
import org.jetbrains.kotlin.native.interop.indexer.ObjCProperty
import org.jetbrains.kotlin.native.interop.indexer.ObjCProtocol
import org.jetbrains.kotlin.native.interop.indexer.Parameter
import org.jetbrains.kotlin.native.interop.indexer.StructDecl
import org.jetbrains.kotlin.native.interop.indexer.Type
import org.jetbrains.kotlin.native.interop.indexer.TypeDeclaration
import org.jetbrains.kotlin.native.interop.indexer.ObjCUnavailableMethod
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
    fun `unavailable method parsing`() {
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
    fun `unavailable superclass method is inherited by subclasses`() {
        val base = TestObjCClass("Base", methods = listOf(objCMethod("foo")))
        val middle = TestObjCClass(
                "Middle",
                baseClass = base,
                unavailableMethods = listOf(unavailableObjCMethod("foo"))
        )
        val derived = TestObjCClass("Derived", baseClass = middle)

        // We'd prefer not to expose unavailable methods, but due to the ABI constraints we have to.
        // The next best thing is to import them as @Deprecated.
        assertEquals(listOf("foo"), derived.inheritedMethods(isClass = false).map { it.selector }.toList())
    }

    @Test
    fun `unavailability of init in intermediate superclass propagates to subclass`() {
        // NSObject defines init, Middle subclasses NSObject and marks init as unavailable, Derived subclasses Middle.
        // Derived re-imports init (because it is an initializer) and should inherit the unavailability from Middle.
        val nsObject = TestObjCClass("NSObject", methods = listOf(objCMethod("init", isInit = true)))
        val middle = TestObjCClass(
                "Middle",
                baseClass = nsObject,
                unavailableMethods = listOf(unavailableObjCMethod("init"))
        )
        val derived = TestObjCClass("Derived", baseClass = middle, methods = listOf(objCMethod("initWithX", isInit = true)))

        val derivedStub = classStub(derived)
        assertEquals(setOf("init", "initWithX"), derivedStub.objCConstructorSelectors().toSet())
        val inheritedInit = derivedStub.constructors.single { (it.origin as StubOrigin.ObjCMethod).method.selector == "init" }
        val ownInit = derivedStub.constructors.single { (it.origin as StubOrigin.ObjCMethod).method.selector == "initWithX" }
        assertDeprecatedUnavailable(inheritedInit)
        assertEquals(emptyList(), ownInit.deprecations())
        val initMethod = derivedStub.methods.single { it.name == "init" }
        assertEquals(
                listOf("Use constructor instead" to DeprecationLevel.ERROR),
                initMethod.deprecations()
        )
        assertHasObjCUnavailable(initMethod)
    }

    @Test
    fun `subclass explicitly re-declaring init overrides inherited unavailability`() {
        // NSObject defines init, Middle marks it unavailable, Derived re-declares it explicitly as available.
        // Derived's explicit declaration must win over Middle's inherited unavailability.
        val nsObject = TestObjCClass("NSObject", methods = listOf(objCMethod("init", isInit = true)))
        val middle = TestObjCClass(
                "Middle",
                baseClass = nsObject,
                unavailableMethods = listOf(unavailableObjCMethod("init"))
        )
        val derived = TestObjCClass(
                "Derived",
                baseClass = middle,
                methods = listOf(objCMethod("init", isInit = true))
        )

        val derivedStub = classStub(derived)
        assertEquals(listOf("init"), derivedStub.objCConstructorSelectors())
        assertHasNoObjCUnavailable(derivedStub.constructors.single())
        assertHasNoObjCUnavailable(derivedStub.methods.single { it.name == "init" })
    }

    @Test
    fun `unavailable method in protocol propagates to class`() {
        // BaseProto requires foo, DerivedProto extends BaseProto and marks foo unavailable, Owner adopts DerivedProto.
        // Owner re-imports foo (because DerivedProto requires it through inheritance) and should mark it unavailable.
        val baseProtocol = TestObjCProtocol("BaseProto", methods = listOf(objCMethod("foo")))
        val derivedProtocol = TestObjCProtocol(
                "DerivedProto",
                protocols = listOf(baseProtocol),
                unavailableMethods = listOf(unavailableObjCMethod("foo"))
        )
        val owner = TestObjCClass("Owner", protocols = listOf(derivedProtocol))

        val foo = classStub(owner).methods.single { it.name == "foo" }
        assertDeprecatedUnavailable(foo)
    }

    @Test
    fun `subclass re-adopting protocol reintroduces method that superclass marked unavailable`() {
        // Protocol P requires foo. Base adopts P and marks foo unavailable. Derived extends Base and also adopts P.
        // Per Clang's method-declaration lookup order (class own decls -> adopted protocols left-to-right -> baseClass,
        // first match wins), Derived's direct adoption of P reintroduces foo before the search reaches Base — so foo
        // must be considered available in Derived, not deprecated/unavailable.
        val protocol = TestObjCProtocol("P", methods = listOf(objCMethod("foo")))
        val base = TestObjCClass(
                "Base",
                protocols = listOf(protocol),
                unavailableMethods = listOf(unavailableObjCMethod("foo"))
        )
        val derived = TestObjCClass(
                "Derived",
                baseClass = base,
                protocols = listOf(protocol)
        )

        val foo = classStub(derived).methods.single { it.name == "foo" }
        assertHasNoObjCUnavailable(foo)
    }

    @Test
    fun `unavailable method declared in class deprecates required protocol method with the same selector`() {
        val protocol = TestObjCProtocol("SomeDelegate", methods = listOf(objCMethod("layer")))
        val availableOwner = TestObjCClass("AvailableLayerOwner", protocols = listOf(protocol))
        val unavailableOwner = TestObjCClass(
                "UnavailableLayerOwner",
                protocols = listOf(protocol),
                unavailableMethods = listOf(unavailableObjCMethod("layer"))
        )

        assertEquals(listOf("layer"), classStub(availableOwner).methods.map { it.name })
        val unavailableMethod = classStub(unavailableOwner).methods.single()
        assertEquals("layer", unavailableMethod.name)
        assertDeprecatedUnavailable(unavailableMethod)
    }

    @Test
    fun `unavailable default init is imported as deprecated inherited constructor`() {
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
        assertEquals(listOf("init"), unavailableInitSubclassStub.objCConstructorSelectors())
        assertDeprecatedUnavailable(unavailableInitSubclassStub.constructors.single())
        assertEquals(
                listOf("Use constructor instead" to DeprecationLevel.ERROR),
                unavailableInitSubclassStub.methods.single().deprecations()
        )
        assertHasObjCUnavailable(unavailableInitSubclassStub.methods.single())
    }

    @Test
    fun `unavailable alloc keeps existing alloc deprecation`() {
        val instancetype = ObjCInstanceType(ObjCPointer.Nullability.Unspecified)
        val nsObject = TestObjCClass(
                "NSObject",
                methods = listOf(objCMethod("alloc", isClass = true, returnType = instancetype))
        )
        val unavailableAllocSubclass = TestObjCClass(
                "UnavailableAllocSubclass",
                baseClass = nsObject,
                unavailableMethods = listOf(unavailableObjCMethod("alloc", isClass = true))
        )

        val alloc = metaClassStub(unavailableAllocSubclass).methods.single()
        // +alloc was deprecated prior to deprecation by unavailability, so we're keeping it even if it's also
        // unavailable.
        // KT-86333: Revise this once we upgrade the deprecation by unavailability to ERROR.
        assertEquals(listOf("Use constructor or factory method instead" to DeprecationLevel.WARNING), alloc.deprecations())
        assertHasObjCUnavailable(alloc)
    }

    @Test
    fun `objc meta class has synthetic default constructor`() {
        val clazz = TestObjCClass("MyClass")

        assertEquals(
                listOf(StubOrigin.Synthetic.DefaultConstructor),
                metaClassStub(clazz).constructors.map { it.origin }
        )
    }

    private fun indexObjCClass(headerContents: String, className: String = "Foo"): ObjCClass =
            index(headerContents, language = Language.OBJECTIVE_C).index.objCClasses.single { it.name == className }

    private fun classStub(clazz: ObjCClass): ClassStub.Simple =
            ObjCClassStubBuilder(TestStubsBuildingContext(), clazz).build()
                    .filterIsInstance<ClassStub.Simple>()
                    .single { it.classifier.topLevelName == clazz.name }

    private fun metaClassStub(clazz: ObjCClass): ClassStub.Simple =
            ObjCClassStubBuilder(TestStubsBuildingContext(), clazz).build()
                    .filterIsInstance<ClassStub.Simple>()
                    .single { it.classifier.topLevelName == "${clazz.name}Meta" }

    private fun ClassStub.Simple.objCConstructorSelectors(): List<String> =
            constructors.mapNotNull { (it.origin as? StubOrigin.ObjCMethod)?.method?.selector }

    private fun assertDeprecatedUnavailable(stub: AnnotationHolder) {
        val deprecation = stub.annotations.filterIsInstance<AnnotationStub.Deprecated>().single()
        assertEquals("This Objective-C declaration is unavailable", deprecation.message)
        // KT-86333: This should be updated once we upgrade the level to ERROR
        assertEquals(DeprecationLevel.WARNING, deprecation.level)
        assertHasObjCUnavailable(stub)
    }

    private fun AnnotationHolder.deprecations(): List<Pair<String, DeprecationLevel>> =
            annotations.filterIsInstance<AnnotationStub.Deprecated>().map { it.message to it.level }

    private fun assertHasObjCUnavailable(stub: AnnotationHolder) {
        assertEquals(listOf(AnnotationStub.ObjC.Unavailable), stub.annotations.filterIsInstance<AnnotationStub.ObjC.Unavailable>())
    }

    private fun assertHasNoObjCUnavailable(stub: AnnotationHolder) {
        assertEquals(emptyList(), stub.annotations.filterIsInstance<AnnotationStub.ObjC.Unavailable>())
    }

    private fun objCMethod(
            selector: String,
            isClass: Boolean = false,
            isInit: Boolean = false,
            returnType: Type = VoidType,
    ): ObjCMethod =
            ObjCMethod(
                    selector = selector,
                    encoding = "",
                    parameters = emptyList<Parameter>(),
                    returnType = returnType,
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

    private fun unavailableObjCMethod(selector: String, isClass: Boolean = false): ObjCUnavailableMethod =
            ObjCUnavailableMethod(selector, isClass)

    private class TestObjCClass(
            name: String,
            override val baseClass: ObjCClass? = null,
            override val protocols: List<ObjCProtocol> = emptyList(),
            override val methods: List<ObjCMethod> = emptyList(),
            override val unavailableMethods: List<ObjCUnavailableMethod> = emptyList(),
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
            override val unavailableMethods: List<ObjCUnavailableMethod> = emptyList(),
            override val properties: List<ObjCProperty> = emptyList(),
            override val location: Location = testLocation,
            override val isForwardDeclaration: Boolean = false,
            override val binaryName: String? = name,
    ) : ObjCProtocol(name)

    /**
     * Minimal [StubsBuildingContext] for tests in this file. Provides just enough machinery to drive the
     * ObjC-method stub-generation path (configuration, [declarationMapper] with [getPackageFor], and a
     * [mirror] that delegates to the standard implementation for ObjC pointer types). Members the tested
     * path does not touch throw, both to keep the test setup small and to make any future code change
     * that starts depending on them fail loudly here.
     */
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
                target = KonanTarget.MACOS_ARM64,
                cCallMode = CCallMode.INDIRECT,
        )
        override val platform: KotlinPlatform = KotlinPlatform.NATIVE
        override val generationMode: GenerationMode = GenerationMode.METADATA
        override val generatedObjCCategoriesMembers: MutableMap<ObjCClass, GeneratedObjCCategoriesMembers> = mutableMapOf()
        override val bridgeComponentsBuilder: BridgeGenerationComponentsBuilder = BridgeGenerationComponentsBuilder()
        override val wrapperComponentsBuilder: WrapperGenerationComponentsBuilder = WrapperGenerationComponentsBuilder()

        override val declarationMapper: DeclarationMapper = object : DeclarationMapper {
            override fun getPackageFor(declaration: TypeDeclaration): String = configuration.pkgName

            override val useUnsignedTypes: Boolean get() = unsupported()
            override fun getKotlinClassForPointed(structDecl: StructDecl): Classifier = unsupported()
            override fun isMappedToStrict(enumDef: EnumDef): Boolean = unsupported()
            override fun getKotlinNameForValue(enumDef: EnumDef): String = unsupported()
        }

        override fun getKotlinClassFor(objCClassOrProtocol: ObjCClassOrProtocol, isMeta: Boolean): Classifier =
                declarationMapper.getKotlinClassFor(objCClassOrProtocol, isMeta)

        override fun mirror(type: Type): TypeMirror =
                org.jetbrains.kotlin.native.interop.gen.mirror(declarationMapper, type)

        override val macroConstantsByName: Map<String, MacroDef> get() = unsupported()
        override fun generateBridgeSymbol(category: String, stubName: String): String = unsupported()
        override fun isStrictEnum(enumDef: EnumDef): Boolean = unsupported()
        override fun tryCreateIntegralStub(type: Type, value: Long): IntegralConstantStub? = unsupported()
        override fun tryCreateDoubleStub(type: Type, value: Double): DoubleConstantStub? = unsupported()
        override fun getKotlinClassForPointed(structDecl: StructDecl): Classifier = unsupported()
        override fun tryRegisterFunction(name: String, types: List<StubType>): Boolean = unsupported()

        private fun unsupported(): Nothing = error("Not used by ObjCUnavailableMethodsTest")
    }

    private companion object {
        val testLocation: Location = Location(HeaderId("test.h"))
    }
}
