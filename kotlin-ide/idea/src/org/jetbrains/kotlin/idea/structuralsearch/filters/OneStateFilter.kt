package org.jetbrains.kotlin.idea.structuralsearch.filters

import com.intellij.psi.PsiElement
import com.intellij.structuralsearch.MatchVariableConstraint
import com.intellij.structuralsearch.NamedScriptableDefinition
import com.intellij.structuralsearch.plugin.ui.filters.FilterAction
import com.intellij.structuralsearch.plugin.ui.filters.FilterEditor
import com.intellij.ui.SimpleColoredComponent
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import java.util.function.Supplier
import javax.swing.GroupLayout
import javax.swing.JComponent
import javax.swing.JLabel

/**
 * Simple one state [FilterAction] with a minimal UI – one label.
 * Used to add a boolean constraint.
 */
abstract class OneStateFilter(@Nls val name: Supplier<String>, @Nls val label: String, @NonNls val constraintName: String) :
    FilterAction(name) {

    companion object {
        const val ENABLED: String = "enabled"
    }

    override fun setLabel(component: SimpleColoredComponent?) {
        myLabel.append(label)
    }

    override fun hasFilter(): Boolean {
        val variable = myTable.matchVariable ?: return false
        return variable.getAdditionalConstraint(constraintName) == ENABLED
    }

    override fun clearFilter() {
        myTable.matchVariable?.putAdditionalConstraint(constraintName, null)
    }

    override fun isApplicable(nodes: MutableList<out PsiElement>, completePattern: Boolean, target: Boolean): Boolean =
        myTable.variable is MatchVariableConstraint
                && myTable.profile.isApplicableConstraint(constraintName, nodes, completePattern, target)

    override fun getEditor(): FilterEditor<out NamedScriptableDefinition> =
        object : FilterEditor<MatchVariableConstraint>(myTable.matchVariable, myTable.constraintChangedCallback) {

            val myLabel = JLabel(label)

            override fun getPreferredFocusedComponent(): JComponent = myLabel

            override fun getFocusableComponents(): Array<JComponent> = arrayOf(myLabel)

            override fun layoutComponents() {
                val layout = GroupLayout(this)
                setLayout(layout)
                layout.autoCreateContainerGaps = true

                layout.setHorizontalGroup(
                    layout.createParallelGroup()
                        .addGroup(
                            layout.createSequentialGroup()
                                .addComponent(myLabel)
                        )
                )
                layout.setVerticalGroup(
                    layout.createSequentialGroup()
                        .addGroup(
                            layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                .addComponent(myLabel)
                        )
                )
            }

            override fun loadValues() {}

            override fun saveValues() {
                myConstraint.putAdditionalConstraint(constraintName, ENABLED)
            }
        }
}