/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.formatter

import com.intellij.application.options.CodeStyleAbstractPanel
import com.intellij.openapi.application.ApplicationBundle
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.util.NlsContexts
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.ui.OptionGroup
import com.intellij.ui.components.JBScrollPane
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleSettings
import org.jetbrains.kotlin.idea.core.formatter.KotlinPackageEntryTable
import java.awt.*
import javax.swing.*
import javax.swing.table.AbstractTableModel

class ImportSettingsPanelWrapper(settings: CodeStyleSettings) : CodeStyleAbstractPanel(KotlinLanguage.INSTANCE, null, settings) {
    private val importsPanel = ImportSettingsPanel()

    override fun getRightMargin() = throw UnsupportedOperationException()

    override fun createHighlighter(scheme: EditorColorsScheme) = throw UnsupportedOperationException()

    override fun getFileType() = throw UnsupportedOperationException()

    override fun getPreviewText(): String? = null

    override fun apply(settings: CodeStyleSettings) = importsPanel.apply(settings.kotlinCustomSettings)

    override fun isModified(settings: CodeStyleSettings) = importsPanel.isModified(settings.kotlinCustomSettings)

    override fun getPanel() = importsPanel

    override fun resetImpl(settings: CodeStyleSettings) {
        importsPanel.reset(settings.kotlinCustomSettings)
    }

    override fun getTabTitle() = ApplicationBundle.message("title.imports")
}

class ImportSettingsPanel : JPanel() {
    private val cbImportNestedClasses = JCheckBox(KotlinBundle.message("formatter.checkbox.text.insert.imports.for.nested.classes"))

    private val starImportLayoutPanel = KotlinStarImportLayoutPanel()
    private val importOrderLayoutPanel = KotlinImportOrderLayoutPanel()

    private val nameCountToUseStarImportSelector = NameCountToUseStarImportSelector(
        KotlinBundle.message("formatter.title.top.level.symbols"),
        KotlinCodeStyleSettings.DEFAULT_NAME_COUNT_TO_USE_STAR_IMPORT,
    )

    private val nameCountToUseStarImportForMembersSelector = NameCountToUseStarImportSelector(
        KotlinBundle.message("formatter.title.java.statics.and.enum.members"),
        KotlinCodeStyleSettings.DEFAULT_NAME_COUNT_TO_USE_STAR_IMPORT_FOR_MEMBERS,
    )

    init {
        layout = BorderLayout()
        add(JBScrollPane(JPanel(GridBagLayout()).apply {
            val constraints = GridBagConstraints().apply {
                weightx = 1.0
                insets = Insets(0, 10, 10, 10)
                fill = GridBagConstraints.HORIZONTAL
                gridy = 0
            }

            add(nameCountToUseStarImportSelector.createPanel(), constraints.apply { gridy++ })

            add(nameCountToUseStarImportForMembersSelector.createPanel(), constraints.apply { gridy++ })

            add(
                OptionGroup(KotlinBundle.message("formatter.title.other")).apply { add(cbImportNestedClasses) }.createPanel(),
                constraints.apply { gridy++ }
            )

            add(starImportLayoutPanel, constraints.apply {
                gridy++
                fill = GridBagConstraints.BOTH
                weighty = 1.0
            })

            add(importOrderLayoutPanel, constraints.apply {
                gridy++
                fill = GridBagConstraints.BOTH
                weighty = 1.0
            })
        }), BorderLayout.CENTER)
    }

    fun reset(settings: KotlinCodeStyleSettings) {
        nameCountToUseStarImportSelector.value = settings.NAME_COUNT_TO_USE_STAR_IMPORT
        nameCountToUseStarImportForMembersSelector.value = settings.NAME_COUNT_TO_USE_STAR_IMPORT_FOR_MEMBERS

        cbImportNestedClasses.isSelected = settings.IMPORT_NESTED_CLASSES

        starImportLayoutPanel.packageTable.copyFrom(settings.PACKAGES_TO_USE_STAR_IMPORTS)
        (starImportLayoutPanel.layoutTable.model as AbstractTableModel).fireTableDataChanged()
        if (starImportLayoutPanel.layoutTable.rowCount > 0) {
            starImportLayoutPanel.layoutTable.selectionModel.setSelectionInterval(0, 0)
        }

        importOrderLayoutPanel.packageTable.copyFrom(settings.PACKAGES_IMPORT_LAYOUT)
        (importOrderLayoutPanel.layoutTable.model as AbstractTableModel).fireTableDataChanged()
        if (importOrderLayoutPanel.layoutTable.rowCount > 0) {
            importOrderLayoutPanel.layoutTable.selectionModel.setSelectionInterval(0, 0)
        }

        importOrderLayoutPanel.recomputeAliasesCheckbox()
    }

