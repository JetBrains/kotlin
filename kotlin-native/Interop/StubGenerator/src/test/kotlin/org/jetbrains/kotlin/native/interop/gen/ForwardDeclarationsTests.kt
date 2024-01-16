/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.native.interop.indexer.IndexerResult
import org.jetbrains.kotlin.native.interop.indexer.ObjCClassOrProtocol
import org.jetbrains.kotlin.native.interop.indexer.StructDecl
import org.junit.Assume
import kotlin.test.*

class ForwardDeclarationsTests : InteropTestsBase() {

    private fun StructDecl.getName() = spelling.removePrefix("struct ")

    @Test
    fun `struct forward declarations`() {
        fun IndexerResult.assertHasOnlyForwardStructs(vararg names: String) {
            this.index.structs.forEach {
                assertNull(it.def, "${it.spelling} is not a forward declaration")
            }
            assertEquals(names.toSet(), this.index.structs.map { it.getName() }.toSet())
        }

        val dir = "ForwardDeclarations/struct"
        val dependency = buildNativeIndex(dir, "dependency.def")
        val main = buildNativeIndex(dir, "main.def", mockImports(dependency))

        dependency.assertHasOnlyForwardStructs("DependencyUsed", "DependencyUnused", "DependencyAndMain")

        main.assertHasOnlyForwardStructs(
                "ImportedUsed", "IncludedUnused", "IncludedUsed",
                "MainUnused", "MainUsed", "DependencyAndMain"
        )
    }

    @Test
    fun `objc forward declarations`() {
        Assume.assumeTrue(HostManager.hostIsMac)

        fun Collection<ObjCClassOrProtocol>.assertHasOnlyForward(vararg names: String) {
            this.forEach {
                assertTrue(it.isForwardDeclaration, "${it.name} is not a forward declaration")
            }
            assertEquals(names.toSet(), this.map { it.name }.toSet())
        }

        val dir = "ForwardDeclarations/objc"
        val dependency = buildNativeIndex(dir, "dependency.def")
        val main = buildNativeIndex(dir, "main.def", mockImports(dependency))

        dependency.index.objCClasses.assertHasOnlyForward(
                "DependencyClassUsed", "DependencyClassUnused", "DependencyAndMainClass"
        )
        dependency.index.objCProtocols.assertHasOnlyForward(
                "DependencyProtocolUsed", "DependencyProtocolUnused", "DependencyAndMainProtocol"
        )

        main.index.objCClasses.assertHasOnlyForward(
                "ImportedClassUsed", "IncludedClassUsed", "IncludedClassUnused",
                "MainClassUsed", "MainClassUnused", "DependencyAndMainClass"
        )

        main.index.objCProtocols.assertHasOnlyForward(
                "ImportedProtocolUsed", "IncludedProtocolUsed", "IncludedProtocolUnused",
                "MainProtocolUsed", "MainProtocolUnused", "DependencyAndMainProtocol"
        )
    }

    @Test
    fun `struct forward declarations with definitions`() {
        val dir = "ForwardDeclarations/structWithDefinition"
        val main = buildNativeIndex(dir, "main.def")
        val structs = main.index.structs

        structs.forEach {
            assertNotNull(it.def, "${it.spelling} is forward declaration")
        }

        assertEquals(setOf("Struct1", "Struct2", "Struct3", "Struct4"), structs.map { it.getName() }.toSet())
    }

    @Test
    fun `objc forward declarations with definitions`() {
        Assume.assumeTrue(HostManager.hostIsMac)

        fun Collection<ObjCClassOrProtocol>.assertHasOnlyNonForward(vararg names: String) {
            this.forEach {
                assertFalse(it.isForwardDeclaration, "${it.name} is a forward declaration")
            }
            assertEquals(names.toSet(), this.map { it.name }.toSet())
        }

        val dir = "ForwardDeclarations/objcWithDefinition"
        val main = buildNativeIndex(dir, "main.def")

        main.index.objCClasses.assertHasOnlyNonForward("Class1", "Class2", "Class3", "Class4")
        main.index.objCProtocols.assertHasOnlyNonForward("Protocol1", "Protocol2", "Protocol3", "Protocol4")
    }
}
