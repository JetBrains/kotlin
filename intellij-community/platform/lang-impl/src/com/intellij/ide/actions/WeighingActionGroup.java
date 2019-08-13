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
package com.intellij.ide.actions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.PresentationFactory;
import com.intellij.openapi.actionSystem.impl.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * @author peter
 */
abstract class WeighingActionGroup extends ActionGroup {
  private final PresentationFactory myPresentationFactory = new PresentationFactory();

  @Override
  public void update(@NotNull AnActionEvent e) {
    getDelegate().update(e);
  }

  protected abstract ActionGroup getDelegate();

  @Override
  @NotNull
  public AnAction[] getChildren(@Nullable AnActionEvent e) {
    ActionGroup delegate = getDelegate();
    AnAction[] children = delegate.getChildren(e);
    if (e == null) {
      return children;
    }

    List<AnAction> visibleActions = Utils.expandActionGroup(false, delegate, myPresentationFactory, e.getDataContext(), e.getPlace());

    LinkedHashSet<AnAction> heaviest = null;
    double maxWeight = Presentation.DEFAULT_WEIGHT;
    for (AnAction action : visibleActions) {
      Presentation presentation = myPresentationFactory.getPresentation(action);
      if (presentation.isEnabled() && presentation.isVisible()) {
        if (presentation.getWeight() > maxWeight) {
          maxWeight = presentation.getWeight();
          heaviest = new LinkedHashSet<>();
        }
        if (presentation.getWeight() == maxWeight && heaviest != null) {
          heaviest.add(action);
        }
      }
    }

    if (heaviest == null) {
      return children;
    }

    final DefaultActionGroup chosen = new DefaultActionGroup();
    boolean prevSeparator = true;
    for (AnAction action : visibleActions) {
      final boolean separator = action instanceof Separator;
      if (separator && !prevSeparator) {
        chosen.add(action);
      }
      prevSeparator = separator;

      if (shouldBeChosenAnyway(action)) {
        heaviest.add(action);
      }

      if (heaviest.contains(action)) {
        chosen.add(action);
      }
    }

    ActionGroup other = new ExcludingActionGroup(delegate, heaviest);
    other.setPopup(true);
    other.getTemplatePresentation().setText("Other...");
    return new AnAction[]{chosen, new Separator(), other};
  }

  protected boolean shouldBeChosenAnyway(AnAction action) {
    return false;
  }

}
