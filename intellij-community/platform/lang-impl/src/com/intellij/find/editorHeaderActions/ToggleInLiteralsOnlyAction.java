package com.intellij.find.editorHeaderActions;

import com.intellij.find.FindModel;

public class ToggleInLiteralsOnlyAction extends EditorHeaderSetSearchContextAction {
  public ToggleInLiteralsOnlyAction() {
    super("In String &Literals", FindModel.SearchContext.IN_STRING_LITERALS);
  }
}
