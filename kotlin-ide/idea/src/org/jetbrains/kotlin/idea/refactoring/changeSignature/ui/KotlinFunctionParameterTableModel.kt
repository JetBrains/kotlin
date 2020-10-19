/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.refactoring.changeSignature.ParameterTableModelItemBase
import com.intellij.refactoring.ui.StringTableCellEditor
import com.intellij.ui.BooleanTableCellRenderer
import com.intellij.util.ui.ColumnInfo
import org.jetbrains.kotlin.idea.KotlinBundle.message
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinMethodDescriptor
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinParameterInfo
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

class KotlinFunctionParameterTableModel(
    methodDescriptor: KotlinMethodDescriptor,
    typeContext: PsiElement,
    defaultValueContext: PsiElement
) : KotlinCallableParameterTableModel(
    methodDescriptor,
    typeContext,
    defaultValueContext,
    NameColumn<KotlinParameterInfo, ParameterTableModelItemBase<KotlinParameterInfo>>(typeContext.project),
    TypeColumn<KotlinParameterInfo, ParameterTableModelItemBase<KotlinParameterInfo>>(typeContext.project, KotlinFileType.INSTANCE),
    DefaultValueColumn<KotlinParameterInfo, ParameterTableModelItemBase<KotlinParameterInfo>>(
        typeContext.project,
        KotlinFileType.INSTANCE
    ),
    ReceiverColumn<ParameterTableModelItemBase<KotlinParameterInfo?>>(typeContext.project, methodDescriptor)
) {
    override fun removeRow(idx: Int) {
        if (getRowValue(idx).parameter == receiver) {
            receiver = null
        }

        super.removeRow(idx)
    }

    override var receiver: KotlinParameterInfo?
        get() = (columnInfos[columnCount - 1] as ReceiverColumn<*>).receiver
        set(receiver) {
            (columnInfos[columnCount - 1] as ReceiverColumn<*>).receiver = receiver
        }

    private class ReceiverColumn<TableItem : ParameterTableModelItemBase<KotlinParameterInfo?>>(
        private val project: Project, methodDescriptor: KotlinMethodDescriptor
    ) : ColumnInfoBase<KotlinParameterInfo?, TableItem, Boolean>(
        message("column.name.receiver")
    ) {
        var receiver: KotlinParameterInfo? = methodDescriptor.receiver
        override fun valueOf(item: TableItem): Boolean = item.parameter == receiver

        override fun setValue(item: TableItem, value: Boolean) {
            receiver = if (value) item.parameter else null
        }

        override fun isCellEditable(pParameterTableModelItemBase: TableItem): Boolean = true

        public override fun doCreateRenderer(item: TableItem): TableCellRenderer = BooleanTableCellRenderer()

        public override fun doCreateEditor(o: TableItem): TableCellEditor = StringTableCellEditor(project)
    }

    companion object {
        fun isReceiverColumn(column: ColumnInfo<*, *>?): Boolean = column is ReceiverColumn<*>
    }
}