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
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.cli.common.disposeRootInWriteAction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
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

        val expanded = expandObjCEntryPoints(entryPoints, listOf(module))

        assertTrue(expanded.shouldBeExposed(runnableRun), "Runnable.run should be exposed")
        assertTrue(expanded.shouldBeExposed(aRun), "A.run should be exposed")
        assertTrue(expanded.shouldBeExposed(bRun), "B.run should be exposed")
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

        val expanded = expandObjCEntryPoints(entryPoints, listOf(module))

        assertFalse(expanded.shouldBeExposed(runnableRun), "Runnable.run should not be exposed")
        assertTrue(expanded.shouldBeExposed(aRun), "A.run should be exposed")
        assertFalse(expanded.shouldBeExposed(bRun), "B.run should not be exposed")
    }

    @Test
    fun `test expansion through intermediate classes and properties`() {
        val source = """
            interface Root {
                fun run()
                val size: Int
            }
            abstract class Middle : Root {
                override fun run() {}
            }
            class Leaf : Middle() {
                override fun run() {}
                override val size: Int get() = 1
            }
            class Inherits : Middle() {
                override val size: Int get() = 2
            }
            class Unrelated {
                fun run() {}
            }
        """.trimIndent()

        val module = createModuleDescriptor(source)
        val root = module.findClassAcrossModuleDependencies(ClassId.fromString("Root"))!!
        val rootMembers = root.unsubstitutedMemberScope.getContributedDescriptors()
            .filterIsInstance<CallableMemberDescriptor>()
        val allCallables = listOf("Root", "Middle", "Leaf", "Inherits", "Unrelated").flatMap { name ->
            val descriptor = module.findClassAcrossModuleDependencies(ClassId.fromString(name))!!
            descriptor.unsubstitutedMemberScope.getContributedDescriptors()
                .filterIsInstance<CallableMemberDescriptor>()
                .flatMap { member ->
                    listOf(member) + ((member as? PropertyDescriptor)?.accessors.orEmpty())
                }
        }

        val exposed = rootMembers.associate { entry ->
            val expanded = expandObjCEntryPoints(
                object : ObjCEntryPoints {
                    override fun shouldBeExposed(descriptor: CallableMemberDescriptor): Boolean = descriptor.original == entry.original
                },
                listOf(module),
            )
            entry.name.asString() to allCallables.filter { expanded.shouldBeExposed(it) }
                .map { "${it.containingDeclaration.name}.${it.name}" }.toSet()
        }
        assertEquals(setOf("Root.run", "Middle.run", "Leaf.run", "Inherits.run"), exposed["run"])
        assertEquals(setOf("Root.size", "Middle.size", "Leaf.size", "Inherits.size"), exposed["size"])
    }
}
