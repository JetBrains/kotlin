/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

    override fun getProjectDescriptor() = KotlinLightProjectDescriptor.INSTANCE!!
}