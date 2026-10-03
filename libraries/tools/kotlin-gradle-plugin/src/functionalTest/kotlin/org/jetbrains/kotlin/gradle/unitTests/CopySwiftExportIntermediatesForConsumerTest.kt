/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.plugin.mpp.apple.SerializationTools
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.GradleSwiftExportModule
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.GradleSwiftExportModules
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks.CopySwiftExportIntermediatesForConsumer
import org.jetbrains.kotlin.gradle.util.buildProject
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CopySwiftExportIntermediatesForConsumerTest {

    @Test
    fun `test the task declares the files it writes into the products directory as its outputs`() {
        val project = buildProject()
        val products = project.projectDir.resolve("products")
        val library = project.projectDir.resolve("merged/libSwiftExportBinary.a").apply { parentFile.mkdirs(); writeText("") }
        val interfaces = project.projectDir.resolve("interfaces").apply {
            resolve("Shared.swiftmodule/arm64.swiftinterface").apply { parentFile.mkdirs(); writeText("") }
            resolve("Other.swiftmodule/arm64.swiftinterface").apply { parentFile.mkdirs(); writeText("") }
            resolve("Shared.o").writeText("")
        }
        val includes = project.projectDir.resolve("OtherIncludes").apply {
            resolve("SharedBridge/Shared.h").apply { parentFile.mkdirs(); writeText("") }
            resolve("SharedBridge/module.modulemap").writeText("")
        }
        val modulesFile = project.projectDir.resolve("modules/Shared.json").apply {
            parentFile.mkdirs()
            val shared = GradleSwiftExportModule.SwiftOnly(File("/Shared.swift"), "Shared", emptyList())
            writeText(SerializationTools.writeToJson(GradleSwiftExportModules(listOf(shared), 0)))
        }

        val task = project.tasks.register("copyDebugSPMIntermediates", CopySwiftExportIntermediatesForConsumer::class.java) { task ->
            task.builtProductsDirectory.set(products)
            task.library.set(library)
            task.libraryName.set("libShared.a")
            task.addInterface(project.provider { interfaces })
            task.includes.from(includes)
            task.filterInterfacesToOwnModules.set(true)
            task.swiftModulesFile.set(modulesFile)
        }.get()

        val expected = setOf(
            products.resolve("libShared.a"),
            products.resolve("Shared.swiftmodule/arm64.swiftinterface"),
            products.resolve("SharedBridge/Shared.h"),
            products.resolve("SharedBridge/module.modulemap"),
        )
        assertTrue(task.outputs.hasOutput, "The task has to declare outputs to ever be up to date")
        assertEquals(expected, task.copiedFiles.get().toSet())

        task.copy()
        assertEquals(expected, products.walkTopDown().filter { it.isFile }.toSet())
    }

    @Test
    fun `test every interface is copied when the task does not filter them`() {
        val project = buildProject()
        val products = project.projectDir.resolve("products")
        val interfaces = project.projectDir.resolve("interfaces").apply {
            resolve("Shared.swiftmodule/arm64.swiftinterface").apply { parentFile.mkdirs(); writeText("") }
            resolve("Other.swiftmodule/arm64.swiftinterface").apply { parentFile.mkdirs(); writeText("") }
        }

        val task = project.tasks.register("copyDebugSPMIntermediates", CopySwiftExportIntermediatesForConsumer::class.java) { task ->
            task.builtProductsDirectory.set(products)
            task.library.set(project.projectDir.resolve("libSwiftExportBinary.a").apply { writeText("") })
            task.libraryName.set("libShared.a")
            task.addInterface(project.provider { interfaces })
        }.get()

        assertEquals(
            setOf(
                products.resolve("libShared.a"),
                products.resolve("Shared.swiftmodule/arm64.swiftinterface"),
                products.resolve("Other.swiftmodule/arm64.swiftinterface"),
            ),
            task.copiedFiles.get().toSet(),
        )
    }
}
