// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

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
import com.intellij.util.ArrayUtil;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;

public class PsiElementListNavigator {

  private PsiElementListNavigator() {
  }

  public static void openTargets(MouseEvent e, NavigatablePsiElement[] targets, String title, final String findUsagesTitle, ListCellRenderer listRenderer) {
    openTargets(e, targets, title, findUsagesTitle, listRenderer, (BackgroundUpdaterTask)null);
  }

  public static void openTargets(MouseEvent e,
                                 NavigatablePsiElement[] targets,
                                 String title,
                                 final String findUsagesTitle,
                                 ListCellRenderer listRenderer,
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

  public static void openTargets(Editor e, NavigatablePsiElement[] targets, String title, final String findUsagesTitle, ListCellRenderer listRenderer) {
    openTargets(e, targets, title, findUsagesTitle, listRenderer, null);
  }

  public static void openTargets(Editor e, NavigatablePsiElement[] targets, String title, final String findUsagesTitle,
                                 ListCellRenderer listRenderer, @Nullable BackgroundUpdaterTask listUpdaterTask) {
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
  public static JBPopup navigateOrCreatePopup(final NavigatablePsiElement[] targets,
                                               final String title,
                                               final String findUsagesTitle,
                                               final ListCellRenderer listRenderer,
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
  public static JBPopup navigateOrCreatePopup(@NotNull final NavigatablePsiElement[] targets,
                                              final String title,
                                              final String findUsagesTitle,
                                              final ListCellRenderer listRenderer,
                                              @Nullable final BackgroundUpdaterTask listUpdaterTask,
                                              @NotNull final Consumer<Object[]> consumer) {
    return new NavigateOrPopupHelper(targets, title)
      .setFindUsagesTitle(findUsagesTitle)
      .setListRenderer(listRenderer)
      .setListUpdaterTask(listUpdaterTask)
      .setTargetsConsumer(consumer)
      .navigateOrCreatePopup();
  }

  // Helper makes it easier to customize shown popup.
  public static class NavigateOrPopupHelper {

    @NotNull
    private final NavigatablePsiElement[] myTargets;

    private final String myTitle;

    @NotNull
    private Consumer<Object[]> myTargetsConsumer;

    @Nullable
    private String myFindUsagesTitle;

    @Nullable
    private ListCellRenderer myListRenderer;

    @Nullable
    private BackgroundUpdaterTask myListUpdaterTask;

    @Nullable
    private Project myProject;

    public NavigateOrPopupHelper(@NotNull NavigatablePsiElement[] targets, String title) {
      myTargets = targets;
      myTitle = title;
      myTargetsConsumer = selectedElements -> {
        for (Object element : selectedElements) {
          PsiElement selected = (PsiElement)element;
          if (selected.isValid()) {
            ((NavigatablePsiElement)selected).navigate(true);
          }
        }
      };
    }

    @NotNull
    public NavigateOrPopupHelper setFindUsagesTitle(@Nullable String findUsagesTitle) {
      myFindUsagesTitle = findUsagesTitle;
      return this;
    }

    @NotNull
    public NavigateOrPopupHelper setListRenderer(@Nullable ListCellRenderer listRenderer) {
      myListRenderer = listRenderer;
      return this;
    }

    @NotNull
    public NavigateOrPopupHelper setListUpdaterTask(@Nullable BackgroundUpdaterTask listUpdaterTask) {
      myListUpdaterTask = listUpdaterTask;
      return this;
    }

    @NotNull
    public NavigateOrPopupHelper setTargetsConsumer(@NotNull Consumer<Object[]> targetsConsumer) {
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
      List<NavigatablePsiElement> initialTargetsList = Arrays.asList(myTargets);
      Ref<NavigatablePsiElement[]> updatedTargetsList = Ref.create(myTargets);

      final IPopupChooserBuilder<NavigatablePsiElement> builder = JBPopupFactory.getInstance().createPopupChooserBuilder(initialTargetsList);
      afterPopupBuilderCreated(builder);
      if (myListRenderer instanceof PsiElementListCellRenderer) {
        ((PsiElementListCellRenderer)myListRenderer).installSpeedSearch(builder);
      }

      IPopupChooserBuilder<NavigatablePsiElement> popupChooserBuilder = builder.
        setTitle(myTitle).
        setMovable(true).
        setFont(EditorUtil.getEditorFont()).
        setRenderer(myListRenderer).
        withHintUpdateSupply().
        setResizable(true).
        setItemsChosenCallback(selectedValues -> myTargetsConsumer.consume(ArrayUtil.toObjectArray(selectedValues))).
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
            updatedTargetsList.set(data.toArray(NavigatablePsiElement.EMPTY_NAVIGATABLE_ELEMENT_ARRAY));
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

    protected void afterPopupBuilderCreated(@NotNull IPopupChooserBuilder<NavigatablePsiElement> builder) {
      // Do nothing by default
    }
  }

  /**
   * @deprecated use {@link #openTargets(MouseEvent, NavigatablePsiElement[], String, String, ListCellRenderer, BackgroundUpdaterTask)} instead
   */
  @Deprecated
  public static void openTargets(MouseEvent e,
                                 NavigatablePsiElement[] targets,
                                 String title,
                                 final String findUsagesTitle,
                                 ListCellRenderer listRenderer,
                                 @Nullable ListBackgroundUpdaterTask listUpdaterTask) {
    openTargets(e, targets, title, findUsagesTitle, listRenderer, (BackgroundUpdaterTask)listUpdaterTask);
  }
}
