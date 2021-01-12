/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder;
import com.intellij.concurrency.JobLauncher;
import com.intellij.ide.IdeBundle;
import com.intellij.injected.editor.DocumentWindow;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.ProperTextRange;
import com.intellij.openapi.util.Segment;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageManagerImpl;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.psi.impl.source.tree.injected.Place;
import com.intellij.util.CommonProcessors;
import com.intellij.util.Processor;
import com.intellij.util.Processors;
import com.intellij.util.containers.ContainerUtil;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.intellij.openapi.editor.colors.EditorColors.createInjectedLanguageFragmentKey;

public class InjectedGeneralHighlightingPass extends GeneralHighlightingPass {
  InjectedGeneralHighlightingPass(@NotNull Project project,
                                  @NotNull PsiFile file,
                                  @NotNull Document document,
                                  int startOffset,
                                  int endOffset,
                                  boolean updateAll,
                                  @NotNull ProperTextRange priorityRange,
                                  @Nullable Editor editor,
                                  @NotNull HighlightInfoProcessor highlightInfoProcessor) {
    super(project, file, document, startOffset, endOffset, updateAll, priorityRange, editor, highlightInfoProcessor);
  }

  @Override
  protected String getPresentableName() {
    return IdeBundle.message("highlighting.pass.injected.presentable.name");
  }

  @Override
  protected void collectInformationWithProgress(@NotNull final ProgressIndicator progress) {
    if (!Registry.is("editor.injected.highlighting.enabled")) return;

    List<Divider.DividedElements> allDivided = new ArrayList<>();
    Divider.divideInsideAndOutsideAllRoots(myFile, myRestrictRange, myPriorityRange, SHOULD_HIGHLIGHT_FILTER, new CommonProcessors.CollectProcessor<>(allDivided));

    List<PsiElement> allInsideElements = ContainerUtil.concat((List<List<PsiElement>>)ContainerUtil.map(allDivided, d -> d.inside));
    List<PsiElement> allOutsideElements = ContainerUtil.concat((List<List<PsiElement>>)ContainerUtil.map(allDivided, d -> d.outside));

    // all infos for the "injected fragment for the host which is inside" are indeed inside
    // but some of the infos for the "injected fragment for the host which is outside" can be still inside
    Set<PsiFile> injected = getInjectedPsiFiles(allInsideElements, allOutsideElements, progress);

    Set<HighlightInfo> injectedResult = new THashSet<>();
    if (!addInjectedPsiHighlights(injected, progress, Collections.synchronizedSet(injectedResult))) {
      throw new ProcessCanceledException();
    }

    Set<HighlightInfo> result;
    synchronized (injectedResult) {
      // sync here because all writes happened in another thread
      result = injectedResult;
    }
    final Set<HighlightInfo> gotHighlights = new THashSet<>(100);
    final List<HighlightInfo> injectionsOutside = new ArrayList<>(gotHighlights.size());
    for (HighlightInfo info : result) {
      if (myRestrictRange.contains(info)) {
        gotHighlights.add(info);
      }
      else {
        // nonconditionally apply injected results regardless whether they are in myStartOffset,myEndOffset
        injectionsOutside.add(info);
      }
    }

    if (!injectionsOutside.isEmpty()) {
      final ProperTextRange priorityIntersection = myPriorityRange.intersection(myRestrictRange);
      if ((!allInsideElements.isEmpty() || !gotHighlights.isEmpty()) &&
          priorityIntersection != null) { // do not apply when there were no elements to highlight
        // clear infos found in visible area to avoid applying them twice
        final List<HighlightInfo> toApplyInside = new ArrayList<>(gotHighlights);
        myHighlights.addAll(toApplyInside);
        gotHighlights.clear();

        myHighlightInfoProcessor.highlightsInsideVisiblePartAreProduced(myHighlightingSession, getEditor(), toApplyInside, myPriorityRange, myRestrictRange,
                                                                        getId());
      }

      List<HighlightInfo> toApply = new ArrayList<>();
      for (HighlightInfo info : gotHighlights) {
        if (!myRestrictRange.contains(info)) continue;
        if (!myPriorityRange.contains(info)) {
          toApply.add(info);
        }
      }
      toApply.addAll(injectionsOutside);

      myHighlightInfoProcessor.highlightsOutsideVisiblePartAreProduced(myHighlightingSession, getEditor(), toApply, myRestrictRange, new ProperTextRange(0, myDocument.getTextLength()),
                                                                       getId());
    }
    else {
      // else apply only result (by default apply command) and only within inside
      myHighlights.addAll(gotHighlights);
      myHighlightInfoProcessor.highlightsInsideVisiblePartAreProduced(myHighlightingSession, getEditor(), myHighlights, myRestrictRange, myRestrictRange,
                                                                      getId());
    }
  }

