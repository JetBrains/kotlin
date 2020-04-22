// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.profile.codeInspection.ui.filter;

import com.intellij.analysis.AnalysisBundle;
import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInsight.daemon.impl.SeverityRegistrar;
import com.intellij.codeInspection.ex.InspectionProfileImpl;
import com.intellij.codeInspection.ex.ScopeToolState;
import com.intellij.icons.AllIcons;
import com.intellij.idea.ActionsBundle;
import com.intellij.lang.Language;
import com.intellij.lang.MetaLanguage;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.CheckboxAction;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.profile.codeInspection.ui.LevelChooserAction;
import com.intellij.profile.codeInspection.ui.SingleInspectionProfilePanel;
import com.intellij.ui.FilterComponent;
import com.intellij.util.SmartList;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * @author Dmitry Batkovich
 */
public class InspectionFilterAction extends DefaultActionGroup implements Toggleable, DumbAware {
  private final static int MIN_LANGUAGE_COUNT_TO_WRAP = 11;

  private final SeverityRegistrar mySeverityRegistrar;
  private final InspectionsFilter myInspectionsFilter;
  @NotNull private final FilterComponent myFilterComponent;

  public InspectionFilterAction(@NotNull InspectionProfileImpl profile,
                                @NotNull InspectionsFilter inspectionsFilter,
                                @NotNull Project project,
                                @NotNull FilterComponent filterComponent) {
    super(ActionsBundle.message("action.InspectionFilterAction.filter.inspections.text"), true);
    myInspectionsFilter = inspectionsFilter;
    myFilterComponent = filterComponent;
    mySeverityRegistrar = profile.getProfileManager().getSeverityRegistrar();
    getTemplatePresentation().setIcon(AllIcons.General.Filter);
    tune(profile, project);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    super.update(e);
    Toggleable.setSelected(e.getPresentation(), !myInspectionsFilter.isEmptyFilter());
  }

  private void tune(InspectionProfileImpl profile, Project project) {
    addAction(new ResetFilterAction());
    addSeparator();
    if (ApplicationNamesInfo.getInstance().getProductName().contains("IDEA")) {
      // minor IDEs don't have "New in XXX" in inspection descriptions
      addAction(new ShowNewInspectionsAction());
    }
    addSeparator();

    addAction(new ShowEnabledOrDisabledInspectionsAction(true));
    addAction(new ShowEnabledOrDisabledInspectionsAction(false));
    addAction(new ShowOnlyModifiedInspectionsAction());
    addSeparator();

    for (final HighlightSeverity severity : LevelChooserAction.getSeverities(mySeverityRegistrar)) {
      add(new ShowWithSpecifiedSeverityInspectionsAction(severity));
    }
    addSeparator();

    final Set<String> languageIds = new THashSet<>();
    for (ScopeToolState state : profile.getDefaultStates(project)) {
      final String language = state.getTool().getLanguage();
      if (language != null) languageIds.add(language);
    }

    final List<Language> languages = new SmartList<>();
    for (String id : languageIds) {
      if (id != null) {
        final Language language = Language.findLanguageByID(id);
        if (language != null && !(language instanceof MetaLanguage)) {
          languages.add(language);
        }
      }
    }

    if (!languages.isEmpty()) {
      final DefaultActionGroup languageActionGroupParent =
        new DefaultActionGroup(ActionsBundle.message("action.InspectionFilterAction.filter.by.language.text"), languages.size() >= MIN_LANGUAGE_COUNT_TO_WRAP);
      add(languageActionGroupParent);
      languages.sort(Comparator.comparing(Language::getDisplayName));
      for (Language language : languages) {
        languageActionGroupParent.add(new LanguageFilterAction(language));
      }
      languageActionGroupParent.add(new LanguageFilterAction(null));
      addSeparator();
    }

    add(new ShowAvailableOnlyOnAnalyzeInspectionsAction());
    add(new ShowOnlyCleanupInspectionsAction());
  }

  private class ResetFilterAction extends DumbAwareAction {
    ResetFilterAction() {
      super(ActionsBundle.messagePointer("action.ResetFilterAction.text"));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      myInspectionsFilter.reset();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      final Presentation presentation = e.getPresentation();
      presentation.setEnabled(!myInspectionsFilter.isEmptyFilter());
    }
  }

  private class ShowOnlyCleanupInspectionsAction extends CheckboxAction implements DumbAware{
    ShowOnlyCleanupInspectionsAction() {
      super(AnalysisBundle.message("inspections.settings.show.only.cleanup.text"));
    }

