/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.psi.impl.source.tree.injected;

import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;

public class Place extends SmartList<PsiLanguageInjectionHost.Shred> {
  Place() {
  }

  @NotNull
  public SmartPsiElementPointer<PsiLanguageInjectionHost> getHostPointer() {
    return ((ShredImpl)get(0)).getSmartPointer();
  }

  boolean isValid() {
    for (PsiLanguageInjectionHost.Shred shred : this) {
      if (!shred.isValid()) {
        return false;
      }
    }
    return true;
  }

  void dispose() {
    for (PsiLanguageInjectionHost.Shred shred : this) {
      shred.dispose();
    }
  }
}
