// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.util.ui

import javax.swing.Icon

abstract class DataView<out Data> {
  abstract val data: Data
  abstract val location: String
  abstract val icon: Icon
  abstract val presentationName: String
  abstract val groupId: String
  abstract val version: String

  open val isPresent: Boolean = true

  override fun hashCode() = if (isPresent) data.hashCode() else super.hashCode()

  override fun equals(other: Any?): Boolean {
    if (other === this) return true
    if (!isPresent) return false
    if (other !is DataView<*>) return false
    if (!other.isPresent) return false
    return data == other.data
  }

  companion object {
    fun <Data : Any> getData(view: DataView<Data>): Data? {
      return if (view.isPresent) view.data else null
    }

    fun getIcon(view: DataView<*>): Icon? {
      return if (view.isPresent) view.icon else null
    }
  }
}