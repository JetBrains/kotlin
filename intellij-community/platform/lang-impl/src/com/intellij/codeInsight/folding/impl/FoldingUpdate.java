// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.folding.impl;

import com.intellij.diagnostic.AttachmentFactory;
import com.intellij.injected.editor.DocumentWindow;
import com.intellij.injected.editor.EditorWindow;
import com.intellij.lang.Language;
import com.intellij.lang.folding.FoldingBuilder;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.lang.folding.LanguageFolding;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Attachment;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Getter;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.DebugUtil;
import com.intellij.psi.impl.source.tree.injected.FoldingRegionWindow;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class FoldingUpdate {
  private static final Logger LOG = Logger.getInstance(FoldingUpdate.class);

  private static final Key<CachedValue<Runnable>> CODE_FOLDING_KEY = Key.create("code folding");

  private FoldingUpdate() {
  }

  @Nullable
  static Runnable updateFoldRegions(@NotNull final Editor editor, @NotNull PsiFile file, final boolean applyDefaultState, final boolean quick) {
    ApplicationManager.getApplication().assertReadAccessAllowed();

    final Project project = file.getProject();
    final Document document = editor.getDocument();
    LOG.assertTrue(!PsiDocumentManager.getInstance(project).isUncommited(document));

    CachedValue<Runnable> value = editor.getUserData(CODE_FOLDING_KEY);

    if (value != null && !applyDefaultState) {
      Getter<Runnable> cached = value.getUpToDateOrNull();
      if (cached != null) {
        return cached.get();
      }
    }
    if (quick || applyDefaultState) return getUpdateResult(file, document, quick, project, editor, applyDefaultState).getValue();

    return CachedValuesManager.getManager(project).getCachedValue(
      editor, CODE_FOLDING_KEY, () -> {
        PsiFile file1 = PsiDocumentManager.getInstance(project).getPsiFile(document);
        return getUpdateResult(file1, document, false, project, editor, false);
      }, false);
  }

  private static CachedValueProvider.Result<Runnable> getUpdateResult(@NotNull PsiFile file,
                                                                      @NotNull Document document,
                                                                      boolean quick,
                                                                      final Project project,
                                                                      final Editor editor,
                                                                      final boolean applyDefaultState) {
    PsiUtilCore.ensureValid(file);
    final List<RegionInfo> elementsToFold = getFoldingsFor(file, quick);
    final UpdateFoldRegionsOperation operation = new UpdateFoldRegionsOperation(project, editor, file, elementsToFold,
                                                                                applyDefaultStateMode(applyDefaultState),
                                                                                !applyDefaultState, false);
    int documentLength = document.getTextLength();
    AtomicBoolean alreadyExecuted = new AtomicBoolean();
    Runnable runnable = () -> {
      if (alreadyExecuted.compareAndSet(false, true)) {
        if (documentLength != document.getTextLength() || !PsiDocumentManager.getInstance(project).isCommitted(document)) {
          reportUnexpectedDocumentChange(file, document, documentLength);
        }
        editor.getFoldingModel().runBatchFoldingOperationDoNotCollapseCaret(operation);
      }
    };
    Set<Object> dependencies = new HashSet<>();
    dependencies.add(file);
    dependencies.add(editor.getFoldingModel());
    for (RegionInfo info : elementsToFold) {
      dependencies.addAll(info.descriptor.getDependencies());
    }
    return CachedValueProvider.Result.create(runnable, ArrayUtil.toObjectArray(dependencies));
  }

  private static void reportUnexpectedDocumentChange(@NotNull PsiFile file, @NotNull Document document, int prevLength) {
    Document fileDoc = file.getViewProvider().getDocument();
    VirtualFile vFile = file.getViewProvider().getVirtualFile();
    PsiDocumentManager pdm = PsiDocumentManager.getInstance(file.getProject());
    PsiFile docFile = pdm.getCachedPsiFile(document);
    LOG.error("Document has changed since fold regions were calculated:\n" +
              "  lengths: " + prevLength + " vs " + document.getTextLength() + "\n" +
              "  document=" + document + "\n" +
              "  file.document=" + (fileDoc == document ? "same" : fileDoc) + "\n" +
              "  document.file=" + (docFile == file ? "same" : docFile) + "\n" +
              "  committed=" + pdm.isCommitted(document) + "\n" +
              "  psiFile=" + file + "\n" +
              "  vFile.length=" + (vFile.isValid() ? vFile.getLength() : -1));
  }

  @NotNull
  private static UpdateFoldRegionsOperation.ApplyDefaultStateMode applyDefaultStateMode(boolean applyDefaultState) {
    return applyDefaultState ? UpdateFoldRegionsOperation.ApplyDefaultStateMode.EXCEPT_CARET_REGION : UpdateFoldRegionsOperation.ApplyDefaultStateMode.NO;
  }

  private static final Key<Object> LAST_UPDATE_INJECTED_STAMP_KEY = Key.create("LAST_UPDATE_INJECTED_STAMP_KEY");
  @Nullable
  public static Runnable updateInjectedFoldRegions(@NotNull final Editor editor, @NotNull final PsiFile file, final boolean applyDefaultState) {
    if (file instanceof PsiCompiledElement) return null;
    ApplicationManager.getApplication().assertReadAccessAllowed();

    final Project project = file.getProject();
    Document document = editor.getDocument();
    LOG.assertTrue(!PsiDocumentManager.getInstance(project).isUncommited(document));
    final FoldingModel foldingModel = editor.getFoldingModel();

    final long timeStamp = document.getModificationStamp();
    Object lastTimeStamp = editor.getUserData(LAST_UPDATE_INJECTED_STAMP_KEY);
    if (lastTimeStamp instanceof Long && ((Long)lastTimeStamp).longValue() == timeStamp) return null;

    List<DocumentWindow> injectedDocuments = InjectedLanguageManager.getInstance(project).getCachedInjectedDocumentsInRange(file, file.getTextRange());
    if (injectedDocuments.isEmpty()) return null;
    final List<EditorWindow> injectedEditors = new ArrayList<>();
    final List<PsiFile> injectedFiles = new ArrayList<>();
    final List<List<RegionInfo>> lists = new ArrayList<>();
    for (final DocumentWindow injectedDocument : injectedDocuments) {
      if (!injectedDocument.isValid()) {
        continue;
      }
      InjectedLanguageUtil.enumerate(injectedDocument, file, (injectedFile, places) -> {
        if (!injectedFile.isValid()) return;
        Editor injectedEditor = InjectedLanguageUtil.getInjectedEditorForInjectedFile(editor, injectedFile);
        if (!(injectedEditor instanceof EditorWindow)) return;

        injectedEditors.add((EditorWindow)injectedEditor);
        injectedFiles.add(injectedFile);
        final List<RegionInfo> map = new ArrayList<>();
        lists.add(map);
        getFoldingsFor(injectedFile, map, false);
      });
    }

    return () -> {
      final ArrayList<Runnable> updateOperations = new ArrayList<>(injectedEditors.size());
      for (int i = 0; i < injectedEditors.size(); i++) {
        EditorWindow injectedEditor = injectedEditors.get(i);
        PsiFile injectedFile = injectedFiles.get(i);
        if (!injectedEditor.getDocument().isValid()) continue;
        List<RegionInfo> list = lists.get(i);
        updateOperations.add(new UpdateFoldRegionsOperation(project, injectedEditor, injectedFile, list,
                                                            applyDefaultStateMode(applyDefaultState), !applyDefaultState, true));
      }
      foldingModel.runBatchFoldingOperation(() -> {
        for (Runnable operation : updateOperations) {
          operation.run();
        }
      });
      EditorFoldingInfo info = EditorFoldingInfo.get(editor);
      for (FoldRegion region : editor.getFoldingModel().getAllFoldRegions()) {
        FoldingRegionWindow injectedRegion = FoldingRegionWindow.getInjectedRegion(region);
        if (injectedRegion != null && !injectedRegion.isValid()) {
          info.removeRegion(region);
        }
      }

      editor.putUserData(LAST_UPDATE_INJECTED_STAMP_KEY, timeStamp);
    };
  }

  /**
   * Checks the ability to initialize folding in the Dumb Mode. Due to language injections it may depend on
   * edited file and active injections (not yet implemented).
   *
   * @param editor the editor that holds file view
   * @return true  if folding initialization available in the Dumb Mode
   */
  public static boolean supportsDumbModeFolding(@NotNull Editor editor) {
    Project project = editor.getProject();
    if (project != null) {
      PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
      if (file != null) {
        return supportsDumbModeFolding(file);
      }
    }
    return true;
  }

  /**
   * Checks the ability to initialize folding in the Dumb Mode for file.
   *
   * @param file the file to test
   * @return true  if folding initialization available in the Dumb Mode
   */
  private static boolean supportsDumbModeFolding(@NotNull PsiFile file) {
    final FileViewProvider viewProvider = file.getViewProvider();
    for (final Language language : viewProvider.getLanguages()) {
      final FoldingBuilder foldingBuilder = LanguageFolding.INSTANCE.forLanguage(language);
      if(foldingBuilder != null && !DumbService.isDumbAware(foldingBuilder))
        return false;
    }
    return true;
  }

  static List<RegionInfo> getFoldingsFor(@NotNull PsiFile file, boolean quick) {
    if (file instanceof PsiCompiledFile) {
      file = ((PsiCompiledFile)file).getDecompiledPsiFile();
    }
    List<RegionInfo> foldingMap = new ArrayList<>();
    getFoldingsFor(file, foldingMap, quick);
    return foldingMap;
  }

  private static void getFoldingsFor(@NotNull PsiFile file,
                                     @NotNull List<? super RegionInfo> elementsToFold,
                                     boolean quick) {
    final FileViewProvider viewProvider = file.getViewProvider();
    Document document = viewProvider.getDocument();
    if (document == null) {
      LOG.error("No document for " + viewProvider);
      return;
    }

    LOG.assertTrue(PsiDocumentManager.getInstance(file.getProject()).isCommitted(document));

    int textLength = document.getTextLength();
    TextRange docRange = TextRange.from(0, textLength);
    Comparator<Language> preferBaseLanguage = Comparator.comparing((Language l) -> l != viewProvider.getBaseLanguage());
    List<Language> languages = ContainerUtil.sorted(viewProvider.getLanguages(), preferBaseLanguage.thenComparing(Language::getID));

    DocumentEx copyDoc = languages.size() > 1 ? new DocumentImpl(document.getImmutableCharSequence()) : null;
    List<RangeMarker> hardRefToRangeMarkers = new ArrayList<>();

    for (Language language : languages) {
      final PsiFile psi = viewProvider.getPsi(language);
      final FoldingBuilder foldingBuilder = LanguageFolding.INSTANCE.forLanguage(language);
      if (psi != null && foldingBuilder != null) {
        if (psi.getTextLength() != textLength) {
          LOG.error(DebugUtil.diagnosePsiDocumentInconsistency(psi, document));
          return;
        }

        for (FoldingDescriptor descriptor : LanguageFolding.buildFoldingDescriptors(foldingBuilder, psi, document, quick)) {
          PsiElement psiElement = descriptor.getElement().getPsi();
          if (psiElement == null) {
            LOG.error("No PSI for folding descriptor " + descriptor);
            continue;
          }
          TextRange range = descriptor.getRange();
          if (!docRange.contains(range)) {
            diagnoseIncorrectRange(psi, document, language, foldingBuilder, descriptor, psiElement);
            continue;
          }

          if (copyDoc != null && !addNonConflictingRegion(copyDoc, range, hardRefToRangeMarkers)) {
            continue;
          }

          RegionInfo regionInfo = new RegionInfo(descriptor, psiElement, foldingBuilder);
          elementsToFold.add(regionInfo);
        }
      }
    }
  }

  private static boolean addNonConflictingRegion(DocumentEx document, TextRange range, List<? super RangeMarker> hardRefToRangeMarkers) {
    int start = range.getStartOffset();
    int end = range.getEndOffset();
    if (!document.processRangeMarkersOverlappingWith(start, end, rm -> !areConflicting(range, TextRange.create(rm)))) {
      return false;
    }
    RangeMarker marker = document.createRangeMarker(start, end);
    hardRefToRangeMarkers.add(marker); //prevent immediate GC
    return true;
  }

  private static boolean areConflicting(TextRange range1, TextRange range2) {
    if (range1.equals(range2)) return true;
    if (range1.contains(range2) || range2.contains(range1)) return false;

    TextRange intersection = range1.intersection(range2);
    return intersection != null && !intersection.isEmpty();
  }

  private static void diagnoseIncorrectRange(@NotNull PsiFile file,
                                             @NotNull Document document,
                                             Language language,
                                             FoldingBuilder foldingBuilder, FoldingDescriptor descriptor, PsiElement psiElement) {
    String message = "Folding descriptor " + descriptor +
                     " made by " + foldingBuilder +
                     " for " + language +
                     " is outside document range" +
                     ", PSI element: " + psiElement +
                     ", PSI element range: " + psiElement.getTextRange() + "; " + DebugUtil.diagnosePsiDocumentInconsistency(psiElement, document);
    LOG.error(message, ApplicationManager.getApplication().isInternal()
                               ? new Attachment[]{AttachmentFactory.createAttachment(document), new Attachment("psiTree.txt", DebugUtil.psiToString(file, false, true))}
                               : Attachment.EMPTY_ARRAY);
  }

  static void clearFoldingCache(@NotNull Editor editor) {
    editor.putUserData(CODE_FOLDING_KEY, null);
    editor.putUserData(LAST_UPDATE_INJECTED_STAMP_KEY, null);
  }

  static class RegionInfo {
    @NotNull
    final FoldingDescriptor descriptor;
    final PsiElement element;
    final String signature;
    final boolean collapsedByDefault;

    private RegionInfo(@NotNull FoldingDescriptor descriptor,
                       @NotNull PsiElement psiElement,
                       @NotNull FoldingBuilder foldingBuilder) {
      this.descriptor = descriptor;
      element = psiElement;
      Boolean hardCoded = descriptor.isCollapsedByDefault();
      collapsedByDefault = hardCoded == null ? FoldingPolicy.isCollapsedByDefault(descriptor, foldingBuilder) : hardCoded;
      signature = createSignature(psiElement);
    }

    private static String createSignature(@NotNull PsiElement element) {
      String signature = FoldingPolicy.getSignature(element);
      if (signature != null && Registry.is("folding.signature.validation")) {
        PsiFile containingFile = element.getContainingFile();
        PsiElement restoredElement = FoldingPolicy.restoreBySignature(containingFile, signature);
        if (!element.equals(restoredElement)) {
          StringBuilder trace = new StringBuilder();
          PsiElement restoredAgain = FoldingPolicy.restoreBySignature(containingFile, signature, trace);
          LOG.error("element: " + element + "(" + element.getText()
                    + "); restoredElement: " + restoredElement
                    + "; signature: '" + signature
                    + "'; file: " + containingFile
                    + "; injected: " + InjectedLanguageManager.getInstance(element.getProject()).isInjectedFragment(containingFile)
                    + "; languages: " + containingFile.getViewProvider().getLanguages()
                    + "; restored again: " + restoredAgain +
                    "; restore produces same results: " + (restoredAgain == restoredElement)
                    + "; trace:\n" + trace);
        }
      }
      return signature;
    }

    @Override
    public String toString() {
      return descriptor + ", collapsedByDefault=" + collapsedByDefault;
    }
  }
}
