/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.run

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinLightProjectDescriptor

class KotlinConsoleFilterTest : KotlinLightCodeInsightFixtureTestCase() {
    fun testWindowsPath() {
        val filter = KotlinConsoleFilter(project, GlobalSearchScope.allScope(project))
        val line = """w: E:\Work\src\settings\jmx\JMX.kt: (54, 56): This cast can never succeed"""
        val result = filter.applyFilter(line, line.length)!!
        val resultItem = result.resultItems.single()
        assertEquals(3, resultItem.getHighlightStartOffset())
        assertEquals(44, resultItem.getHighlightEndOffset())
        val hyperlinkInfo = resultItem.getHyperlinkInfo() as LocalFileHyperlinkInfo
        assertEquals("E:\\Work\\src\\settings\\jmx\\JMX.kt", hyperlinkInfo.path)
    }

    fun testLinuxPath() {
        val filter = KotlinConsoleFilter(project, GlobalSearchScope.allScope(project))
        val line = """e: /Users/Work/app-web/src/main/kotlin/services/teamDirectory/NewOrEditTeamPageDescriptor.kt: (58, 52): The boolean literal does not conform to the expected type TeamMemberBuilder.() -> Unit"""
        val result = filter.applyFilter(line, line.length)!!
        val resultItem = result.resultItems.single()
        assertEquals(3, resultItem.getHighlightStartOffset())
        assertEquals(102, resultItem.getHighlightEndOffset())
    }

    fun testNoStringIndexOutOfBoundsException() {
        val filter = KotlinConsoleFilter(project, GlobalSearchScope.allScope(project))
        val line = """NewOrEditTeamPageDescriptor.kt: (58, 52): The message with something/looks/like/a/path"""
        assertEquals(null, filter.applyFilter(line, line.length))
    }

    override fun getProjectDescriptor() = KotlinLightProjectDescriptor.INSTANCE!!
}