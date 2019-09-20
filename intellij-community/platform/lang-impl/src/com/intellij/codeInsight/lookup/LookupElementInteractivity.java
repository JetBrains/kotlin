// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.lookup;

import com.intellij.openapi.util.Key;

/**
 * @author yole
 */
public interface LookupElementInteractivity {
  Key<LookupElementInteractivity> KEY = Key.create("LookupElementInteractivity");

  boolean isInsertHandlerInteractive(LookupElement element);

  class Simple implements LookupElementInteractivity {
    private final boolean myInteractive;

    private Simple(boolean interactive) {
      myInteractive = interactive;
    }

    @Override
    public boolean isInsertHandlerInteractive(LookupElement element) {
      return myInteractive;
    }
  }

  LookupElementInteractivity NEVER = new Simple(false);
  LookupElementInteractivity ALWAYS = new Simple(true);
}
