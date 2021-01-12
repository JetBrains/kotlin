// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.application.options;

/**
 * @author yole
 */
public final class OptionId {
  private OptionId() {
  }

  public static final OptionId RENAME_IN_PLACE = new OptionId();
  public static final OptionId COMPLETION_SMART_TYPE = new OptionId();
  public static final OptionId AUTOCOMPLETE_ON_BASIC_CODE_COMPLETION = new OptionId();
  public static final OptionId SHOW_PARAMETER_NAME_HINTS_ON_COMPLETION = new OptionId();
  public static final OptionId PROJECT_VIEW_SHOW_VISIBILITY_ICONS = new OptionId();
}