  @NotNull
  private Set<PsiFile> getInjectedPsiFiles(@NotNull final List<? extends PsiElement> elements1,
                                           @NotNull final List<? extends PsiElement> elements2,
                                           @NotNull final ProgressIndicator progress) {
    ApplicationManager.getApplication().assertReadAccessAllowed();

    List<DocumentWindow> injected = InjectedLanguageManager.getInstance(myProject).getCachedInjectedDocumentsInRange(myFile, myFile.getTextRange());
    final Collection<PsiElement> hosts = new THashSet<>(elements1.size() + elements2.size() + injected.size());

    //rehighlight all injected PSI regardless the range,
    //since change in one place can lead to invalidation of injected PSI in (completely) other place.
    for (DocumentWindow documentRange : injected) {
      ProgressManager.checkCanceled();
      if (!documentRange.isValid()) continue;
      PsiFile file = PsiDocumentManager.getInstance(myProject).getPsiFile(documentRange);
      if (file == null) continue;
      PsiElement context = InjectedLanguageManager.getInstance(file.getProject()).getInjectionHost(file);
      if (context != null
          && context.isValid()
          && !file.getProject().isDisposed()
          && (myUpdateAll || myRestrictRange.intersects(context.getTextRange()))) {
        hosts.add(context);
      }
    }

    InjectedLanguageManagerImpl injectedLanguageManager = InjectedLanguageManagerImpl.getInstanceImpl(myProject);
    Processor<PsiElement> collectInjectableProcessor = Processors.cancelableCollectProcessor(hosts);
    injectedLanguageManager.processInjectableElements(elements1, collectInjectableProcessor);
    injectedLanguageManager.processInjectableElements(elements2, collectInjectableProcessor);

    final Set<PsiFile> outInjected = new THashSet<>();
    final PsiLanguageInjectionHost.InjectedPsiVisitor visitor = (injectedPsi, places) -> {
      synchronized (outInjected) {
        ProgressManager.checkCanceled();
        outInjected.add(injectedPsi);
      }
    };

    // the most expensive process is running injectors for these hosts, comparing to highlighting the resulting injected fragments,
    // so instead of showing "highlighted 1% of injected fragments", show "ran injectors for 1% of hosts"
    setProgressLimit(hosts.size());

    if (!JobLauncher.getInstance().invokeConcurrentlyUnderProgress(new ArrayList<>(hosts), progress,
                                                                   element -> {
                                                                     ApplicationManager.getApplication().assertReadAccessAllowed();
                                                                     InjectedLanguageManager.getInstance(myFile.getProject()).enumerateEx(
                                                                       element, myFile, false, visitor);
                                                                     advanceProgress(1);
                                                                     return true;
                                                                   })) {
      throw new ProcessCanceledException();
    }
    synchronized (outInjected) {
      return outInjected.isEmpty() ? Collections.emptySet() : new THashSet<>(outInjected);
    }
  }

