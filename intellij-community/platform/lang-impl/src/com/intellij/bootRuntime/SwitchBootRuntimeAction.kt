// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.bootRuntime

import com.intellij.bootRuntime.bundles.Local
import com.intellij.bootRuntime.bundles.Runtime
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.ComponentWithBrowseButton
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.*
import java.awt.*
import java.io.File
import javax.swing.*

/**
 * @author denis
 */
class SwitchBootRuntimeAction : AnAction(), DumbAware {

  var actions:List<Action> = emptyList()

  var bundles:MutableList<Runtime> = mutableListOf()

  lateinit var installed:Runtime

  override fun actionPerformed(e: AnActionEvent) {

    ProgressManager.getInstance().run(object : Task.Modal(e.project, "Loading Runtime List...", false) {
      override fun run(progressIndicator: ProgressIndicator) {
          try {
            val file = File(System.getProperty("java.home"))
            if (file.exists()) {
              val runtime = Local(project, javaHomeToInstallationLocation(file))
              installed = runtime
              bundles.add(installed)
            }
          } catch (exc : Exception) {
            // todo ask for file removal if it is broken
          }
        bundles.addAll(RuntimeLocationsFactory().localBundles(e.project!!))
        bundles.addAll(RuntimeLocationsFactory().bintrayBundles(e.project!!))
      }
    })

    // todo change to dsl
    val southPanel = ActionPanel()

    val controller = Controller(e.project!!, southPanel, Model(installed, bundles))

    val repositoryUrlFieldSpinner = JLabel(AnimatedIcon.Default())
    repositoryUrlFieldSpinner.isVisible = false

    val combobox = ComboBox<Runtime>()

    val myRuntimeUrlComboboxModel = CollectionComboBoxModel<Runtime>()

    val runtimeCompletionProvider = RuntimeCompletionProvider(bundles)

    val comboboxWithBrowserButton = ComponentWithBrowseButton(combobox) {
      val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
      val chooser = FileChooserFactory.getInstance().createPathChooser(descriptor, e.project, WindowManager.getInstance().suggestParentWindow(e.project))
      chooser.choose(LocalFileSystem.getInstance().findFileByIoFile(File("~"))) {
        file -> file.first()?.let{f ->
        File(f.path).walk().filter { file -> file.name == "tools.jar" ||
                                             file.name == "jrt-fs.jar"
        }.firstOrNull()?.let { file ->

          val local = ProgressManager.getInstance().
            runProcessWithProgressSynchronously<Local, RuntimeException>(
              {Local(e.project!!, javaHomeToInstallationLocation(javaHomeFromFile(file)))},
              "Initializing Runtime Info", false, e.project!!
            )

          myRuntimeUrlComboboxModel.add(local)
          bundles.add(local)
          runtimeCompletionProvider.setItems(bundles)
          controller.add(local)
          combobox.selectedItem = local
        }
      }
      }
    }

    val myRuntimeUrlField = TextFieldWithAutoCompletion<Runtime>(e.project,
                                                                 runtimeCompletionProvider,
                                                                 false,
                                                                 "")

    combobox.isEditable = true
    /*combobox.editor = ComboBoxCompositeEditor.
      withComponents<Any,TextFieldWithAutoCompletion<Runtime>>(myRuntimeUrlField, repositoryUrlFieldSpinner)*/

    combobox.editor = object : ComboBoxCompositeEditor<JLabel, TextFieldWithAutoCompletion<Runtime>>(myRuntimeUrlField, repositoryUrlFieldSpinner) {
      override fun setItem(anObject: Any?) {
        super.setItem(anObject)
        if (installed == anObject) {
          myRuntimeUrlField.font = combobox.font.deriveFont(Font.BOLD)
        } else {
          myRuntimeUrlField.font = combobox.font.deriveFont(Font.PLAIN)
        }
      }
    }

    combobox.renderer = object : EditorComboBoxRenderer(combobox.editor) {
      override fun getListCellRendererComponent(list: JList<*>?,
                                                value: Any?,
                                                index: Int,
                                                isSelected: Boolean,
                                                cellHasFocus: Boolean): Component {

        val listCellRendererComponent = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)

        if (installed == value) {
          list?.let {
            listCellRendererComponent.font = list.font.deriveFont(Font.BOLD)
          }
        }

        return  listCellRendererComponent
      }
    }

    bundles.let{ bundle -> myRuntimeUrlComboboxModel.add(bundle) }

    combobox.model = myRuntimeUrlComboboxModel

    myRuntimeUrlField.addDocumentListener(object : DocumentListener {
      override fun documentChanged(event: com.intellij.openapi.editor.event.DocumentEvent) {
        val runtime = bundles.firstOrNull { it.toString() == myRuntimeUrlField.text }
        if (runtime == null) {
          myRuntimeUrlField.font = combobox.font.deriveFont(Font.PLAIN)
          controller.noRuntimeSelected()
        } else {
          controller.runtimeSelected(runtime)
        }
      }
    })

    combobox.selectedItem = installed

    val centralPanel = JPanel(GridBagLayout())
    val constraint = GridBagConstraints()
    constraint.insets = Insets(10, 0,10, 0)
    centralPanel.add(comboboxWithBrowserButton, constraint)

    val myDialogWrapper = object: DialogWrapper(e.project) {
      init {
        title = "Switch Boot Runtime"
        init()
        peer.window.isAutoRequestFocus = true
        controller.updateRuntime()

      }

      override fun getPreferredFocusedComponent(): JComponent? {
        return comboboxWithBrowserButton.childComponent
      }

      override fun createCenterPanel(): JComponent? = centralPanel

      override fun createSouthPanel(): JComponent = southPanel

      override fun createActions(): Array<Action> {
        return emptyArray()
      }
    }

    myDialogWrapper.setResizable(false)
    myDialogWrapper.show()
  }
}

class RuntimeCompletionProvider(variants: Collection<Runtime>?) : TextFieldWithAutoCompletionListProvider<Runtime>(
  variants), DumbAware {
  override fun getLookupString(item: Runtime): String {
    return item.toString()
  }

  override fun compare(r1: Runtime, r2: Runtime): Int {
    return StringUtil.compare(r1.toString(), r2.toString(), false)
  }
}