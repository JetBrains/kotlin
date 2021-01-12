// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.util

import junit.framework.AssertionFailedError
import junit.framework.TestCase.assertEquals
import org.jetbrains.plugins.gradle.DefaultExternalDependencyId
import org.jetbrains.plugins.gradle.util.GradleAttachSourcesProvider.getSourcesArtifactNotation
import org.junit.Test

class GradleAttachSourcesProviderTest {
  @Test
  fun `test sources artifact notation parsing`() {
    val dependencyId = DefaultExternalDependencyId("mygroup", "myartifact", "myversion")
    assertEquals("mygroup:myartifact:myversion:sources", getSourcesArtifactNotation({ true }, dependencyId.presentableName))

    dependencyId.classifier = "myclassifier"
    assertEquals("mygroup:myartifact:myversion:sources", getSourcesArtifactNotation({ true }, dependencyId.presentableName))

    dependencyId.packaging = "mypackaging"
    assertEquals("mygroup:myartifact:myversion:sources", getSourcesArtifactNotation({ true }, dependencyId.presentableName))

    assertEquals("mygroup:myartifact:myversion:sources", getSourcesArtifactNotation(
      { true }, DefaultExternalDependencyId("mygroup", "myartifact", "myversion").apply { packaging = "mypackaging" }.presentableName))

    assertEquals("myartifact:myversion:sources",
                 getSourcesArtifactNotation({ throw AssertionFailedError("artifactIdChecker shouldn't be called") },
                                            "myartifact:myversion"))

    assertEquals("mygroup:myartifact:sources",
                 getSourcesArtifactNotation({ throw AssertionFailedError("artifactIdChecker shouldn't be called") },
                                            "mygroup:myartifact"))
  }
}