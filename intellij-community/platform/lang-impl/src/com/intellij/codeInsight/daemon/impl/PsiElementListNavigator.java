// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeInsight.navigation.BackgroundUpdaterTask;
import com.intellij.codeInsight.navigation.ListBackgroundUpdaterTask;
import com.intellij.find.FindUtil;
import com.intellij.ide.PsiCopyPasteManager;
import com.intellij.ide.util.PsiElementListCellRenderer;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ListComponentUpdater;
import com.intellij.openapi.ui.popup.IPopupChooserBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupChooserBuilder;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.psi.PsiElement;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBList;
import com.intellij.usages.UsageView;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;

public final class PsiElementListNavigator {

  private PsiElementListNavigator() {
  }

  public static <T extends NavigatablePsiElement> void openTargets(MouseEvent e,
                                                                   T[] targets,
                                                                   @Nls String title,
                                                                   @Nls final String findUsagesTitle,
                                                                   ListCellRenderer<? super T> listRenderer) {
    openTargets(e, targets, title, findUsagesTitle, listRenderer, (BackgroundUpdaterTask)null);
  }

  public static <T extends NavigatablePsiElement> void openTargets(MouseEvent e,
                                                                   T[] targets,
                                                                   @Nls String title,
                                                                   @Nls final String findUsagesTitle,
                                                                   ListCellRenderer<? super T> listRenderer,
                                                                   @Nullable BackgroundUpdaterTask listUpdaterTask) {
    JBPopup popup = navigateOrCreatePopup(targets, title, findUsagesTitle, listRenderer, listUpdaterTask);
    if (popup != null) {
      RelativePoint point = new RelativePoint(e);
      if (listUpdaterTask != null) {
        runActionAndListUpdaterTask(() -> popup.show(point), listUpdaterTask);
      }
      else {
        popup.show(point);
      }
    }
  }

  public static <T extends NavigatablePsiElement> void openTargets(Editor e,
                                                                   T[] targets,
                                                                   @Nls String title,
                                                                   @Nls final String findUsagesTitle,
                                                                   ListCellRenderer<? super T> listRenderer) {
    openTargets(e, targets, title, findUsagesTitle, listRenderer, null);
  }

  public static <T extends NavigatablePsiElement> void openTargets(Editor e,
                                                                   T[] targets,
                                                                   @Nls String title,
                                                                   @Nls final String findUsagesTitle,
                                                                   ListCellRenderer<? super T> listRenderer,
                                                                   @Nullable BackgroundUpdaterTask listUpdaterTask) {
    final JBPopup popup = navigateOrCreatePopup(targets, title, findUsagesTitle, listRenderer, listUpdaterTask);
    if (popup != null) {
      if (listUpdaterTask != null) {
        runActionAndListUpdaterTask(() -> popup.showInBestPositionFor(e), listUpdaterTask);
      }
      else {
        popup.showInBestPositionFor(e);
      }
    }
  }

  /**
   * @see #navigateOrCreatePopup(NavigatablePsiElement[], String, String, ListCellRenderer, BackgroundUpdaterTask, Consumer)
   */
  private static void runActionAndListUpdaterTask(@NotNull Runnable action, @NotNull BackgroundUpdaterTask listUpdaterTask) {
    action.run();
    ProgressManager.getInstance().run(listUpdaterTask);
  }

  @Nullable
  public static <T extends NavigatablePsiElement> JBPopup navigateOrCreatePopup(final T[] targets,
                                                                                @Nls final String title,
                                                                                @Nls final String findUsagesTitle,
                                                                                final ListCellRenderer<? super T> listRenderer,
                                                                                @Nullable final BackgroundUpdaterTask listUpdaterTask) {
    return navigateOrCreatePopup(targets, title, findUsagesTitle, listRenderer, listUpdaterTask, selectedElements -> {
      for (Object element : selectedElements) {
        PsiElement selected = (PsiElement)element;
        if (selected.isValid()) {
          ((NavigatablePsiElement)selected).navigate(true);
        }
      }
    });
  }

  /**
   * listUpdaterTask should be started after alarm is initialized so one-item popup won't blink
   */
  @Nullable
  public static <T extends NavigatablePsiElement> JBPopup navigateOrCreatePopup(final T @NotNull [] targets,
                                                                                @Nls final String title,
                                                                                @Nls final String findUsagesTitle,
                                                                                final ListCellRenderer<? super T> listRenderer,
                                                                                @Nullable final BackgroundUpdaterTask listUpdaterTask,
                                                                                @NotNull final Consumer<? super T[]> consumer) {
    return new NavigateOrPopupHelper(targets, title)
      .setFindUsagesTitle(findUsagesTitle)
      .setListRenderer(listRenderer)
      .setListUpdaterTask(listUpdaterTask)
      .setTargetsConsumer(consumer)
      .navigateOrCreatePopup();
  }

  // Helper makes it easier to customize shown popup.
  public static class NavigateOrPopupHelper<T extends NavigatablePsiElement> {

    private final T @NotNull [] myTargets;

    private final String myTitle;

    private Consumer<? super T[]> myTargetsConsumer;

    @Nullable
    private String myFindUsagesTitle;

    private @Nullable ListCellRenderer<? super T> myListRenderer;

    @Nullable
    private BackgroundUpdaterTask myListUpdaterTask;

    @Nullable
    private Project myProject;

    public NavigateOrPopupHelper(T @NotNull [] targets, @Nls String title) {
      myTargets = targets;
      myTitle = title;
      myTargetsConsumer = selectedElements -> {
        for (PsiElement element : selectedElements) {
          if (element.isValid()) {
            ((NavigatablePsiElement)element).navigate(true);
          }
        }
      };
    }

