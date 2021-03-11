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

package org.jetbrains.kotlin.idea.refactoring.memberInfo

import com.intellij.icons.AllIcons
import com.intellij.refactoring.classMembers.MemberInfoModel
import com.intellij.refactoring.ui.AbstractMemberSelectionTable
import com.intellij.ui.RowIcon
import org.jetbrains.kotlin.idea.KotlinIconProviderBase
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import javax.swing.Icon

class KotlinMemberSelectionTable(
    memberInfos: List<KotlinMemberInfo>,
    memberInfoModel: MemberInfoModel<KtNamedDeclaration, KotlinMemberInfo>?,
    abstractColumnHeader: String?
) : AbstractMemberSelectionTable<KtNamedDeclaration, KotlinMemberInfo>(memberInfos, memberInfoModel, abstractColumnHeader) {
    override fun getAbstractColumnValue(memberInfo: KotlinMemberInfo): Any? {
        if (memberInfo.isStatic || memberInfo.isCompanionMember) return null

        val member = memberInfo.member
        if (member !is KtNamedFunction && member !is KtProperty && member !is KtParameter) return null

        if (member.hasModifier(KtTokens.ABSTRACT_KEYWORD)) {
            myMemberInfoModel.isFixedAbstract(memberInfo)?.let { return it }
        }
        if (myMemberInfoModel.isAbstractEnabled(memberInfo)) return memberInfo.isToAbstract
        return myMemberInfoModel.isAbstractWhenDisabled(memberInfo)
    }

    override fun isAbstractColumnEditable(rowIndex: Int): Boolean {
        val memberInfo = myMemberInfos[rowIndex]

        if (memberInfo.isStatic) return false

        val member = memberInfo.member
        if (member !is KtNamedFunction && member !is KtProperty && member !is KtParameter) return false

        if (member.hasModifier(KtTokens.ABSTRACT_KEYWORD)) {
            myMemberInfoModel.isFixedAbstract(memberInfo)?.let { return false }
        }

        return memberInfo.isChecked && myMemberInfoModel.isAbstractEnabled(memberInfo)
    }

    override fun setVisibilityIcon(memberInfo: KotlinMemberInfo, icon: RowIcon) {
        icon.setIcon(KotlinIconProviderBase.getVisibilityIcon(memberInfo.member.modifierList), 1)
    }

    override fun getOverrideIcon(memberInfo: KotlinMemberInfo): Icon? {
        val defaultIcon = EMPTY_OVERRIDE_ICON

        val member = memberInfo.member
        if (member !is KtNamedFunction && member !is KtProperty && member !is KtParameter) return defaultIcon

        return when (memberInfo.overrides) {
            true -> AllIcons.General.OverridingMethod
            false -> AllIcons.General.ImplementingMethod
            else -> defaultIcon
        }
    }
}