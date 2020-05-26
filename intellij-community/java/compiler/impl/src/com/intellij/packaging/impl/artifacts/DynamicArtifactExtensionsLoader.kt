// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.packaging.impl.artifacts

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.extensions.ExtensionPointListener
import com.intellij.openapi.extensions.PluginDescriptor
import com.intellij.packaging.artifacts.Artifact
import com.intellij.packaging.artifacts.ArtifactPropertiesProvider
import com.intellij.packaging.artifacts.ArtifactType
import com.intellij.packaging.elements.PackagingElement
import com.intellij.packaging.elements.PackagingElementType
import com.intellij.util.Processor
import org.jetbrains.annotations.NotNull

internal class DynamicArtifactExtensionsLoader(private val artifactManager: ArtifactManagerImpl) {
  fun installListeners(disposable: Disposable) {
    ArtifactType.EP_NAME.getPoint().addExtensionPointListener(object : ExtensionPointListener<ArtifactType> {
      override fun extensionAdded(extension: ArtifactType, pluginDescriptor: PluginDescriptor) {
        runWriteAction {
          reloadArtifacts(artifactManager.allArtifactsIncludingInvalid.filter {
            (it as? InvalidArtifact)?.state?.artifactType == extension.id
          })
        }
      }

      override fun extensionRemoved(extension: ArtifactType, pluginDescriptor: PluginDescriptor) {
        reloadArtifacts(artifactManager.getArtifactsByType(extension))
      }
    }, false, disposable)

    PackagingElementType.EP_NAME.getPoint().addExtensionPointListener(object : ExtensionPointListener<PackagingElementType<out PackagingElement<*>>> {
      override fun extensionAdded(extension: PackagingElementType<out PackagingElement<*>>, pluginDescriptor: PluginDescriptor) {
        runWriteAction {
          reloadArtifacts(artifactManager.allArtifactsIncludingInvalid.filter {
            it.artifactType == InvalidArtifactType.getInstance()
          })
        }
      }

      override fun extensionRemoved(extension: PackagingElementType<out PackagingElement<*>>, pluginDescriptor: PluginDescriptor) {
        reloadArtifacts(artifactManager.artifactsList.filter { containsElementsOfType(it, extension) })
      }
    }, false, disposable)

    ArtifactPropertiesProvider.EP_NAME.getPoint().addExtensionPointListener(object : ExtensionPointListener<ArtifactPropertiesProvider> {
      override fun extensionAdded(extension: ArtifactPropertiesProvider, pluginDescriptor: PluginDescriptor) {
        runWriteAction {
          reloadArtifacts(artifactManager.allArtifactsIncludingInvalid.filter { artifact ->
            (artifact as? InvalidArtifact)?.state?.propertiesList?.any { it.id == extension.id } ?: false
          })
        }
      }

      override fun extensionRemoved(extension: ArtifactPropertiesProvider, pluginDescriptor: PluginDescriptor) {
        reloadArtifacts(artifactManager.artifactsList.filter { extension in it.propertiesProviders })
      }
    }, false, disposable)
  }

  private fun <E : PackagingElement<*>> containsElementsOfType(artifact: Artifact, type: PackagingElementType<E>): Boolean {
    return !ArtifactUtil.processPackagingElements(artifact, type, Processor { false },
                                                  artifactManager.resolvingContext, false)
  }

  private fun reloadArtifacts(toReplace: @NotNull Collection<Artifact>) {
    artifactManager.replaceArtifacts(toReplace) {
      artifactManager.loadArtifact(artifactManager.saveArtifact(it))
    }
  }
}