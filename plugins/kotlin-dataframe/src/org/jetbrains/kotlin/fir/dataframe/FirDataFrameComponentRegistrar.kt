/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.dataframe

import com.intellij.mock.MockProject
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import java.util.*
import kotlin.collections.ArrayDeque

class FirDataFrameExtensionRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        val ids = List(100) {
            val name = Name.identifier(UUID.randomUUID().toString())
            ClassId(FqName.fromSegments(listOf("org", "jetbrains", "kotlinx", "dataframe")), name)
        }.toSet()
        val queue = ArrayDeque(ids)
        val state = mutableMapOf<ClassId, SchemaContext>()

        +{ it: FirSession -> FirDataFrameExtensionsGenerator(it, ids, state) }
        +{ it: FirSession -> FirDataFrameReceiverInjector(it, state, queue) }
//        +::FirDataFrameExtensionsGenerator
//        +::FirDataFrameReceiverInjector
    }
}

class FirDataFrameComponentRegistrar : ComponentRegistrar {
    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        FirExtensionRegistrar.registerExtension(project, FirDataFrameExtensionRegistrar())
        IrGenerationExtension.registerExtension(project, DataFrameIrBodyFiller())
    }
}