  // returns false if canceled
  private boolean addInjectedPsiHighlights(@NotNull final Set<? extends PsiFile> injectedFiles,
                                           @NotNull final ProgressIndicator progress,
                                           @NotNull final Collection<? super HighlightInfo> outInfos) {
    if (injectedFiles.isEmpty()) return true;
    final InjectedLanguageManager injectedLanguageManager = InjectedLanguageManager.getInstance(myProject);
    final TextAttributes injectedAttributes = myGlobalScheme.getAttributes(createInjectedLanguageFragmentKey(myFile.getLanguage()));

    return JobLauncher.getInstance()
      .invokeConcurrentlyUnderProgress(new ArrayList<>(injectedFiles), progress,
                                       injectedPsi -> addInjectedPsiHighlights(injectedPsi, injectedAttributes, outInfos,
                                                                               injectedLanguageManager));
  }

  @Override
  protected void queueInfoToUpdateIncrementally(@NotNull HighlightInfo info) {
    // do not send info to highlight immediately - we need to convert its offsets first
    // see addPatchedInfos()
  }

  private boolean addInjectedPsiHighlights(@NotNull PsiFile injectedPsi,
                                           @Nullable TextAttributes injectedAttributes,
                                           @NotNull Collection<? super HighlightInfo> outInfos,
                                           @NotNull InjectedLanguageManager injectedLanguageManager) {
    DocumentWindow documentWindow = (DocumentWindow)PsiDocumentManager.getInstance(myProject).getCachedDocument(injectedPsi);
    if (documentWindow == null) return true;
    Place places = InjectedLanguageUtil.getShreds(injectedPsi);
    boolean addTooltips = places.size() < 100;
    for (PsiLanguageInjectionHost.Shred place : places) {
      PsiLanguageInjectionHost host = place.getHost();
      if (host == null) continue;
      TextRange textRange = place.getRangeInsideHost().shiftRight(host.getTextRange().getStartOffset());
      if (textRange.isEmpty()) continue;
      HighlightInfo.Builder builder = HighlightInfo.newHighlightInfo(HighlightInfoType.INJECTED_LANGUAGE_BACKGROUND).range(textRange);
      if (injectedAttributes != null && InjectedLanguageUtil.isHighlightInjectionBackground(host)) {
        builder.textAttributes(injectedAttributes);
      }
      if (addTooltips) {
        String desc = injectedPsi.getLanguage().getDisplayName() + ": " + injectedPsi.getText();
        builder.unescapedToolTip(desc);
      }
      HighlightInfo info = builder.createUnconditionally();
      info.setFromInjection(true);
      outInfos.add(info);
    }

    HighlightInfoHolder holder = createInfoHolder(injectedPsi);
    runHighlightVisitorsForInjected(injectedPsi, holder);
    for (int i = 0; i < holder.size(); i++) {
      HighlightInfo info = holder.get(i);
      final int startOffset = documentWindow.injectedToHost(info.startOffset);
      final TextRange fixedTextRange = getFixedTextRange(documentWindow, startOffset);
      addPatchedInfos(info, injectedPsi, documentWindow, injectedLanguageManager, fixedTextRange, outInfos);
    }
    int injectedStart = holder.size();
    highlightInjectedSyntax(injectedPsi, holder);
    for (int i = injectedStart; i < holder.size(); i++) {
      HighlightInfo info = holder.get(i);
      final int startOffset = info.startOffset;
      final TextRange fixedTextRange = getFixedTextRange(documentWindow, startOffset);
      if (fixedTextRange == null) {
        info.setFromInjection(true);
        outInfos.add(info);
      }
      else {
        HighlightInfo patched =
          new HighlightInfo(info.forcedTextAttributes, info.forcedTextAttributesKey,
                            info.type, fixedTextRange.getStartOffset(),
                            fixedTextRange.getEndOffset(),
                            info.getDescription(), info.getToolTip(), info.type.getSeverity(null),
                            info.isAfterEndOfLine(), null, false, 0, info.getProblemGroup(), info.getInspectionToolId(), info.getGutterIconRenderer(), info.getGroup());
        patched.setFromInjection(true);
        outInfos.add(patched);
      }
    }

    if (!isDumbMode()) {
      List<HighlightInfo> todos = new ArrayList<>();
      highlightTodos(injectedPsi, injectedPsi.getText(), 0, injectedPsi.getTextLength(), myPriorityRange, todos, todos);
      for (HighlightInfo info : todos) {
        addPatchedInfos(info, injectedPsi, documentWindow, injectedLanguageManager, null, outInfos);
      }
    }
    return true;
  }

