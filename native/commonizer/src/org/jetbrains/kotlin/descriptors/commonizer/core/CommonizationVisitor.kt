/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.commonizer.cir.CirType
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.*
import org.jetbrains.kotlin.descriptors.commonizer.utils.CommonizedGroupMap

internal class CommonizationVisitor(
    private val root: CirRootNode
) : CirNodeVisitor<Unit, Unit> {
    override fun visitRootNode(node: CirRootNode, data: Unit) {
        check(node === root)
        check(node.common() != null) // root should already be commonized

        node.modules.forEach { module ->
            module.accept(this, Unit)
        }
    }

    override fun visitModuleNode(node: CirModuleNode, data: Unit) {
        node.common() // commonize module

        node.packages.forEach { pkg ->
            pkg.accept(this, Unit)
        }
    }

    override fun visitPackageNode(node: CirPackageNode, data: Unit) {
        node.common() // commonize package

        node.properties.forEach { property ->
            property.accept(this, Unit)
        }

        node.functions.forEach { function ->
            function.accept(this, Unit)
        }

        node.classes.forEach { clazz ->
            clazz.accept(this, Unit)
        }

        node.typeAliases.forEach { typeAlias ->
            typeAlias.accept(this, Unit)
        }
    }

    override fun visitPropertyNode(node: CirPropertyNode, data: Unit) {
        node.common() // commonize property
    }

    override fun visitFunctionNode(node: CirFunctionNode, data: Unit) {
        node.common() // commonize function
    }

    override fun visitClassNode(node: CirClassNode, data: Unit) {
        val commonClass = node.common() // commonized class

        node.constructors.forEach { constructor ->
            constructor.accept(this, Unit)
        }

        node.properties.forEach { property ->
            property.accept(this, Unit)
        }

        node.functions.forEach { function ->
            function.accept(this, Unit)
        }

        node.classes.forEach { clazz ->
            clazz.accept(this, Unit)
        }

        if (commonClass != null) {
            // companion object should have the same FQ name for each target class, then it could be set to common class
            val companionObjectFqName = node.target.mapTo(HashSet()) { it!!.companion }.singleOrNull()
            if (companionObjectFqName != null) {
                val companionObjectNode = root.cache.classes[companionObjectFqName]
                    ?: error("Can't find companion object with FQ name $companionObjectFqName")

                if (companionObjectNode.common() != null) {
                    // companion object has been successfully commonized
                    commonClass.companion = companionObjectFqName
                }
            }

            // find out common (and commonized) supertypes
            val supertypesMap = CommonizedGroupMap<String, CirType>(node.target.size)
            node.target.forEachIndexed { index, clazz ->
                for (supertype in clazz!!.supertypes) {
                    supertypesMap[supertype.fqNameWithTypeParameters][index] = supertype
                }
            }

            for ((_, supertypesGroup) in supertypesMap) {
                val commonSupertype = commonize(supertypesGroup.toList(), TypeCommonizer(root.cache))
                if (commonSupertype != null)
                    commonClass.supertypes.add(commonSupertype)
            }
        }
    }

    override fun visitClassConstructorNode(node: CirClassConstructorNode, data: Unit) {
        node.common() // commonize constructor
    }

    override fun visitTypeAliasNode(node: CirTypeAliasNode, data: Unit) {
        node.common() // commonize type alias
    }
}
