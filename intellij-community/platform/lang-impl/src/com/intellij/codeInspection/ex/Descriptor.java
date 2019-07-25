// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

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
  private final HighlightDisplayKey myKey;
  private final InspectionToolWrapper myToolWrapper;
  private final HighlightDisplayLevel myLevel;
  @Nullable
  private final NamedScope myScope;
  private final ScopeToolState myState;
  @NotNull
  private final InspectionProfileModifiableModel myInspectionProfile;
  private final String myScopeName;

  private Element myConfig;
  private boolean myEnabled;

  public Descriptor(@NotNull ScopeToolState state, @NotNull InspectionProfileModifiableModel inspectionProfile, @NotNull Project project) {
    myState = state;
    myInspectionProfile = inspectionProfile;
    InspectionToolWrapper tool = state.getTool();
    myText = tool.getDisplayName();
    final String[] groupPath = tool.getGroupPath();
    myGroup = groupPath.length == 0 ? new String[]{InspectionProfileEntry.GENERAL_GROUP_NAME} : groupPath;
    myKey = HighlightDisplayKey.find(tool.getShortName());
    myScopeName = state.getScopeName();
    myScope = state.getScope(project);
    myLevel = inspectionProfile.getErrorLevel(myKey, myScope, project);
    myEnabled = inspectionProfile.isToolEnabled(myKey, myScope, project);
    myToolWrapper = tool;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Descriptor)) return false;
    final Descriptor descriptor = (Descriptor)obj;
    return myKey.equals(descriptor.getKey()) &&
           myLevel.equals(descriptor.getLevel()) &&
           myEnabled == descriptor.isEnabled() &&
           myState.equalTo(descriptor.getState());
  }

  @Override
  public int hashCode() {
    final int hash = myKey.hashCode() + 29 * myLevel.hashCode();
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
    return myKey;
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

  @NotNull
  public String[] getGroup() {
    return myGroup;
  }

  @NotNull
  public String getScopeName() {
    return myScopeName;
  }

  @Nullable
  public NamedScope getScope() {
    return myScope;
  }

  @NotNull
  public ScopeToolState getState() {
    return myState;
  }

  @Override
  public String toString() {
    return myKey.toString();
  }
}
