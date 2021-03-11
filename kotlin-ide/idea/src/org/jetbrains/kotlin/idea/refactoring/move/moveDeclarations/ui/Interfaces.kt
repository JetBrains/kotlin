/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.ui

import com.intellij.openapi.options.ConfigurationException
import com.intellij.refactoring.BaseRefactoringProcessor
import org.jetbrains.kotlin.idea.statistics.KotlinMoveRefactoringFUSCollector.MovedEntity
import org.jetbrains.kotlin.idea.statistics.KotlinMoveRefactoringFUSCollector.MoveRefactoringDestination

internal class ModelResultWithFUSData(
  val processor: BaseRefactoringProcessor,
  val elementsCount: Int,
  val entityToMove: MovedEntity,
  val destination: MoveRefactoringDestination
)

internal interface Model {
    @Throws(ConfigurationException::class)
    fun computeModelResult(throwOnConflicts: Boolean = false): ModelResultWithFUSData

    @Throws(ConfigurationException::class)
    fun computeModelResult(): ModelResultWithFUSData
}