// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.jps.gradle.build;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.builders.artifacts.ArtifactBuildTaskProvider;
import org.jetbrains.jps.gradle.model.JpsGradleExtensionService;
import org.jetbrains.jps.gradle.model.artifacts.JpsGradleArtifactExtension;
import org.jetbrains.jps.gradle.model.impl.artifacts.GradleArtifactExtensionProperties;
import org.jetbrains.jps.incremental.BuildTask;
import org.jetbrains.jps.incremental.CompileContext;
import org.jetbrains.jps.model.artifact.JpsArtifact;
import org.jetbrains.jps.model.artifact.elements.JpsArtifactRootElement;

import java.io.File;
import java.util.*;
import java.util.jar.JarFile;

/**
 * @author Vladislav.Soroka
 */
public class GradleArtifactBuildTaskProvider extends ArtifactBuildTaskProvider {
  @NotNull
  @Override
  public List<? extends BuildTask> createArtifactBuildTasks(@NotNull JpsArtifact artifact, @NotNull ArtifactBuildPhase buildPhase) {
    String artifactName = artifact.getName();
    if (buildPhase == ArtifactBuildPhase.PRE_PROCESSING && (artifactName.endsWith(" (exploded)"))
        && artifact.getRootElement() instanceof JpsArtifactRootElement) {
      JpsGradleArtifactExtension extension = getArtifactExtension(artifact, buildPhase);
      if (extension != null && extension.getProperties() != null) {
        return Arrays.asList(new GradleManifestGenerationBuildTask(artifact, extension.getProperties()),
                             new GradleAdditionalFilesGenerationBuildTask(artifact, extension.getProperties()));
      }
    }

    return Collections.emptyList();
  }

  @Nullable
  private static JpsGradleArtifactExtension getArtifactExtension(JpsArtifact artifact, ArtifactBuildPhase buildPhase) {
    if (buildPhase == ArtifactBuildPhase.PRE_PROCESSING) {
      return JpsGradleExtensionService.getArtifactExtension(artifact);
    }
    return null;
  }

  private abstract static class GradleGenerationBuildTask extends BuildTask {
    protected final JpsArtifact myArtifact;
    protected final GradleArtifactExtensionProperties myProperties;

    GradleGenerationBuildTask(@NotNull JpsArtifact artifact, @NotNull GradleArtifactExtensionProperties properties) {
      myArtifact = artifact;
      myProperties = properties;
    }
  }

  private static class GradleManifestGenerationBuildTask extends GradleGenerationBuildTask {
    private static final Logger LOG = Logger.getInstance(GradleManifestGenerationBuildTask.class);

    GradleManifestGenerationBuildTask(@NotNull JpsArtifact artifact,
                                             @NotNull GradleArtifactExtensionProperties properties) {
      super(artifact, properties);
    }

    @Override
    public void build(final CompileContext context) {
      if (myProperties.manifest != null) {
        try {
          File output = new File(myArtifact.getOutputPath(), JarFile.MANIFEST_NAME);
          FileUtil.writeToFile(output, Base64.getDecoder().decode(myProperties.manifest));
        }
        // do not fail the whole 'Make' if there is an invalid manifest cached
        catch (Exception e) {
          LOG.debug(e);
        }
      }
    }
  }

  private static class GradleAdditionalFilesGenerationBuildTask extends GradleGenerationBuildTask {
    private static final Logger LOG = Logger.getInstance(GradleAdditionalFilesGenerationBuildTask.class);

    GradleAdditionalFilesGenerationBuildTask(@NotNull JpsArtifact artifact,
                                                    @NotNull GradleArtifactExtensionProperties properties) {
      super(artifact, properties);
    }

    @Override
    public void build(final CompileContext context) {
      if (myProperties.additionalFiles != null) {
        for (Map.Entry<String, String> entry : myProperties.additionalFiles.entrySet()) {
          try {
            File output = new File(entry.getKey());
            FileUtil.writeToFile(output, Base64.getDecoder().decode(entry.getValue()));
          }
          // do not fail the whole 'Make' if there is an invalid file cached
          catch (Exception e) {
            LOG.debug(e);
          }
        }
      }
    }
  }
}

