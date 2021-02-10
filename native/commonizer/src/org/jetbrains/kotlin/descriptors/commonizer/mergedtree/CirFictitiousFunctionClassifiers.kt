/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree

import gnu.trove.THashMap
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirEntityId
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirName
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirPackageName
import org.jetbrains.kotlin.types.Variance

object CirFictitiousFunctionClassifiers : CirProvidedClassifiers {
    private const val MIN_ARITY = 0
    private const val MAX_ARITY = 255

    private val FUNCTION_PREFIXES = arrayOf("Function", "SuspendFunction")
    private val PACKAGE_NAME = CirPackageName.create("kotlin")

    private val classifiers: Map<CirEntityId, CirProvided.Class> = THashMap<CirEntityId, CirProvided.Class>().apply {
        (MIN_ARITY..MAX_ARITY).forEach { arity ->
            FUNCTION_PREFIXES.forEach { prefix ->
                buildFictitiousFunctionClass(prefix, arity, this::set)
            }
        }
    }

    override fun hasClassifier(classifierId: CirEntityId) = classifierId in classifiers
    override fun classifier(classifierId: CirEntityId): CirProvided.Class? = classifiers[classifierId]

    private inline fun buildFictitiousFunctionClass(prefix: String, arity: Int, consumer: (CirEntityId, CirProvided.Class) -> Unit) {
        val typeParameters = List(arity + 1) { index ->
            CirProvided.TypeParameter(
                index = index,
                variance = if (index == arity) Variance.OUT_VARIANCE else Variance.IN_VARIANCE
            )
        }

        val classId = CirEntityId.create(PACKAGE_NAME, CirName.create("$prefix$arity"))
        val clazz = CirProvided.Class(typeParameters, DescriptorVisibilities.PUBLIC)

        consumer(classId, clazz)
    }
}
