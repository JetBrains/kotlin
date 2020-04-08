package com.intellij.find.impl

import com.intellij.find.FindModel
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ex.ProgressIndicatorEx
import com.intellij.usages.FindUsagesProcessPresentation
import com.intellij.usages.UsageInfo2UsageAdapter
import com.intellij.usages.UsageInfoAdapter
import javax.swing.table.TableCellRenderer

open class FindInProjectExecutor {

    companion object {
        fun getInstance(): FindInProjectExecutor {
            return ServiceManager.getService(FindInProjectExecutor::class.java)
        }
    }

    open fun createTableCellRenderer(): TableCellRenderer? {
        return null
    }

    open fun findUsages(
        project: Project,
        progressIndicator: ProgressIndicatorEx,
        presentation: FindUsagesProcessPresentation,
        findModel: FindModel,
        filesToScanInitially: Set<VirtualFile>,
        onResult: (UsageInfoAdapter) -> Boolean
    ) {
        FindInProjectUtil.findUsages(findModel, project, presentation, filesToScanInitially) { info ->
            val usage = UsageInfo2UsageAdapter.CONVERTER.`fun`(info) as UsageInfoAdapter
            usage.presentation.icon // cache icon

            onResult(usage)
        }
    }
}