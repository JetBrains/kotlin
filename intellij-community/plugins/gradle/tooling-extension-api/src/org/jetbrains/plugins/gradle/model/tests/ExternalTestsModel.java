// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.model.tests;

import org.gradle.tooling.model.Model;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.List;

public interface ExternalTestsModel extends Model, Serializable {

  @NotNull
  List<ExternalTestSourceMapping> getTestSourceMappings();
}
