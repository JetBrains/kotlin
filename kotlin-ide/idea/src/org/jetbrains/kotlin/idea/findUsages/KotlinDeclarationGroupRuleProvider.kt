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

package org.jetbrains.kotlin.idea.findUsages

import com.intellij.openapi.project.Project
import com.intellij.usages.PsiNamedElementUsageGroupBase
import com.intellij.usages.Usage
import com.intellij.usages.UsageGroup
import com.intellij.usages.UsageInfo2UsageAdapter
import com.intellij.usages.impl.FileStructureGroupRuleProvider
import com.intellij.usages.rules.PsiElementUsage
import com.intellij.usages.rules.UsageGroupingRule
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.parents

class KotlinDeclarationGroupingRule(val level: Int = 0) : UsageGroupingRule {
    override fun groupUsage(usage: Usage): UsageGroup? {
        var element = (usage as? PsiElementUsage)?.element ?: return null

        if (element.containingFile !is KtFile) return null

        if (element is KtFile) {
            val offset = (usage as? UsageInfo2UsageAdapter)?.usageInfo?.navigationOffset
            if (offset != null) {
                element = element.findElementAt(offset) ?: element
            }
        }

        val parentList = element.parents.filterIsInstance<KtNamedDeclaration>().filterNot { it is KtProperty && it.isLocal }.toList()
        if (parentList.size <= level) {
            return null
        }
        return PsiNamedElementUsageGroupBase(parentList[parentList.size - level - 1])
    }
}

class KotlinDeclarationGroupRuleProvider : FileStructureGroupRuleProvider {
    override fun getUsageGroupingRule(project: Project): UsageGroupingRule = KotlinDeclarationGroupingRule(0)
}

class KotlinDeclarationSecondLevelGroupRuleProvider : FileStructureGroupRuleProvider {
    override fun getUsageGroupingRule(project: Project): UsageGroupingRule = KotlinDeclarationGroupingRule(1)
}
