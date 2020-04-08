package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.secondStep.modulesEditor

import com.intellij.icons.AllIcons
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBLabel
import com.intellij.ui.treeStructure.Tree
import org.jetbrains.kotlin.tools.projectWizard.core.Context
import org.jetbrains.kotlin.tools.projectWizard.core.entity.ValidationResult
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Sourceset
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.path
import org.jetbrains.kotlin.tools.projectWizard.wizard.KotlinNewProjectWizardUIBundle
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.borderPanel
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.icon
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.awt.Component
import java.awt.Graphics2D
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JTree
import javax.swing.tree.*

class ModulesEditorTree(
    private val onSelected: (DisplayableSettingItem?) -> Unit,
    context: Context,
    isTreeEditable: Boolean,
    private val addModule: (JComponent) -> Unit
) : Tree(DefaultMutableTreeNode(PROJECT_USER_OBJECT)) {
    val model: DefaultTreeModel get() = super.getModel() as DefaultTreeModel

    @Suppress("ClassName")
    object PROJECT_USER_OBJECT

    fun reload() {
        val openPaths = (0 until rowCount).mapNotNull { row ->
            getPathForRow(row).takeIf { isExpanded(it) }
        }
        model.reload()
        openPaths.forEach(::expandPath)
    }

    fun selectModule(moduleToSelect: Module) {
        for (value in (model.root as DefaultMutableTreeNode).depthFirstEnumeration().iterator()) {
            val node = value as? DefaultMutableTreeNode ?: continue
            val module = node.userObject as? Module ?: continue
            if (module.path == moduleToSelect.path) {
                selectionPath = TreePath(node.path)
                break
            }
        }
    }

    val selectedSettingItem
        get() = selectedNode?.userObject?.safeAs<DisplayableSettingItem>()

    val selectedNode
        get() = getSelectedNodes(DefaultMutableTreeNode::class.java) {
            it.userObject is DisplayableSettingItem
        }.singleOrNull()


    init {
        getSelectionModel().selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
        setShowsRootHandles(true)

        addTreeSelectionListener {
            onSelected(selectedSettingItem)
        }

        emptyText.clear()
        emptyText.appendText(KotlinNewProjectWizardUIBundle.message("editor.modules.no.modules"))
        emptyText.appendSecondaryText(
            KotlinNewProjectWizardUIBundle.message("editor.modules.add.module"),
            SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES
        ) {
            addModule(this)
        }

        setCellRenderer(CellRenderer(context, renderErrors = isTreeEditable))
    }
}

private class CellRenderer(private val context: Context, renderErrors: Boolean) : TreeCellRenderer {
    override fun getTreeCellRendererComponent(
        tree: JTree?,
        value: Any?,
        selected: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean
    ): Component {
        setError(null)
        val renderedCell = coloredCellRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus)
        return borderPanel {
            addToCenter(renderedCell)
            addToRight(iconLabel)
        }
    }

    private val iconLabel = JBLabel("")

    private fun setError(error: ValidationResult.ValidationError?) {
        val message = error?.messages?.firstOrNull()
        if (message == null) {
            iconLabel.icon = null
        } else {
            iconLabel.icon = AllIcons.General.Error
        }
    }

    private val coloredCellRenderer = object : ColoredTreeCellRenderer() {
        override fun customizeCellRenderer(
            tree: JTree,
            value: Any?,
            selected: Boolean,
            expanded: Boolean,
            leaf: Boolean,
            row: Int,
            hasFocus: Boolean
        ) {
            if (value?.safeAs<DefaultMutableTreeNode>()?.userObject == ModulesEditorTree.PROJECT_USER_OBJECT) {
                icon = AllIcons.Nodes.Project
                append(KotlinNewProjectWizardUIBundle.message("editor.modules.project"))
                return
            }
            val setting = (value as? DefaultMutableTreeNode)?.userObject as? DisplayableSettingItem ?: return
            icon = when (setting) {
                is Module -> setting.icon
                is Sourceset -> AllIcons.Nodes.Module
                else -> null
            }
            append(setting.text)
            setting.greyText?.let { greyText ->
                append(" ")
                append(greyText, SimpleTextAttributes.GRAYED_ATTRIBUTES)
            }
            if (renderErrors) {
                if (setting is Module) context.read {
                    val validationResult = setting.validator.validate(this, setting)
                    val error = validationResult.safeAs<ValidationResult.ValidationError>()
                        ?.takeIf { it.target === setting }
                    setError(error)
                } else {
                    setError(null)
                }
            }
        }
    }
}