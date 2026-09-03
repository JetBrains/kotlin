/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.tests

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.backend.konan.descriptors.getPackageFragments
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.backend.konan.testUtils.*
import org.jetbrains.kotlin.cli.common.disposeRootInWriteAction
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.context.withModule
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.ModuleDependenciesImpl
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.library.metadata.CurrentKlibModuleOrigin
import org.jetbrains.kotlin.library.metadata.KlibModuleOrigin
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.native.FakeTopDownAnalyzerFacadeForNative
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue


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

    @Test
    fun `test property accessor matching in ObjCEntryPoints`() {
        val entryPointsFile = testTempDir.resolve("entrypoints.txt").apply {
            writeText("callable Foo.bar\n")
        }
        val entryPoints = org.jetbrains.kotlin.konan.file.File(entryPointsFile.absolutePath).readObjCEntryPoints()

        val source = """
            class Foo {
                val bar: String = "value"
                val other: String = "other"
            }
        """.trimIndent()

        val module = createModuleDescriptor(source)
        val foo = module.findClassAcrossModuleDependencies(ClassId.fromString("Foo"))!!
        val propertyDescriptors = foo.unsubstitutedMemberScope.getContributedDescriptors()
            .filterIsInstance<org.jetbrains.kotlin.descriptors.PropertyDescriptor>()
        val barProperty = propertyDescriptors.single { it.name.asString() == "bar" }
        val otherProperty = propertyDescriptors.single { it.name.asString() == "other" }

        assertTrue(entryPoints.shouldBeExposed(barProperty.getter!!), "Foo.bar getter should be exposed")
        assertFalse(entryPoints.shouldBeExposed(otherProperty.getter!!), "Foo.other getter should not be exposed")
    }

    @Test
    fun `test var property getter and setter`() {
        val entryPointsFile = testTempDir.resolve("entrypoints_var.txt").apply {
            writeText("callable Foo.bar\n")
        }
        val entryPoints = org.jetbrains.kotlin.konan.file.File(entryPointsFile.absolutePath).readObjCEntryPoints()

        val source = """
            class Foo {
                var bar: String = "value"
                var other: String = "other"
            }
        """.trimIndent()

        val module = createModuleDescriptor(source)
        val foo = module.findClassAcrossModuleDependencies(ClassId.fromString("Foo"))!!
        val propertyDescriptors = foo.unsubstitutedMemberScope.getContributedDescriptors()
            .filterIsInstance<PropertyDescriptor>()
        val barProperty = propertyDescriptors.single { it.name.asString() == "bar" }
        val otherProperty = propertyDescriptors.single { it.name.asString() == "other" }

        assertTrue(entryPoints.shouldBeExposed(barProperty.getter!!), "Foo.bar getter should be exposed")
        assertTrue(entryPoints.shouldBeExposed(barProperty.setter!!), "Foo.bar setter should be exposed")
        assertFalse(entryPoints.shouldBeExposed(otherProperty.getter!!), "Foo.other getter should not be exposed")
        assertFalse(entryPoints.shouldBeExposed(otherProperty.setter!!), "Foo.other setter should not be exposed")
    }

    @Test
    fun `test wildcard pattern matching`() {
        val entryPointsFile = testTempDir.resolve("entrypoints_wildcard.txt").apply {
            writeText("callable Foo.*\n")
        }
        val entryPoints = org.jetbrains.kotlin.konan.file.File(entryPointsFile.absolutePath).readObjCEntryPoints()

        val source = """
            class Foo {
                val prop: String = "value"
                var mutableProp: Int = 10
                fun action(): Int = 42
            }
            class Other {
                val prop: String = "value"
                fun action(): Int = 42
            }
        """.trimIndent()

        val module = createModuleDescriptor(source)
        val foo = module.findClassAcrossModuleDependencies(ClassId.fromString("Foo"))!!
        val other = module.findClassAcrossModuleDependencies(ClassId.fromString("Other"))!!

        val fooDescriptors = foo.unsubstitutedMemberScope.getContributedDescriptors()
        val fooProp = fooDescriptors.filterIsInstance<PropertyDescriptor>().single { it.name.asString() == "prop" }
        val fooMutableProp = fooDescriptors.filterIsInstance<PropertyDescriptor>().single { it.name.asString() == "mutableProp" }
        val fooAction = fooDescriptors.filterIsInstance<FunctionDescriptor>().single { it.name.asString() == "action" }

        val otherDescriptors = other.unsubstitutedMemberScope.getContributedDescriptors()
        val otherProp = otherDescriptors.filterIsInstance<PropertyDescriptor>().single { it.name.asString() == "prop" }
        val otherAction = otherDescriptors.filterIsInstance<FunctionDescriptor>().single { it.name.asString() == "action" }

        assertTrue(entryPoints.shouldBeExposed(fooProp.getter!!), "Foo.prop getter should be exposed by wildcard")
        assertTrue(entryPoints.shouldBeExposed(fooMutableProp.getter!!), "Foo.mutableProp getter should be exposed by wildcard")
        assertTrue(entryPoints.shouldBeExposed(fooMutableProp.setter!!), "Foo.mutableProp setter should be exposed by wildcard")
        assertTrue(entryPoints.shouldBeExposed(fooAction), "Foo.action should be exposed by wildcard")

        assertFalse(entryPoints.shouldBeExposed(otherProp.getter!!), "Other.prop getter should not be exposed")
        assertFalse(entryPoints.shouldBeExposed(otherAction), "Other.action should not be exposed")
    }

    @Test
    fun `test entry points parsing robustness`() {
        val validFile = testTempDir.resolve("entrypoints_robustness.txt").apply {
            writeText(
                """
                // Leading comment
                   // Indented comment
                
                callable Foo.bar
                   property Foo.baz   
                
                // Comment before last entry
                function Foo.qux
                
                """.trimIndent()
            )
        }
        val entryPoints = org.jetbrains.kotlin.konan.file.File(validFile.absolutePath).readObjCEntryPointList()
        assertEquals(3, entryPoints.size)
        assertEquals(ObjCEntryPoint.Kind.CALLABLE, entryPoints[0].kind)
        assertEquals(listOf("Foo"), entryPoints[0].pattern.path)
        assertEquals(ObjCEntryPoint.Pattern.Name.Explicit("bar"), entryPoints[0].pattern.name)

        assertEquals(ObjCEntryPoint.Kind.PROPERTY, entryPoints[1].kind)
        assertEquals(listOf("Foo"), entryPoints[1].pattern.path)
        assertEquals(ObjCEntryPoint.Pattern.Name.Explicit("baz"), entryPoints[1].pattern.name)

        assertEquals(ObjCEntryPoint.Kind.FUNCTION, entryPoints[2].kind)
        assertEquals(listOf("Foo"), entryPoints[2].pattern.path)
        assertEquals(ObjCEntryPoint.Pattern.Name.Explicit("qux"), entryPoints[2].pattern.name)

        // Verify malformed lines throw IllegalArgumentException
        val invalidKindFile = testTempDir.resolve("invalid_kind.txt").apply {
            writeText("unknown Foo.bar\n")
        }
        assertFailsWith<IllegalArgumentException> {
            org.jetbrains.kotlin.konan.file.File(invalidKindFile.absolutePath).readObjCEntryPointList()
        }

        val missingPatternFile = testTempDir.resolve("missing_pattern.txt").apply {
            writeText("callable\n")
        }
        assertFailsWith<IllegalArgumentException> {
            org.jetbrains.kotlin.konan.file.File(missingPatternFile.absolutePath).readObjCEntryPointList()
        }

        val tooManyTokensFile = testTempDir.resolve("too_many_tokens.txt").apply {
            writeText("callable Foo.bar extraToken\n")
        }
        assertFailsWith<IllegalArgumentException> {
            org.jetbrains.kotlin.konan.file.File(tooManyTokensFile.absolutePath).readObjCEntryPointList()
        }
    }

    private fun createDependentModuleDescriptor(
        sourceCode: String,
        dependencyModules: List<ModuleDescriptor>,
        name: String = "dependent_module"
    ): ModuleDescriptor {
        val stdlibModule = dependencyModules.first().builtIns.builtInsModule
        val moduleDescriptor = ModuleDescriptorImpl(
            moduleName = Name.special("<$name>"),
            storageManager = LockBasedStorageManager.NO_LOCKS,
            builtIns = dependencyModules.first().builtIns,
            platform = dependencyModules.first().platform,
            capabilities = mapOf(
                KlibModuleOrigin.CAPABILITY to CurrentKlibModuleOrigin,
            )
        )

        moduleDescriptor.setDependencies(
            ModuleDependenciesImpl(
                allDependencies = listOf(moduleDescriptor, stdlibModule) + dependencyModules.map { it as ModuleDescriptorImpl },
                modulesWhoseInternalsAreVisible = emptySet(),
                directExpectedByDependencies = emptyList(),
                allExpectedByDependencies = emptySet()
            )
        )

        val projectContext = ProjectContext(kotlinCoreEnvironment.project, "test project context")
        val psiFactory = KtPsiFactory(kotlinCoreEnvironment.project)
        val psiFile = psiFactory.createFile("$name.kt", sourceCode)

        return FakeTopDownAnalyzerFacadeForNative.analyzeFilesWithGivenTrace(
            files = listOf(psiFile),
            trace = NoScopeRecordCliBindingTrace(kotlinCoreEnvironment.project),
            languageVersionSettings = createLanguageVersionSettings(),
            moduleContext = projectContext.withModule(moduleDescriptor)
        ).moduleDescriptor
    }

    @Test
    fun `test multi-module downward closure`() {
        val module1Source = """
            interface BaseInterface {
                fun process()
            }
            class LocalImpl : BaseInterface {
                override fun process() {}
            }
        """.trimIndent()

        val module1 = createModuleDescriptor(module1Source)

        val module2Source = """
            class RemoteImpl : BaseInterface {
                override fun process() {}
            }
            class Unrelated {
                fun other() {}
            }
        """.trimIndent()

        val module2 = createDependentModuleDescriptor(module2Source, listOf(module1), "module2")

        val baseInterface = module1.findClassAcrossModuleDependencies(ClassId.fromString("BaseInterface"))!!
        val localImpl = module1.findClassAcrossModuleDependencies(ClassId.fromString("LocalImpl"))!!
        val remoteImpl = module2.findClassAcrossModuleDependencies(ClassId.fromString("RemoteImpl"))!!
        val unrelated = module2.findClassAcrossModuleDependencies(ClassId.fromString("Unrelated"))!!

        val baseProcess = baseInterface.unsubstitutedMemberScope.getContributedDescriptors()
            .filterIsInstance<CallableMemberDescriptor>().single { it.name.asString() == "process" }
        val localProcess = localImpl.unsubstitutedMemberScope.getContributedDescriptors()
            .filterIsInstance<CallableMemberDescriptor>().single { it.name.asString() == "process" }
        val remoteProcess = remoteImpl.unsubstitutedMemberScope.getContributedDescriptors()
            .filterIsInstance<CallableMemberDescriptor>().single { it.name.asString() == "process" }
        val unrelatedOther = unrelated.unsubstitutedMemberScope.getContributedDescriptors()
            .filterIsInstance<CallableMemberDescriptor>().single { it.name.asString() == "other" }

        val entryPoints = object : ObjCEntryPoints {
            override fun shouldBeExposed(descriptor: CallableMemberDescriptor): Boolean {
                return descriptor.original == baseProcess.original
            }
        }

        val closure = computeDownwardClosure(entryPoints, listOf(module1, module2))

        assertTrue(baseProcess in closure, "BaseInterface.process should be in closure")
        assertTrue(localProcess in closure, "LocalImpl.process in module1 should be in closure")
        assertTrue(remoteProcess in closure, "RemoteImpl.process in module2 should be in closure")
        assertFalse(unrelatedOther in closure, "Unrelated.other in module2 should not be in closure")
    }
}

