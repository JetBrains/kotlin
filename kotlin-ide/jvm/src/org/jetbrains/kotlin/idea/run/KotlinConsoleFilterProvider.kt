/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.run

import com.intellij.execution.filters.ConsoleFilterProviderEx
import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.HyperlinkInfo
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.search.GlobalSearchScope

class KotlinConsoleFilterProvider : ConsoleFilterProviderEx {
    override fun getDefaultFilters(project: Project): Array<Filter> {
        return getDefaultFilters(project, GlobalSearchScope.allScope(project))
    }

    override fun getDefaultFilters(project: Project, scope: GlobalSearchScope): Array<Filter> {
        return arrayOf(KotlinConsoleFilter(project, scope))
    }
}

class KotlinConsoleFilter(val project: Project, val scope: GlobalSearchScope) : Filter {
    override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
        val messageSuffix = pattern.find(line) ?: return null
        val pathEnd = messageSuffix.groups[1]!!.range.last + 1
        val pathWithPrefix = line.substring(0, pathEnd)
        val pathStart = pathWithPrefix.indexOf(":\\").takeIf { it >= 0 }?.minus(1)
            ?: pathWithPrefix.indexOf("/").takeIf { it >= 0 }
            ?: return null
        val path = pathWithPrefix.substring(pathStart)
        val lineNumber = Integer.parseInt(messageSuffix.groupValues[2]) - 1
        val column = Integer.parseInt(messageSuffix.groupValues[3]) - 1
        val attrs = EditorColorsManager.getInstance().globalScheme.getAttributes(CodeInsightColors.HYPERLINK_ATTRIBUTES)

        val offset = entireLength - line.length
        return Filter.Result(offset + pathStart, offset + messageSuffix.range.last, LocalFileHyperlinkInfo(path, lineNumber, column), attrs)
    }

    companion object {
        private val pattern = Regex("""(\.kts?): \((\d+), (\d+)\):""")
    }
}

class LocalFileHyperlinkInfo(val path: String, val line: Int, val column: Int) : HyperlinkInfo {
    override fun navigate(project: Project) {
        val f = LocalFileSystem.getInstance().findFileByPath(FileUtil.toSystemIndependentName(path))
        if (f != null) {
            OpenFileDescriptor(project, f, line, column).navigate(true)
        }
    }
}
