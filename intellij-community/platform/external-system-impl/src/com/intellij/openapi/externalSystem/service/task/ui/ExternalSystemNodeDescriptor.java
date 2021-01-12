/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.openapi.externalSystem.service.task.ui;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.PresentableNodeDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author Denis Zhdanov
 */
public class ExternalSystemNodeDescriptor<T> extends PresentableNodeDescriptor<T> {

  @NotNull private final T myElement;
  @NotNull private final String myDescription;

  public ExternalSystemNodeDescriptor(@NotNull T element, @NotNull String name, @NotNull String description, @Nullable Icon icon) {
    super(null, null);
    myElement = element;
    myName = name;
    setIcon(icon);
    myDescription = description;
    getPresentation().setTooltip(description);
  }

  public void setName(@NotNull String name) {
    myName = name;
  }
  
  @Override
  protected void update(@NotNull PresentationData presentation) {
    presentation.setPresentableText(myName);
    presentation.setIcon(getIcon());
    presentation.setTooltip(myDescription);
  }
  
  @NotNull
  @Override
  public T getElement() {
    return myElement;
  }
}
