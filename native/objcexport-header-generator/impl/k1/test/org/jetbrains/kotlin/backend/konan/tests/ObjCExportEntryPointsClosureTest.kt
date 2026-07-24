/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.tests

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.backend.konan.testUtils.*
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.cli.common.disposeRootInWriteAction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class ObjCExportEntryPointsClosureTest : InlineSourceTestEnvironment {
    override val testDisposable = Disposer.newDisposable("${ObjCExportEntryPointsClosureTest::class.simpleName}.testDisposable")
    override val kotlinCoreEnvironment: KotlinCoreEnvironment = createKotlinCoreEnvironment(testDisposable)

    @TempDir
    override lateinit var testTempDir: File

    @AfterEach
    fun dispose() {
        disposeRootInWriteAction(testDisposable)
    }

    @Test
    fun `test downward closure`() {
        val source = """
            interface MyRunnable {
                fun run()
            }
            class A : MyRunnable {
                override fun run() {}
            }
            class B : MyRunnable {
                override fun run() {}
            }
        """.trimIndent()

        val module = createModuleDescriptor(source)

        val myRunnable = module.findClassAcrossModuleDependencies(ClassId.fromString("MyRunnable"))!!
        val a = module.findClassAcrossModuleDependencies(ClassId.fromString("A"))!!
        val b = module.findClassAcrossModuleDependencies(ClassId.fromString("B"))!!

        val runnableRun = myRunnable.unsubstitutedMemberScope.getContributedDescriptors()
            .filterIsInstance<CallableMemberDescriptor>().single { it.name.asString() == "run" }

        val aRun = a.unsubstitutedMemberScope.getContributedDescriptors()
            .filterIsInstance<CallableMemberDescriptor>().single { it.name.asString() == "run" }

        val bRun = b.unsubstitutedMemberScope.getContributedDescriptors()
            .filterIsInstance<CallableMemberDescriptor>().single { it.name.asString() == "run" }

        val entryPoints = object : ObjCEntryPoints {
            override fun shouldBeExposed(descriptor: CallableMemberDescriptor): Boolean {
                return descriptor.original == runnableRun.original
            }
        }

        val closure = computeDownwardClosure(entryPoints, listOf(module))

        assertTrue(runnableRun in closure, "Runnable.run should be in closure")
        assertTrue(aRun in closure, "A.run should be in closure")
        assertTrue(bRun in closure, "B.run should be in closure")
    }

    @Test
    fun `test downward closure with specific class`() {
        val source = """
            interface MyRunnable {
                fun run()
            }
            class A : MyRunnable {
                override fun run() {}
            }
            class B : MyRunnable {
                override fun run() {}
            }
        """.trimIndent()

        val module = createModuleDescriptor(source)

        val myRunnable = module.findClassAcrossModuleDependencies(ClassId.fromString("MyRunnable"))!!
        val a = module.findClassAcrossModuleDependencies(ClassId.fromString("A"))!!
        val b = module.findClassAcrossModuleDependencies(ClassId.fromString("B"))!!

        val runnableRun = myRunnable.unsubstitutedMemberScope.getContributedDescriptors()
            .filterIsInstance<CallableMemberDescriptor>().single { it.name.asString() == "run" }

        val aRun = a.unsubstitutedMemberScope.getContributedDescriptors()
            .filterIsInstance<CallableMemberDescriptor>().single { it.name.asString() == "run" }

        val bRun = b.unsubstitutedMemberScope.getContributedDescriptors()
            .filterIsInstance<CallableMemberDescriptor>().single { it.name.asString() == "run" }

        val entryPoints = object : ObjCEntryPoints {
            override fun shouldBeExposed(descriptor: CallableMemberDescriptor): Boolean {
                return descriptor.original == aRun.original
            }
        }

        val closure = computeDownwardClosure(entryPoints, listOf(module))

        assertFalse(runnableRun.original in closure, "Runnable.run should not be in closure")
        assertTrue(aRun in closure, "A.run should be in closure")
        assertFalse(bRun in closure, "B.run should not be in closure")
    }
}
