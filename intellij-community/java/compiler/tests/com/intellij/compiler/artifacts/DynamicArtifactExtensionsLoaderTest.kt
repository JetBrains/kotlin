// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.artifacts

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.module.impl.ProjectLoadingErrorsHeadlessNotifier
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.packaging.artifacts.*
import com.intellij.packaging.elements.*
import com.intellij.packaging.impl.artifacts.InvalidArtifactType
import com.intellij.packaging.impl.artifacts.PlainArtifactType
import com.intellij.packaging.ui.ArtifactEditorContext
import com.intellij.packaging.ui.ArtifactPropertiesEditor
import com.intellij.packaging.ui.PackagingElementPresentation
import com.intellij.testFramework.HeavyPlatformTestCase
import com.intellij.util.ui.EmptyIcon
import java.util.function.Consumer
import javax.swing.Icon

class DynamicArtifactExtensionsLoaderTest : HeavyPlatformTestCase() {
  fun `test unload and load artifact type`() {
    ProjectLoadingErrorsHeadlessNotifier.setErrorHandler(Consumer {}, testRootDisposable)
    val artifactManager = ArtifactManager.getInstance(myProject)
    runWithRegisteredExtension(MockArtifactType(), ArtifactType.EP_NAME) {
      artifactManager.addArtifact("mock", MockArtifactType.getInstance(), PackagingElementFactory.getInstance().createArtifactRootElement())
    }

    val invalid = assertOneElement(artifactManager.allArtifactsIncludingInvalid)
    assertEquals(InvalidArtifactType.getInstance(), invalid.artifactType)
    assertEquals("mock", invalid.name)
    registerExtension(MockArtifactType(), ArtifactType.EP_NAME, testRootDisposable)
    assertOneElement(artifactManager.allArtifactsIncludingInvalid)
    val artifact = assertOneElement(artifactManager.getArtifactsByType(MockArtifactType.getInstance()))
    assertEquals("mock", artifact.name)
  }

  fun `test unload and load packaging element type`() {
    val artifactManager = ArtifactManager.getInstance(myProject)
    runWithRegisteredExtension(MockPackagingElementType(), PackagingElementType.EP_NAME) {
      val root = PackagingElementFactory.getInstance().createArtifactRootElement()
      root.addFirstChild(MockPackagingElement().apply { this.state.data = "data" })
      artifactManager.addArtifact("mock", PlainArtifactType.getInstance(), root)
    }

    val invalid = assertOneElement(artifactManager.allArtifactsIncludingInvalid)
    assertEquals(InvalidArtifactType.getInstance(), invalid.artifactType)
    assertEquals("mock", invalid.name)

    registerExtension(MockPackagingElementType(), PackagingElementType.EP_NAME, testRootDisposable)
    assertOneElement(artifactManager.allArtifactsIncludingInvalid)
    val artifact = assertOneElement(artifactManager.getArtifactsByType(PlainArtifactType.getInstance()))
    assertEquals("mock", artifact.name)
    assertEquals("data", (artifact.rootElement.children.single() as MockPackagingElement).state.data)
  }

  fun `test unload and load artifact properties`() {
    val artifactManager = ArtifactManager.getInstance(myProject)
    runWithRegisteredExtension(MockArtifactPropertiesProvider(), ArtifactPropertiesProvider.EP_NAME) {
      val model = artifactManager.createModifiableModel()
      val artifact = model.addArtifact("mock", PlainArtifactType.getInstance())
      artifact.setProperties(MockArtifactPropertiesProvider.getInstance(), MockArtifactProperties().apply { data = "data" })
      runWriteAction { model.commit() }
    }

    val invalid = assertOneElement(artifactManager.allArtifactsIncludingInvalid)
    assertEquals(InvalidArtifactType.getInstance(), invalid.artifactType)
    assertEquals("mock", invalid.name)

    registerExtension(MockArtifactPropertiesProvider(), ArtifactPropertiesProvider.EP_NAME, testRootDisposable)
    assertOneElement(artifactManager.allArtifactsIncludingInvalid)
    val artifact = assertOneElement(artifactManager.getArtifactsByType(PlainArtifactType.getInstance()))
    assertEquals("mock", artifact.name)
    assertEquals("data", (artifact.getProperties(MockArtifactPropertiesProvider.getInstance()) as MockArtifactProperties).data)
  }

