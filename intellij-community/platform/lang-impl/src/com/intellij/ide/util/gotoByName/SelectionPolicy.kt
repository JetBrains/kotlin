/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.ide.util.gotoByName

/**
 * @author peter
 */
internal interface SelectionPolicy {
  fun performSelection(popup: ChooseByNameBase, model: SmartPointerListModel<Any>): List<Int>
}

internal fun fromIndex(index: Int) = if (index <= 0) SelectMostRelevant else SelectIndex(index)

internal object SelectMostRelevant : SelectionPolicy {
  override fun performSelection(popup: ChooseByNameBase, model: SmartPointerListModel<Any>) =
    listOf(popup.calcSelectedIndex(model.items.toTypedArray(), popup.trimmedText))
}

internal data class SelectIndex(private val selectedIndex: Int) : SelectionPolicy {
  override fun performSelection(popup: ChooseByNameBase, model: SmartPointerListModel<Any>) = listOf(selectedIndex)
}

internal object PreserveSelection : SelectionPolicy {
  override fun performSelection(popup: ChooseByNameBase, model: SmartPointerListModel<Any>): List<Int> {
    val chosenElements = popup.currentChosenInfo?.chosenElements.orEmpty()
    val items = model.items
    val preserved = items.indices.filter { items[it] in chosenElements }
    return if (preserved.isNotEmpty()) preserved else SelectMostRelevant.performSelection(popup, model)
  }
}

internal data class SelectionSnapshot(private val pattern: String, internal val chosenElements: Set<Any>) {
  fun hasSamePattern(popup: ChooseByNameBase) = popup.transformPattern(pattern) == popup.transformPattern(popup.trimmedText)
}