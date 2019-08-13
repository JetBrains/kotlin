// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.util.gotoByName;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.util.Processor;
import com.intellij.util.indexing.FindSymbolParameters;
import org.jetbrains.annotations.NotNull;

/**
 * @author gregsh
 */
public interface ChooseByNameInScopeItemProvider extends ChooseByNameItemProvider {
  boolean filterElements(@NotNull ChooseByNameBase base,
                         @NotNull FindSymbolParameters parameters,
                         @NotNull ProgressIndicator indicator,
                         @NotNull Processor<Object> consumer);
}
