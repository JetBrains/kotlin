/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.commonizer.CommonizedGroupMap
import org.jetbrains.kotlin.descriptors.commonizer.fqNameWithTypeParameters
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.*
import org.jetbrains.kotlin.types.KotlinType
import java.lang.IllegalStateException

internal class CommonizationVisitor(
    private val root: RootNode
) : NodeVisitor<Unit, Unit> {
    override fun visitRootNode(node: RootNode, data: Unit) {
        check(node === root)
        check(node.common() != null) // root should already be commonized

        node.modules.forEach { module ->
            module.accept(this, Unit)
        }
    }

    override fun visitModuleNode(node: ModuleNode, data: Unit) {
        node.common() // commonize module

        node.packages.forEach { pkg ->
            pkg.accept(this, Unit)
        }
    }

    override fun visitPackageNode(node: PackageNode, data: Unit) {
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

    override fun visitPropertyNode(node: PropertyNode, data: Unit) {
        node.common() // commonize property
    }

    override fun visitFunctionNode(node: FunctionNode, data: Unit) {
        node.common() // commonize function
    }

    override fun visitClassNode(node: ClassNode, data: Unit) {
        val commonClass = node.common() as CommonClassDeclaration? // commonize class

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
                    ?: throw IllegalStateException("Can't find companion object with FQ name $companionObjectFqName")

                if (companionObjectNode.common() != null) {
                    // companion object has been successfully commonized
                    commonClass.companion = companionObjectFqName
                }
            }

            // find out common (and commonized) supertypes
            val supertypesMap = CommonizedGroupMap<String, KotlinType>(node.target.size)
            node.target.forEachIndexed { index, clazz ->
                for (supertype in clazz!!.supertypes) {
                    supertypesMap[supertype.fqNameWithTypeParameters][index] = supertype
                }
            }

            for ((_, supertypesGroup) in supertypesMap) {
                val commonSupertype = commonize(supertypesGroup.toList(), TypeCommonizer.default(root.cache))
                if (commonSupertype != null)
                    commonClass.supertypes += commonSupertype
            }
        }
    }

    override fun visitClassConstructorNode(node: ClassConstructorNode, data: Unit) {
        node.common() // commonize constructor
    }

    override fun visitTypeAliasNode(node: TypeAliasNode, data: Unit) {
        node.common() // commonize type alias
    }
}
