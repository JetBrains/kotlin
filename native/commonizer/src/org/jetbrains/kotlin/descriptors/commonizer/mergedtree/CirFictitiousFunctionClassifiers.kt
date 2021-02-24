/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree

import gnu.trove.THashSet
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirEntityId
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirName
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirPackageName

object CirFictitiousFunctionClassifiers : CirProvidedClassifiers {
    private const val MIN_ARITY = 0
    private const val MAX_ARITY = 255

    private val FUNCTION_PREFIXES = arrayOf("Function", "SuspendFunction")
    private val PACKAGE_NAME = CirPackageName.create("kotlin")

    private val classifiers: Set<CirEntityId> = THashSet<CirEntityId>().apply {
        (MIN_ARITY..MAX_ARITY).forEach { arity ->
            FUNCTION_PREFIXES.forEach { prefix ->
                this += buildFictitiousFunctionClass(prefix, arity)
            }
        }
    }

    override fun hasClassifier(classifierId: CirEntityId): Boolean = classifierId in classifiers

    private fun buildFictitiousFunctionClass(prefix: String, arity: Int): CirEntityId =
        CirEntityId.create(PACKAGE_NAME, CirName.create("$prefix$arity"))
}
