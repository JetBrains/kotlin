// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.model.data;

import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.serialization.PropertyMapping;
import com.intellij.util.containers.WeakInterner;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

import static com.intellij.util.containers.ContainerUtil.immutableList;

public class AnnotationProcessingData {
  public static final Key<AnnotationProcessingData> KEY = Key.create(AnnotationProcessingData.class, ExternalSystemConstants.UNORDERED);
  public static final Key<AnnotationProcessorOutput> OUTPUT_KEY =
    Key.create(AnnotationProcessorOutput.class, ExternalSystemConstants.UNORDERED);

  private static final WeakInterner<AnnotationProcessingData> ourInterner = new WeakInterner<>();

  private final Collection<String> path;
  private final Collection<String> arguments;

  public static AnnotationProcessingData create(@NotNull Collection<String> path,
                       @NotNull Collection<String> arguments) {
    return ourInterner.intern(new AnnotationProcessingData(path, arguments));
  }

  @PropertyMapping({"path", "arguments"})
  private AnnotationProcessingData(@NotNull Collection<String> path,
                                  @NotNull Collection<String> arguments) {
    this.path = immutableList(new ArrayList<>(path));
    this.arguments = immutableList(new ArrayList<>(arguments));
  }

  /**
   * Annotation processor arguments
   * @return immutable collection of arguments
   */
  public Collection<String> getArguments() {
    return arguments;
  }

  /**
   * Annotation processor path
   * @return immutable collection of path elements
   */
  public Collection<String> getPath() {
    return path;
  }

  public static class AnnotationProcessorOutput {
    private final String outputPath;
    private final boolean testSources;

    @PropertyMapping({"outputPath", "testSources"})
    public AnnotationProcessorOutput(@NotNull String path, boolean isTestSources) {
      outputPath = path;
      testSources = isTestSources;
    }

    @NotNull
    public String getOutputPath() {
      return outputPath;
    }

    public boolean isTestSources() {
      return testSources;
    }
  }
}
