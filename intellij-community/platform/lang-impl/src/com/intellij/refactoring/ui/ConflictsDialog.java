// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.refactoring.ui;

import com.intellij.codeInsight.highlighting.ReadWriteAccessDetector;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.*;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.regex.Pattern;

public class ConflictsDialog extends DialogWrapper{
  private static final int SHOW_CONFLICTS_EXIT_CODE = 4;

  protected final String[] myConflictDescriptions;
  protected MultiMap<PsiElement, String> myElementConflictDescription;
  private final Project myProject;
  private Runnable myDoRefactoringRunnable;
  private final boolean myCanShowConflictsInView;
  private String myCommandName;

  public ConflictsDialog(@NotNull Project project, @NotNull MultiMap<PsiElement, String> conflictDescriptions) {
    this(project, conflictDescriptions, null, true, true);
  }

  public ConflictsDialog(@NotNull Project project,
                         @NotNull MultiMap<PsiElement, String> conflictDescriptions,
                         @Nullable Runnable doRefactoringRunnable) {
    this(project, conflictDescriptions, doRefactoringRunnable, true, true);
  }

  public ConflictsDialog(@NotNull Project project,
                         @NotNull MultiMap<PsiElement, String> conflictDescriptions,
                         @Nullable Runnable doRefactoringRunnable,
                         boolean alwaysShowOkButton,
                         boolean canShowConflictsInView) {
    super(project, true);
    myProject = project;
    myDoRefactoringRunnable = doRefactoringRunnable;
    myCanShowConflictsInView = canShowConflictsInView;

    final LinkedHashSet<String> conflicts = new LinkedHashSet<>(conflictDescriptions.values());
    myConflictDescriptions = ArrayUtilRt.toStringArray(conflicts);
    myElementConflictDescription = conflictDescriptions;
    setTitle(RefactoringBundle.message("problems.detected.title"));
    setOKButtonText(RefactoringBundle.message("continue.button"));
    setOKActionEnabled(alwaysShowOkButton || getDoRefactoringRunnable(null) != null);
    init();
  }

  /**
   * @deprecated use other CTORs
   */
  @Deprecated
  public ConflictsDialog(Project project, String... conflictDescriptions) {
    super(project, true);
    myProject = project;
    myConflictDescriptions = conflictDescriptions;
    myCanShowConflictsInView = true;
    setTitle(RefactoringBundle.message("problems.detected.title"));
    setOKButtonText(RefactoringBundle.message("continue.button"));
    init();
  }

  @Override
  @NotNull
  protected Action[] createActions(){
    final Action okAction = getOKAction();
    boolean showUsagesButton = myElementConflictDescription != null && myCanShowConflictsInView;

    if (showUsagesButton || !okAction.isEnabled()) {
      okAction.putValue(DEFAULT_ACTION, null);
    }

    if (!showUsagesButton) {
      return new Action[]{okAction,new CancelAction()};
    }
    return new Action[]{okAction, new MyShowConflictsInUsageViewAction(), new CancelAction()};
  }

  public boolean isShowConflicts() {
    return getExitCode() == SHOW_CONFLICTS_EXIT_CODE;
  }

