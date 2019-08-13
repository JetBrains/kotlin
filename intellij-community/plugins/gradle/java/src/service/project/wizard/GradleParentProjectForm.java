// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.project.wizard;

import com.intellij.icons.AllIcons;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.externalSystem.model.ExternalProjectInfo;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager;
import com.intellij.openapi.externalSystem.service.ui.ExternalProjectPathField;
import com.intellij.openapi.externalSystem.service.ui.SelectExternalProjectDialog;
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.EditorTextField;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Consumer;
import com.intellij.util.EmptyConsumer;
import com.intellij.util.NullableConsumer;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;

import static org.jetbrains.plugins.gradle.service.project.wizard.GradleModuleWizardStep.isGradleModuleExist;

public class GradleParentProjectForm implements Disposable {

  private static final String EMPTY_PARENT = "<none>";

  @Nullable
  private final Project myProjectOrNull;
  @Nullable
  private ProjectData myParent;
  @NotNull
  private final Consumer<ProjectData> myConsumer;

  private final boolean myIsVisible;

  private JPanel myPanel;
  private JButton mySelectParent;
  private EditorTextField myParentPathField;
  @NotNull
  private final WizardContext myContext;

  public GradleParentProjectForm(@NotNull WizardContext context, @Nullable NullableConsumer<ProjectData> consumer) {
    myProjectOrNull = context.getProject();
    myContext = context;
    myConsumer = consumer == null ? EmptyConsumer.getInstance() : consumer;
    myIsVisible = !context.isCreatingNewProject() && myProjectOrNull != null && isGradleModuleExist(context);
    initComponents();
  }

  private void createUIComponents() {
    myParentPathField = new TextViewer("", getProject());
  }

  private void initComponents() {
    myPanel.setVisible(myIsVisible);
    if (!myIsVisible) return;
    mySelectParent.setIcon(AllIcons.Actions.Module);
    mySelectParent.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        myParent = doSelectProject(myParent);
        myConsumer.consume(myParent);
      }
    });
    if (myParent == null) {
      myParent = findPotentialParentProject(myProjectOrNull);
    }
  }

  public JPanel getComponent() {
    return myPanel;
  }

  @Nullable
  public ProjectData getParentProject() {
    return myParent;
  }

  public boolean isVisible() {
    return myIsVisible;
  }

  public void updateComponents() {
    if (myIsVisible) {
      myParentPathField.setText(myParent == null ? EMPTY_PARENT : myParent.getLinkedExternalProjectPath());
      collapseIfPossible(myParentPathField, GradleConstants.SYSTEM_ID, getProject());
    }
  }

  @Override
  public void dispose() {
    myParentPathField.removeNotify();
  }

  private ProjectData doSelectProject(ProjectData current) {
    assert myProjectOrNull != null : "must not be called when creating a new project";

    SelectExternalProjectDialog d = new SelectExternalProjectDialog(GradleConstants.SYSTEM_ID, myProjectOrNull, current);
    if (!d.showAndGet()) {
      return current;
    }

    return d.getResult();
  }

  @NotNull
  private Project getProject() {
    Project project = myProjectOrNull != null ? myProjectOrNull : ArrayUtil.getFirstElement(ProjectManager.getInstance().getOpenProjects());
    return project == null ? ProjectManager.getInstance().getDefaultProject() : project;
  }

  @Nullable
  private ProjectData findPotentialParentProject(@Nullable Project project) {
    if (project == null) return null;

    String contextProjectFileDirectory = myContext.getProjectFileDirectory();
    ExternalProjectSettings linkedProjectSettings = null;
    for (Object settings : ExternalSystemApiUtil.getSettings(project, GradleConstants.SYSTEM_ID).getLinkedProjectsSettings()) {
      if (settings instanceof ExternalProjectSettings) {
        String projectPath = ((ExternalProjectSettings)settings).getExternalProjectPath();
        if (FileUtil.isAncestor(projectPath, contextProjectFileDirectory, false)) {
          linkedProjectSettings = (ExternalProjectSettings)settings;
          break;
        }
      }
    }

    if(linkedProjectSettings == null) return null;

    final ExternalProjectInfo projectInfo =
      ProjectDataManager.getInstance().getExternalProjectData(project, GradleConstants.SYSTEM_ID, linkedProjectSettings.getExternalProjectPath());
    return projectInfo != null && projectInfo.getExternalProjectStructure() != null
           ? projectInfo.getExternalProjectStructure().getData() : null;
  }

  private static void collapseIfPossible(@NotNull EditorTextField editorTextField,
                                         @NotNull ProjectSystemId systemId,
                                         @NotNull Project project) {
    Editor editor = editorTextField.getEditor();
    if (editor != null) {
      String rawText = editor.getDocument().getText();
      if (StringUtil.isEmpty(rawText)) return;
      if (EMPTY_PARENT.equals(rawText)) {
        editorTextField.setEnabled(false);
        return;
      }
      final Collection<ExternalProjectInfo> projectsData =
        ProjectDataManager.getInstance().getExternalProjectsData(project, systemId);
      for (ExternalProjectInfo projectInfo : projectsData) {
        if (projectInfo.getExternalProjectStructure() != null && projectInfo.getExternalProjectPath().equals(rawText)) {
          editorTextField.setEnabled(true);
          ExternalProjectPathField.collapse(
            editorTextField.getEditor(), projectInfo.getExternalProjectStructure().getData().getExternalName());
          return;
        }
      }
    }
  }

  private static class TextViewer extends EditorTextField {
    private final boolean myEmbeddedIntoDialogWrapper;
    private final boolean myUseSoftWraps;

    TextViewer(@NotNull String initialText, @NotNull Project project) {
      this(createDocument(initialText), project, true, true);
    }

    TextViewer(@NotNull Document document, @NotNull Project project, boolean embeddedIntoDialogWrapper, boolean useSoftWraps) {
      super(document, project, FileTypes.PLAIN_TEXT, true, false);
      myEmbeddedIntoDialogWrapper = embeddedIntoDialogWrapper;
      myUseSoftWraps = useSoftWraps;
      setFontInheritedFromLAF(false);
    }

    private static Document createDocument(@NotNull String initialText) {
      return EditorFactory.getInstance().createDocument(initialText);
    }

    @Override
    public void setText(@Nullable String text) {
      super.setText(text != null ? StringUtil.convertLineSeparators(text) : null);
    }

    @Override
    protected EditorEx createEditor() {
      final EditorEx editor = super.createEditor();
      editor.setHorizontalScrollbarVisible(true);
      editor.setCaretEnabled(isEnabled());
      editor.getScrollPane().setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
      editor.setEmbeddedIntoDialogWrapper(myEmbeddedIntoDialogWrapper);
      editor.setBorder(UIUtil.getTextFieldBorder());
      editor.setOneLineMode(true);
      editor.getComponent().setPreferredSize(null);
      editor.getSettings().setUseSoftWraps(myUseSoftWraps);
      return editor;
    }

    @Override
    protected void setViewerEnabled(boolean enabled) {
      // do not reset com.intellij.ui.EditorTextField.myIsViewer field
    }

    @Override
    public void removeNotify() {
      // The editor needs to be removed manually because it normally is removed by invokeLater, which may happen to late
      Editor editor = getEditor();
      if (editor != null && !editor.isDisposed()) {
        EditorFactory.getInstance().releaseEditor(editor);
      }
      super.removeNotify();
    }
  }
}
