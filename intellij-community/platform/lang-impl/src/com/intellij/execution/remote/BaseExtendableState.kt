// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.remote

import com.intellij.execution.remote.BaseExtendableConfiguration.Companion.getTypeImpl
import com.intellij.openapi.components.BaseState
import com.intellij.util.xmlb.XmlSerializer
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag
import org.jdom.Element

open class BaseExtendableState : BaseState() {
  @get:Attribute("type")
  var typeId by string()

  @get:Attribute("name")
  var name by string()

  @get:Tag("config")
  var innerState: Element? by property<Element?>(null) { it === null }

  open fun loadFromConfiguration(config: BaseExtendableConfiguration) {
    typeId = config.typeId
    name = config.displayName
    innerState = config.getSerializer().state?.let { XmlSerializer.serialize(it) }
  }

  companion object {
    private fun BaseExtendableConfiguration.getSerializer() = getTypeImpl().createSerializer(this)
  }
}