  @Override
  protected JComponent createCenterPanel() {
    JPanel panel = new JPanel(new BorderLayout(0, 2));

    panel.add(new JLabel(RefactoringBundle.message("the.following.problems.were.found")), BorderLayout.NORTH);

    @NonNls StringBuilder buf = new StringBuilder();
    for (String description : myConflictDescriptions) {
      buf.append(description);
      buf.append("<br><br>");
    }
    JEditorPane messagePane = new JEditorPane();
    messagePane.setEditorKit(UIUtil.getHTMLEditorKit());
    messagePane.setText(buf.toString());
    messagePane.setEditable(false);
    JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(messagePane,
                                                                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                                                                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    scrollPane.setPreferredSize(JBUI.size(500, 400));
    panel.add(scrollPane, BorderLayout.CENTER);

    if (getOKAction().isEnabled()) {
      panel.add(new JLabel(RefactoringBundle.message("do.you.wish.to.ignore.them.and.continue")), BorderLayout.SOUTH);
    }

    return panel;
  }

  public void setCommandName(String commandName) {
    myCommandName = commandName;
  }

  private class CancelAction extends AbstractAction {
    CancelAction() {
      super(RefactoringBundle.message("cancel.button"));
      putValue(DEFAULT_ACTION,Boolean.TRUE);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      doCancelAction();
    }
  }

  protected Runnable getDoRefactoringRunnable(@Nullable UsageView usageView) {
    return myDoRefactoringRunnable;
  }

  private class MyShowConflictsInUsageViewAction extends AbstractAction {


    MyShowConflictsInUsageViewAction() {
      super("Show Conflicts in View");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      final UsageViewPresentation presentation = new UsageViewPresentation();
      final String codeUsagesString = "Conflicts";
      presentation.setCodeUsagesString(codeUsagesString);
      presentation.setTabName(codeUsagesString);
      presentation.setTabText(codeUsagesString);
      presentation.setShowCancelButton(true);

      final ArrayList<Usage> usages = new ArrayList<>(myElementConflictDescription.values().size());
      for (final PsiElement element : myElementConflictDescription.keySet()) {
        if (element == null) {
          usages.add(new DescriptionOnlyUsage());
          continue;
        }
        boolean isRead = false;
        boolean isWrite = false;
        ReadWriteAccessDetector detector = ReadWriteAccessDetector.findDetector(element);
        if (detector != null) {
          final ReadWriteAccessDetector.Access access = detector.getExpressionAccess(element);
          isRead = access != ReadWriteAccessDetector.Access.Write;
          isWrite = access != ReadWriteAccessDetector.Access.Read;
        }

        for (final String conflictDescription : myElementConflictDescription.get(element)) {
          final UsagePresentation usagePresentation = new DescriptionOnlyUsage(conflictDescription).getPresentation();
          Usage usage = isRead || isWrite ? new ReadWriteAccessUsageInfo2UsageAdapter(new UsageInfo(element), isRead, isWrite) {
            @NotNull
            @Override
            public UsagePresentation getPresentation() {
              return usagePresentation;
            }
          } : new UsageInfo2UsageAdapter(new UsageInfo(element)) {
            @NotNull
            @Override
            public UsagePresentation getPresentation() {
              return usagePresentation;
            }
          };
          usages.add(usage);
        }
      }
      final UsageView usageView = UsageViewManager.getInstance(myProject)
        .showUsages(UsageTarget.EMPTY_ARRAY, usages.toArray(Usage.EMPTY_ARRAY), presentation);
      Runnable doRefactoringRunnable = getDoRefactoringRunnable(usageView);
      if (doRefactoringRunnable != null) {
        usageView.addPerformOperationAction(
          doRefactoringRunnable,
          myCommandName != null ? myCommandName : RefactoringBundle.message("retry.command"),
          "Unable to perform refactoring. There were changes in code after the usages have been found.", RefactoringBundle.message("usageView.doAction"));
      }
      close(SHOW_CONFLICTS_EXIT_CODE);
    }

    private class DescriptionOnlyUsage implements Usage {
      private final String myConflictDescription;

      DescriptionOnlyUsage(@NotNull String conflictDescription) {
        myConflictDescription = StringUtil.unescapeXmlEntities(conflictDescription)
          .replaceAll("<code>", "")
          .replaceAll("</code>", "")
          .replaceAll("<b>", "")
          .replaceAll("</b>", "");
      }

      DescriptionOnlyUsage() {
        myConflictDescription =
          Pattern.compile("<[^<>]*>").matcher(StringUtil.join(new LinkedHashSet<>(myElementConflictDescription.get(null)), "\n")).replaceAll("");
      }

      @Override
      @NotNull
      public UsagePresentation getPresentation() {
        return new UsagePresentation() {
          @Override
          @NotNull
          public TextChunk[] getText() {
            return new TextChunk[] {new TextChunk(SimpleTextAttributes.REGULAR_ATTRIBUTES.toTextAttributes(), myConflictDescription)};
          }

          @Override
          @Nullable
          public Icon getIcon() {
            return null;
          }

          @Override
          public String getTooltipText() {
            return myConflictDescription;
          }

          @Override
          @NotNull
          public String getPlainText() {
            return myConflictDescription;
          }
        };
      }

      @Override
      public boolean canNavigateToSource() {
        return false;
      }

      @Override
      public boolean canNavigate() {
        return false;
      }
      @Override
      public void navigate(boolean requestFocus) {}

      @Override
      public FileEditorLocation getLocation() {
        return null;
      }

      @Override
      public boolean isReadOnly() {
        return false;
      }

      @Override
      public boolean isValid() {
        return true;
      }

      @Override
      public void selectInEditor() {}
      @Override
      public void highlightInEditor() {}
    }
  }
}
