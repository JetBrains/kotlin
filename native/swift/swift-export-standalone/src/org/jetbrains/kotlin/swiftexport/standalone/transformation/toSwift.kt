/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.transformation

import org.jetbrains.kotlin.sir.SirDeclaration
import org.jetbrains.kotlin.sir.SirElement
import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.sir.builder.buildModule
import org.jetbrains.sir.passes.SirInflatePackagesPass
import org.jetbrains.sir.passes.SirModulePass
import org.jetbrains.sir.passes.SirPass
import org.jetbrains.sir.passes.run

internal fun SirModule.transformToSwift(): SirModule {
    return SirPassesConfiguration.passes.fold(this) { module, pass ->
        pass.run(module)
    }
}

private object SirPassesConfiguration {
    val passes: List<SirModulePass> = listOf(
        SirInflatePackagesPass(),
    )

    @Suppress("Unused")
    class WholeModuleTranslationByElementPass(
        val pass: SirPass<SirElement, Nothing?, SirDeclaration>
    ) : SirModulePass {
        override fun run(element: SirModule, data: Nothing?): SirModule {
            return buildModule {
                name = element.name
                element.declarations.forEach {
                    val newDecl = pass.run(it)
                    declarations.add(newDecl)
                }
            }.apply {
                declarations.forEach { it.parent = this }
            }
        }
    }
}
