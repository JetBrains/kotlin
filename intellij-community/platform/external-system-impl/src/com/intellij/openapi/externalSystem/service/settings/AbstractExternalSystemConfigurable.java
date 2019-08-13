// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.settings;

import com.intellij.openapi.externalSystem.ExternalSystemManager;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings;
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.openapi.externalSystem.settings.ExternalSystemSettingsListener;
import com.intellij.openapi.externalSystem.util.*;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.SideBorder;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.ContainerUtilRt;
import com.intellij.util.ui.GridBag;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Base class that simplifies external system settings management.
 * @author Denis Zhdanov
 */
public abstract class AbstractExternalSystemConfigurable<
  ProjectSettings extends ExternalProjectSettings,
  L extends ExternalSystemSettingsListener<ProjectSettings>,
  SystemSettings extends AbstractExternalSystemSettings<SystemSettings, ProjectSettings, L>
  > implements SearchableConfigurable, Configurable.NoScroll
{

  @NotNull private final List<ExternalSystemSettingsControl<ProjectSettings>> myProjectSettingsControls =
    new ArrayList<>();

  @NotNull private final ProjectSystemId myExternalSystemId;
  @NotNull private final Project         myProject;

  @Nullable private ExternalSystemSettingsControl<SystemSettings>  mySystemSettingsControl;
  @Nullable private ExternalSystemSettingsControl<ProjectSettings> myActiveProjectSettingsControl;

  private PaintAwarePanel  myComponent;
  private JBList           myProjectsList;
  private DefaultListModel myProjectsModel;

  protected AbstractExternalSystemConfigurable(@NotNull Project project, @NotNull ProjectSystemId externalSystemId) {
    myProject = project;
    myExternalSystemId = externalSystemId;
  }

  @NotNull
  public Project getProject() {
    return myProject;
  }

  @Nls
  @Override
  public String getDisplayName() {
    return myExternalSystemId.getReadableName();
  }

  @Nullable
  @Override
  public JComponent createComponent() {
    if (myComponent == null) {
      myComponent = new PaintAwarePanel(new GridBagLayout());
      SystemSettings settings = getSettings();
      prepareSystemSettings(settings);
      prepareProjectSettings(settings);
    }
    return myComponent;
  }

  @SuppressWarnings("unchecked")
  @NotNull
  private SystemSettings getSettings() {
    ExternalSystemManager<ProjectSettings, L, SystemSettings, ?, ?> manager =
      (ExternalSystemManager<ProjectSettings, L, SystemSettings, ?, ?>)ExternalSystemApiUtil.getManager(myExternalSystemId);
    assert manager != null;
    return manager.getSettingsProvider().fun(myProject);
  }

  @SuppressWarnings("unchecked")
  private void prepareProjectSettings(@NotNull SystemSettings s) {
    List<ProjectSettings> settings = ContainerUtilRt.newArrayList(s.getLinkedProjectsSettings());
    if (settings.isEmpty()) {
      ExternalSystemUiUtil.fillBottom(myComponent);
      return;
    }

    myComponent.add(new TitledSeparator(ExternalSystemBundle.message("settings.title.projects.settings",
                                                                     myExternalSystemId.getReadableName())),
                    ExternalSystemUiUtil.getFillLineConstraints(0));

    OnePixelSplitter splitter = new OnePixelSplitter(false, .16f);

    myProjectsModel = new DefaultListModel();
    myProjectsList = new JBList(myProjectsModel);
    myProjectsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    JBScrollPane scrollPane = new JBScrollPane(myProjectsList);
    scrollPane.setBorder(IdeBorderFactory.createBorder(SideBorder.LEFT | SideBorder.TOP | SideBorder.BOTTOM));
    splitter.setFirstComponent(scrollPane);

    PaintAwarePanel details = new PaintAwarePanel(new GridBagLayout());
    splitter.setSecondComponent(details);

    myComponent.add(splitter,
                    ExternalSystemUiUtil
                      .getFillLineConstraints(1)
                      .fillCell()
                      .weighty(1)
                      .pady(JBUIScale.scale(30)));

    ContainerUtil.sort(settings, Comparator.comparing(s2 -> getProjectName(s2.getExternalProjectPath())));

    myProjectSettingsControls.clear();
    for (ProjectSettings setting : settings) {
      ExternalSystemSettingsControl<ProjectSettings> control = createProjectSettingsControl(setting);
      control.fillUi(details, 1);
      myProjectsModel.addElement(getProjectName(setting.getExternalProjectPath()));
      myProjectSettingsControls.add(control);
      if (control instanceof AbstractExternalProjectSettingsControl<?>) {
        ((AbstractExternalProjectSettingsControl)control).setCurrentProject(myProject);
      }
      control.showUi(false);
    }
    ExternalSystemUiUtil.fillBottom(details);

    myProjectsList.addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) {
          return;
        }
        int i = myProjectsList.getSelectedIndex();
        if (i < 0) {
          return;
        }
        if (myActiveProjectSettingsControl != null) {
          myActiveProjectSettingsControl.showUi(false);
        }
        myActiveProjectSettingsControl = myProjectSettingsControls.get(i);
        myActiveProjectSettingsControl.showUi(true);
      }
    });


    if (!myProjectsModel.isEmpty()) {
      myProjectsList.setSelectedIndex(0);
    }
  }

  public void selectProject(@NotNull String linkedProjectPath) {
    myProjectsList.setSelectedValue(getProjectName(linkedProjectPath), true);
  }

  /**
   * Creates a control for managing given project settings.
   *
   * @param settings  target external project settings
   * @return          control for managing given project settings
   */
  @NotNull
  protected abstract ExternalSystemSettingsControl<ProjectSettings> createProjectSettingsControl(@NotNull ProjectSettings settings);

  @NotNull
  protected String getProjectName(@NotNull String path) {
    File file = new File(path);
    return file.isDirectory() || file.getParentFile() == null ? file.getName() : file.getParentFile().getName();
  }

  private void prepareSystemSettings(@NotNull SystemSettings s) {
    mySystemSettingsControl = createSystemSettingsControl(s);
    if (mySystemSettingsControl != null) {
      PaintAwarePanel panel = new PaintAwarePanel();
      GridBag constraints = new GridBag().weightx(1).coverLine().fillCellHorizontally().anchor(GridBagConstraints.WEST);
      constraints.insetBottom(UIUtil.DEFAULT_VGAP);
      myComponent.add(panel, constraints);

      constraints = ExternalSystemUiUtil.getFillLineConstraints(0);
      constraints.insets.top = 0;

      panel.add(new TitledSeparator(ExternalSystemBundle.message("settings.title.system.settings")), constraints);
      mySystemSettingsControl.fillUi(panel, 1);
    }
  }

  /**
   * Creates a control for managing given system-level settings (if any).
   *
   * @param settings  target system settings
   * @return          a control for managing given system-level settings;
   *                  {@code null} if current external system doesn't have system-level settings (only project-level settings)
   */
  @Nullable
  protected abstract ExternalSystemSettingsControl<SystemSettings> createSystemSettingsControl(@NotNull SystemSettings settings);

  @Override
  public boolean isModified() {
    for (ExternalSystemSettingsControl<ProjectSettings> control : myProjectSettingsControls) {
      if (control.isModified()) {
        return true;
      }
    }
    return mySystemSettingsControl != null && mySystemSettingsControl.isModified();
  }

  @Override
  public void apply() throws ConfigurationException {
    SystemSettings systemSettings = getSettings();
    L publisher = systemSettings.getPublisher();
    publisher.onBulkChangeStart();
    try {
      List<ProjectSettings> projectSettings = new ArrayList<>();
      for (ExternalSystemSettingsControl<ProjectSettings> control : myProjectSettingsControls) {
        ProjectSettings s = newProjectSettings();
        s.setupNewProjectDefault();
        control.apply(s);
        projectSettings.add(s);
      }
      systemSettings.setLinkedProjectsSettings(projectSettings);
      for (ExternalSystemSettingsControl<ProjectSettings> control : myProjectSettingsControls) {
        if(control instanceof AbstractExternalProjectSettingsControl) {
          ((AbstractExternalProjectSettingsControl)control).updateInitialSettings();
        }
      }
      if (mySystemSettingsControl != null) {
        mySystemSettingsControl.apply(systemSettings);
      }
    }
    finally {
      publisher.onBulkChangeEnd();
    }
  }

  /**
   * @return    new empty project-level settings object
   */
  @NotNull
  protected abstract ProjectSettings newProjectSettings();

  @Override
  public void reset() {
    for (ExternalSystemSettingsControl<ProjectSettings> control : myProjectSettingsControls) {
      control.reset(myProject);
    }
    if (mySystemSettingsControl != null) {
      mySystemSettingsControl.reset(myProject);
    }
  }

  @Override
  public void disposeUIResources() {
    for (ExternalSystemSettingsControl<ProjectSettings> control : myProjectSettingsControls) {
      control.disposeUIResources();
    }
    if (mySystemSettingsControl != null) {
      mySystemSettingsControl.disposeUIResources();
    }
    myProjectSettingsControls.clear();
    myComponent = null;
    myProjectsList = null;
    myProjectsModel = null;
    mySystemSettingsControl = null;
  }

  @TestOnly
  @NotNull
  List<ExternalSystemSettingsControl<ProjectSettings>> getProjectSettingsControls() {
    return Collections.unmodifiableList(myProjectSettingsControls);
  }
}
