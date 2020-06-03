package org.jetbrains.kotlin.idea.refactoring.move.moveMethod

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.ide.util.TreeClassChooser
import com.intellij.ide.util.TreeJavaClassChooserDialog
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.refactoring.ui.RefactoringDialog
import com.intellij.ui.EditorTextField
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.TitledSeparator
import com.intellij.ui.components.JBList
import com.intellij.util.ui.JBUI
import com.jetbrains.rd.util.firstOrNull
import org.jetbrains.kotlin.asJava.classes.KtLightClassForSourceDeclaration
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.KotlinIconProvider.Companion.getBaseIcon
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.completion.extraCompletionFilter
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester.suggestNamesByType
import org.jetbrains.kotlin.idea.core.NewDeclarationNameValidator
import org.jetbrains.kotlin.idea.core.completion.DeclarationLookupObject
import org.jetbrains.kotlin.idea.core.completion.PackageLookupObject
import org.jetbrains.kotlin.idea.projectView.KtClassOrObjectTreeNode
import org.jetbrains.kotlin.idea.refactoring.canRefactor
import org.jetbrains.kotlin.idea.refactoring.ui.KotlinTypeReferenceEditorComboWithBrowseButton
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.name.Name.isValidIdentifier
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.renderer.ClassifierNamePolicy
import java.awt.Component
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.ActionListener
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode


