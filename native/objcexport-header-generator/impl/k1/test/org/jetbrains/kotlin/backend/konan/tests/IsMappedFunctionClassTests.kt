package org.jetbrains.kotlin.backend.konan.tests

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.backend.konan.objcexport.isMappedFunctionClass
import org.jetbrains.kotlin.backend.konan.testUtils.InlineSourceTestEnvironment
import org.jetbrains.kotlin.backend.konan.testUtils.createKotlinCoreEnvironment
import org.jetbrains.kotlin.backend.konan.testUtils.createModuleDescriptor
import org.jetbrains.kotlin.backend.konan.testUtils.getTopLevelFunExtensionType
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IsMappedFunctionClassTests : InlineSourceTestEnvironment {
    @Test
    fun `test - isMappedFunctionClass`() {
        val module = createModuleDescriptor(
            """
            fun Function1<Any, Any>.mappedFooTrue() = Unit
            fun Function<Any>.mappedFooFalse() = Unit
        """.trimIndent()
        )

        assertTrue(module.getTopLevelFunExtensionType("mappedFooTrue").isMappedFunctionClass())
        assertFalse(module.getTopLevelFunExtensionType("mappedFooFalse").isMappedFunctionClass())
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