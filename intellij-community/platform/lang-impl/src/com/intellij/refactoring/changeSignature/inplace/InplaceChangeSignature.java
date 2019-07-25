// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.refactoring.changeSignature.inplace;

import com.intellij.codeInsight.highlighting.HighlightManager;
import com.intellij.ide.util.PsiNavigationSupport;
import com.intellij.lang.findUsages.DescriptiveNameUtil;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.command.impl.FinishMarkAction;
import com.intellij.openapi.command.impl.StartMarkAction;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.BalloonBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.changeSignature.ChangeInfo;
import com.intellij.refactoring.changeSignature.ChangeSignatureHandler;
import com.intellij.refactoring.rename.inplace.InplaceRefactoring;
import com.intellij.ui.NonFocusableCheckBox;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.PositionTracker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class InplaceChangeSignature implements DocumentListener {
  public static final Key<InplaceChangeSignature> INPLACE_CHANGE_SIGNATURE = Key.create("EditorInplaceChangeSignature");
  private ChangeInfo myCurrentInfo;
  private ChangeInfo myStableChange;
  private String myInitialSignature;
  private String myInitialName;
  private Editor myEditor;
  private LanguageChangeSignatureDetector<ChangeInfo> myDetector;

  private final Project myProject;
  private final PsiDocumentManager myDocumentManager;
  private final ArrayList<RangeHighlighter> myHighlighters = new ArrayList<>();
  private StartMarkAction myMarkAction;
  private Balloon myBalloon;
  private boolean myDelegate;
  private EditorEx myPreview;

  public InplaceChangeSignature(Project project, Editor editor, @NotNull PsiElement element) {
    myDocumentManager = PsiDocumentManager.getInstance(project);
    myProject = project;
    try {
      myMarkAction = StartMarkAction.start(editor, project, ChangeSignatureHandler.REFACTORING_NAME);
    }
    catch (StartMarkAction.AlreadyStartedException e) {
      final int exitCode = Messages.showYesNoDialog(myProject, e.getMessage(), ChangeSignatureHandler.REFACTORING_NAME, "Navigate to Started", "Cancel", Messages.getErrorIcon());
      if (exitCode == Messages.CANCEL) return;
      PsiElement method = myStableChange.getMethod();
      VirtualFile virtualFile = PsiUtilCore.getVirtualFile(method);
      PsiNavigationSupport.getInstance().createNavigatable(project, virtualFile, method.getTextOffset())
                          .navigate(true);
      return;
    }


    myEditor = editor;
    myDetector = LanguageChangeSignatureDetectors.INSTANCE.forLanguage(element.getLanguage());
    myStableChange = myDetector.createInitialChangeInfo(element);
    myInitialSignature = myDetector.extractSignature(myStableChange);
    myInitialName = DescriptiveNameUtil.getDescriptiveName(myStableChange.getMethod());
    TextRange highlightingRange = myDetector.getHighlightingRange(myStableChange);

    HighlightManager highlightManager = HighlightManager.getInstance(myProject);
    TextAttributes attributes = EditorColorsManager.getInstance().getGlobalScheme().getAttributes(EditorColors.LIVE_TEMPLATE_ATTRIBUTES);
    highlightManager.addRangeHighlight(editor, highlightingRange.getStartOffset(), highlightingRange.getEndOffset(), attributes, false, myHighlighters);
    for (RangeHighlighter highlighter : myHighlighters) {
      highlighter.setGreedyToRight(true);
      highlighter.setGreedyToLeft(true);
    }
    myEditor.getDocument().addDocumentListener(this);
    myEditor.putUserData(INPLACE_CHANGE_SIGNATURE, this);
    myPreview = InplaceRefactoring.createPreviewComponent(project, myDetector.getFileType());
    showBalloon();
  }

  @Nullable
  public static InplaceChangeSignature getCurrentRefactoring(@NotNull Editor editor) {
    return editor.getUserData(INPLACE_CHANGE_SIGNATURE);
  }

  public ChangeInfo getCurrentInfo() {
    return myCurrentInfo;
  }

  public String getInitialName() {
    return myInitialName;
  }

  public String getInitialSignature() {
    return myInitialSignature;
  }

  @NotNull
  public ChangeInfo getStableChange() {
    return myStableChange;
  }

  public void cancel() {
    TextRange highlightingRange = myDetector.getHighlightingRange(getStableChange());
    Document document = myEditor.getDocument();
    String initialSignature = myInitialSignature;
    detach();
    temporallyRevertChanges(highlightingRange, document, initialSignature, myProject);
  }

  @Override
  public void documentChanged(@NotNull DocumentEvent event) {
    RangeMarker marker = event.getDocument().createRangeMarker(event.getOffset(), event.getOffset());
    myDocumentManager.performWhenAllCommitted(() -> {
      if (myDetector == null) {
        return;
      }
      PsiFile file = myDocumentManager.getPsiFile(event.getDocument());
      if (file == null) {
        return;
      }
      PsiElement element = file.findElementAt(marker.getStartOffset());
      marker.dispose();
      if (element == null || myDetector.ignoreChanges(element)) return;

      if (element instanceof PsiWhiteSpace) {
        PsiElement method = myStableChange.getMethod();
        if (PsiTreeUtil.skipWhitespacesForward(element) == method) {
          return;
        }
      }

      if (!myDetector.isChangeSignatureAvailableOnElement(element, myStableChange)) {
        detach();
        return;
      }

      updateCurrentInfo();
    });
  }

  private void updateCurrentInfo() {
    if (myCurrentInfo == null) {
      myCurrentInfo = myStableChange;
    }
    String signature = myDetector.extractSignature(myCurrentInfo);
    ChangeInfo changeInfo = myDetector.createNextChangeInfo(signature, myCurrentInfo, myDelegate);
    if (changeInfo == null && myCurrentInfo != null) {
      myStableChange = myCurrentInfo;
    }
    if (changeInfo != null) {
      updateMethodSignature(changeInfo);
    }
    myCurrentInfo = changeInfo;
  }

  private void updateMethodSignature(ChangeInfo changeInfo) {
    ArrayList<TextRange> deleteRanges = new ArrayList<>();
    ArrayList<TextRange> newRanges = new ArrayList<>();
    String methodSignature = myDetector.getMethodSignaturePreview(changeInfo, deleteRanges, newRanges);

    myPreview.getMarkupModel().removeAllHighlighters();
    WriteCommandAction.writeCommandAction(null).run(() -> myPreview.getDocument().replaceString(0, myPreview.getDocument().getTextLength(), methodSignature));
    TextAttributes deprecatedAttributes =
      EditorColorsManager.getInstance().getGlobalScheme().getAttributes(CodeInsightColors.DEPRECATED_ATTRIBUTES);
    for (TextRange range : deleteRanges) {
      myPreview.getMarkupModel().addRangeHighlighter(range.getStartOffset(), range.getEndOffset(), HighlighterLayer.ADDITIONAL_SYNTAX,
                                                     deprecatedAttributes, HighlighterTargetArea.EXACT_RANGE);
    }
    TextAttributes todoAttributes =
      EditorColorsManager.getInstance().getGlobalScheme().getAttributes(CodeInsightColors.TODO_DEFAULT_ATTRIBUTES);
    for (TextRange range : newRanges) {
      myPreview.getMarkupModel().addRangeHighlighter(range.getStartOffset(), range.getEndOffset(), HighlighterLayer.ADDITIONAL_SYNTAX,
                                                     todoAttributes, HighlighterTargetArea.EXACT_RANGE);
    }
  }

  protected void showBalloon() {
    NonFocusableCheckBox checkBox = new NonFocusableCheckBox(RefactoringBundle.message("delegation.panel.delegate.via.overloading.method"));
    checkBox.addActionListener(e -> {
      myDelegate = checkBox.isSelected();
      updateCurrentInfo();
    });
    JPanel content = new JPanel(new BorderLayout());
    content.add(new JBLabel("Performed signature modifications:"), BorderLayout.NORTH);
    content.add(myPreview.getComponent(), BorderLayout.CENTER);
    updateMethodSignature(myStableChange);
    content.add(checkBox, BorderLayout.SOUTH);
    final BalloonBuilder balloonBuilder = JBPopupFactory.getInstance().createDialogBalloonBuilder(content, null).setSmallVariant(true);
    myBalloon = balloonBuilder.createBalloon();
    myEditor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
    myBalloon.show(new PositionTracker<Balloon>(myEditor.getContentComponent()) {
      @Override
      public RelativePoint recalculateLocation(Balloon object) {
        int offset = myStableChange.getMethod().getTextOffset();
        VisualPosition visualPosition = myEditor.offsetToVisualPosition(offset);
        Point point = myEditor.visualPositionToXY(new VisualPosition(visualPosition.line, visualPosition.column));
        return new RelativePoint(myEditor.getContentComponent(), point);
      }
    }, Balloon.Position.above);
    Disposer.register(myBalloon, () -> {
      EditorFactory.getInstance().releaseEditor(myPreview);
      myPreview = null;
    });
  }

  public void detach() {
    myEditor.getDocument().removeDocumentListener(this);
    HighlightManager highlightManager = HighlightManager.getInstance(myProject);
    for (RangeHighlighter highlighter : myHighlighters) {
      highlightManager.removeSegmentHighlighter(myEditor, highlighter);
    }
    myHighlighters.clear();
    myBalloon.hide();
    myDetector = null;
    FinishMarkAction.finish(myProject, myEditor, myMarkAction);
    myEditor.putUserData(INPLACE_CHANGE_SIGNATURE, null);
  }

  public static void temporallyRevertChanges(final TextRange signatureRange,
                                             final Document document,
                                             final String initialSignature,
                                             Project project) {
    WriteCommandAction.runWriteCommandAction(project, () -> {
      document.replaceString(signatureRange.getStartOffset(), signatureRange.getEndOffset(), initialSignature);
      PsiDocumentManager.getInstance(project).commitDocument(document);
    });
  }
}
