/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.wizard.ui

import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SeparatorWithText
import com.intellij.ui.components.JBList
import java.awt.Component
import javax.swing.JList
import javax.swing.ListCellRenderer

class ListWithSeparators<V>(
    private val groups: List<ListGroup<V>>,
    render: ColoredListCellRenderer<V>.(V) -> Unit,
    onValueSelected: (V?) -> Unit
) : JBList<V>() {
    private val values: List<V> = groups.flatMap { it.values }

    @OptIn(ExperimentalStdlibApi::class)
    private val elementIndexToPreviousSeparator: Map<Int, ListGroup<V>> = groups
        .scan(0) { startElement, group -> startElement + group.values.size }
        .dropLast(1)
        .mapIndexed { groupIndex, startElement -> startElement to groups[groupIndex] }
        .toMap()

    private fun updateValues(newValues: List<V>) {
        setModel(createDefaultListModel(newValues))
        if (newValues.isNotEmpty()) {
            selectedIndex = 0
        }
    }

    init {
        updateValues(values)

        selectionModel.addListSelectionListener { e ->
            if (e.valueIsAdjusting) return@addListSelectionListener
            val selectedValue = when (selectedIndex) {
                -1 -> null
                else -> this.model.getElementAt(selectedIndex)
            }
            onValueSelected(selectedValue)
        }

        cellRenderer = object : ListCellRenderer<V> {
            val separator = SeparatorWithText()
            private val simpleRenderer = object : ColoredListCellRenderer<V>() {
                public override fun customizeCellRenderer(
                    list: JList<out V>,
                    value: V?,
                    index: Int,
                    selected: Boolean,
                    hasFocus: Boolean
                ) {
                    render(value ?: return)
                }
            }

            override fun getListCellRendererComponent(
                list: JList<out V>?,
                value: V,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): Component {
                return borderPanel {
                    background = this@ListWithSeparators.background
                    elementIndexToPreviousSeparator[index]?.let { group ->
                        addToTop(separator.apply { caption = group.title })
                    }
                    addToCenter(simpleRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus))
                }
            }
        }
    }

    data class ListGroup<V>(
        val title: String,
        val values: List<V>
    )
}