  private inline fun <T> runWithRegisteredExtension(extension: T, extensionPoint: ExtensionPointName<T>, action: () -> Unit) {
    val disposable = Disposer.newDisposable()
    registerExtension(extension, extensionPoint, disposable)
    try {
      action()
    }
    finally {
      Disposer.dispose(disposable)
    }
  }

  private fun <T> registerExtension(type: T, extensionPointName: ExtensionPointName<T>, disposable: Disposable) {
    val artifactTypeDisposable = Disposer.newDisposable()
    Disposer.register(disposable, Disposable {
      runWriteAction {
        Disposer.dispose(artifactTypeDisposable)
      }
    })
    extensionPointName.getPoint().registerExtension(type, artifactTypeDisposable)
  }

  override fun setUp() {
    super.setUp()
    ProjectLoadingErrorsHeadlessNotifier.setErrorHandler(Consumer {}, testRootDisposable)
  }
}

private class MockArtifactType : ArtifactType("mock", "Mock") {
  companion object {
    fun getInstance() = EP_NAME.findExtension(MockArtifactType::class.java)!!
  }

  override fun getIcon(): Icon = EmptyIcon.ICON_16

  override fun getDefaultPathFor(kind: PackagingElementOutputKind): String? = ""

  override fun createRootElement(artifactName: String): CompositePackagingElement<*> {
    return PackagingElementFactory.getInstance().createArtifactRootElement()
  }
}

private class MockPackagingElement : PackagingElement<MockPackagingElementState>(PackagingElementType.EP_NAME.findExtensionOrFail(MockPackagingElementType::class.java)) {
  private val state: MockPackagingElementState = MockPackagingElementState("")

  override fun getState(): MockPackagingElementState = state

  override fun loadState(state: MockPackagingElementState) {
    this.state.data = state.data
  }

  override fun isEqualTo(element: PackagingElement<*>): Boolean = (element as? MockPackagingElement)?.state?.data == state.data

  override fun createPresentation(context: ArtifactEditorContext): PackagingElementPresentation {
    throw UnsupportedOperationException()
  }
}

private class MockPackagingElementState(var data: String = "")

private class MockPackagingElementType : PackagingElementType<MockPackagingElement>("mock-element", "Mock Element") {
  override fun canCreate(context: ArtifactEditorContext, artifact: Artifact): Boolean = true

  override fun chooseAndCreate(context: ArtifactEditorContext,
                               artifact: Artifact,
                               parent: CompositePackagingElement<*>): MutableList<out PackagingElement<*>> {
    throw UnsupportedOperationException()
  }

  override fun createEmpty(project: Project): MockPackagingElement {
    return MockPackagingElement()
  }
}

private class MockArtifactProperties : ArtifactProperties<MockArtifactProperties>() {
  var data: String = ""

  override fun getState(): MockArtifactProperties? {
    return this
  }

  override fun loadState(state: MockArtifactProperties) {
    data = state.data
  }

  override fun createEditor(context: ArtifactEditorContext): ArtifactPropertiesEditor {
    throw UnsupportedOperationException()
  }
}

private class MockArtifactPropertiesProvider : ArtifactPropertiesProvider("mock-properties") {
  companion object {
    fun getInstance(): MockArtifactPropertiesProvider = EP_NAME.findExtensionOrFail(MockArtifactPropertiesProvider::class.java)
  }

  override fun createProperties(artifactType: ArtifactType): ArtifactProperties<*> = MockArtifactProperties()
}