  @Nullable("null means invalid")
  private static TextRange getFixedTextRange(@NotNull DocumentWindow documentWindow, int startOffset) {
    final TextRange fixedTextRange;
    TextRange textRange = documentWindow.getHostRange(startOffset);
    if (textRange == null) {
      // todo[cdr] check this fix. prefix/suffix code annotation case
      textRange = findNearestTextRange(documentWindow, startOffset);
      if (textRange == null) return null;
      final boolean isBefore = startOffset < textRange.getStartOffset();
      fixedTextRange = new ProperTextRange(isBefore ? textRange.getStartOffset() - 1 : textRange.getEndOffset(),
                                     isBefore ? textRange.getStartOffset() : textRange.getEndOffset() + 1);
    }
    else {
      fixedTextRange = null;
    }
    return fixedTextRange;
  }

  private static void addPatchedInfos(@NotNull HighlightInfo info,
                                      @NotNull PsiFile injectedPsi,
                                      @NotNull DocumentWindow documentWindow,
                                      @NotNull InjectedLanguageManager injectedLanguageManager,
                                      @Nullable TextRange fixedTextRange,
                                      @NotNull Collection<? super HighlightInfo> out) {
    ProperTextRange textRange = new ProperTextRange(info.startOffset, info.endOffset);
    List<TextRange> editables = injectedLanguageManager.intersectWithAllEditableFragments(injectedPsi, textRange);
    for (TextRange editable : editables) {
      TextRange hostRange = fixedTextRange == null ? documentWindow.injectedToHost(editable) : fixedTextRange;

      boolean isAfterEndOfLine = info.isAfterEndOfLine();
      if (isAfterEndOfLine) {
        // convert injected afterEndOfLine to either host' afterEndOfLine or not-afterEndOfLine highlight of the injected fragment boundary
        int hostEndOffset = hostRange.getEndOffset();
        int lineNumber = documentWindow.getDelegate().getLineNumber(hostEndOffset);
        int hostLineEndOffset = documentWindow.getDelegate().getLineEndOffset(lineNumber);
        if (hostEndOffset < hostLineEndOffset) {
          // convert to non-afterEndOfLine
          isAfterEndOfLine = false;
          hostRange = new ProperTextRange(hostRange.getStartOffset(), hostEndOffset+1);
        }
      }

      HighlightInfo patched =
        new HighlightInfo(info.forcedTextAttributes, info.forcedTextAttributesKey, info.type,
                          hostRange.getStartOffset(), hostRange.getEndOffset(),
                          info.getDescription(), info.getToolTip(), info.type.getSeverity(null), isAfterEndOfLine, null,
                          false, 0, info.getProblemGroup(), info.getInspectionToolId(), info.getGutterIconRenderer(), info.getGroup());
      patched.setHint(info.hasHint());

      if (info.quickFixActionRanges != null) {
        for (Pair<HighlightInfo.IntentionActionDescriptor, TextRange> pair : info.quickFixActionRanges) {
          TextRange quickfixTextRange = pair.getSecond();
          List<TextRange> editableQF = injectedLanguageManager.intersectWithAllEditableFragments(injectedPsi, quickfixTextRange);
          for (TextRange editableRange : editableQF) {
            HighlightInfo.IntentionActionDescriptor descriptor = pair.getFirst();
            if (patched.quickFixActionRanges == null) patched.quickFixActionRanges = new ArrayList<>();
            TextRange hostEditableRange = documentWindow.injectedToHost(editableRange);
            patched.quickFixActionRanges.add(Pair.create(descriptor, hostEditableRange));
          }
        }
      }
      patched.setFromInjection(true);
      out.add(patched);
    }
  }

