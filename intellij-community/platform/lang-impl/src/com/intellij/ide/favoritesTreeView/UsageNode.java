/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.ide.favoritesTreeView;

import com.intellij.pom.Navigatable;
import com.intellij.usages.Usage;

public class UsageNode implements Navigatable {
  private Usage myUsage;
  private NoteNode myComment;

  public Usage getUsage() {
    return myUsage;
  }

  public void setUsage(Usage usage) {
    myUsage = usage;
  }

  public NoteNode getComment() {
    return myComment;
  }

  public void setComment(NoteNode comment) {
    myComment = comment;
  }

  @Override
  public void navigate(boolean requestFocus) {
    myUsage.navigate(requestFocus);
  }

  @Override
  public boolean canNavigate() {
    return myUsage.isValid() && myUsage.canNavigate();
  }

  @Override
  public boolean canNavigateToSource() {
    return myUsage.isValid() && myUsage.canNavigate();
  }
}
