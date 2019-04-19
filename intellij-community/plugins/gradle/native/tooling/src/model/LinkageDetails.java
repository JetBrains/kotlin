// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.nativeplatform.tooling.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.model.ExternalTask;

import java.io.File;
import java.io.Serializable;
import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public interface LinkageDetails extends Serializable {
  ExternalTask getLinkTask();

  File getOutputLocation();

  @NotNull
  List<String> getAdditionalArgs();
}
