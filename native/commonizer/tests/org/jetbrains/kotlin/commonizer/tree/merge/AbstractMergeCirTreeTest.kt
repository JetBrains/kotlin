/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("TestFunctionName")

package org.jetbrains.kotlin.commonizer.tree.merge

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.commonizer.DefaultCommonizerSettings
import org.jetbrains.kotlin.commonizer.LeafCommonizerTarget
import org.jetbrains.kotlin.commonizer.TargetDependent
import org.jetbrains.kotlin.commonizer.mergedtree.*
import org.jetbrains.kotlin.commonizer.toTargetDependent
import org.jetbrains.kotlin.commonizer.tree.CirTreeModule
import org.jetbrains.kotlin.commonizer.tree.CirTreeRoot
import org.jetbrains.kotlin.commonizer.utils.KtInlineSourceCommonizerTestCase
import org.jetbrains.kotlin.commonizer.utils.MockModulesProvider
import org.jetbrains.kotlin.storage.LockBasedStorageManager

abstract class AbstractMergeCirTreeTest : KtInlineSourceCommonizerTestCase() {
    private val storageManager = LockBasedStorageManager(this::class.simpleName)

    fun mergeCirTree(vararg modules: Pair<String, CirTreeModule>): CirRootNode {
        return org.jetbrains.kotlin.commonizer.tree.mergeCirTree(
            storageManager,
            createDefaultKnownClassifiers(),
            TargetDependent(*modules),
            DefaultCommonizerSettings
        )
    }

    fun CirRootNode.assertSingleModule(): CirModuleNode {
        kotlin.test.assertEquals(1, modules.size, "Expected exactly one merged module. Found ${modules.map { it.key }}")
        return modules.values.single()
    }

    fun CirModuleNode.assertSinglePackage(): CirPackageNode {
        kotlin.test.assertEquals(1, packages.size, "Expected exactly one package. Found ${packages.map { it.key }}")
        return packages.values.single()
    }

    fun CirPackageNode.assertSingleProperty(): CirPropertyNode {
        kotlin.test.assertEquals(1, properties.size, "Expected exactly one property. Found ${properties.map { it.key }}")
        return properties.values.single()
    }

    fun CirPackageNode.assertSingleFunction(): CirFunctionNode {
        kotlin.test.assertEquals(1, functions.size, "Expected exactly one function. Found ${functions.map { it.key }}")
        return functions.values.single()
    }

    fun CirPackageNode.assertSingleTypeAlias(): CirTypeAliasNode {
        kotlin.test.assertEquals(1, typeAliases.size, "Expected exactly one type alias. Found ${typeAliases.map { it.key }}")
        return typeAliases.values.single()
    }

    fun CirPackageNode.assertSingleClass(): CirClassNode {
        kotlin.test.assertEquals(1, classes.size, "Expected exactly one class. Found ${classes.map { it.key }}")
        return classes.values.single()
    }

    fun CirClassNode.assertSingleConstructor(): CirClassConstructorNode {
        kotlin.test.assertEquals(1, constructors.size, "Expected exactly one function. Found ${constructors.map { it.key }}")
        return constructors.values.single()
    }

    fun CirNode<*, *>.assertNoMissingTargetDeclaration() {
        this.targetDeclarations.forEachIndexed { index, target ->
            kotlin.test.assertNotNull(target, "Missing target declaration at index $index")
        }
    }

    fun CirNode<*, *>.assertOnlyTargetDeclarationAtIndex(vararg indices: Int) {
        this.targetDeclarations.forEachIndexed { index, target ->
            if (index in indices) {
                kotlin.test.assertNotNull(target, "Expected target declaration at index $index")
            } else {
                kotlin.test.assertNull(target, "Expected *no* target declaration at index $index")
            }
        }
    }

    private fun TargetDependent(vararg modules: Pair<String, CirTreeModule>): TargetDependent<CirTreeRoot> {
        return TargetDependent(modules.toList())
    }

    private fun TargetDependent(modules: Iterable<Pair<String, CirTreeModule>>): TargetDependent<CirTreeRoot> {
        return modules.toMap().mapKeys { (targetName, _) -> LeafCommonizerTarget(targetName) }
            .mapValues { (_, module) -> CirTreeRoot(listOf(module)) }
            .toTargetDependent()
    }

    private fun createDefaultKnownClassifiers(): CirKnownClassifiers {
        return CirKnownClassifiers(
            TargetDependent.empty(),
            TargetDependent.empty(),
            CirCommonizedClassifierNodes.default(),
            CirProvidedClassifiers.of(
                CirFictitiousFunctionClassifiers,
                CirProvidedClassifiers.by(MockModulesProvider.create(DefaultBuiltIns.Instance.builtInsModule))
            )
        )
    }
}
