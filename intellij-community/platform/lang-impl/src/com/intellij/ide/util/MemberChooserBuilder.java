/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

/*
 * @author max
 */
package com.intellij.ide.util;

import com.intellij.codeInsight.generation.ClassMember;
import com.intellij.openapi.project.Project;

import javax.swing.*;

public class MemberChooserBuilder<T extends ClassMember> {
  private final Project myProject;
  private boolean myAllowEmptySelection = false;
  private boolean myAllowMultiSelection = true;
  private boolean myIsInsertOverrideVisible = false;
  private boolean myIsCopyJavadocVisible = false;
  private JComponent myHeaderPanel;
  private String myTitle;

  public MemberChooserBuilder(final Project project) {
    myProject = project;
  }

  public MemberChooser<T> createBuilder(T[] elements) {
    final MemberChooser<T> chooser =
      new MemberChooser<>(elements, myAllowEmptySelection, myAllowMultiSelection, myProject, myIsInsertOverrideVisible, myHeaderPanel);

    if (myTitle != null) {
      chooser.setTitle(myTitle);
    }

    chooser.setCopyJavadocVisible(myIsCopyJavadocVisible);

    return chooser;
  }

  public void allowEmptySelection(final boolean allowEmptySelection) {
    myAllowEmptySelection = allowEmptySelection;
  }

  public void allowMultiSelection(final boolean allowMultiSelection) {
    myAllowMultiSelection = allowMultiSelection;
  }

  public void overrideAnnotationVisible(final boolean isInsertOverrideVisible) {
    myIsInsertOverrideVisible = isInsertOverrideVisible;
  }

  public void setHeaderPanel(final JComponent headerPanel) {
    myHeaderPanel = headerPanel;
  }

  public void copyJavadocVisible(final boolean isCopyJavadocVisible) {
    myIsCopyJavadocVisible = isCopyJavadocVisible;
  }

  public void setTitle(final String title) {
    myTitle = title;
  }
}