// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.fileEditor.impl;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;

public class TestEditorTabGroup {
  private final String name;

  private final LinkedHashMap<VirtualFile, Pair<FileEditor, FileEditorProvider>> myOpenedTabs = new LinkedHashMap<>();
  private VirtualFile myOpenedFile;

  public TestEditorTabGroup(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void openTab(VirtualFile virtualFile, FileEditor fileEditor, FileEditorProvider fileEditorProvider) {
    ApplicationManager.getApplication().assertIsDispatchThread();

    myOpenedTabs.put(virtualFile, Pair.pair(fileEditor, fileEditorProvider));
    myOpenedFile = virtualFile;
  }

  @Nullable
  public Pair<FileEditor, FileEditorProvider> getOpenedEditor(){
    VirtualFile openedFile = getOpenedFile();
    if (openedFile == null) {
      return null;
    }

    return myOpenedTabs.get(openedFile);
  }

  @Nullable
  public VirtualFile getOpenedFile() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    return myOpenedFile;
  }

  public void setOpenedFile(VirtualFile file) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    myOpenedFile = file;
  }

  public void closeTab(VirtualFile virtualFile) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    myOpenedFile = null;
    myOpenedTabs.remove(virtualFile);
  }

  @Nullable
  public Pair<FileEditor, FileEditorProvider> getEditorAndProvider(VirtualFile file) {
    return myOpenedTabs.get(file);
  }

  public boolean contains(VirtualFile file) {
    return myOpenedTabs.containsKey(file);
  }

  public int getTabCount() {
    return myOpenedTabs.size();
  }
}