    @Override
    public boolean isSelected(@NotNull final AnActionEvent e) {
      return myInspectionsFilter.isShowOnlyCleanupInspections();
    }

    @Override
    public void setSelected(@NotNull final AnActionEvent e, final boolean state) {
      myInspectionsFilter.setShowOnlyCleanupInspections(state);
    }
  }

  private class ShowAvailableOnlyOnAnalyzeInspectionsAction extends CheckboxAction implements DumbAware {

    ShowAvailableOnlyOnAnalyzeInspectionsAction() {
      super(AnalysisBundle.message("inspections.settings.show.only.batch.text"));
    }

    @Override
    public boolean isSelected(@NotNull final AnActionEvent e) {
      return myInspectionsFilter.isAvailableOnlyForAnalyze();
    }

    @Override
    public void setSelected(@NotNull final AnActionEvent e, final boolean state) {
      myInspectionsFilter.setAvailableOnlyForAnalyze(state);
    }
  }

  private class ShowWithSpecifiedSeverityInspectionsAction extends CheckboxAction implements DumbAware {

    private final HighlightSeverity mySeverity;

    private ShowWithSpecifiedSeverityInspectionsAction(final HighlightSeverity severity) {
      super(SingleInspectionProfilePanel.renderSeverity(severity),
            null,
            HighlightDisplayLevel.find(severity).getIcon());
      mySeverity = severity;
    }


    @Override
    public boolean isSelected(@NotNull final AnActionEvent e) {
      return myInspectionsFilter.containsSeverity(mySeverity);
    }

    @Override
    public void setSelected(@NotNull final AnActionEvent e, final boolean state) {
      if (state) {
        myInspectionsFilter.addSeverity(mySeverity);
      } else {
        myInspectionsFilter.removeSeverity(mySeverity);
      }
    }
  }

  private class ShowEnabledOrDisabledInspectionsAction extends CheckboxAction implements DumbAware{

    private final Boolean myShowEnabledActions;

    ShowEnabledOrDisabledInspectionsAction(final boolean showEnabledActions) {
      super(showEnabledActions ? AnalysisBundle.message("inspections.settings.show.only.enabled.text")
                               : AnalysisBundle.message("inspections.settings.show.only.disabled.text"));
      myShowEnabledActions = showEnabledActions;
    }


    @Override
    public boolean isSelected(@NotNull final AnActionEvent e) {
      return myInspectionsFilter.getSuitableInspectionsStates() == myShowEnabledActions;
    }

    @Override
    public void setSelected(@NotNull final AnActionEvent e, final boolean state) {
      final boolean previousState = isSelected(e);
      myInspectionsFilter.setSuitableInspectionsStates(previousState ? null : myShowEnabledActions);
    }
  }

  private class LanguageFilterAction extends CheckboxAction implements DumbAware {
    private final Language myLanguage;

    LanguageFilterAction(final @Nullable Language language) {
      super(language == null ? AnalysisBundle.message("inspections.settings.language.not.specified.warning") : language.getDisplayName());
      myLanguage = language;
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
      return myInspectionsFilter.containsLanguage(myLanguage);
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
      if (state) {
        myInspectionsFilter.addLanguage(myLanguage);
      } else {
        myInspectionsFilter.removeLanguage(myLanguage);
      }
    }
  }

  private final String version = ApplicationInfo.getInstance().getMajorVersion() +
                                 (StringUtil.isEmptyOrSpaces(StringUtil.trimStart(ApplicationInfo.getInstance().getMinorVersion(),"0")) ?
                                 "" : "."+ApplicationInfo.getInstance().getMinorVersion());
  private final String presentableVersion = ApplicationNamesInfo.getInstance().getProductName() + " " + version;
  private class ShowNewInspectionsAction extends AnAction implements DumbAware {
    private ShowNewInspectionsAction() {
      super(AnalysisBundle.message("inspections.settings.show.new.text", presentableVersion),
            AnalysisBundle.message("inspections.settings.show.new.description", presentableVersion),
            AllIcons.Actions.Lightning);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      myFilterComponent.setFilter("\"New in " + version + "\"");
    }
  }

  private class ShowOnlyModifiedInspectionsAction extends CheckboxAction implements DumbAware {
    ShowOnlyModifiedInspectionsAction() {
      super(AnalysisBundle.message("inspections.settings.show.modified.text"));
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
      return myInspectionsFilter.isShowOnlyModifiedInspections();
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
      myInspectionsFilter.setShowOnlyModifiedInspections(state);
    }
  }
}