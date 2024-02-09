/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.passes

import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.builder.buildEnum
import org.jetbrains.kotlin.sir.builder.buildModule
import org.jetbrains.kotlin.sir.visitors.SirTransformer
import org.jetbrains.sir.passes.builder.KotlinSource

/**
 * Pass that for every occurring declaration in package x.y.z generates a mirroring type scope and puts it there.
 * Right now, enums without cases are used for namespace simulation.
 */
public class SirInflatePackagesPass : SirModulePass {
    private data class Namespace(
        val elements: MutableList<SirDeclaration> = mutableListOf(),
        val children: MutableMap<String, Namespace> = mutableMapOf(),
    ) {
        fun <R> reduce(transform: (List<String>, List<SirDeclaration>, List<R>) -> R): R {
            fun reduceFrom(
                node: Namespace,
                rootPath: List<String>,
                transform: (List<String>, List<SirDeclaration>, List<R>) -> R,
            ): R = transform(
                rootPath,
                node.elements,
                node.children.map { reduceFrom(it.value, rootPath + it.key, transform) }
            )

            return reduceFrom(this, listOf(""), transform)
        }

        fun getOrCreate(path: List<String>): Namespace {
            if (path.isEmpty()) {
                return this
            }

            val key = path.first()
            val next = children.getOrPut(key) { Namespace() }
            return next.getOrCreate(path.drop(1))
        }
    }

    private class Context(val root: Namespace = Namespace())

    private object Transformer : SirTransformer<Context>() {
        override fun <E : SirElement> transformElement(element: E, data: Context): E = element

        override fun transformModule(module: SirModule, data: Context): SirModule = buildModule {
            name = module.name

            for (declaration in module.declarations) {
                val origin = declaration.origin as? KotlinSource
                if (origin != null) {
                    // FIXME: for now we assume everything before the last dot is a package name.
                    //  This should change as we add type declarations into the mix
                    val path = (origin.symbol as? KtCallableSymbol)
                        ?.callableIdIfNonLocal?.packageName
                        ?.pathSegments()
                        ?.map { it.toString() }
                        ?: emptyList()
                    data.root.getOrCreate(path).elements.add(declaration)
                    continue
                }
                declarations += declaration
            }

            val additions = data.root.reduce { path, declarations, children ->
                buildEnum {
                    origin = SirOrigin.Namespace(path.drop(1))
                    name = path.last()
                    this.declarations += children
                    this.declarations += declarations
                }
            }

            declarations += additions.declarations
        }.also(SirDeclarationContainer::fixParents)
    }

    public override fun run(element: SirModule, data: Nothing?): SirModule = element.transform(Transformer, Context())
}

private fun SirDeclarationContainer.fixParents() = declarations
    .onEach { it.parent = this }
    .filterIsInstance<SirDeclarationContainer>()
    .forEach(SirDeclarationContainer::fixParents)