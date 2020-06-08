// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.framework.detection.impl;

import com.intellij.facet.FacetType;
import com.intellij.framework.FrameworkType;
import com.intellij.framework.detection.DetectedFrameworkDescription;
import com.intellij.framework.detection.FacetBasedFrameworkDetector;
import com.intellij.framework.detection.FrameworkDetector;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.roots.ModifiableModelsProvider;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FrameworkDetectionUtil {
  private FrameworkDetectionUtil() {
  }

  @Nullable
  public static FrameworkType findFrameworkTypeForFacetDetector(@NotNull FacetType<?, ?> facetType) {
    for (FrameworkDetector detector : FrameworkDetector.EP_NAME.getExtensions()) {
      if (detector instanceof FacetBasedFrameworkDetector<?, ?> &&
          ((FacetBasedFrameworkDetector)detector).getFacetType().equals(facetType)) {
        return detector.getFrameworkType();
      }
    }
    return null;
  }

  public static List<? extends DetectedFrameworkDescription> removeDisabled(List<? extends DetectedFrameworkDescription> descriptions) {
    return removeDisabled(descriptions, Collections.emptyList());
  }

  public static List<DetectedFrameworkDescription> getDisabledDescriptions(@NotNull List<? extends DetectedFrameworkDescription> currentDescriptions,
                                                                           @NotNull List<? extends DetectedFrameworkDescription> otherDescriptions) {
    return doGetDisabledDescriptions(currentDescriptions, ContainerUtil.concat(currentDescriptions, otherDescriptions));
  }

  private static List<DetectedFrameworkDescription> doGetDisabledDescriptions(@NotNull List<? extends DetectedFrameworkDescription> currentDescriptions,
                                                                              @NotNull List<? extends DetectedFrameworkDescription> allDescriptions) {
    List<DetectedFrameworkDescription> disabled = new ArrayList<>();
    for (DetectedFrameworkDescription description : currentDescriptions) {
      if (!description.canSetupFramework(allDescriptions)) {
        disabled.add(description);
      }
    }
    if (!disabled.isEmpty()) {
      List<DetectedFrameworkDescription> remaining = new ArrayList<>(currentDescriptions);
      remaining.removeAll(disabled);
      disabled.addAll(doGetDisabledDescriptions(remaining, allDescriptions));
    }
    return disabled;
  }

  public static List<? extends DetectedFrameworkDescription> removeDisabled(@NotNull List<? extends DetectedFrameworkDescription> currentDescriptions,
                                                                            @NotNull List<? extends DetectedFrameworkDescription> otherDescriptions) {
    final List<DetectedFrameworkDescription> disabled = getDisabledDescriptions(currentDescriptions, otherDescriptions);
    if (disabled.isEmpty()) return currentDescriptions;
    final List<DetectedFrameworkDescription> descriptions = new ArrayList<>(currentDescriptions);
    descriptions.removeAll(disabled);
    return descriptions;
  }

  public static void setupFrameworks(List<? extends DetectedFrameworkDescription> descriptions,
                                     final ModifiableModelsProvider modelsProvider, final ModulesProvider modulesProvider) {
    WriteAction.run(() -> {
      List<DetectedFrameworkDescription> sortedDescriptions = new ArrayList<>();
      //todo[nik] perform real sorting
      for (DetectedFrameworkDescription description : descriptions) {
        if (description.getDetector().getUnderlyingFrameworkType() == null) {
          sortedDescriptions.add(description);
        }
      }
      for (DetectedFrameworkDescription description : descriptions) {
        if (description.getDetector().getUnderlyingFrameworkType() != null) {
          sortedDescriptions.add(description);
        }
      }
      for (DetectedFrameworkDescription description : sortedDescriptions) {
        description.setupFramework(modelsProvider, modulesProvider);
      }
    });
  }
}