  // finds the first nearest text range
  @Nullable("null means invalid")
  private static TextRange findNearestTextRange(final DocumentWindow documentWindow, final int startOffset) {
    TextRange textRange = null;
    for (Segment marker : documentWindow.getHostRanges()) {
      TextRange curRange = ProperTextRange.create(marker);
      if (curRange.getStartOffset() > startOffset && textRange != null) break;
      textRange = curRange;
    }
    return textRange;
  }

  private void runHighlightVisitorsForInjected(@NotNull PsiFile injectedPsi, @NotNull HighlightInfoHolder holder) {
    HighlightVisitor[] filtered = getHighlightVisitors(injectedPsi);
    try {
      final List<PsiElement> elements = CollectHighlightsUtil.getElementsInRange(injectedPsi, 0, injectedPsi.getTextLength());
      for (final HighlightVisitor visitor : filtered) {
        visitor.analyze(injectedPsi, true, holder, () -> {
          for (PsiElement element : elements) {
            ProgressManager.checkCanceled();
            visitor.visit(element);
          }
        });
      }
    }
    finally {
      incVisitorUsageCount(-1);
    }
  }

  private void highlightInjectedSyntax(@NotNull PsiFile injectedPsi, @NotNull HighlightInfoHolder holder) {
    List<InjectedLanguageUtil.TokenInfo> tokens = InjectedLanguageUtil.getHighlightTokens(injectedPsi);
    if (tokens == null) return;

    final TextAttributes defaultAttrs = myGlobalScheme.getAttributes(HighlighterColors.TEXT);

    Place shreds = InjectedLanguageUtil.getShreds(injectedPsi);
    int shredIndex = -1;
    int injectionHostTextRangeStart = -1;
    for (InjectedLanguageUtil.TokenInfo token : tokens) {
      ProgressManager.checkCanceled();
      TextRange range = token.rangeInsideInjectionHost;
      if (range.getLength() == 0) continue;
      if (shredIndex != token.shredIndex) {
        shredIndex = token.shredIndex;
        PsiLanguageInjectionHost.Shred shred = shreds.get(shredIndex);
        PsiLanguageInjectionHost host = shred.getHost();
        if (host == null) return;
        injectionHostTextRangeStart = host.getTextRange().getStartOffset();
      }
      TextRange annRange = range.shiftRight(injectionHostTextRangeStart);

      // force attribute colors to override host' ones
      TextAttributes attributes = token.attributes;
      TextAttributes forcedAttributes;
      if (attributes == null || attributes.isEmpty() || attributes.equals(defaultAttrs)) {
        forcedAttributes = TextAttributes.ERASE_MARKER;
      }
      else {
        HighlightInfo info = HighlightInfo.newHighlightInfo(HighlightInfoType.INJECTED_LANGUAGE_FRAGMENT).range(annRange).textAttributes(
          TextAttributes.ERASE_MARKER).createUnconditionally();
        holder.add(info);

        forcedAttributes = new TextAttributes(attributes.getForegroundColor(), attributes.getBackgroundColor(),
                                              attributes.getEffectColor(), attributes.getEffectType(), attributes.getFontType());
      }

      HighlightInfo info =
        HighlightInfo.newHighlightInfo(HighlightInfoType.INJECTED_LANGUAGE_FRAGMENT).range(annRange).textAttributes(forcedAttributes)
          .createUnconditionally();
      holder.add(info);
    }
  }

  @Override
  protected void applyInformationWithProgress() {
  }
}
