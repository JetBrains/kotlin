/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.mergedtree

import kotlinx.metadata.KmTypeParameter
import org.jetbrains.kotlin.commonizer.CommonizerParameters
import org.jetbrains.kotlin.commonizer.CommonizerTarget
import org.jetbrains.kotlin.commonizer.LeafCommonizerTarget
import org.jetbrains.kotlin.commonizer.ModulesProvider.ModuleInfo
import org.jetbrains.kotlin.commonizer.mergedtree.mergers.*
import org.jetbrains.kotlin.commonizer.mergedtree.mergers.ClassesToProcess
import org.jetbrains.kotlin.commonizer.mergedtree.mergers.ModuleMerger
import org.jetbrains.kotlin.commonizer.mergedtree.mergers.PropertyMerger
import org.jetbrains.kotlin.commonizer.mergedtree.mergers.RootMerger
import org.jetbrains.kotlin.commonizer.mergedtree.mergers.TargetMerger
import org.jetbrains.kotlin.commonizer.metadata.CirTypeResolver
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

class CirTreeMergeResult(
    val root: CirRootNode,
    val missingModuleInfos: Map<CommonizerTarget, Collection<ModuleInfo>>
)

internal class CirTargetMergingContext(
    val storageManager: StorageManager,
    val classifiers: CirKnownClassifiers,
    val parameters: CommonizerParameters,
    val targetIndex: Int,
    val typeResolver: CirTypeResolver
) {
    val targets = parameters.targetProviders.size
}

internal fun CirTargetMergingContext.create(classEntry: ClassesToProcess.ClassEntry): CirTargetMergingContext = when (classEntry) {
    is ClassesToProcess.ClassEntry.RegularClassEntry -> create(classEntry.clazz.typeParameters)
    is ClassesToProcess.ClassEntry.EnumEntry -> this
}

internal fun CirTargetMergingContext.create(typeParameters: List<KmTypeParameter>): CirTargetMergingContext {
    val newTypeResolver = typeResolver.create(typeParameters)
    return if (newTypeResolver !== typeResolver)
        CirTargetMergingContext(storageManager, classifiers, parameters, targetIndex, newTypeResolver)
    else
        this
}

fun mergeCirTree(
    storageManager: StorageManager,
    classifiers: CirKnownClassifiers,
    parameters: CommonizerParameters
): CirTreeMergeResult {
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
