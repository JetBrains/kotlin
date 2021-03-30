/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.utils

import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.commonizer.tree.CirTreeModule
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.library.SerializedMetadata

interface InlineSourceBuilder {
    annotation class ModuleBuilderDsl

    data class SourceFile(val name: String, @Language("kotlin") val content: String)

    data class Module(
        val name: String, val sourceFiles: List<SourceFile>, val dependencies: List<Module>
    )

    @ModuleBuilderDsl
    class ModuleBuilder {
        var name: String = "test-module"
        private var sourceFiles: List<SourceFile> = emptyList()
        private var dependencies: List<Module> = emptyList()


        @ModuleBuilderDsl
        fun source(@Language("kotlin") content: String, name: String = "test.kt") {
            sourceFiles = sourceFiles + SourceFile(name, content)
        }

        @ModuleBuilderDsl
        fun dependency(builder: ModuleBuilder.() -> Unit) {
            dependency(ModuleBuilder().also(builder).build())
        }

        @ModuleBuilderDsl
        fun dependency(module: Module) {
            this.dependencies += module.copy(name = "${this.name}-dependency-${module.name}-${dependencies.size}")
        }

        fun build(): Module = Module(name, sourceFiles.toList(), dependencies.toList())
    }

    fun createModule(builder: ModuleBuilder.() -> Unit): Module {
        return ModuleBuilder().also(builder).build()
    }

    fun createCirTreeFromSourceCode(@Language("kotlin") sourceCode: String): CirTreeModule {
        return createCirTree {
            source(content = sourceCode, "test.kt")
        }
    }


    fun createCirTree(module: Module): CirTreeModule

    fun createModuleDescriptor(module: Module): ModuleDescriptor

    fun createMetadata(module: Module): SerializedMetadata = createModuleDescriptor(module).toMetadata()
}


@InlineSourceBuilder.ModuleBuilderDsl
fun InlineSourceBuilder.createCirTree(builder: InlineSourceBuilder.ModuleBuilder.() -> Unit): CirTreeModule {
    return createCirTree(createModule(builder))
}

@InlineSourceBuilder.ModuleBuilderDsl
fun InlineSourceBuilder.createModuleDescriptor(builder: InlineSourceBuilder.ModuleBuilder.() -> Unit): ModuleDescriptor {
    return createModuleDescriptor(createModule(builder))
}

@InlineSourceBuilder.ModuleBuilderDsl
fun InlineSourceBuilder.createMetadata(builder: InlineSourceBuilder.ModuleBuilder.() -> Unit): SerializedMetadata {
    return createMetadata(createModule(builder))
}

