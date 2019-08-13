/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.profile.codeInspection.ui.filter;

import com.intellij.codeInspection.ex.GlobalInspectionToolWrapper;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.codeInspection.ex.ScopeToolState;
import com.intellij.codeInspection.ex.Tools;
import com.intellij.lang.Language;
import com.intellij.lang.MetaLanguage;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.profile.codeInspection.ui.inspectionsTree.InspectionConfigTreeNode;
import java.util.HashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * @author Dmitry Batkovich
 */
public abstract class InspectionsFilter {

  private final Set<HighlightSeverity> mySuitableSeverities = new HashSet<>();
  private final Set<Language> mySuitableLanguages = new HashSet<>();
  private Boolean mySuitableInspectionsStates;
  private boolean myAvailableOnlyForAnalyze;
  private boolean myShowOnlyCleanupInspections;
  private boolean myShowOnlyModifiedInspections;

  public boolean isAvailableOnlyForAnalyze() {
    return myAvailableOnlyForAnalyze;
  }

  public boolean isShowOnlyCleanupInspections() {
    return myShowOnlyCleanupInspections;
  }

  public Boolean getSuitableInspectionsStates() {
    return mySuitableInspectionsStates;
  }

  public boolean containsSeverity(final HighlightSeverity severity) {
    return mySuitableSeverities.contains(severity);
  }

  public boolean containsLanguage(final Language  language) {
    return mySuitableLanguages.contains(language);
  }

  public void setShowOnlyCleanupInspections(final boolean showOnlyCleanupInspections) {
    myShowOnlyCleanupInspections = showOnlyCleanupInspections;
    filterChanged();
  }

  public void setShowOnlyModifiedInspections(boolean showOnlyModifiedInspections) {
    myShowOnlyModifiedInspections = showOnlyModifiedInspections;
    filterChanged();
  }

  public void setAvailableOnlyForAnalyze(final boolean availableOnlyForAnalyze) {
    myAvailableOnlyForAnalyze = availableOnlyForAnalyze;
    filterChanged();
  }

  public void setSuitableInspectionsStates(@Nullable final Boolean suitableInspectionsStates) {
    mySuitableInspectionsStates = suitableInspectionsStates;
    filterChanged();
  }

  public void addSeverity(final HighlightSeverity severity) {
    mySuitableSeverities.add(severity);
    filterChanged();
  }

  public void removeSeverity(final HighlightSeverity severity) {
    mySuitableSeverities.remove(severity);
    filterChanged();
  }

  public void addLanguage(Language language) {
    mySuitableLanguages.add(language);
    filterChanged();
  }

  public void removeLanguage(Language language) {
    mySuitableLanguages.remove(language);
    filterChanged();
  }

  public void reset() {
    mySuitableInspectionsStates = null;
    myAvailableOnlyForAnalyze = false;
    myShowOnlyCleanupInspections = false;
    myShowOnlyModifiedInspections = false;
    mySuitableSeverities.clear();
    mySuitableLanguages.clear();
    filterChanged();
  }

  public boolean isEmptyFilter() {
    return mySuitableInspectionsStates == null
           && !myAvailableOnlyForAnalyze
           && !myShowOnlyCleanupInspections
           && !myShowOnlyModifiedInspections
           && mySuitableSeverities.isEmpty()
           && mySuitableLanguages.isEmpty();
  }

  public boolean matches(@NotNull Tools tools, final InspectionConfigTreeNode node) {
    if (myShowOnlyCleanupInspections && !tools.getTool().isCleanupTool()) {
      return false;
    }

    if (mySuitableInspectionsStates != null && mySuitableInspectionsStates != tools.isEnabled()) {
      return false;
    }

    if (myAvailableOnlyForAnalyze && !isAvailableOnlyForAnalyze(tools)) {
      return false;
    }

    if (!mySuitableSeverities.isEmpty()) {
      boolean suitable = false;
      for (final ScopeToolState state : tools.getTools()) {
        if (mySuitableInspectionsStates != null && mySuitableInspectionsStates != state.isEnabled()) {
          continue;
        }
        if (mySuitableSeverities.contains(tools.getDefaultState().getLevel().getSeverity())) {
          suitable = true;
          break;
        }
      }
      if (!suitable) {
        return false;
      }
    }

    if (!mySuitableLanguages.isEmpty()) {
      String languageId = tools.getDefaultState().getTool().getLanguage();
      if (languageId == null) return false;
      Language language = Language.findLanguageByID(languageId);
      if (language instanceof MetaLanguage) {
        if (((MetaLanguage)language).getMatchingLanguages().stream().noneMatch(mySuitableLanguages::contains)) return false;
      } else {
        if (!mySuitableLanguages.contains(language)) return false;
      }
    }
    return !myShowOnlyModifiedInspections || node.isProperSetting();
  }

  protected abstract void filterChanged();

  private static boolean isAvailableOnlyForAnalyze(final Tools tools) {
    final InspectionToolWrapper tool = tools.getTool();
    return tool instanceof GlobalInspectionToolWrapper && ((GlobalInspectionToolWrapper)tool).worksInBatchModeOnly();
  }

  public boolean isShowOnlyModifiedInspections() {
    return myShowOnlyModifiedInspections;
  }
}