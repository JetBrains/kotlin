// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints;

import com.intellij.codeHighlighting.EditorBoundHighlightingPass;
import com.intellij.codeInsight.daemon.impl.ParameterHintsPresentationManager;
import com.intellij.codeInsight.daemon.impl.analysis.HighlightingLevelManager;
import com.intellij.lang.Language;
import com.intellij.openapi.diff.impl.DiffUtil;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.ex.util.EditorScrollingPositionKeeper;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.SyntaxTraverser;
import gnu.trove.TIntObjectHashMap;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.intellij.codeInsight.hints.ParameterHintsPassFactory.forceHintsUpdateOnNextPass;
import static com.intellij.codeInsight.hints.ParameterHintsPassFactory.putCurrentPsiModificationStamp;

// TODO This pass should be rewritten with new API
public class ParameterHintsPass extends EditorBoundHighlightingPass {
  private final TIntObjectHashMap<List<HintData>> myHints = new TIntObjectHashMap<>();
  private final TIntObjectHashMap<String> myShowOnlyIfExistedBeforeHints = new TIntObjectHashMap<>();
  private final SyntaxTraverser<PsiElement> myTraverser;
  private final PsiElement myRootElement;
  private final HintInfoFilter myHintInfoFilter;
  private final boolean myForceImmediateUpdate;

  public static void syncUpdate(@NotNull PsiElement element, @NotNull Editor editor) {
    MethodInfoBlacklistFilter filter = MethodInfoBlacklistFilter.forLanguage(element.getLanguage());
    ParameterHintsPass pass = new ParameterHintsPass(element, editor, filter, true);
    try {
      pass.doCollectInformation(new ProgressIndicatorBase());
    }
    catch (IndexNotReadyException e) {
      return; // cannot update synchronously, hints will be updated after indexing ends by the complete pass
    }
    pass.applyInformationToEditor();
  }

  public ParameterHintsPass(@NotNull PsiElement element,
                            @NotNull Editor editor,
                            @NotNull HintInfoFilter hintsFilter,
                            boolean forceImmediateUpdate) {
    super(editor, element.getContainingFile(), true);
    myRootElement = element;
    myTraverser = SyntaxTraverser.psiTraverser(element);
    myHintInfoFilter = hintsFilter;
    myForceImmediateUpdate = forceImmediateUpdate;
  }

  @Override
  public void doCollectInformation(@NotNull ProgressIndicator progress) {
    assert myDocument != null;
    myHints.clear();

    Language language = myFile.getLanguage();
    InlayParameterHintsProvider provider = InlayParameterHintsExtension.INSTANCE.forLanguage(language);
    if (provider == null || !provider.canShowHintsWhenDisabled() && !isEnabled(language) || DiffUtil.isDiffEditor(myEditor)) return;
    if (!HighlightingLevelManager.getInstance(myFile.getProject()).shouldHighlight(myFile)) return;

    myTraverser.forEach(element -> process(element, provider));
  }

  private static boolean isEnabled(Language language) {
    return HintUtilsKt.isParameterHintsEnabledForLanguage(language);
  }

  private void process(PsiElement element, InlayParameterHintsProvider provider) {
    List<InlayInfo> hints = provider.getParameterHints(element);
    if (hints.isEmpty()) return;
    HintInfo info = provider.getHintInfo(element);

    boolean showHints = info == null || info instanceof HintInfo.OptionInfo || myHintInfoFilter.showHint(info);

    Stream<InlayInfo> inlays = hints.stream();
    if (!showHints) {
      inlays = inlays.filter((inlayInfo -> !inlayInfo.isFilterByBlacklist()));
    }

    inlays.forEach((hint) -> {
      int offset = hint.getOffset();
      if (!canShowHintsAtOffset(offset)) return;

      String presentation = provider.getInlayPresentation(hint.getText());
      if (hint.isShowOnlyIfExistedBefore()) {
        myShowOnlyIfExistedBeforeHints.put(offset, presentation);
      }
      else {
        List<HintData> hintList = myHints.get(offset);
        if (hintList == null) myHints.put(offset, hintList = new ArrayList<>());
        HintWidthAdjustment widthAdjustment = convertHintPresentation(hint.getWidthAdjustment(), provider);
        hintList.add(new HintData(presentation, hint.getRelatesToPrecedingText(), widthAdjustment));
      }
    });
  }

  private static HintWidthAdjustment convertHintPresentation(HintWidthAdjustment widthAdjustment,
                                                             InlayParameterHintsProvider provider) {
    if (widthAdjustment != null) {
      String hintText = widthAdjustment.getHintTextToMatch();
      if (hintText != null) {
        String adjusterHintPresentation = provider.getInlayPresentation(hintText);
        if (!hintText.equals(adjusterHintPresentation)) {
          widthAdjustment = new HintWidthAdjustment(widthAdjustment.getEditorTextToMatch(),
                                                    adjusterHintPresentation,
                                                    widthAdjustment.getAdjustmentPosition());
        }
      }
    }
    return widthAdjustment;
  }

  @Override
  public void doApplyInformationToEditor() {
    EditorScrollingPositionKeeper.perform(myEditor, false, () -> {
      ParameterHintsPresentationManager manager = ParameterHintsPresentationManager.getInstance();
      List<Inlay> hints = hintsInRootElementArea(manager);
      ParameterHintsUpdater updater = new ParameterHintsUpdater(myEditor, hints, myHints, myShowOnlyIfExistedBeforeHints, myForceImmediateUpdate);
      updater.update();
    });

    if (ParameterHintsUpdater.hintRemovalDelayed(myEditor)) {
      forceHintsUpdateOnNextPass(myEditor);
    }
    else if (myRootElement == myFile) {
      putCurrentPsiModificationStamp(myEditor, myFile);
    }
  }

  @NotNull
  private List<Inlay> hintsInRootElementArea(ParameterHintsPresentationManager manager) {
    assert myDocument != null;

    TextRange range = myRootElement.getTextRange();
    int elementStart = range.getStartOffset();
    int elementEnd = range.getEndOffset();

    // Adding hints on the borders is allowed only in case root element is a document
    // See: canShowHintsAtOffset
    if (myDocument.getTextLength() != range.getLength()) {
      ++elementStart;
      --elementEnd;
    }

    return manager.getParameterHintsInRange(myEditor, elementStart, elementEnd);
  }

  /**
   * Adding hints on the borders of root element (at startOffset or endOffset)
   * is allowed only in the case when root element is a document
   *
   * @return true if a given offset can be used for hint rendering
   */
  private boolean canShowHintsAtOffset(int offset) {
    TextRange rootRange = myRootElement.getTextRange();

    if (!rootRange.containsOffset(offset)) return false;
    if (offset > rootRange.getStartOffset() && offset < rootRange.getEndOffset()) return true;

    return myDocument != null && myDocument.getTextLength() == rootRange.getLength();
  }

  public static class HintData {
    public final String presentationText;
    public final boolean relatesToPrecedingText;
    public final HintWidthAdjustment widthAdjustment;

    public HintData(String text, boolean relatesToPrecedingText, HintWidthAdjustment widthAdjustment) {
      presentationText = text;
      this.relatesToPrecedingText = relatesToPrecedingText;
      this.widthAdjustment = widthAdjustment;
    }
  }
}