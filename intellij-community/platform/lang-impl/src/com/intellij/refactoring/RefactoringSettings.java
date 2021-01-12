// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.refactoring;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;

@State(name = "BaseRefactoringSettings", storages = @Storage("baseRefactoring.xml"))
public class RefactoringSettings implements PersistentStateComponent<RefactoringSettings> {
  public static RefactoringSettings getInstance() {
    return ServiceManager.getService(RefactoringSettings.class);
  }

  public boolean SAFE_DELETE_WHEN_DELETE = true;
  public boolean SAFE_DELETE_SEARCH_IN_COMMENTS = true;
  public boolean SAFE_DELETE_SEARCH_IN_NON_JAVA = true;

  public boolean RENAME_SEARCH_IN_COMMENTS_FOR_FILE = true;
  public boolean RENAME_SEARCH_FOR_TEXT_FOR_FILE = true;

  public boolean RENAME_SEARCH_FOR_REFERENCES_FOR_FILE = true;
  public boolean RENAME_SEARCH_FOR_REFERENCES_FOR_DIRECTORY = true;

  public boolean MOVE_SEARCH_FOR_REFERENCES_FOR_FILE = true;

  @Override
  public RefactoringSettings getState() {
    return this;
  }

  @Override
  public void loadState(@NotNull final RefactoringSettings state) {
    XmlSerializerUtil.copyBean(state, this);
  }
}
