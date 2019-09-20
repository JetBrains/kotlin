// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore.xml

import com.intellij.configurationStore.DataWriter
import com.intellij.configurationStore.StateMap
import com.intellij.configurationStore.XmlElementStorage
import com.intellij.configurationStore.toBufferExposingByteArray
import com.intellij.openapi.util.JDOMUtil
import org.assertj.core.api.Assertions.assertThat
import org.jdom.Element
import org.junit.Test

class XmlElementStorageTest {
  @Test fun testGetStateSucceeded() {
    val storage = MyXmlElementStorage(Element("root").addContent(Element("component").setAttribute("name", "test").addContent(Element("foo"))))
    val state = storage.getState(this, "test", Element::class.java)
    assertThat(state).isNotNull
    assertThat(state!!.name).isEqualTo("component")
    assertThat(state.getChild("foo")).isNotNull
  }

  @Test fun `get state not succeeded`() {
    val storage = MyXmlElementStorage(Element("root"))
    val state = storage.getState(this, "test", Element::class.java)
    assertThat(state).isNull()
  }

  @Test fun `set state overrides old state`() {
    val storage = MyXmlElementStorage(Element("root").addContent(Element("component").setAttribute("name", "test").addContent(Element("foo"))))
    val newState = Element("component").setAttribute("name", "test").addContent(Element("bar"))
    val externalizationSession = storage.createSaveSessionProducer()!!
    externalizationSession.setState(null, "test", newState)
    externalizationSession.createSaveSession()!!.save()
    assertThat(storage.savedElement).isNotNull
    assertThat(storage.savedElement!!.getChild("component").getChild("bar")).isNotNull
    assertThat(storage.savedElement!!.getChild("component").getChild("foo")).isNull()
  }

  private class MyXmlElementStorage(private val element: Element) : XmlElementStorage("", "root") {
    override val isUseVfsForWrite: Boolean
      get() = false

    var savedElement: Element? = null

    override fun loadLocalData() = element

    override fun createSaveSession(states: StateMap) = object : XmlElementStorageSaveSession<MyXmlElementStorage>(states, this) {
      override fun saveLocally(dataWriter: DataWriter?) {
        if (dataWriter == null) {
          savedElement = null
        }
        else {
          savedElement = JDOMUtil.load(dataWriter.toBufferExposingByteArray().toByteArray().inputStream())
        }
      }
    }
  }
}
