/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.memberInfo

import com.intellij.openapi.util.NlsContexts
import com.intellij.refactoring.ui.AbstractMemberSelectionPanel
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SeparatorFactory
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import java.awt.BorderLayout

class KotlinMemberSelectionPanel(
    @NlsContexts.DialogTitle title: String,
    memberInfo: List<KotlinMemberInfo>,
    abstractColumnHeader: String?
) : AbstractMemberSelectionPanel<KtNamedDeclaration, KotlinMemberInfo>() {
    private val table = createMemberSelectionTable(memberInfo, abstractColumnHeader)

    init {
        layout = BorderLayout()

        val scrollPane = ScrollPaneFactory.createScrollPane(table)
        add(SeparatorFactory.createSeparator(title, table), BorderLayout.NORTH)
        add(scrollPane, BorderLayout.CENTER)
    }

    private fun createMemberSelectionTable(
        memberInfo: List<KotlinMemberInfo>,
        abstractColumnHeader: String?
    ): KotlinMemberSelectionTable {
        return KotlinMemberSelectionTable(memberInfo, null, abstractColumnHeader)
    }

    override fun getTable() = table
}