    @NotNull
    public NavigateOrPopupHelper setFindUsagesTitle(@Nls @Nullable String findUsagesTitle) {
      myFindUsagesTitle = findUsagesTitle;
      return this;
    }

    @NotNull
    public NavigateOrPopupHelper setListRenderer(@Nullable ListCellRenderer<? super NavigatablePsiElement> listRenderer) {
      myListRenderer = listRenderer;
      return this;
    }

    @NotNull
    public NavigateOrPopupHelper setListUpdaterTask(@Nullable BackgroundUpdaterTask listUpdaterTask) {
      myListUpdaterTask = listUpdaterTask;
      return this;
    }

    @NotNull
    public NavigateOrPopupHelper setTargetsConsumer(@NotNull Consumer<? super NavigatablePsiElement[]> targetsConsumer) {
      myTargetsConsumer = targetsConsumer;
      return this;
    }

    @NotNull
    public NavigateOrPopupHelper setProject(@Nullable Project project) {
      myProject = project;
      return this;
    }

    @Nullable
    public final JBPopup navigateOrCreatePopup() {
      if (myTargets.length == 0) {
        if (!allowEmptyTargets())
          return null; // empty initial targets are not allowed
        if (myListUpdaterTask == null || myListUpdaterTask.isFinished())
          return null; // there will be no targets.
      }
      if (myTargets.length == 1 && (myListUpdaterTask == null || myListUpdaterTask.isFinished())) {
        myTargetsConsumer.consume(myTargets);
        return null;
      }
      List<T> initialTargetsList = Arrays.asList(myTargets);
      Ref<T[]> updatedTargetsList = Ref.create(myTargets);

      final IPopupChooserBuilder<T> builder = JBPopupFactory.getInstance().createPopupChooserBuilder(initialTargetsList);
      afterPopupBuilderCreated(builder);
      if (myListRenderer instanceof PsiElementListCellRenderer) {
        ((PsiElementListCellRenderer)myListRenderer).installSpeedSearch(builder);
      }

      IPopupChooserBuilder<T> popupChooserBuilder = builder.
        setTitle(myTitle).
        setMovable(true).
        setFont(EditorUtil.getEditorFont()).
        setRenderer(myListRenderer).
        withHintUpdateSupply().
        setResizable(true).
        setItemsChosenCallback(elements -> myTargetsConsumer.consume((T[])elements.toArray(NavigatablePsiElement.EMPTY_NAVIGATABLE_ELEMENT_ARRAY))).
        setCancelCallback(() -> {
          if (myListUpdaterTask != null) {
            myListUpdaterTask.cancelTask();
          }
          return true;
        });
      final Ref<UsageView> usageView = new Ref<>();
      if (myFindUsagesTitle != null) {
        popupChooserBuilder = popupChooserBuilder.setCouldPin(popup -> {
          usageView.set(FindUtil.showInUsageView(null, updatedTargetsList.get(), myFindUsagesTitle, getProject()));
          popup.cancel();
          return false;
        });
      }

      final JBPopup popup = popupChooserBuilder.createPopup();
      if (builder instanceof PopupChooserBuilder) {
        JBList<NavigatablePsiElement> list = (JBList)((PopupChooserBuilder)builder).getChooserComponent();
        list.setTransferHandler(new TransferHandler() {
          @Override
          protected Transferable createTransferable(JComponent c) {
            final Object[] selectedValues = list.getSelectedValues();
            final PsiElement[] copy = new PsiElement[selectedValues.length];
            for (int i = 0; i < selectedValues.length; i++) {
              copy[i] = (PsiElement)selectedValues[i];
            }
            return new PsiCopyPasteManager.MyTransferable(copy);
          }

          @Override
          public int getSourceActions(JComponent c) {
            return COPY;
          }
        });

        JScrollPane pane = ((PopupChooserBuilder)builder).getScrollPane();
        pane.setBorder(null);
        pane.setViewportBorder(null);
      }

      if (myListUpdaterTask != null) {
        ListComponentUpdater popupUpdater = builder.getBackgroundUpdater();
        myListUpdaterTask.init(popup, new ListComponentUpdater() {
          @Override
          public void replaceModel(@NotNull List<? extends PsiElement> data) {
            updatedTargetsList.set((T[])data.toArray(NavigatablePsiElement.EMPTY_NAVIGATABLE_ELEMENT_ARRAY));
            popupUpdater.replaceModel(data);
          }

          @Override
          public void paintBusy(boolean paintBusy) {
            popupUpdater.paintBusy(paintBusy);
          }
        }, usageView);
      }
      return popup;
    }

    @NotNull
    private Project getProject() {
      if (myProject != null) {
        return myProject;
      }
      assert !allowEmptyTargets() : "Project was not set and cannot be taken from targets";
      return myTargets[0].getProject();
    }

    protected boolean allowEmptyTargets() {
      return false;
    }

    protected void afterPopupBuilderCreated(@NotNull IPopupChooserBuilder<T> builder) {
      // Do nothing by default
    }
  }

  /**
   * @deprecated use {@link #openTargets(MouseEvent, NavigatablePsiElement[], String, String, ListCellRenderer, BackgroundUpdaterTask)} instead
   */
  @Deprecated
  public static void openTargets(MouseEvent e,
                                 NavigatablePsiElement[] targets,
                                 @Nls String title,
                                 @Nls final String findUsagesTitle,
                                 ListCellRenderer listRenderer,
                                 @Nullable ListBackgroundUpdaterTask listUpdaterTask) {
    openTargets(e, targets, title, findUsagesTitle, listRenderer, (BackgroundUpdaterTask)listUpdaterTask);
  }
}
