/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.commonizer.cir.CirClass
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirType
import org.jetbrains.kotlin.descriptors.commonizer.cir.factory.CirTypeFactory
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.*
import org.jetbrains.kotlin.descriptors.commonizer.utils.*
import org.jetbrains.kotlin.descriptors.commonizer.utils.compactMapNotNull
import org.jetbrains.kotlin.descriptors.commonizer.utils.internedClassId
import org.jetbrains.kotlin.name.ClassId

internal class CommonizationVisitor(
    private val classifiers: CirKnownClassifiers,
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
        val commonClass = node.commonDeclaration() // commonized class

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

        if (commonClass != null) {
            // companion object should have the same name for each target class, then it could be set to common class
            val companionObjectName = node.targetDeclarations.mapTo(HashSet()) { it!!.companion }.singleOrNull()
            if (companionObjectName != null) {
                val companionObjectClassId = internedClassId(node.classId, companionObjectName)
                val companionObjectNode = classifiers.commonized.classNode(companionObjectClassId)
                    ?: error("Can't find companion object with class ID $companionObjectClassId")

                if (companionObjectNode.commonDeclaration() != null) {
                    // companion object has been successfully commonized
                    commonClass.companion = companionObjectName
                }
            }

            // find out common (and commonized) supertypes
            commonClass.commonizeSupertypes(node.classId, node.collectCommonSupertypes())
        }
    }

    override fun visitClassConstructorNode(node: CirClassConstructorNode, data: Unit) {
        node.commonDeclaration() // commonize constructor
    }

    override fun visitTypeAliasNode(node: CirTypeAliasNode, data: Unit) {
        val commonClassifier = node.commonDeclaration() // commonize type alias

        if (commonClassifier is CirClass) {
            // find out common (and commonized) supertypes
            commonClassifier.commonizeSupertypes(node.classId, node.collectCommonSupertypes())
        }
    }

    private fun CirClassNode.collectCommonSupertypes(): Map<CirType, CommonizedGroup<CirType>> {
        val supertypesMap: MutableMap<CirType, CommonizedGroup<CirType>> = linkedMapOf() // preserve supertype order
        for ((index, clazz) in targetDeclarations.withIndex()) {
            for (supertype in clazz!!.supertypes) {
                supertypesMap.getOrPut(supertype) { CommonizedGroup(targetDeclarations.size) }[index] = supertype
            }
        }
        return supertypesMap
    }

    private fun CirTypeAliasNode.collectCommonSupertypes(): Map<CirType, CommonizedGroup<CirType>>? {
        val supertypesMap: MutableMap<CirType, CommonizedGroup<CirType>> = linkedMapOf() // preserve supertype order
        for ((index, typeAlias) in targetDeclarations.withIndex()) {
            val expandedClassId = typeAlias!!.expandedType.classifierId
            if (classifiers.commonDependeeLibraries.hasClassifier(expandedClassId))
                return null // this case is not supported yet

            val expandedClassNode = classifiers.commonized.classNode(expandedClassId) ?: return null
            val expandedClass = expandedClassNode.targetDeclarations[index]
                ?: error("Can't find expanded class with class ID $expandedClassId and index $index for type alias $classId")

            for (supertype in expandedClass.supertypes) {
                supertypesMap.getOrPut(supertype) { CommonizedGroup(targetDeclarations.size) }[index] = supertype
            }
        }
        return supertypesMap
    }

    private fun CirClass.commonizeSupertypes(
        classId: ClassId,
        supertypesMap: Map<CirType, CommonizedGroup<CirType>>?
    ) {
        val commonSupertypes = supertypesMap?.values?.compactMapNotNull { supertypesGroup ->
            commonize(supertypesGroup, TypeCommonizer(classifiers))
        }.orEmpty()

        setSupertypes(
            if (commonSupertypes.isEmpty() && classId !in SPECIAL_CLASS_WITHOUT_SUPERTYPES_CLASS_IDS)
                listOf(CirTypeFactory.StandardTypes.ANY)
            else
                commonSupertypes
        )
    }
}
