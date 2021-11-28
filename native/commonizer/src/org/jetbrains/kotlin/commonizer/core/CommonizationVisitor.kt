/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.jetbrains.kotlin.commonizer.mergedtree.*

internal class CommonizationVisitor(
    private val root: CirRootNode
) : CirNodeVisitor<Unit, Unit> {
    override fun visitRootNode(node: CirRootNode, data: Unit) {
        check(node === root)
        check(node.commonDeclaration() != null) // root should already be commonized

        node.modules.values.forEach { module ->
            module.accept(this, Unit)
        }
    }

    override fun visitModuleNode(node: CirModuleNode, data: Unit) {
        node.commonDeclaration() // commonize module

        node.packages.values.forEach { pkg ->
            pkg.accept(this, Unit)
        }
    }

    @Suppress("DuplicatedCode")
    override fun visitPackageNode(node: CirPackageNode, data: Unit) {
        node.commonDeclaration() // commonize package

        node.properties.values.forEach { property ->
            property.accept(this, Unit)
        }

        node.functions.values.forEach { function ->
            function.accept(this, Unit)
        }

        node.classes.values.forEach { clazz ->
            clazz.accept(this, Unit)
        }

        node.typeAliases.values.forEach { typeAlias ->
            typeAlias.accept(this, Unit)
        }
    }

    override fun visitPropertyNode(node: CirPropertyNode, data: Unit) {
        node.commonDeclaration() // commonize property
    }

    override fun visitFunctionNode(node: CirFunctionNode, data: Unit) {
        node.commonDeclaration() // commonize function
    }

    @Suppress("DuplicatedCode")
    override fun visitClassNode(node: CirClassNode, data: Unit) {
        val commonClass = node.commonDeclaration() ?: return // No need to commonize class members

        node.constructors.values.forEach { constructor ->
            constructor.accept(this, Unit)
        }

        node.properties.values.forEach { property ->
            property.accept(this, Unit)
        }

        node.functions.values.forEach { function ->
            function.accept(this, Unit)
        }

        node.classes.values.forEach { clazz ->
            clazz.accept(this, Unit)
        }


        // companion object should have the same name for each target class, then it could be set to common class
        val companionObjectName = node.targetDeclarations.mapTo(HashSet()) { it!!.companion }.singleOrNull()
        if (companionObjectName != null) {
            val companionObjectNode = node.classes[companionObjectName]
                ?: error("Can't find node for companion object $companionObjectName in node for class ${node.classifierName}")

            if (companionObjectNode.commonDeclaration() != null) {
                // companion object has been successfully commonized
                commonClass.companion = companionObjectName
            }
        }
    }

    override fun visitClassConstructorNode(node: CirClassConstructorNode, data: Unit) {
        node.commonDeclaration() // commonize constructor
    }

    override fun visitTypeAliasNode(node: CirTypeAliasNode, data: Unit) {
        node.commonDeclaration() // commonize type alias
    }
}
