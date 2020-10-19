/*
 * Copyright 2010-2013 JetBrains s.r.o.
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
package org.jetbrains.kotlin.idea.refactoring.changeSignature.ui

import com.intellij.openapi.ui.ComboBoxTableRenderer
import com.intellij.psi.PsiElement
import com.intellij.refactoring.changeSignature.ParameterTableModelItemBase
import com.intellij.util.ui.ColumnInfo
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinMethodDescriptor
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinParameterInfo
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinValVar
import javax.swing.DefaultCellEditor
import javax.swing.JComboBox
import javax.swing.JTable
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

class KotlinPrimaryConstructorParameterTableModel(
    methodDescriptor: KotlinMethodDescriptor,
    typeContext: PsiElement,
    defaultValueContext: PsiElement
) : KotlinCallableParameterTableModel(
    methodDescriptor,
    typeContext,
    defaultValueContext,
    ValVarColumn(),
    NameColumn<KotlinParameterInfo, ParameterTableModelItemBase<KotlinParameterInfo>>(typeContext.project),
    TypeColumn<KotlinParameterInfo, ParameterTableModelItemBase<KotlinParameterInfo>>(typeContext.project, KotlinFileType.INSTANCE),
    DefaultValueColumn<KotlinParameterInfo, ParameterTableModelItemBase<KotlinParameterInfo>>(
        typeContext.project,
        KotlinFileType.INSTANCE,
    )
) {
    private class ValVarColumn : ColumnInfoBase<KotlinParameterInfo, ParameterTableModelItemBase<KotlinParameterInfo>, KotlinValVar>(
        KotlinBundle.message("column.name.val.var")
    ) {
        override fun isCellEditable(item: ParameterTableModelItemBase<KotlinParameterInfo>): Boolean {
            return !item.isEllipsisType && item.parameter.isNewParameter
        }

        override fun valueOf(item: ParameterTableModelItemBase<KotlinParameterInfo>): KotlinValVar = item.parameter.valOrVar

        override fun setValue(item: ParameterTableModelItemBase<KotlinParameterInfo>, value: KotlinValVar) {
            item.parameter.valOrVar = value
        }

        override fun doCreateRenderer(item: ParameterTableModelItemBase<KotlinParameterInfo>): TableCellRenderer {
            return ComboBoxTableRenderer(KotlinValVar.values())
        }

        override fun doCreateEditor(item: ParameterTableModelItemBase<KotlinParameterInfo>): TableCellEditor {
            return DefaultCellEditor(JComboBox<ParameterTableModelItemBase<KotlinParameterInfo>>())
        }

        override fun getWidth(table: JTable): Int = table.getFontMetrics(table.font).stringWidth(name) + 8
    }

    companion object {
        fun isValVarColumn(column: ColumnInfo<*, *>?): Boolean = column is ValVarColumn
    }
}