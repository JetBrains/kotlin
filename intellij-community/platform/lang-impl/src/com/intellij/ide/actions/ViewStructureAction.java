/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package com.intellij.ide.actions;

import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.ide.structureView.StructureView;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder;
import com.intellij.ide.structureView.impl.StructureViewComposite;
import com.intellij.ide.util.FileStructurePopup;
import com.intellij.ide.util.StructureViewCompositeModel;
import com.intellij.ide.util.treeView.smartTree.TreeStructureUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.PlaceHolder;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class ViewStructureAction extends DumbAwareAction {

  public ViewStructureAction() {
    setEnabledInModalContext(true);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getData(CommonDataKeys.PROJECT);
    if (project == null) return;
    FileEditor fileEditor = e.getData(PlatformDataKeys.FILE_EDITOR);
    if (fileEditor == null) return;

    VirtualFile virtualFile = fileEditor.getFile();
    Editor editor = fileEditor instanceof TextEditor ? ((TextEditor)fileEditor).getEditor() :
                    e.getData(CommonDataKeys.EDITOR);
    if (editor != null) {
      PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument());
    }

    FeatureUsageTracker.getInstance().triggerFeatureUsed("navigation.popup.file.structure");

    FileStructurePopup popup = createPopup(project, fileEditor);
    if (popup == null) return;

    String title = virtualFile == null ? fileEditor.getName() : virtualFile.getName();
    popup.setTitle(title);
    popup.show();
  }

  @Nullable
  public static FileStructurePopup createPopup(@NotNull Project project, @NotNull FileEditor fileEditor) {
    PsiDocumentManager.getInstance(project).commitAllDocuments();
    StructureViewBuilder builder = fileEditor.getStructureViewBuilder();
    if (builder == null) return null;
    StructureView structureView;
    StructureViewModel treeModel;
    if (builder instanceof TreeBasedStructureViewBuilder) {
      structureView = null;
      treeModel = ((TreeBasedStructureViewBuilder)builder).createStructureViewModel(EditorUtil.getEditorEx(fileEditor));
    }
    else {
      structureView = builder.createStructureView(fileEditor, project);
      treeModel = createStructureViewModel(project, fileEditor, structureView);
    }
    if (treeModel instanceof PlaceHolder) {
      //noinspection unchecked
      ((PlaceHolder)treeModel).setPlace(TreeStructureUtil.PLACE);
    }
    FileStructurePopup popup = new FileStructurePopup(project, fileEditor, treeModel);
    if (structureView != null) Disposer.register(popup, structureView);
    return popup;
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    Project project = e.getData(CommonDataKeys.PROJECT);
    if (project == null) {
      e.getPresentation().setEnabled(false);
      return;
    }

    FileEditor fileEditor = e.getData(PlatformDataKeys.FILE_EDITOR);
    Editor editor = fileEditor instanceof TextEditor ? ((TextEditor)fileEditor).getEditor() :
                    e.getData(CommonDataKeys.EDITOR);

    boolean enabled = fileEditor != null &&
                      (!Boolean.TRUE.equals(EditorTextField.SUPPLEMENTARY_KEY.get(editor))) &&
                      fileEditor.getStructureViewBuilder() != null;
    e.getPresentation().setEnabled(enabled);
  }

  @NotNull
  public static StructureViewModel createStructureViewModel(@NotNull Project project,
                                                            @NotNull FileEditor fileEditor,
                                                            @NotNull StructureView structureView) {
    StructureViewModel treeModel;
    VirtualFile virtualFile = fileEditor.getFile();
    if (structureView instanceof StructureViewComposite && virtualFile != null) {
      StructureViewComposite.StructureViewDescriptor[] views = ((StructureViewComposite)structureView).getStructureViews();
      PsiFile psiFile = ObjectUtils.notNull(PsiManager.getInstance(project).findFile(virtualFile));
      treeModel = new StructureViewCompositeModel(psiFile, EditorUtil.getEditorEx(fileEditor), Arrays.asList(views));
      Disposer.register(structureView, treeModel);
    }
    else {
      treeModel = structureView.getTreeModel();
    }
    return treeModel;
  }
}
