package com.intellij.find.editorHeaderActions;

import com.intellij.find.FindModel;

public class ToggleInCommentsAction extends EditorHeaderSetSearchContextAction {
  public ToggleInCommentsAction() {
    super("In &Comments", FindModel.SearchContext.IN_COMMENTS);
  }
}
