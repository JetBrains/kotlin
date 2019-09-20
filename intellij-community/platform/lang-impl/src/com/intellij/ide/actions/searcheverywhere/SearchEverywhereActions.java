// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.searcheverywhere;

import org.jetbrains.annotations.NonNls;

public interface SearchEverywhereActions {

  @NonNls String SWITCH_TO_NEXT_TAB = "SearchEverywhere.NextTab";
  @NonNls String SWITCH_TO_PREV_TAB = "SearchEverywhere.PrevTab";
  @NonNls String AUTOCOMPLETE_COMMAND = "SearchEverywhere.CompleteCommand";
  @NonNls String SELECT_ITEM = "SearchEverywhere.SelectItem";
  @NonNls String NAVIGATE_TO_NEXT_GROUP = "SearchEverywhere.NavigateToNextGroup";
  @NonNls String NAVIGATE_TO_PREV_GROUP = "SearchEverywhere.NavigateToPrevGroup";

}
