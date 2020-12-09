/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.commonizer.api.LeafCommonizerTarget
import org.jetbrains.kotlin.descriptors.commonizer.ModulesProvider.ModuleInfo
import org.jetbrains.kotlin.descriptors.commonizer.CommonizerParameters
import org.jetbrains.kotlin.descriptors.commonizer.EmptyModulesProvider
import org.jetbrains.kotlin.descriptors.commonizer.TargetProvider
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirClass
import org.jetbrains.kotlin.descriptors.commonizer.cir.factory.*
import org.jetbrains.kotlin.descriptors.commonizer.utils.intern
import org.jetbrains.kotlin.descriptors.commonizer.utils.internedClassId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.NullableLazyValue
import org.jetbrains.kotlin.storage.StorageManager

/**
 * N.B. Limitations on C/Obj-C interop.
 *
 * [Case 1]: An interop library with two fragments for two targets. The first fragment has a forward declaration of classifier A.
 * The second one has a definition of class A. Both fragments have a top-level callable (ex: function)
 * with the same signature that refers to type "A" as its return type.
 *
 * What will happen: Forward declarations will be ignored during building CIR merged tree. So the node for class A
 * will contain CirClass "A" for the second target only. This node will not succeed in commonization, and no common class
 * declaration will be produced. As a result the top-level callable will not be commonized, as it refers to the type "A"
 * that is not formally commonized.
 *
 * This is not strictly correct: The classifier "A" exists in both targets though in different form. So if the user
 * would write shared source code that uses "A" and the callable, then this code would successfully compile against both targets.
 *
 * The reason why commonization of such classifiers is not supported yet is that this is quite a rare case that requires
 * a complex implementation with potential performance penalty.
 *
 * [Case 2]: A library with two fragments for two targets. The first fragment is interop. The second one is not.
 * Similarly to case 1, the 1st fragment has a forward declaration of a classifier, and the 2nd has a real classifier.
 *
 * At the moment, this is an exotic case. It could happen if someone tries to commonize an MPP library for Native and non-Native
 * targets (which is not supported yet), or a Native library where one fragment is produced via C-interop tool and the other one
 * is compiled from Kotlin/Native source code (not sure this should be supported at all).
 */

class CirTreeMerger {
    class CirTreeMergeResult(
        val root: CirRootNode,
        val missingModuleInfos: Map<LeafCommonizerTarget, Collection<ModuleInfo>>
    )
}

fun mergeTree(
    storageManager: StorageManager,
    classifiers: CirKnownClassifiers,
    parameters: CommonizerParameters
): CirTreeMerger.CirTreeMergeResult {
    val rootMerger = RootMerger(
        targetMerger = TargetMerger(
            moduleMerger = ModuleMerger(
                packageMerger = PackageMerger(
                    propertyMerger = PropertyMerger,
                    functionMerger = FunctionMerger,
                    classMerger = ClassMerger(
                        classConstructorMerger = ClassConstructorMerger,
                        propertyMerger = PropertyMerger,
                        functionMerger = FunctionMerger,
                    ),
                    typeAliasMerger = TypeAliasMerger
                )
            )
        )
    )

    return rootMerger.processRoot(storageManager, classifiers, parameters)
}

fun mergeDependencyTree(
    storageManager: StorageManager,
    classifiers: CirKnownClassifiers,
    parameters: CommonizerParameters
): CirTreeMerger.CirTreeMergeResult {

    /* Build parameters for commonizing only dependencies */
    val dependencyParameters = CommonizerParameters(
        statsCollector = parameters.statsCollector,
        progressLogger = parameters.progressLogger
    ).apply {
        dependeeModulesProvider = parameters.dependeeModulesProvider
        parameters.targetProviders.forEach { targetProvider ->
            addTarget(
                targetProvider.copy(
                    modulesProvider = targetProvider.dependeeModulesProvider ?: EmptyModulesProvider,
                    dependeeModulesProvider = null
                )
            )
        }
    }

    /*  Build merger for types only */
    val rootMerger = RootMerger(
        targetMerger = TargetMerger(
            moduleMerger = ModuleMerger(
                packageMerger = PackageMerger(
                    propertyMerger = null,
                    functionMerger = null,
                    classMerger = ClassMerger(
                        classConstructorMerger = null,
                        propertyMerger = null,
                        functionMerger = null,
                    ),
                    typeAliasMerger = TypeAliasMerger
                )
            )
        )
    )

    return rootMerger.processRoot(storageManager, classifiers, dependencyParameters)
}
