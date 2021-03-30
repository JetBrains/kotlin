/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.mergedtree.mergers

import kotlinx.metadata.KmModuleFragment
import kotlinx.metadata.KmTypeParameter
import org.jetbrains.kotlin.commonizer.cir.CirPackage
import org.jetbrains.kotlin.commonizer.cir.CirPackageName
import org.jetbrains.kotlin.commonizer.mergedtree.*
import org.jetbrains.kotlin.commonizer.mergedtree.CirTargetMergingContext
import org.jetbrains.kotlin.commonizer.mergedtree.buildPackageNode

internal class PackageMerger(
    private val propertyMerger: PropertyMerger?,
    private val functionMerger: FunctionMerger?,
    private val classMerger: ClassMerger?,
    private val typeAliasMerger: TypeAliasMerger?
) {
    fun processFragments(
        context: CirTargetMergingContext,
        moduleNode: CirModuleNode,
        fragments: Collection<KmModuleFragment>,
        packageName: CirPackageName
    ): Unit = with(context) {
        val packageNode: CirPackageNode = moduleNode.packages.getOrPut(packageName) {
            buildPackageNode(storageManager, targets)
        }
        packageNode.targetDeclarations[context.targetIndex] = CirPackage.create(packageName)

        val classesToProcess = ClassesToProcess()
        fragments.forEach { fragment ->
            classesToProcess.addClassesFromFragment(fragment)

            fragment.pkg?.let { pkg ->
                pkg.properties.forEach { property ->
                    val propertyContext = context.create(property.typeParameters)
                    propertyMerger?.processProperty(propertyContext, packageNode, property)
                }
                pkg.functions.forEach { function ->
                    val functionContext = context.create(function.typeParameters)
                    functionMerger?.processFunction(functionContext, packageNode, function)
                }
                pkg.typeAliases.forEach { typeAlias ->
                    val typeAliasContext = context.create(typeAlias.typeParameters)
                    typeAliasMerger?.processTypeAlias(typeAliasContext, packageNode, typeAlias)
                }
            }
        }

        classesToProcess.forEachClassInScope(parentClassId = null) { classEntry ->
            val classContext = context.create(classEntry)
            classMerger?.processClass(classContext, packageNode, classEntry, classesToProcess)
        }
    }
}
