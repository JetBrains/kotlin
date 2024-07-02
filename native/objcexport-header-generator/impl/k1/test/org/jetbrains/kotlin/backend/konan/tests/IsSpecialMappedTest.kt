package org.jetbrains.kotlin.backend.konan.tests

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.backend.konan.objcexport.isSpecialMapped
import org.jetbrains.kotlin.backend.konan.testUtils.*
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.test.util.JUnit4Assertions.assertFalse
import org.jetbrains.kotlin.test.util.JUnit4Assertions.assertTrue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class IsSpecialMappedTest : InlineSourceTestEnvironment {
    @Test
    fun `test - basic class`() {
        val module = createModuleDescriptor("class Foo")
        assertFalse(isSpecialMapped(module.getClass("Foo")))
    }

    @Test
    fun `test - any type`() {
        val module = createModuleDescriptor("fun Any.anyFoo() = Unit")
        assertTrue(isSpecialMapped(module.getTopLevelFunExtensionType("anyFoo")))
    }

    @Test
    fun `test - list`() {
        val module = createModuleDescriptor("fun List<Any>.listFoo() = Unit")
        assertTrue(isSpecialMapped(module.getTopLevelFunExtensionType("listFoo")))
    }

    @Test
    fun `test - mutable list`() {
        val module = createModuleDescriptor("fun MutableList<Any>.mutableList() = Unit")
        assertTrue(isSpecialMapped(module.getTopLevelFunExtensionType("mutableList")))
    }

    @Test
    fun `test - set`() {
        val module = createModuleDescriptor("fun Set<Any>.set() = Unit")
        assertTrue(isSpecialMapped(module.getTopLevelFunExtensionType("set")))
    }

    @Test
    fun `test - mutable set`() {
        val module = createModuleDescriptor("fun MutableSet<Any>.mutableSet() = Unit")
        assertTrue(isSpecialMapped(module.getTopLevelFunExtensionType("mutableSet")))
    }

    @Test
    fun `test - map`() {
        val module = createModuleDescriptor("fun Map<Any, Any>.map() = Unit")
        assertTrue(isSpecialMapped(module.getTopLevelFunExtensionType("map")))
    }

    @Test
    fun `test - mutable map`() {
        val module = createModuleDescriptor("fun MutableMap<Any, Any>.mutableMap() = Unit")
        assertTrue(isSpecialMapped(module.getTopLevelFunExtensionType("mutableMap")))
    }

    @Test
    fun `test - long`() {
        val module = createModuleDescriptor("fun Long.long() = Unit")
        assertTrue(isSpecialMapped(module.getTopLevelFunExtensionType("long")))
    }

    @Test
    fun `test - string`() {
        val module = createModuleDescriptor("fun String.string() = Unit")
        assertTrue(isSpecialMapped(module.getTopLevelFunExtensionType("string")))
    }

    @Test
    fun `test - super type`() {
        val module = createModuleDescriptor("abstract class Foo: List<String>")
        assertTrue(isSpecialMapped(module.getClass("Foo")))
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