package org.jetbrains.kotlin.backend.konan.tests

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.backend.konan.objcexport.getClassIfCategory
import org.jetbrains.kotlin.backend.konan.testUtils.*
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.test.util.JUnit4Assertions.assertEquals
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertNull

class GetClassIfCategoryTest : InlineSourceTestEnvironment {

    @Test
    fun `test - null when there is receiver`() {
        val module = createModuleDescriptor(
            """
            class Bar
            class Foo {
                fun memberFoo() = Unit
                fun Bar.memberBarExtension() = Unit
                fun String.memberStringExtension() = Unit
                val prop = 42
            }
        """.trimMargin()
        )
        val fooClass = module.getClass("Foo")

        assertNull(getClassIfCategory(fooClass.getMemberFun("memberFoo")))
        assertNull(getClassIfCategory(fooClass.getMemberFun("memberBarExtension")))
        assertNull(getClassIfCategory(fooClass.getMemberFun("memberStringExtension")))
        assertNull(getClassIfCategory(fooClass.getMemberProperty("prop")))
    }

    @Test
    fun `test - null when there is no extension and no receiver`() {
        val module = createModuleDescriptor(
            """
            fun topLevelFoo() = Unit
            val prop = 42
        """.trimMargin()
        )
        assertNull(getClassIfCategory(module.getTopLevelFun("topLevelFoo")))
        assertNull(getClassIfCategory(module.getTopLevelProp("prop")))
    }

    @Test
    fun `test - null when extension isObjCObjectType == true`() {
        val module = createModuleDescriptor(
            """
            import kotlinx.cinterop.ObjCObject
            fun ObjCObject.foo() = Unit
        """.trimMargin()
        )
        assertNull(getClassIfCategory(module.getTopLevelFun("foo")))
    }

    @Test
    fun `test - null when extension is interface`() {
        val module = createModuleDescriptor(
            """
            interface Foo
            fun Foo.foo() = Unit
        """.trimMargin()
        )
        assertNull(getClassIfCategory(module.getTopLevelFun("foo")))
    }

    @Test
    fun `test - null when extension type is inlined`() {
        val module = createModuleDescriptor(
            """
            @JvmInline
            value class Foo(val i: Int)
            fun Foo.foo() = Unit
        """.trimMargin()
        )
        assertNull(getClassIfCategory(module.getTopLevelFun("foo")))
    }

    @Test
    fun `test - null when extension type isSpecialMapped == true`() {
        val module = createModuleDescriptor(
            """
            fun Any.anyFoo() = Unit
            fun List<String>.listFoo() = Unit
            fun String.stringFoo() = Unit
            fun Function<String>.fooFun() = Unit
        """.trimMargin()
        )
        assertNull(getClassIfCategory(module.getTopLevelFun("anyFoo")))
        assertNull(getClassIfCategory(module.getTopLevelFun("listFoo")))
        assertNull(getClassIfCategory(module.getTopLevelFun("stringFoo")))
        assertNull(getClassIfCategory(module.getTopLevelFun("fooFun")))
    }

    @Test
    fun `test - not inline class && not any && not mapped`() {

        val module = createModuleDescriptor(
            """
            class Foo(val i: Int)
            fun Foo.bar() = Unit
        """.trimMargin()
        )

        val foo = checkNotNull(getClassIfCategory(module.getClass("Foo").defaultType))
        val bar = checkNotNull(getClassIfCategory(module.getTopLevelFun("bar")))

        assertEquals("Foo", foo.name.identifier)
        assertEquals("Foo", bar.name.identifier)
    }

    override val testDisposable = Disposer.newDisposable("${this::class.simpleName}.testDisposable")

    override val kotlinCoreEnvironment: KotlinCoreEnvironment = createKotlinCoreEnvironment(testDisposable)

    @TempDir
    override lateinit var testTempDir: File

    @AfterEach
    fun dispose() {
        Disposer.dispose(testDisposable)
    }
}

