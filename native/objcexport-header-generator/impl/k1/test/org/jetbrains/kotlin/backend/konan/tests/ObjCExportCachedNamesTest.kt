/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.tests

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.backend.common.serialization.findSourceFile
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.backend.konan.testUtils.*
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.cli.common.disposeRootInWriteAction
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ObjCExportCachedNamesTest : InlineSourceTestEnvironment {
    override val testDisposable = Disposer.newDisposable("${ObjCExportCachedNamesTest::class.simpleName}.testDisposable")
    override val kotlinCoreEnvironment: KotlinCoreEnvironment = createKotlinCoreEnvironment(testDisposable)

    @TempDir
    override lateinit var testTempDir: File

    @AfterEach
    fun dispose() {
        disposeRootInWriteAction(testDisposable)
    }

    private class ExposedMembers(val mapper: ObjCExportMapper, val methods: List<FunctionDescriptor>, val properties: List<PropertyDescriptor>) {
        val classClaims = (methods + properties).mapNotNull { mapper.claimOwner(it) }.distinct().map { classClaim(it) }.associateBy { it.classId }
    }

    private fun exposedMembers(): ExposedMembers {
        val module = createModuleDescriptor(
            """
            interface I {
                fun f(x: Int)
                fun g(x: String): Int
                fun <T> h(x: List<T>)
                val p: Int
            }
            interface J {
                fun f(x: String)
                fun <T> h(x: List<T>, y: Int)
                fun <R> h(x: Set<R>)
                val p: String
            }
            open class A {
                constructor()
                constructor(x: Int)
                open fun f(x: Int) {}
                fun g(x: Int): Int = 0
                fun h(): String = ""
                fun <T> m(x: T) {}
                open val p: Int get() = 0
                var q: Int = 0
            }
            open class B : A {
                constructor() : super()
                constructor(x: String) : super()
                fun g(x: String): Int = 0
                fun f(x: Int, y: Int) {}
                fun h(x: Int) {}
                fun <R> m(x: R, y: Int) {}
                fun m(x: List<String>?) {}
                val r: String get() = ""
                var q2: String = ""
            }
            class C : B(), I {
                override fun f(x: Int) {}
                override fun g(x: String): Int = 0
                override fun <T> h(x: List<T>) {}
                override val p: Int get() = 0
                fun k(x: Int): Int = x
            }
            class D {
                fun f(x: Int) {}
                fun g(x: Int): Int = 0
                val p: Int get() = 0
            }
            open class E : J {
                override fun f(x: String) {}
                override fun <T> h(x: List<T>, y: Int) {}
                override fun <R> h(x: Set<R>) {}
                override val p: String get() = ""
                fun k(block: () -> Unit) {}
                fun k(block: (Int) -> Int) {}
                fun k(x: Int): String = ""
            }
            abstract class F {
                abstract fun f(x: Int)
                abstract var s: String
            }
            class G : F() {
                override fun f(x: Int) {}
                override var s: String = ""
                suspend fun f(x: String) {}
                @Throws(Exception::class) fun t(x: Int) {}
                fun t(x: Long) {}
            }
            enum class H { ONE; fun f(x: Int) {} }
            object O { fun f(x: Int) {} }
            fun A.ext(x: Int) {}
            fun B.ext(x: String) {}
            fun D.ext(x: Int) {}
            fun E.ext(x: Int) {}
            fun C.k(x: Int): Int = x
            val A.extProp: Int get() = 0
            var B.extVar: Int
                get() = 0
                set(value) {}
            val D.p: String get() = ""
            val E.extProp: Long get() = 0L
            suspend fun A.suspendExt(x: Int) {}
            fun top(x: Int) {}
            fun top(x: String) {}
            fun <T> top(x: List<T>) {}
            val topProp: Int get() = 0
            """.trimIndent()
        )
        val mapper = createObjCExportMapper()
        val classes = mutableListOf<ClassDescriptor>()
        val packageMembers = mutableListOf<CallableMemberDescriptor>()
        module.getPackage(FqName.ROOT).memberScope.getContributedDescriptors().forEach {
            when (it) {
                is ClassDescriptor -> classes += it
                is CallableMemberDescriptor -> packageMembers += it
            }
        }
        val members = classes.flatMap { it.unsubstitutedMemberScope.getContributedDescriptors() + it.constructors }
            .filterIsInstance<CallableMemberDescriptor>()
            .filter { it.kind.isReal } + packageMembers
        val methods = members.filterIsInstance<FunctionDescriptor>().filter { mapper.shouldBeExposed(it) && mapper.isBaseMethod(it) }
        val properties = members.filterIsInstance<PropertyDescriptor>()
            .filter { mapper.shouldBeExposed(it) && mapper.isBaseProperty(it) && isObjCProperty(it) }
        return ExposedMembers(mapper, methods, properties)
    }

    @Test
    fun `claims conflict exactly when the declarations cannot share a name`() {
        val members = exposedMembers()
        val mapper = members.mapper
        val methods = members.methods
        val properties = members.properties
        val classClaims = members.classClaims
        val methodClaims = methods.associateWith { mapper.memberClaim(it) }
        val propertyClaims = properties.associateWith { mapper.propertyClaim(it) }

        var conflicts = 0
        for (ignoreInterfaceMethodCollisions in listOf(false, true)) {
            for (first in methods) for (second in methods) {
                if (first == second) continue
                val expected = !mapper.canHaveSameSelector(first, second, ignoreInterfaceMethodCollisions)
                val actual = membersConflict(
                    methodClaims.getValue(first), methodClaims.getValue(second), isProperty = false, classClaims::get, ignoreInterfaceMethodCollisions,
                )
                assertEquals(expected, actual, "$first and $second, ignoreInterfaceMethodCollisions=$ignoreInterfaceMethodCollisions")
                if (expected) conflicts++
            }
            for (first in properties) for (second in properties) {
                if (first == second) continue
                val expected = !mapper.canHaveSameName(first, second, ignoreInterfaceMethodCollisions)
                val actual = membersConflict(
                    propertyClaims.getValue(first), propertyClaims.getValue(second), isProperty = true, classClaims::get, ignoreInterfaceMethodCollisions,
                )
                assertEquals(expected, actual, "$first and $second, ignoreInterfaceMethodCollisions=$ignoreInterfaceMethodCollisions")
                if (expected) conflicts++
            }
        }
        assertTrue(methods.size > 30 && properties.size > 8 && conflicts > 200, "${methods.size} methods, ${properties.size} properties, $conflicts conflicts")
    }

    @Test
    fun `the name index offers every holder the namer could treat as a conflict`() {
        val members = exposedMembers()
        val mapper = members.mapper
        var skipped = 0
        for (ignoreInterfaceMethodCollisions in listOf(false, true)) {
            fun <D : CallableMemberDescriptor> check(declarations: List<D>, conflict: (D, D) -> Boolean) {
                val index = ObjCExportNameIndex<D>(members.classClaims::get, ignoreInterfaceMethodCollisions)
                declarations.forEach { index.add(it, mapper.namePlace(it)) }
                for (first in declarations) {
                    val candidateList = index.candidates(mapper.namePlace(first))
                    val candidates = candidateList.toSet()
                    assertEquals(declarations.filter { it in candidates }, candidateList)
                    skipped += declarations.size - candidates.size
                    for (second in declarations) {
                        if (first != second && conflict(first, second)) {
                            assertTrue(second in candidates, "$first and $second, ignoreInterfaceMethodCollisions=$ignoreInterfaceMethodCollisions")
                        }
                    }
                }
            }
            check(members.methods) { first, second -> !mapper.canHaveSameSelector(first, second, ignoreInterfaceMethodCollisions) }
            check(members.properties) { first, second -> !mapper.canHaveSameName(first, second, ignoreInterfaceMethodCollisions) }
        }
        assertTrue(skipped > 0, "The index offered every holder")
    }

    @Test
    fun `top-level members of different files are not candidates of each other`() {
        val module = createModuleDescriptor {
            source("fun top(x: Int) {}\nfun top(x: String) {}")
            source("fun top(x: Long) {}")
        }
        val mapper = createObjCExportMapper()
        val functions = module.getPackage(FqName.ROOT).memberScope.getContributedDescriptors().filterIsInstance<FunctionDescriptor>()
        val index = ObjCExportNameIndex<FunctionDescriptor>({ null }, ignoreInterfaceMethodCollisions = false)
        functions.forEach { index.add(it, mapper.namePlace(it)) }
        for (function in functions) {
            val sameFile = functions.filter { it.findSourceFile() == function.findSourceFile() }
            assertEquals(sameFile, index.candidates(mapper.namePlace(function)))
        }
    }

    @Test
    fun `cached names are adopted by live declarations and checked for collisions`() {
        val module = createModuleDescriptor(
            """
            open class Alpha {
                open fun execute(item: String): String = ""
                open fun execute(item: Int): String = ""
            }
            """.trimIndent()
        )
        val mapper = createObjCExportMapper()
        val alpha = module.findClassAcrossModuleDependencies(ClassId.fromString("Alpha"))!!
        val [byInt, byString] = alpha.unsubstitutedMemberScope.getContributedFunctions(Name.identifier("execute"), NoLookupLocation.FROM_TEST)
            .sortedBy { it.valueParameters.single().type.toString() }
        val classClaims = listOf(classClaim(alpha))
        fun record(method: FunctionDescriptor, name: String) =
            ObjCExportNameRecord(ObjCExportNameKind.SELECTOR, objCExportNameKey(method)!!, name, mapper.memberClaim(method))
        fun namer(vararg records: ObjCExportNameRecord) =
            (createObjCExportNamer(mapper = mapper) as ObjCExportNamerImpl).apply { useCachedNames(classClaims, records.toList()) }

        val adopting = namer(record(byInt, "executeItem:"))
        assertEquals("executeItem:", adopting.getSelector(byInt))
        assertFalse(adopting.hasCollisions)

        val live = namer(record(byInt, "executeItem:"))
        assertEquals("executeItem_:", live.getSelector(byString))
        assertTrue(live.hasCollisions)

        assertTrue(namer(record(byInt, "executeItem:"), record(byString, "executeItem:")).hasCollisions)
        assertTrue(namer(record(byInt, "executeItem:"), record(byInt, "executeItem_:")).hasCollisions)
        assertFalse(namer(record(byInt, "executeItem:"), record(byString, "executeItem_:")).hasCollisions)
    }

    @Test
    fun `the namer reports mangled names and type parameters renamed for a class name`() {
        val module = createModuleDescriptor(
            """
            class Alpha {
                fun execute(item: Int) {}
                fun execute(item: Long) {}
            }
            class Box<T>
            class T
            """.trimIndent()
        )
        val alpha = module.findClassAcrossModuleDependencies(ClassId.fromString("Alpha"))!!
        val box = module.findClassAcrossModuleDependencies(ClassId.fromString("Box"))!!
        val t = module.findClassAcrossModuleDependencies(ClassId.fromString("T"))!!
        val [byInt, byLong] = alpha.unsubstitutedMemberScope.getContributedFunctions(Name.identifier("execute"), NoLookupLocation.FROM_TEST)
            .sortedBy { it.valueParameters.single().type.toString() }

        val namer = createObjCExportNamer() as ObjCExportNamerImpl
        assertEquals("executeItem:", namer.getSelector(byInt))
        assertFalse(namer.hasCollisions)
        assertEquals("executeItem_:", namer.getSelector(byLong))
        assertTrue(namer.hasCollisions)

        assertEquals("T", namer.getClassOrProtocolName(t).objCName)
        assertEquals("T_", namer.getTypeParameterName(box.declaredTypeParameters.single()))
        assertEquals(listOf("T of /Box"), namer.typeParameterClashes())

        val cached = createObjCExportNamer() as ObjCExportNamerImpl
        val typeParameter = box.declaredTypeParameters.single()
        val record = ObjCExportNameRecord(ObjCExportNameKind.TYPE_PARAMETER, objCExportNameKey(typeParameter)!!, "T", null)
        cached.useCachedNames(emptyList(), listOf(record))
        assertEquals("T", cached.getTypeParameterName(typeParameter))
        assertEquals(emptyList(), cached.typeParameterClashes())
        cached.getClassOrProtocolName(t)
        assertEquals(listOf("T of /Box"), cached.typeParameterClashes())
        assertFalse(cached.hasCollisions)
    }

    @Test
    fun `a recording namer records each assigned name once until the units are taken`() {
        val module = createModuleDescriptor(
            """
            class Alpha<T> {
                fun execute(item: Int) {}
                val size: Int get() = 0
            }
            class Beta
            """.trimIndent()
        )
        val alpha = module.findClassAcrossModuleDependencies(ClassId.fromString("Alpha"))!!
        val beta = module.findClassAcrossModuleDependencies(ClassId.fromString("Beta"))!!
        val scope = alpha.unsubstitutedMemberScope
        val execute = scope.getContributedFunctions(Name.identifier("execute"), NoLookupLocation.FROM_TEST).single()
        val size = scope.getContributedVariables(Name.identifier("size"), NoLookupLocation.FROM_TEST).single()
        val namer = ObjCExportNamerImpl(
            createObjCExportNamerConfiguration(), DefaultBuiltIns.Instance, createObjCExportMapper(), ObjCExportProblemCollector.SILENT,
            local = false, recordNames = true,
        )
        namer.getClassOrProtocolName(alpha)
        namer.getSelector(execute)
        namer.getSelector(execute)
        namer.getPropertyName(size)
        namer.getTypeParameterName(alpha.declaredTypeParameters.single())
        fun emptyUnits() = ObjCExportUnits(emptyList(), emptyList(), emptyList(), emptySet(), emptySet())
        val units = namer.withRecordedNames(emptyUnits())
        namer.getClassOrProtocolName(beta)

        assertEquals(
            listOf("CLASS Alpha", "PROPERTY size", "SELECTOR executeItem:", "SWIFT_CLASS Alpha", "TYPE_PARAMETER T"),
            units.names.map { "${it.kind} ${it.name}" }.sorted(),
        )
        assertEquals(listOf("/Alpha"), units.classClaims.map { it.classId })
        assertFailsWith<IllegalStateException> { namer.withRecordedNames(emptyUnits()) }
    }
}