    fun apply(settings: KotlinCodeStyleSettings) {
        settings.NAME_COUNT_TO_USE_STAR_IMPORT = nameCountToUseStarImportSelector.value
        settings.NAME_COUNT_TO_USE_STAR_IMPORT_FOR_MEMBERS = nameCountToUseStarImportForMembersSelector.value
        settings.IMPORT_NESTED_CLASSES = cbImportNestedClasses.isSelected
        settings.PACKAGES_TO_USE_STAR_IMPORTS.copyFrom(getCopyWithoutEmptyPackages(starImportLayoutPanel.packageTable))
        settings.PACKAGES_IMPORT_LAYOUT.copyFrom(importOrderLayoutPanel.packageTable)
    }

    fun isModified(settings: KotlinCodeStyleSettings): Boolean {
        return with(settings) {
            var isModified = false
            isModified = isModified || nameCountToUseStarImportSelector.value != NAME_COUNT_TO_USE_STAR_IMPORT
            isModified = isModified || nameCountToUseStarImportForMembersSelector.value != NAME_COUNT_TO_USE_STAR_IMPORT_FOR_MEMBERS
            isModified = isModified || isModified(cbImportNestedClasses, IMPORT_NESTED_CLASSES)
            isModified = isModified ||
                    isModified(getCopyWithoutEmptyPackages(starImportLayoutPanel.packageTable), PACKAGES_TO_USE_STAR_IMPORTS)

            isModified = isModified || isModified(importOrderLayoutPanel.packageTable, PACKAGES_IMPORT_LAYOUT)

            isModified
        }
    }

    companion object {
        private fun isModified(checkBox: JCheckBox, value: Boolean): Boolean {
            return checkBox.isSelected != value
        }

        private fun isModified(list: KotlinPackageEntryTable, table: KotlinPackageEntryTable): Boolean {
            if (list.entryCount != table.entryCount) {
                return true
            }

            for (i in 0 until list.entryCount) {
                val entry1 = list.getEntryAt(i)
                val entry2 = table.getEntryAt(i)
                if (entry1 != entry2) {
                    return true
                }
            }

            return false
        }

        private fun getCopyWithoutEmptyPackages(table: KotlinPackageEntryTable): KotlinPackageEntryTable {
            try {
                val copy = table.clone()
                copy.removeEmptyPackages()
                return copy
            } catch (ignored: CloneNotSupportedException) {
                throw IllegalStateException("Clone should be supported")
            }
        }
    }

    private class NameCountToUseStarImportSelector(@NlsContexts.BorderTitle title: String, default: Int) : OptionGroup(title) {
        private val rbUseSingleImports = JRadioButton(KotlinBundle.message("formatter.button.text.use.single.name.import"))
        private val rbUseStarImports = JRadioButton(KotlinBundle.message("formatter.button.text.use.import.with"))
        private val rbUseStarImportsIfAtLeast = JRadioButton(KotlinBundle.message("formatter.button.text.use.import.with.when.at.least"))
        private val starImportLimitModel = SpinnerNumberModel(default, 2, 100, 1)
        private val starImportLimitField = JSpinner(starImportLimitModel)

        init {
            ButtonGroup().apply {
                add(rbUseSingleImports)
                add(rbUseStarImports)
                add(rbUseStarImportsIfAtLeast)
            }

            add(rbUseSingleImports, true)
            add(rbUseStarImports, true)
            val jPanel = JPanel(GridBagLayout())
            add(jPanel.apply {
                val constraints = GridBagConstraints().apply { gridx = GridBagConstraints.RELATIVE }
                this.add(rbUseStarImportsIfAtLeast, constraints)
                this.add(starImportLimitField, constraints)
                this.add(
                    JLabel(KotlinBundle.message("formatter.text.names.used")),
                    constraints.apply { fill = GridBagConstraints.HORIZONTAL; weightx = 1.0 })
            }, true)

            fun updateEnabled() {
                starImportLimitField.isEnabled = rbUseStarImportsIfAtLeast.isSelected
            }
            rbUseStarImportsIfAtLeast.addChangeListener { updateEnabled() }
            updateEnabled()
        }

        var value: Int
            get() {
                return when {
                    rbUseSingleImports.isSelected -> Int.MAX_VALUE
                    rbUseStarImports.isSelected -> 1
                    else -> starImportLimitModel.number as Int
                }
            }
            set(value) {
                when {
                    value > MAX_VALUE -> rbUseSingleImports.isSelected = true

                    value < MIN_VALUE -> rbUseStarImports.isSelected = true

                    else -> {
                        rbUseStarImportsIfAtLeast.isSelected = true
                        starImportLimitField.value = value
                    }
                }
            }

        companion object {
            private const val MIN_VALUE = 2
            private const val MAX_VALUE = 100
        }
    }
}
