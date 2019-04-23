// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints;

import com.intellij.codeInsight.daemon.impl.ParameterHintsPresentationManager;
import com.intellij.diff.util.DiffUtil;
import com.intellij.lang.Language;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable;
import com.intellij.openapi.editor.ex.util.EditorScrollingPositionKeeper;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.SyntaxTraverser;
import gnu.trove.TIntObjectHashMap;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;


// TODO fix inheritance!
public class ParameterHintsPass  {
  private final TIntObjectHashMap<List<HintData>> myHints = new TIntObjectHashMap<>();
  private final TIntObjectHashMap<String> myShowOnlyIfExistedBeforeHints = new TIntObjectHashMap<>();
  private final SyntaxTraverser<PsiElement> myTraverser;
  private final PsiElement myRootElement;
  private final HintInfoFilter myHintInfoFilter;
  private final boolean myForceImmediateUpdate;
  private final SettingsKey<NoSettings> myKey;

  public static void syncUpdate(@NotNull PsiElement element, @NotNull Editor editor) {
    MethodInfoBlacklistFilter filter = MethodInfoBlacklistFilter.forLanguage(element.getLanguage());
    ParameterHintsPass pass = new ParameterHintsPass(element, filter,  true, ProxyInlayParameterHintsProvider.getOurKey());
    NoSettings settings = new NoSettings();
    try {
      pass.collect(element, editor, settings, true);
    }
    catch (IndexNotReadyException e) {
      return; // cannot update synchronously, hints will be updated after indexing ends by the complete pass
    }
    pass.apply(element, editor, new InlayModelWrapper(editor.getInlayModel()), settings);
  }

  public ParameterHintsPass(@NotNull PsiElement element,
                            @NotNull HintInfoFilter hintsFilter,
                            boolean forceImmediateUpdate,
                            SettingsKey<NoSettings> key) {
    myRootElement = element;
    myTraverser = SyntaxTraverser.psiTraverser(element);
    myHintInfoFilter = hintsFilter;
    myForceImmediateUpdate = forceImmediateUpdate;
    myKey = key;
  }

  @NotNull
  //@Override
  public SettingsKey<NoSettings> getKey() {
    return myKey;
  }

  //@Override
  public void collect(@NotNull PsiElement element, @NotNull Editor editor, @NotNull NoSettings settings, boolean isEnabled) {
    myHints.clear();
    Language language = element.getLanguage();
    InlayParameterHintsProvider provider = InlayParameterHintsExtension.INSTANCE.forLanguage(language);
    if (provider == null || !provider.canShowHintsWhenDisabled() || DiffUtil.isDiffEditor(editor)) return;
    Project project = element.getProject();
    InlayHintsSettings hintsSettings = ServiceManager.getService(project, InlayHintsSettings.class);
    if (!ProxyInlayParameterHintsProvider.isEnabledFor(element.getLanguage(), hintsSettings)) return;
    myTraverser.forEach(elem -> process(elem, provider, editor));
  }

  private void process(PsiElement element, InlayParameterHintsProvider provider, @NotNull Editor editor) {
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
      if (!canShowHintsAtOffset(offset, editor.getDocument())) return;

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

    if (ParameterHintsUpdater.hintRemovalDelayed(editor)) {
      ParameterHintsPassFactory.forceHintsUpdateOnNextPass(editor);
    }
    else if (myRootElement == element) {
      ParameterHintsPassFactory.putCurrentPsiModificationStamp(editor, element.getContainingFile());
    }
  }

  @NotNull
  private List<Inlay> hintsInRootElementArea(ParameterHintsPresentationManager manager, @NotNull Editor editor) {
    Document document = editor.getDocument();
    TextRange range = myRootElement.getTextRange();
    int elementStart = range.getStartOffset();
    int elementEnd = range.getEndOffset();

    // Adding hints on the borders is allowed only in case root element is a document
    // See: canShowHintsAtOffset
    if (document.getTextLength() != range.getLength()) {
      ++elementStart;
      --elementEnd;
    }

    return manager.getParameterHintsInRange(editor, elementStart, elementEnd);
  }

  /**
   * Adding hints on the borders of root element (at startOffset or endOffset)
   * is allowed only in the case when root element is a document
   *
   * @return true if a given offset can be used for hint rendering
   */
  private boolean canShowHintsAtOffset(int offset, @NotNull Document document) {
    TextRange rootRange = myRootElement.getTextRange();

    if (!rootRange.containsOffset(offset)) return false;
    if (offset > rootRange.getStartOffset() && offset < rootRange.getEndOffset()) return true;

    return document.getTextLength() == rootRange.getLength();
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