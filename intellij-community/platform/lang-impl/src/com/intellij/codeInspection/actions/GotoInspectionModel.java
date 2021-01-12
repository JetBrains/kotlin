// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInspection.actions;

import com.intellij.codeInspection.ex.InspectionProfileImpl;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper;
import com.intellij.codeInspection.ex.ScopeToolState;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.util.gotoByName.SimpleChooseByNameModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.profile.codeInspection.InspectionProfileManager;
import com.intellij.psi.PsiManager;
import com.intellij.util.ArrayUtilRt;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Konstantin Bulenkov
 */
public class GotoInspectionModel extends SimpleChooseByNameModel {
  private final Map<String, InspectionToolWrapper> myToolNames = new HashMap<>();
  private final String[] myNames;
  private final InspectionListCellRenderer myListCellRenderer = new InspectionListCellRenderer();


  public GotoInspectionModel(@NotNull Project project) {
    super(project, IdeBundle.message("prompt.goto.inspection.enter.name"), "goto.inspection.help.id");

    InspectionProfileImpl rootProfile = InspectionProfileManager.getInstance(project).getCurrentProfile();
    for (ScopeToolState state : rootProfile.getAllTools()) {
      InspectionToolWrapper tool = LocalInspectionToolWrapper.findTool2RunInBatch(project, null, rootProfile, state.getTool());
      if (tool != null) {
        myToolNames.put(getSearchString(tool), tool);
      }
    }
    myNames = ArrayUtilRt.toStringArray(myToolNames.keySet());
  }

  private static String getSearchString(InspectionToolWrapper tool) {
    return tool.getDisplayName() + " " + StringUtil.join(tool.getGroupPath(), " ") + " " + tool.getShortName();
  }

  @NotNull
  @Override
  public ListCellRenderer getListCellRenderer() {
    return myListCellRenderer;
  }

  @Override
  public String[] getNames() {
    return myNames;
  }

  @Override
  public Object[] getElementsByName(final String name, final String pattern) {
    final InspectionToolWrapper tool = myToolNames.get(name);
    if (tool == null) {
      return InspectionElement.EMPTY_ARRAY;
    }
    return new InspectionElement[] {new InspectionElement(tool, PsiManager.getInstance(getProject()))};
  }

  @Override
  public String getElementName(@NotNull final Object element) {
    if (element instanceof InspectionElement) {
      return getSearchString(((InspectionElement)element).getToolWrapper());
    }
    return null;
  }
}
