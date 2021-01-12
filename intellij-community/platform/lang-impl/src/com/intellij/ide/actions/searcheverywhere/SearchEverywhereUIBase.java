// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.searcheverywhere;

import com.intellij.ide.actions.BigPopupUI;
import com.intellij.ide.actions.bigPopup.ShowFilterAction;
import com.intellij.ide.util.ElementsChooser;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.Future;

public abstract class SearchEverywhereUIBase extends BigPopupUI {
  public static final String SEARCH_EVERYWHERE_SEARCH_FILED_KEY = "search-everywhere-textfield"; //only for testing purposes

  public SearchEverywhereUIBase(Project project) {super(project);}

  public abstract void toggleEverywhereFilter();

  public abstract void switchToContributor(@NotNull String contributorID);

  public abstract String getSelectedContributorID();

  @Nullable
  public abstract Object getSelectionIdentity();

  protected static class FiltersAction extends ShowFilterAction {
    final PersistentSearchEverywhereContributorFilter<?> filter;
    final Runnable rebuildRunnable;

    public FiltersAction(@NotNull PersistentSearchEverywhereContributorFilter<?> filter,
                  @NotNull Runnable rebuildRunnable) {
      this.filter = filter;
      this.rebuildRunnable = rebuildRunnable;
    }

    @Override
    public boolean isEnabled() {
      return true;
    }

    @Override
    protected boolean isActive() {
      return filter.getAllElements().size() != filter.getSelectedElements().size();
    }

    @Override
    protected ElementsChooser<?> createChooser() {
      return createChooser(filter, rebuildRunnable);
    }

    private static <T> ElementsChooser<T> createChooser(@NotNull PersistentSearchEverywhereContributorFilter<T> filter,
                                                        @NotNull Runnable rebuildRunnable) {
      ElementsChooser<T> res = new ElementsChooser<T>(filter.getAllElements(), false) {
        @Override
        protected String getItemText(@NotNull T value) {
          return filter.getElementText(value);
        }

        @Nullable
        @Override
        protected Icon getItemIcon(@NotNull T value) {
          return filter.getElementIcon(value);
        }
      };
      res.markElements(filter.getSelectedElements());
      ElementsChooser.ElementsMarkListener<T> listener = (element, isMarked) -> {
        filter.setSelected(element, isMarked);
        rebuildRunnable.run();
      };
      res.addElementsMarkListener(listener);
      return res;
    }
  }

  @TestOnly public abstract Future<List<Object>> findElementsForPattern(String pattern);
  @TestOnly public abstract void clearResults();
}