class MoveKotlinMethodDialog(
    private val method: KtNamedFunction,
    private val variableToClassMap: Map<KtNamedDeclaration, KtClass>,
    private val targetContainer: KtClassOrObject?
) : RefactoringDialog(method.project, true) {
    private val variables = variableToClassMap.keys.toList()
    private val targetVariableList = createTargetVariableChooser()
    private val thisClassesToMembers = getThisClassesToMembers(method)
    private val oldClassParameterNameFields = LinkedHashMap<KtClass, EditorTextField>()
    private val targetObjectChooser = createTargetObjectChooser()
    private val toClassRadioButton = JRadioButton(KotlinBundle.message("label.text.to.class"))
    private val toObjectRadioButton = JRadioButton(KotlinBundle.message("label.text.to.object"))
    private var selectedTarget: KtNamedDeclaration? = null
    private val openInEditorCheckBox = JCheckBox(KotlinBundle.message("open.moved.method.in.editor"), true)

    init {
        super.init()
        title = KotlinBundle.message("title.move.method")
    }

    override fun doAction() {
        if (toClassRadioButton.isSelected) selectedTarget = targetVariableList.selectedValue

        if (toObjectRadioButton.isSelected && selectedTarget == null) {
            setErrorText(KotlinBundle.message("text.no.destination.object.specified"))
            return
        }

        val oldClassParameterNames = LinkedHashMap<KtClass, String>()
        for ((ktClass, field) in oldClassParameterNameFields) {
            if (!isValidIdentifier(field.text)) {
                setErrorText(KotlinBundle.message("parameter.name.is.invalid", field.text))
                return
            }
            if (field.isEnabled) {
                oldClassParameterNames[ktClass] = field.text
            }
        }

        val processor = MoveKotlinMethodProcessor(method, selectedTarget!!, oldClassParameterNames, openInEditorCheckBox.isSelected)
        invokeRefactoring(processor)
    }

    override fun createCenterPanel(): JComponent? {
        val mainPanel = JPanel(GridBagLayout())
        val buttonGroup = ButtonGroup()

        toClassRadioButton.addActionListener { enableTargetChooser() }
        toObjectRadioButton.addActionListener { enableTargetChooser() }
        buttonGroup.add(toClassRadioButton)
        buttonGroup.add(toObjectRadioButton)

        targetVariableList.addListSelectionListener { enableTextFields() }
        val scrollPane = ScrollPaneFactory.createScrollPane(targetVariableList)
        mainPanel.add(
            scrollPane, GridBagConstraints(
                1, 0, 1, 1, 1.0, 1.0,
                GridBagConstraints.WEST, GridBagConstraints.BOTH, JBUI.emptyInsets(), 0, 0
            )
        )

        mainPanel.add(
            targetObjectChooser, GridBagConstraints(
                1, GridBagConstraints.RELATIVE, 1, 1, 1.0, 1.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, JBUI.emptyInsets(), 0, 0
            )
        )
        mainPanel.add(
            toClassRadioButton, GridBagConstraints(
                0, GridBagConstraints.RELATIVE, 1, 1, 1.0, 1.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, JBUI.emptyInsets(), 0, 0
            )
        )
        mainPanel.add(
            toObjectRadioButton, GridBagConstraints(
                0, GridBagConstraints.RELATIVE, 1, 1, 1.0, 1.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, JBUI.emptyInsets(), 0, 0
            )
        )

        val parametersPanel: JPanel? = createParametersPanel()
        if (parametersPanel != null) {
            mainPanel.add(
                parametersPanel, GridBagConstraints(
                    0, GridBagConstraints.RELATIVE, 2, 1, 1.0, 0.0,
                    GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, JBUI.emptyInsets(), 0, 0
                )
            )
        }
        mainPanel.add(
            openInEditorCheckBox, GridBagConstraints(
                0, GridBagConstraints.RELATIVE, 2, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, JBUI.emptyInsets(), 0, 0
            )
        )

        if (variables.isNotEmpty() && targetContainer !is KtObjectDeclaration) {
            toClassRadioButton.isSelected = true
        } else {
            toObjectRadioButton.isSelected = true
            if (variables.isEmpty()) {
                toClassRadioButton.isEnabled = false
            }
        }

        enableTextFields()
        enableTargetChooser()
        updateOnChanged(targetVariableList)
        return mainPanel
    }

    private fun createTargetVariableChooser(): JList<KtNamedDeclaration> {
        val listModel = object : AbstractListModel<KtNamedDeclaration>() {
            override fun getElementAt(index: Int): KtNamedDeclaration = variables[index]
            override fun getSize(): Int = variables.size
        }
        val list = JBList(listModel)
        val listCellRenderer = object : DefaultListCellRenderer() {
            private val renderer = IdeDescriptorRenderers.SOURCE_CODE_TYPES.withOptions {
                classifierNamePolicy = ClassifierNamePolicy.SHORT
            }

            override fun getListCellRendererComponent(
                list: JList<*>?, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean
            ): Component {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                if (value is KtNamedDeclaration) {
                    icon = value.getBaseIcon()
                    text = value.nameAsSafeName.identifier
                    (value.resolveToDescriptorIfAny() as? CallableDescriptor)?.returnType?.let { type ->
                        text = "$text: ${renderer.renderType(type)}"
                    }
                }
                return this
            }
        }

        list.cellRenderer = listCellRenderer
        list.selectionMode = ListSelectionModel.SINGLE_SELECTION
        val defaultVariableIndex = variables.indexOf(variableToClassMap.filter { it.value == targetContainer }.firstOrNull()?.key)
        list.selectedIndex = if (defaultVariableIndex != -1) defaultVariableIndex else 0
        list.selectionModel.addListSelectionListener { updateOnChanged(list) }
        return list
    }

    private fun createTargetObjectChooser(): KotlinTypeReferenceEditorComboWithBrowseButton {
        val targetObjectChooser = KotlinTypeReferenceEditorComboWithBrowseButton(
            ActionListener {
                val chooser: TreeClassChooser = object : TreeJavaClassChooserDialog(
                    KotlinBundle.message("title.choose.destination.object"),
                    project,
                    GlobalSearchScope.projectScope(project),
                    { psiClass ->
                        if (psiClass !is KtLightClassForSourceDeclaration) {
                            false
                        } else {
                            val ktClassOrObject = psiClass.kotlinOrigin
                            ktClassOrObject is KtObjectDeclaration && !ktClassOrObject.isObjectLiteral()
                        }
                    },
                    null,
                    null,
                    true
                ) {
                    override fun getSelectedFromTreeUserObject(node: DefaultMutableTreeNode): PsiClass? {
                        val psiClass = super.getSelectedFromTreeUserObject(node)
                        if (psiClass != null) return psiClass
                        val userObject = node.userObject
                        return if (userObject !is KtClassOrObjectTreeNode) null else userObject.value.toLightClass()
                    }
                }
                chooser.selectDirectory(method.containingFile.containingDirectory)
                chooser.showDialog()
                val psiClass = chooser.selected
                if (psiClass is KtLightClassForSourceDeclaration) {
                    selectedTarget = psiClass.kotlinOrigin
                    targetObjectChooser.text = psiClass.kotlinOrigin.fqName.toString()
                }
            },
            (targetContainer ?: method.containingClassOrObject as? KtObjectDeclaration)?.fqName?.asString(),
            targetContainer ?: method.containingClassOrObject!!,
            RECENTS_KEY
        )
        targetObjectChooser.codeFragment?.let { codeFragment ->
            codeFragment.extraCompletionFilter = { lookupElement: LookupElement ->
                val lookupObject = lookupElement.getObject() as? DeclarationLookupObject
                val psiElement = lookupObject?.psiElement
                lookupObject is PackageLookupObject || psiElement is KtObjectDeclaration && psiElement.canRefactor()
            }
        }
        return targetObjectChooser
    }

    private fun updateOnChanged(list: JList<*>) {
        okAction.isEnabled = !list.selectionModel.isSelectionEmpty
    }

    private fun enableTextFields() {
        for (textField in oldClassParameterNameFields.values) {
            textField.isEnabled = true
        }
        if (toClassRadioButton.isSelected) {
            val variable = variables[targetVariableList.selectedIndex]
            val containingClass = variable.containingClassOrObject as? KtClass ?: return
            if (variable !is KtParameter || variable.hasValOrVar()) {
                if (thisClassesToMembers[containingClass]?.size == 1
                    && thisClassesToMembers[containingClass]?.contains(variable) == true
                ) {
                    oldClassParameterNameFields[containingClass]?.isEnabled = false
                }
            }
        }
    }

    private fun enableTargetChooser() {
        if (toClassRadioButton.isSelected) {
            targetVariableList.isEnabled = true
            targetObjectChooser.isEnabled = false
        } else {
            targetVariableList.isEnabled = false
            targetObjectChooser.isEnabled = true
        }
        enableTextFields()
    }

    private fun createParametersPanel(): JPanel? {
        if (thisClassesToMembers.isEmpty()) return null
        val panel = JPanel(VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 0, true, true))
        val validator = NewDeclarationNameValidator(method, null, NewDeclarationNameValidator.Target.VARIABLES)
        for (ktClass in thisClassesToMembers.keys) {
            val text = KotlinBundle.message("text.select.a.name.for.this.parameter", ktClass.nameAsSafeName.identifier)
            panel.add(TitledSeparator(text, null))
            val suggestedName = suggestNamesByType(ktClass.resolveToDescriptorIfAny()!!.defaultType, validator).firstOrNull() ?: "parameter"
            val field = EditorTextField(suggestedName, project, KotlinFileType.INSTANCE)
            field.minimumSize = Dimension(field.preferredSize)
            oldClassParameterNameFields[ktClass] = field
            panel.add(field)
        }
        panel.border = BorderFactory.createEmptyBorder(5, 0, 5, 0)
        return panel
    }

    companion object {
        val RECENTS_KEY = "${MoveKotlinMethodDialog::class.java.name}.RECENTS_KEY"
    }
}