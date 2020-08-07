package com.jetbrains.kotlin.structuralsearch.filters

import com.intellij.psi.PsiElement
import com.intellij.structuralsearch.MatchVariableConstraint
import com.intellij.structuralsearch.NamedScriptableDefinition
import com.intellij.structuralsearch.plugin.ui.filters.FilterAction
import com.intellij.structuralsearch.plugin.ui.filters.FilterEditor
import com.intellij.ui.SimpleColoredComponent
import com.jetbrains.kotlin.structuralsearch.KSSRBundle
import org.jetbrains.annotations.NonNls
import javax.swing.GroupLayout
import javax.swing.JCheckBox
import javax.swing.JComponent

@Suppress("ComponentNotRegistered")
class VarValFilter : FilterAction(KSSRBundle.messagePointer("text.filter.var.val")) {

    companion object {
        @NonNls
        const val CONSTRAINT_NAME: String = "kotlinVarVal"

        @NonNls
        private const val CONSTRAINT_ON: String = "enabled"

        @NonNls
        private const val CONSTRAINT_OFF: String = "disabled"
    }

    override fun setLabel(p0: SimpleColoredComponent?) {
        myLabel.append("Match val and var keywords")
    }

    override fun hasFilter(): Boolean {
        val variable = myTable.matchVariable ?: return false
        return variable.getAdditionalConstraint(CONSTRAINT_NAME) == CONSTRAINT_ON
    }

    override fun clearFilter() {
        myTable.matchVariable?.putAdditionalConstraint(CONSTRAINT_NAME, CONSTRAINT_OFF)
    }

    override fun isApplicable(nodes: MutableList<out PsiElement>, completePattern: Boolean, target: Boolean): Boolean =
        myTable.variable is MatchVariableConstraint
                && myTable.profile.isApplicableConstraint(CONSTRAINT_NAME, nodes, completePattern, target)

    override fun getEditor(): FilterEditor<out NamedScriptableDefinition> =
        object : FilterEditor<MatchVariableConstraint>(myTable.matchVariable, myTable.constraintChangedCallback) {

            private val myCheckBox = JCheckBox("Match val with var", false)

            override fun getPreferredFocusedComponent(): JComponent = myCheckBox

            override fun getFocusableComponents(): Array<JComponent> = arrayOf(myCheckBox)

            override fun layoutComponents() {

                val layout = GroupLayout(this)
                setLayout(layout)
                layout.autoCreateContainerGaps = true

                layout.setHorizontalGroup(
                    layout.createParallelGroup().addGroup(
                        layout.createSequentialGroup()
                            .addComponent(myCheckBox)
                    )
                )
                layout.setVerticalGroup(
                    layout.createSequentialGroup().addGroup(
                        layout.createParallelGroup()
                            .addComponent(myCheckBox)
                    )
                )
            }

            override fun loadValues() {
                val value = myConstraint.getAdditionalConstraint(CONSTRAINT_NAME)
                myCheckBox.isSelected = value == CONSTRAINT_ON
            }

            override fun saveValues() {
                myConstraint.putAdditionalConstraint(
                    CONSTRAINT_NAME,
                    if (myCheckBox.isSelected) CONSTRAINT_ON else CONSTRAINT_OFF
                )
            }

        }

}