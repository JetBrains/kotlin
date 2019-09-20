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
package com.intellij.application.options.codeStyle;

import com.intellij.application.options.schemes.SchemeNameGenerator;
import com.intellij.application.options.schemes.SchemesModel;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.codeStyle.CodeStyleScheme;
import com.intellij.psi.codeStyle.CodeStyleSchemes;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.psi.codeStyle.modifier.CodeStyleSettingsModifier;
import com.intellij.psi.impl.source.codeStyle.CodeStyleSchemeImpl;
import com.intellij.psi.impl.source.codeStyle.CodeStyleSchemesImpl;
import com.intellij.util.EventDispatcher;
import com.intellij.util.containers.ContainerUtil;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CodeStyleSchemesModel implements SchemesModel<CodeStyleScheme> {
  private final static Logger LOG = Logger.getInstance(CodeStyleSchemesModel.class);

  private final List<CodeStyleScheme> mySchemes = new ArrayList<>();
  private CodeStyleScheme mySelectedScheme;
  private final CodeStyleScheme myProjectScheme;
  private final CodeStyleScheme myDefault;
  private final Map<CodeStyleScheme, CodeStyleSettings> mySettingsToClone = new HashMap<>();

  private final EventDispatcher<CodeStyleSchemesModelListener> myDispatcher = EventDispatcher.create(CodeStyleSchemesModelListener.class);
  private final Project myProject;
  private boolean myUiEventsEnabled = true;

  private final @NotNull OverridingStatus myOverridingStatus = new OverridingStatus();

  public CodeStyleSchemesModel(Project project) {
    myProject = project;
    myProjectScheme = new ProjectScheme();
    myDefault = CodeStyleSchemes.getInstance().getDefaultScheme();
    reset();
  }

  public void selectScheme(final CodeStyleScheme selected, @Nullable Object source) {
    if (mySelectedScheme != selected) {
      mySelectedScheme = selected;
      myDispatcher.getMulticaster().currentSchemeChanged(source);
    }
  }

  public void addScheme(final CodeStyleScheme newScheme, boolean changeSelection) {
    mySchemes.add(newScheme);
    myDispatcher.getMulticaster().schemeListChanged();
    if (changeSelection) {
      selectScheme(newScheme, this);
    }
  }

  @Override
  public void removeScheme(@NotNull final CodeStyleScheme scheme) {
    mySchemes.remove(scheme);
    myDispatcher.getMulticaster().schemeListChanged();
    if (mySelectedScheme == scheme) {
      selectScheme(myDefault, this);
    }
  }

  @NotNull
  public CodeStyleSettings getCloneSettings(final CodeStyleScheme scheme) {
    if (!mySettingsToClone.containsKey(scheme)) {
      mySettingsToClone.put(scheme, ModelSettings.createFrom(scheme.getCodeStyleSettings()));
    }
    return mySettingsToClone.get(scheme);
  }

  public CodeStyleScheme getSelectedScheme(){
    return mySelectedScheme;
  }

  public void addListener(CodeStyleSchemesModelListener listener) {
    myDispatcher.addListener(listener);
  }

  public List<CodeStyleScheme> getSchemes() {
    return Collections.unmodifiableList(mySchemes);
  }

  public void reset() {
    mySchemes.clear();
    mySchemes.addAll(CodeStyleSchemesImpl.getSchemeManager().getAllSchemes());
    mySchemes.add(myProjectScheme);
    updateClonedSettings();

    mySelectedScheme = getProjectSettings().USE_PER_PROJECT_SETTINGS ? myProjectScheme : CodeStyleSchemes.getInstance().findPreferredScheme(getProjectSettings().PREFERRED_PROJECT_CODE_STYLE);

    myDispatcher.getMulticaster().schemeListChanged();
    myDispatcher.getMulticaster().currentSchemeChanged(this);

    updateOverridingStatus();
  }

  private void updateClonedSettings() {
    for (Iterator<CodeStyleScheme> schemeIterator = mySettingsToClone.keySet().iterator(); schemeIterator.hasNext();) {
      CodeStyleScheme scheme = schemeIterator.next();
      if (!mySchemes.contains(scheme)) {
        schemeIterator.remove();
      }
    }
    for (CodeStyleScheme scheme : mySchemes) {
      CodeStyleSettings current = scheme.getCodeStyleSettings();
      CodeStyleSettings clonedSettings = getCloneSettings(scheme);
      clonedSettings.copyFrom(current);
    }
  }

  public boolean isUsePerProjectSettings() {
    return mySelectedScheme instanceof ProjectScheme;
  }

  private CodeStyleSettingsManager getProjectSettings() {
    return CodeStyleSettingsManager.getInstance(myProject);
  }

  public boolean isSchemeListModified() {
    CodeStyleSchemes schemes = CodeStyleSchemes.getInstance();
    if (getProjectSettings().USE_PER_PROJECT_SETTINGS != isProjectScheme(mySelectedScheme)) return true;
    if (!isProjectScheme(mySelectedScheme) &&
        getSelectedScheme() != schemes.findPreferredScheme(getProjectSettings().PREFERRED_PROJECT_CODE_STYLE)) {
      return true;
    }
    Set<CodeStyleScheme> configuredSchemesSet = new HashSet<>(getIdeSchemes());
    return !configuredSchemesSet.equals(new THashSet<>(CodeStyleSchemesImpl.getSchemeManager().getAllSchemes()));
  }

  public void apply() {
    commitClonedSettings();
    commitProjectSettings();
    CodeStyleSchemesImpl.getSchemeManager().setSchemes(getIdeSchemes(), mySelectedScheme instanceof ProjectScheme ? null : mySelectedScheme, null);
    updateOverridingStatus();
  }

  private void commitProjectSettings() {
    CodeStyleSettingsManager projectSettingsManager = getProjectSettings();
    projectSettingsManager.USE_PER_PROJECT_SETTINGS = isProjectScheme(mySelectedScheme);
    projectSettingsManager.PREFERRED_PROJECT_CODE_STYLE = mySelectedScheme instanceof ProjectScheme ? null : mySelectedScheme.getName();
    CodeStyleSettings projectSettings = myProjectScheme.getCodeStyleSettings();
    projectSettings.getModificationTracker().incModificationCount();
    projectSettingsManager.setMainProjectCodeStyle(projectSettings);
  }

  private void commitClonedSettings() {
    for (CodeStyleScheme scheme : mySettingsToClone.keySet()) {
      if (!(scheme instanceof ProjectScheme)) {
        CodeStyleSettings settings = scheme.getCodeStyleSettings();
        settings.copyFrom(mySettingsToClone.get(scheme));
        settings.getModificationTracker().incModificationCount();
      }
    }
  }

  private @NotNull List<CodeStyleScheme> getIdeSchemes() {
    return ContainerUtil.filter(mySchemes, scheme -> !(scheme instanceof ProjectScheme));
  }

  @SuppressWarnings("unused")
  @Deprecated
  public static boolean cannotBeModified(final CodeStyleScheme currentScheme) {
    return false;
  }

  public void fireBeforeCurrentSettingsChanged() {
    if (myUiEventsEnabled) myDispatcher.getMulticaster().beforeCurrentSettingsChanged();
  }

  void updateScheme(CodeStyleScheme scheme) {
    CodeStyleSettings clonedSettings = getCloneSettings(scheme);
    clonedSettings.copyFrom(scheme.getCodeStyleSettings());
    myDispatcher.getMulticaster().schemeChanged(scheme);
  }

  public void fireSchemeListChanged() {
    myDispatcher.getMulticaster().schemeListChanged();
  }
  
  public void fireAfterCurrentSettingsChanged() {
    myDispatcher.getMulticaster().afterCurrentSettingsChanged();
  }

  public void copyToProject(final CodeStyleScheme selectedScheme) {
    myProjectScheme.getCodeStyleSettings().copyFrom(selectedScheme.getCodeStyleSettings());
    myDispatcher.getMulticaster().schemeChanged(myProjectScheme);
    commitProjectSettings();
    selectScheme(myProjectScheme, this);
  }

  public CodeStyleScheme exportProjectScheme(@NotNull String name) {
    CodeStyleScheme newScheme = createNewScheme(name, myProjectScheme);
    ((CodeStyleSchemeImpl)newScheme).setCodeStyleSettings(getCloneSettings(myProjectScheme).clone());
    addScheme(newScheme, false);

    return newScheme;
  }

  public CodeStyleScheme createNewScheme(final String preferredName, final CodeStyleScheme parentScheme) {
    final boolean isProjectScheme = isProjectScheme(parentScheme);
    return new CodeStyleSchemeImpl(SchemeNameGenerator.getUniqueName(preferredName, parentScheme, name -> containsScheme(name, isProjectScheme)),
                                   false,
                                   parentScheme);
  }

  @Nullable
  private CodeStyleScheme findSchemeByName(final String name, boolean isProjectScheme) {
    for (CodeStyleScheme scheme : mySchemes) {
      if (isProjectScheme == isProjectScheme(scheme) && name.equals(scheme.getName())) return scheme;
    }
    return null;
  }

  public CodeStyleScheme getProjectScheme() {
    return myProjectScheme;
  }

  @Override
  public boolean canDuplicateScheme(@NotNull CodeStyleScheme scheme) {
    return !isProjectScheme(scheme);
  }

  @Override
  public boolean canResetScheme(@NotNull CodeStyleScheme scheme) {
    return scheme.isDefault();
  }

  @Override
  public boolean canDeleteScheme(@NotNull CodeStyleScheme scheme) {
    return !isProjectScheme(scheme) && !scheme.isDefault();
  }

  @Override
  public boolean isProjectScheme(@NotNull CodeStyleScheme scheme) {
    return scheme instanceof ProjectScheme;
  }

  @Override
  public boolean canRenameScheme(@NotNull CodeStyleScheme scheme) {
    return canDeleteScheme(scheme);
  }

  @Override
  public boolean containsScheme(@NotNull String name, boolean isProjectScheme) {
    return findSchemeByName(name, isProjectScheme) != null;
  }

  @Override
  public boolean differsFromDefault(@NotNull CodeStyleScheme scheme) {
    CodeStyleSettings defaults = CodeStyleSettings.getDefaults();
    CodeStyleSettings clonedSettings = getCloneSettings(scheme);
    return !defaults.equals(clonedSettings);
  }

  public List<CodeStyleScheme> getAllSortedSchemes() {
    List<CodeStyleScheme> schemes = new ArrayList<>(getSchemes());
    Collections.sort(schemes, (s1, s2) -> {
      if (isProjectScheme(s1)) return -1;
      if (isProjectScheme(s2)) return 1;
      if (s1.isDefault()) return -1;
      if (s2.isDefault()) return 1;
      return s1.getName().compareToIgnoreCase(s2.getName());
    });
    return schemes;
  }

  public Project getProject() {
    return myProject;
  }

  private class ProjectScheme extends CodeStyleSchemeImpl {
    ProjectScheme() {
      super(CodeStyleScheme.PROJECT_SCHEME_NAME, false, CodeStyleSchemes.getInstance().getDefaultScheme());
      CodeStyleSettings perProjectSettings = getProjectSettings().getMainProjectCodeStyle();
      if (perProjectSettings != null) setCodeStyleSettings(perProjectSettings);
    }
  }

  public void restoreDefaults(@NotNull CodeStyleScheme scheme) {
    if (canResetScheme(scheme)) {
      CodeStyleSettings currSettings = getCloneSettings(scheme);
      currSettings.copyFrom(CodeStyleSettings.getDefaults());
      fireModelSettingsChanged(currSettings);
    }
  }

  void fireModelSettingsChanged(@NotNull CodeStyleSettings currSettings) {
    myUiEventsEnabled = false;
    try {
      myDispatcher.getMulticaster().settingsChanged(currSettings);
    }
    finally {
      myUiEventsEnabled = true;
    }
  }

  public boolean containsModifiedCodeStyleSettings() {
    for (CodeStyleScheme scheme : mySchemes) {
      CodeStyleSettings originalSettings = scheme.getCodeStyleSettings();
      CodeStyleSettings currentSettings = mySettingsToClone.get(scheme);
      if (currentSettings != null && !originalSettings.equals(currentSettings)) {
        return true;
      }
    }
    return false;
  }

  public void updateOverridingStatus() {
    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      try {
        myOverridingStatus.getLock().lock();
        List<CodeStyleSettingsModifier> modifiers = getOverridingModifiers();
        if (modifiers.size() > 0) {
          myOverridingStatus.update(modifiers);
        }
        else {
          myOverridingStatus.reset();
        }
      }
      finally {
        myOverridingStatus.getLock().unlock();
      }
      myDispatcher.getMulticaster().overridingStatusChanged();
    });
  }

  @Nullable
  public OverridingStatus getOverridingStatus() {
    if (myOverridingStatus.getLock().tryLock()) {
      try {
        return !myOverridingStatus.isEmpty()? myOverridingStatus : null;
      }
      finally {
        myOverridingStatus.getLock().unlock();
      }
    }
    return null;
  }

  private List<CodeStyleSettingsModifier> getOverridingModifiers() {
    return
      ContainerUtil.filter(
        CodeStyleSettingsModifier.EP_NAME.getExtensionList(),
        modifier -> safeGetOverridingStatus(modifier, getProject()));
  }

  private static boolean safeGetOverridingStatus(@NotNull CodeStyleSettingsModifier modifier, @NotNull Project project) {
    try {
      return modifier.mayOverrideSettingsOf(project);
    }
    catch (Throwable t) {
      LOG.error(t);
    }
    return false;
  }

  public void setUiEventsEnabled(boolean enabled) {
    myUiEventsEnabled = enabled;
  }

  public boolean isUiEventsEnabled() {
    return myUiEventsEnabled;
  }

  public static class ModelSettings extends CodeStyleSettings {
    private volatile boolean myLocked;

    public static ModelSettings createFrom(@NotNull CodeStyleSettings settings) {
      ModelSettings modelSettings = new ModelSettings();
      modelSettings.copyFrom(settings);
      return modelSettings;
    }

    public void doWithLockedSettings(@NotNull Runnable runnable) {
      myLocked = true;
      runnable.run();
      myLocked = false;
    }

    public boolean isLocked() {
      return myLocked;
    }
  }

  public static class OverridingStatus {
    private final Lock myLock = new ReentrantLock();

    private final static CodeStyleSettingsModifier[] EMPTY_MODIFIER_ARRAY = new CodeStyleSettingsModifier[0];

    @Nullable
    private List<CodeStyleSettingsModifier> myModifiers;

    @NotNull
    public Lock getLock() {
      return myLock;
    }

    private void update(@NotNull List<CodeStyleSettingsModifier> modifiers) {
      myModifiers = modifiers;
    }

    @NotNull
    public CodeStyleSettingsModifier[] getModifiers() {
      return myModifiers != null && !myModifiers.isEmpty()
             ? myModifiers.toArray(new CodeStyleSettingsModifier[0])
             : EMPTY_MODIFIER_ARRAY;
    }

    private boolean isEmpty() {
      return myModifiers == null || myModifiers.isEmpty();
    }

    private void reset() {
      myModifiers = null;
    }
  }
}
