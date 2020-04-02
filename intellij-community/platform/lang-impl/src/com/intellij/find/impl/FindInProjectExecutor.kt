package com.intellij.find.impl

import com.intellij.find.FindModel
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ex.ProgressIndicatorEx
import com.intellij.usageView.UsageInfo
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer

open class FindInProjectExecutor {

    companion object {
        fun getInstance(): FindInProjectExecutor {
            return ServiceManager.getService(FindInProjectExecutor::class.java)
        }
    }

    open fun createTableModel(): DefaultTableModel? {
        return null
    }

    open fun createTableCellRenderer(): TableCellRenderer? {
        return null
    }

    open fun getUsageInfo(value: Any): UsageInfo? {
        return null
    }

    open fun startSearch(
        progressIndicator: ProgressIndicatorEx,
        model: DefaultTableModel,
        findModel: FindModel,
        project: Project
    ): Boolean {
        return false
    }
}