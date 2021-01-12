// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInspection.ex;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInsight.daemon.HighlightDisplayKey;
import com.intellij.codeInspection.InspectionProfileEntry;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Descriptor {
  private static final Logger LOG = Logger.getInstance(Descriptor.class);

  @NotNull
  private final String myText;
  private final String[] myGroup;
  private final String myShortName;
  private final InspectionToolWrapper myToolWrapper;
  private final HighlightDisplayLevel myLevel;
  @Nullable
  private final NamedScope myScope;
  private final ScopeToolState myState;
  @NotNull
  private final InspectionProfileModifiableModel myInspectionProfile;

  private Element myConfig;
  private boolean myEnabled;

  public Descriptor(@NotNull ScopeToolState state, @NotNull InspectionProfileModifiableModel inspectionProfile, @NotNull Project project) {
    myState = state;
    myInspectionProfile = inspectionProfile;
    InspectionToolWrapper tool = state.getTool();
    myText = tool.getDisplayName();
    final String[] groupPath = tool.getGroupPath();
    myGroup = groupPath.length == 0 ? new String[]{InspectionProfileEntry.getGeneralGroupName()} : groupPath;
    myShortName = tool.getShortName();
    myScope = state.getScope(project);
    final HighlightDisplayKey key = HighlightDisplayKey.findOrRegister(myShortName, myText);
    myLevel = inspectionProfile.getErrorLevel(key, myScope, project);
    myEnabled = inspectionProfile.isToolEnabled(key, myScope, project);
    myToolWrapper = tool;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Descriptor)) return false;
    final Descriptor descriptor = (Descriptor)obj;
    return myShortName.equals(descriptor.myShortName) &&
           myLevel.equals(descriptor.getLevel()) &&
           myEnabled == descriptor.isEnabled() &&
           myState.equalTo(descriptor.getState());
  }

  @Override
  public int hashCode() {
    final int hash = myShortName.hashCode() + 29 * myLevel.hashCode();
    return myScope != null ? myScope.hashCode() + 29 * hash : hash;
  }

  public boolean isEnabled() {
    return myEnabled;
  }

  public void setEnabled(final boolean enabled) {
    myEnabled = enabled;
  }

  @NotNull
  public String getText() {
    return myText;
  }

  @NotNull
  public HighlightDisplayKey getKey() {
    return HighlightDisplayKey.findOrRegister(myShortName, myText);
  }

  public HighlightDisplayLevel getLevel() {
    return myLevel;
  }

  @Nullable
  public Element getConfig() {
    return myConfig;
  }

  public void loadConfig() {
    if (myConfig == null) {
      InspectionToolWrapper toolWrapper = getToolWrapper();
      myConfig = createConfigElement(toolWrapper);
    }
  }

  @NotNull
  public InspectionToolWrapper getToolWrapper() {
    return myToolWrapper;
  }

  @Nullable
  public String loadDescription() {
    loadConfig();
    return myToolWrapper.loadDescription();
  }

  @NotNull
  public InspectionProfileModifiableModel getInspectionProfile() {
    return myInspectionProfile;
  }

  @NotNull
  public static Element createConfigElement(InspectionToolWrapper toolWrapper) {
    Element element = new Element("options");
    try {
      toolWrapper.getTool().writeSettings(element);
    }
    catch (WriteExternalException e) {
      LOG.error(e);
    }
    return element;
  }

  public String @NotNull [] getGroup() {
    return myGroup;
  }

  @NotNull
  public String getScopeName() {
    return myState.getScopeName();
  }

  @Nullable
  public NamedScope getScope() {
    return myScope;
  }

  @NotNull
  public ScopeToolState getState() {
    return myState;
  }

  public String getShortName() {
    return myShortName;
  }

  @Override
  public String toString() {
    return myShortName;
  }
}
