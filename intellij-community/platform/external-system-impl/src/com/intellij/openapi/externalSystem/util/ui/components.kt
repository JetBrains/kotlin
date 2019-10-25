// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.util.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.externalSystem.util.properties.UiProperty
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.layout.*
import java.awt.event.ItemEvent
import javax.swing.ComboBoxModel
import javax.swing.JTextField
import javax.swing.ListCellRenderer
import javax.swing.event.DocumentEvent

fun <T> Cell.myComboBox(model: ComboBoxModel<T>,
                        modelBinding: PropertyBinding<T>,
                        renderer: ListCellRenderer<T>): CellBuilder<ComboBox<T>> {
  val component = ComboBox(model)
  component.selectedItem = modelBinding.get()
  component.renderer = renderer
  return component(growX)
}

fun <T> ComboBox<T>.bind(property: UiProperty<T>, validate: () -> ValidationInfo?, parentDisposable: Disposable) {
  property.bind(this, validate, parentDisposable)
  property.afterChange { if (selectedItem != it) selectedItem = it }
  addItemListener {
    if (it.stateChange == ItemEvent.SELECTED) {
      @Suppress("UNCHECKED_CAST")
      property.set(it.item as T)
    }
  }
}

fun TextFieldWithBrowseButton.bind(property: UiProperty<String>, validate: () -> ValidationInfo?, parentDisposable: Disposable) {
  textField.bind(property, validate, parentDisposable)
}

fun JTextField.bind(property: UiProperty<String>, validate: () -> ValidationInfo?, parentDisposable: Disposable) {
  property.bind(this, validate, parentDisposable)
  property.afterChange { if (text != it) text = it }
  document.addDocumentListener(object : DocumentAdapter() {
    override fun textChanged(e: DocumentEvent) {
      property.set(text)
    }
  })
}
