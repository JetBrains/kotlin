/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.codeInspection.actions;

import com.intellij.analysis.AnalysisScope;
import com.intellij.analysis.BaseAnalysisAction;
import com.intellij.analysis.BaseAnalysisActionDialog;
import com.intellij.application.options.schemes.SchemesCombo;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.InspectionProfile;
import com.intellij.codeInspection.InspectionsBundle;
import com.intellij.codeInspection.ex.GlobalInspectionContextImpl;
import com.intellij.codeInspection.ex.InspectionManagerEx;
import com.intellij.codeInspection.ex.InspectionProfileImpl;
import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.options.ex.SingleConfigurableEditor;
import com.intellij.openapi.project.Project;
import com.intellij.profile.codeInspection.InspectionProfileManager;
import com.intellij.profile.codeInspection.InspectionProjectProfileManager;
import com.intellij.profile.codeInspection.ProjectInspectionProfileManager;
import com.intellij.profile.codeInspection.ui.ErrorsConfigurable;
import com.intellij.profile.codeInspection.ui.header.InspectionProfileSchemesModel;
import com.intellij.profile.codeInspection.ui.header.InspectionToolsConfigurable;
import com.intellij.ui.ComboboxWithBrowseButton;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class CodeInspectionAction extends BaseAnalysisAction {
  private static final Logger LOG = Logger.getInstance(CodeInspectionAction.class);
  private static final String LAST_SELECTED_PROFILE_PROP = "run.code.analysis.last.selected.profile";

  private GlobalInspectionContextImpl myGlobalInspectionContext;
  protected InspectionProfileImpl myExternalProfile;

  public CodeInspectionAction() {
    super(InspectionsBundle.message("inspection.action.title"), InspectionsBundle.message("inspection.action.noun"));
  }

  public CodeInspectionAction(String title, String analysisNoon) {
    super(title, analysisNoon);
  }

  @Override
  protected void analyze(@NotNull Project project, @NotNull AnalysisScope scope) {
    FeatureUsageTracker.getInstance().triggerFeatureUsed("codeassist.inspect.batch");
    try {
      runInspections(project, scope);
    }
    finally {
      myGlobalInspectionContext = null;
      myExternalProfile = null;
    }
  }

  protected void runInspections(Project project, AnalysisScope scope) {
    scope.setSearchInLibraries(false);
    FileDocumentManager.getInstance().saveAllDocuments();
    final GlobalInspectionContextImpl inspectionContext = getGlobalInspectionContext(project);
    inspectionContext.setExternalProfile(myExternalProfile);
    inspectionContext.setCurrentScope(scope);
    inspectionContext.doInspections(scope);
  }


  private GlobalInspectionContextImpl getGlobalInspectionContext(Project project) {
    if (myGlobalInspectionContext == null) {
      myGlobalInspectionContext = ((InspectionManagerEx)InspectionManager.getInstance(project)).createNewGlobalContext();
    }
    return myGlobalInspectionContext;
  }

  @Override
  @NonNls
  protected String getHelpTopic() {
    return "reference.dialogs.inspection.scope";
  }

  @Override
  protected void canceled() {
    super.canceled();
    myGlobalInspectionContext = null;
  }

  @Override
  protected JComponent getAdditionalActionSettings(@NotNull final Project project, final BaseAnalysisActionDialog dialog) {
    final AdditionalPanel panel = new AdditionalPanel();
    final InspectionManagerEx manager = (InspectionManagerEx)InspectionManager.getInstance(project);
    final SchemesCombo<InspectionProfileImpl> profiles = (SchemesCombo<InspectionProfileImpl>)panel.myBrowseProfilesCombo.getComboBox();
    final InspectionProfileManager profileManager = InspectionProfileManager.getInstance();
    final ProjectInspectionProfileManager projectProfileManager = ProjectInspectionProfileManager.getInstance(project);
    panel.myBrowseProfilesCombo.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        final ExternalProfilesComboboxAwareInspectionToolsConfigurable errorConfigurable = createConfigurable(projectProfileManager, profiles);
        final MySingleConfigurableEditor editor = new MySingleConfigurableEditor(project, errorConfigurable, manager);
        if (editor.showAndGet()) {
          reloadProfiles(profiles, profileManager, projectProfileManager, project);
          if (errorConfigurable.mySelectedName != null) {
            final InspectionProfileImpl profile = (errorConfigurable.mySelectedIsProjectProfile ? projectProfileManager : profileManager)
              .getProfile(errorConfigurable.mySelectedName);
            profiles.selectScheme(profile);
          }
        }
        else {
          //if profile was disabled and cancel after apply was pressed
          final InspectionProfile profile = profiles.getSelectedScheme();
          final boolean canExecute = profile != null && profile.isExecutable(project);
          dialog.setOKActionEnabled(canExecute);
        }
      }
    });
    profiles.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        myExternalProfile = profiles.getSelectedScheme();
        final boolean canExecute = myExternalProfile != null && myExternalProfile.isExecutable(project);
        dialog.setOKActionEnabled(canExecute);
        if (canExecute) {
          PropertiesComponent.getInstance(project).setValue(LAST_SELECTED_PROFILE_PROP, (myExternalProfile.isProjectLevel() ? 'p' : 'a') + myExternalProfile.getName());
          manager.setProfile(myExternalProfile.getName());
        }
      }
    });
    reloadProfiles(profiles, profileManager, projectProfileManager, project);
    return panel.myAdditionalPanel;
  }

  protected ExternalProfilesComboboxAwareInspectionToolsConfigurable createConfigurable(ProjectInspectionProfileManager projectProfileManager,
                                                                                        SchemesCombo<InspectionProfileImpl> profilesCombo) {
    return new ExternalProfilesComboboxAwareInspectionToolsConfigurable(projectProfileManager, profilesCombo);
  }

  protected static class ExternalProfilesComboboxAwareInspectionToolsConfigurable extends InspectionToolsConfigurable {
    private final SchemesCombo<InspectionProfileImpl> myProfilesCombo;
    private String mySelectedName;
    private boolean mySelectedIsProjectProfile;

    public ExternalProfilesComboboxAwareInspectionToolsConfigurable(@NotNull ProjectInspectionProfileManager projectProfileManager, SchemesCombo<InspectionProfileImpl> profilesCombo) {
      super(projectProfileManager);
      myProfilesCombo = profilesCombo;
    }

    @Override
    protected InspectionProfileImpl getCurrentProfile() {
      return myProfilesCombo.getSelectedScheme();
    }

    @Override
    protected void applyRootProfile(@NotNull String name, boolean isProjectLevel) {
      mySelectedName = name;
      mySelectedIsProjectProfile = isProjectLevel;
    }
  }

  private void reloadProfiles(SchemesCombo<InspectionProfileImpl> profilesCombo,
                              InspectionProfileManager appProfileManager,
                              InspectionProjectProfileManager projectProfileManager,
                              Project project) {
    profilesCombo.resetSchemes(InspectionProfileSchemesModel.getSortedProfiles(appProfileManager, projectProfileManager));
    InspectionProfileImpl selectedProfile = getProfileToUse(project, appProfileManager, projectProfileManager);
    profilesCombo.selectScheme(selectedProfile);
  }

  @NotNull
  private InspectionProfileImpl getProfileToUse(@NotNull Project project,
                                               @NotNull InspectionProfileManager appProfileManager,
                                               @NotNull InspectionProjectProfileManager projectProfileManager) {
    final String lastSelectedProfile = PropertiesComponent.getInstance(project).getValue(LAST_SELECTED_PROFILE_PROP);
    if (lastSelectedProfile != null) {
      final char type = lastSelectedProfile.charAt(0);
      final String lastSelectedProfileName = lastSelectedProfile.substring(1);
      if (type == 'a') {
        final InspectionProfileImpl profile = appProfileManager.getProfile(lastSelectedProfileName, false);
        if (profile != null) return profile;
      } else {
        LOG.assertTrue(type == 'p', "Unexpected last selected profile: \'" + lastSelectedProfile + "\'");
        final InspectionProfileImpl profile = projectProfileManager.getProfile(lastSelectedProfileName, false);
        if (profile != null && profile.isProjectLevel()) return profile;
      }
    }
    return getGlobalInspectionContext(project).getCurrentProfile();
  }

  private static class AdditionalPanel {
    public ComboboxWithBrowseButton myBrowseProfilesCombo;
    public JPanel myAdditionalPanel;

    private void createUIComponents() {
      myBrowseProfilesCombo = new ComboboxWithBrowseButton(new SchemesCombo<InspectionProfileImpl>() {
        @Override
        protected boolean supportsProjectSchemes() {
          return true;
        }

        @Override
        protected boolean isProjectScheme(@NotNull InspectionProfileImpl profile) {
          return profile.isProjectLevel();
        }

        @NotNull
        @Override
        protected SimpleTextAttributes getSchemeAttributes(InspectionProfileImpl profile) {
          return SimpleTextAttributes.REGULAR_ATTRIBUTES;
        }
      });
    }
  }

  private static class MySingleConfigurableEditor extends SingleConfigurableEditor {
    private final InspectionManagerEx myManager;

    MySingleConfigurableEditor(final Project project, final ErrorsConfigurable configurable, InspectionManagerEx manager) {
      super(project, configurable, createDimensionKey(configurable));
      myManager = manager;
    }


    @Override
    protected void doOKAction() {
      final Object o = ((ErrorsConfigurable)getConfigurable()).getSelectedObject();
      if (o instanceof InspectionProfile) {
        myManager.setProfile(((InspectionProfile)o).getName());
      }
      super.doOKAction();
    }
  }
}
