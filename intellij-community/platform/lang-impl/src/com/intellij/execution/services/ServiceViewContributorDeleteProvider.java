// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.services;

import com.intellij.ide.DeleteProvider;

public interface ServiceViewContributorDeleteProvider extends DeleteProvider {
  void setFallbackProvider(DeleteProvider provider);
}
