// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.psi.impl.source.tree.injected;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.injected.editor.DocumentWindow;
import com.intellij.injected.editor.EditorWindow;
import com.intellij.injected.editor.VirtualFileWindow;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.BooleanRunnable;
import com.intellij.psi.impl.DebugUtil;
import com.intellij.psi.impl.PsiDocumentManagerBase;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.psi.impl.source.DummyHolder;
import com.intellij.psi.injection.ReferenceInjector;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.*;
import com.intellij.reference.SoftReference;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.DeprecatedMethodException;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ConcurrentList;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * @deprecated Use {@link InjectedLanguageManager} instead
 */
@Deprecated
public final class InjectedLanguageUtil {
  public static final Key<IElementType> INJECTED_FRAGMENT_TYPE = Key.create("INJECTED_FRAGMENT_TYPE");
  public static final Key<Boolean> FRANKENSTEIN_INJECTION = InjectedLanguageManager.FRANKENSTEIN_INJECTION;

  @NotNull
  static PsiElement loadTree(@NotNull PsiElement host, @NotNull PsiFile containingFile) {
    if (containingFile instanceof DummyHolder) {
      PsiElement context = containingFile.getContext();
      if (context != null) {
        PsiFile topFile = context.getContainingFile();
        topFile.getNode();  //load tree
        TextRange textRange = host.getTextRange().shiftRight(context.getTextRange().getStartOffset());

        PsiElement inLoadedTree =
          PsiTreeUtil.findElementOfClassAtRange(topFile, textRange.getStartOffset(), textRange.getEndOffset(), host.getClass());
        if (inLoadedTree != null) {
          host = inLoadedTree;
        }
      }
    }
    return host;
  }

  private static final Key<List<TokenInfo>> HIGHLIGHT_TOKENS = Key.create("HIGHLIGHT_TOKENS");
  public static List<TokenInfo> getHighlightTokens(@NotNull PsiFile file) {
    return file.getUserData(HIGHLIGHT_TOKENS);
  }
  public static class TokenInfo {
    @NotNull public final IElementType type;
    @NotNull public final ProperTextRange rangeInsideInjectionHost;
    public final int shredIndex;
    public final TextAttributes attributes;

    public TokenInfo(@NotNull IElementType type,
                     @NotNull ProperTextRange rangeInsideInjectionHost,
                     int shredIndex,
                     @NotNull TextAttributes attributes) {
      this.type = type;
      this.rangeInsideInjectionHost = rangeInsideInjectionHost;
      this.shredIndex = shredIndex;
      this.attributes = attributes;
    }
  }
  static void setHighlightTokens(@NotNull PsiFile file, @NotNull List<TokenInfo> tokens) {
    file.putUserData(HIGHLIGHT_TOKENS, tokens);
  }

  public static Place getShreds(@NotNull PsiFile injectedFile) {
    FileViewProvider viewProvider = injectedFile.getViewProvider();
    return getShreds(viewProvider);
  }

  public static Place getShreds(@NotNull FileViewProvider viewProvider) {
    if (!(viewProvider instanceof InjectedFileViewProvider)) return null;
    InjectedFileViewProvider myFileViewProvider = (InjectedFileViewProvider)viewProvider;
    return getShreds(myFileViewProvider.getDocument());
  }

  @NotNull
  private static Place getShreds(@NotNull DocumentWindow document) {
    return ((DocumentWindowImpl)document).getShreds();
  }

  public static void enumerate(@NotNull DocumentWindow documentWindow,
                               @NotNull PsiFile hostPsiFile,
                               @NotNull PsiLanguageInjectionHost.InjectedPsiVisitor visitor) {
    Segment[] ranges = documentWindow.getHostRanges();
    Segment rangeMarker = ranges.length > 0 ? ranges[0] : null;
    PsiElement element = rangeMarker == null ? null : hostPsiFile.findElementAt(rangeMarker.getStartOffset());
    if (element != null) {
      enumerate(element, hostPsiFile, true, visitor);
    }
  }

  /**
   * @deprecated use {@link InjectedLanguageManager#enumerate(PsiElement, PsiLanguageInjectionHost.InjectedPsiVisitor)} instead
   */
  @Deprecated
  public static boolean enumerate(@NotNull PsiElement host, @NotNull PsiLanguageInjectionHost.InjectedPsiVisitor visitor) {
    PsiFile containingFile = host.getContainingFile();
    PsiUtilCore.ensureValid(containingFile);
    return enumerate(host, containingFile, true, visitor);
  }

  /**
   * @deprecated use {@link InjectedLanguageManager#enumerateEx(PsiElement, PsiFile, boolean, PsiLanguageInjectionHost.InjectedPsiVisitor)} instead
   */
  @Deprecated
  public static boolean enumerate(@NotNull PsiElement host,
                                  @NotNull PsiFile containingFile,
                                  boolean probeUp,
                                  @NotNull PsiLanguageInjectionHost.InjectedPsiVisitor visitor) {
    //do not inject into nonphysical files except during completion
    if (!containingFile.isPhysical() && containingFile.getOriginalFile() == containingFile) {
      final PsiElement context = InjectedLanguageManager.getInstance(containingFile.getProject()).getInjectionHost(containingFile);
      if (context == null) return false;

      final PsiFile file = context.getContainingFile();
      if (file == null || !file.isPhysical() && file.getOriginalFile() == file) return false;
    }

    if (containingFile.getViewProvider() instanceof InjectedFileViewProvider) return false; // no injection inside injection

    PsiElement inTree = loadTree(host, containingFile);
    if (inTree != host) {
      host = inTree;
      containingFile = host.getContainingFile();
    }
    PsiDocumentManager documentManager = PsiDocumentManager.getInstance(containingFile.getProject());
    Document document = documentManager.getDocument(containingFile);
    if (document == null || documentManager.isCommitted(document)) {
      probeElementsUp(host, containingFile, probeUp, visitor);
    }
    return true;
  }

  /**
   * Invocation of this method on uncommitted {@code file} can lead to unexpected results, including throwing an exception!
   */
  @Contract("null,_->null;!null,_->!null")
  public static Editor getEditorForInjectedLanguageNoCommit(@Nullable Editor editor, @Nullable PsiFile file) {
    if (editor == null || file == null || editor instanceof EditorWindow) return editor;

    int offset = editor.getCaretModel().getOffset();
    return getEditorForInjectedLanguageNoCommit(editor, file, offset);
  }

  /**
   * This is a quick check, that can be performed before committing document and invoking
   * {@link #getEditorForInjectedLanguageNoCommit(Editor, Caret, PsiFile)} or other methods here, which don't work
   * for uncommitted documents.
   */
  static boolean mightHaveInjectedFragmentAtCaret(@NotNull Project project, @NotNull Document hostDocument, int hostOffset) {
    PsiFile hostPsiFile = PsiDocumentManager.getInstance(project).getCachedPsiFile(hostDocument);
    if (hostPsiFile == null || !hostPsiFile.isValid()) return false;
    List<DocumentWindow> documents = InjectedLanguageManager.getInstance(project).getCachedInjectedDocumentsInRange(hostPsiFile, TextRange.create(hostOffset, hostOffset));
    for (DocumentWindow document : documents) {
      if (document.isValid() && document.getHostRange(hostOffset) != null) return true;
    }
    return false;
  }

  /**
   * Invocation of this method on uncommitted {@code file} can lead to unexpected results, including throwing an exception!
   */
  public static Editor getEditorForInjectedLanguageNoCommit(@Nullable Editor editor, @Nullable Caret caret, @Nullable PsiFile file) {
    if (editor == null || file == null || editor instanceof EditorWindow || caret == null) return editor;

    PsiFile injectedFile = findInjectedPsiNoCommit(file, caret.getOffset());
    return getInjectedEditorForInjectedFile(editor, caret, injectedFile);
  }

  /**
   * Invocation of this method on uncommitted {@code file} can lead to unexpected results, including throwing an exception!
   */
  public static Caret getCaretForInjectedLanguageNoCommit(@Nullable Caret caret, @Nullable PsiFile file) {
    if (caret == null || file == null || caret instanceof InjectedCaret) return caret;

    PsiFile injectedFile = findInjectedPsiNoCommit(file, caret.getOffset());
    Editor injectedEditor = getInjectedEditorForInjectedFile(caret.getEditor(), injectedFile);
    if (!(injectedEditor instanceof EditorWindow)) {
      return caret;
    }
    for (Caret injectedCaret : injectedEditor.getCaretModel().getAllCarets()) {
      if (((InjectedCaret)injectedCaret).getDelegate() == caret) {
        return injectedCaret;
      }
    }
    return null;
  }

  /**
   * Finds injected language in expression
   *
   * @param expression  where to find
   * @param classToFind class that represents language we look for
   * @param <T>         class that represents language we look for
   * @return instance of class that represents language we look for or null of not found
   */
  @Nullable
  @SuppressWarnings("unchecked") // We check types dynamically (using isAssignableFrom)
  public static <T extends PsiFileBase> T findInjectedFile(@NotNull final PsiElement expression,
                                                           @NotNull final Class<T> classToFind) {
    final List<Pair<PsiElement, TextRange>> files =
      InjectedLanguageManager.getInstance(expression.getProject()).getInjectedPsiFiles(expression);
    if (files == null) {
      return null;
    }
    for (final Pair<PsiElement, TextRange> fileInfo : files) {
      final PsiElement injectedFile = fileInfo.first;
      if (classToFind.isAssignableFrom(injectedFile.getClass())) {
        return (T)injectedFile;
      }
    }
    return null;
  }

  /**
   * Invocation of this method on uncommitted {@code file} can lead to unexpected results, including throwing an exception!
   */
  @Contract("null,_,_->null;!null,_,_->!null")
  public static Editor getEditorForInjectedLanguageNoCommit(@Nullable Editor editor, @Nullable PsiFile file, final int offset) {
    if (editor == null || file == null || editor instanceof EditorWindow) return editor;
    PsiFile injectedFile = findInjectedPsiNoCommit(file, offset);
    return getInjectedEditorForInjectedFile(editor, injectedFile);
  }

  @NotNull
  public static Editor getInjectedEditorForInjectedFile(@NotNull Editor hostEditor, @Nullable final PsiFile injectedFile) {
    return getInjectedEditorForInjectedFile(hostEditor, hostEditor.getCaretModel().getCurrentCaret(), injectedFile);
  }

  /**
   * @param hostCaret if not {@code null}, take into account caret's selection (in case it's not contained completely in injected fragment,
   *                  return host editor)
   */
  @NotNull
  public static Editor getInjectedEditorForInjectedFile(@NotNull Editor hostEditor,
                                                        @Nullable Caret hostCaret,
                                                        @Nullable final PsiFile injectedFile) {
    if (injectedFile == null || hostEditor instanceof EditorWindow || hostEditor.isDisposed()) return hostEditor;
    Project project = hostEditor.getProject();
    if (project == null) project = injectedFile.getProject();
    Document document = PsiDocumentManager.getInstance(project).getDocument(injectedFile);
    if (!(document instanceof DocumentWindowImpl)) return hostEditor;
    DocumentWindowImpl documentWindow = (DocumentWindowImpl)document;
    if (hostCaret != null && hostCaret.hasSelection()) {
      int selstart = hostCaret.getSelectionStart();
      if (selstart != -1) {
        int selend = Math.max(selstart, hostCaret.getSelectionEnd());
        if (!documentWindow.containsRange(selstart, selend)) {
          // selection spreads out the injected editor range
          return hostEditor;
        }
      }
    }
    if (!documentWindow.isValid()) {
      return hostEditor; // since the moment we got hold of injectedFile and this moment call, document may have been dirtied
    }
    return EditorWindowImpl.create(documentWindow, (EditorImpl)hostEditor, injectedFile);
  }

  /**
   * Invocation of this method on uncommitted {@code host} can lead to unexpected results, including throwing an exception!
   */
  @Nullable
  public static PsiFile findInjectedPsiNoCommit(@NotNull PsiFile host, int offset) {
    PsiElement injected = InjectedLanguageManager.getInstance(host.getProject()).findInjectedElementAt(host, offset);
    return injected == null ? null : injected.getContainingFile();
  }

  /**
   * Invocation of this method on uncommitted {@code file} can lead to unexpected results, including throwing an exception!
   */
  // consider injected elements
  public static PsiElement findElementAtNoCommit(@NotNull PsiFile file, int offset) {
    FileViewProvider viewProvider = file.getViewProvider();
    Trinity<PsiElement, PsiElement, Language> result = null;
    if (!(viewProvider instanceof InjectedFileViewProvider)) {
      PsiDocumentManager documentManager = PsiDocumentManager.getInstance(file.getProject());
      result = tryOffset(file, offset, documentManager);
      PsiElement injected = result.first;
      if (injected != null) {
        return injected;
      }
    }
    Language baseLanguage = viewProvider.getBaseLanguage();
    if (result != null && baseLanguage == result.third) {
      return result.second; // already queried
    }
    return viewProvider.findElementAt(offset, baseLanguage);
  }

  // list of injected fragments injected into this psi element (can be several if some crazy injector calls startInjecting()/doneInjecting()/startInjecting()/doneInjecting())
  private static final Key<Getter<InjectionResult>> INJECTED_PSI = Key.create("INJECTED_PSI");

  private static void probeElementsUp(@NotNull PsiElement element,
                                      @NotNull PsiFile hostPsiFile,
                                      boolean probeUp,
                                      @NotNull PsiLanguageInjectionHost.InjectedPsiVisitor visitor) {
    element = skipNonInjectablePsi(element, probeUp);
    if (element == null) return;

    InjectedLanguageManagerImpl injectedManager = InjectedLanguageManagerImpl.getInstanceImpl(hostPsiFile.getProject());
    InjectionResult result = null;
    PsiElement current;

    for (current = element; current != null && current != hostPsiFile && !(current instanceof PsiDirectory); ) {
      ProgressManager.checkCanceled();
      if ("EL".equals(current.getLanguage().getID())) break;
      result = SoftReference.deref(current.getUserData(INJECTED_PSI));
      if (result == null || !result.isModCountUpToDate() || !result.isValid()) {
        result = injectedManager.processInPlaceInjectorsFor(hostPsiFile, current);
        preventResultFromGCWhileInjectedPsiIsReachable(result);
      }

      current = current.getParent();

      if (result != null) {
        if (result.files != null) {
          for (PsiFile injectedPsiFile : result.files) {
            Place place = getShreds(injectedPsiFile);
            if (place.isValid()) {
              // check that injections found intersect with queried element
              boolean intersects = intersects(element, place);
              if (intersects) {
                visitor.visit(injectedPsiFile, place);
              }
            }
          }
        }
        if (result.references != null && visitor instanceof InjectedReferenceVisitor) {
          InjectedReferenceVisitor refVisitor = (InjectedReferenceVisitor)visitor;
          for (Pair<ReferenceInjector, Place> pair : result.references) {
            Place place = pair.getSecond();
            if (place.isValid()) {
              // check that injections found intersect with queried element
              boolean intersects = intersects(element, place);
              if (intersects) {
                ReferenceInjector injector = pair.getFirst();
                refVisitor.visitInjectedReference(injector, place);
              }
            }
          }
        }
        break; // found injection, stop
      }
      if (!probeUp) {
        break;
      }
    }

    if (element != current && (probeUp || result != null)) {
      cacheResults(element, current, hostPsiFile, result);
    }
  }

  private static void cacheResults(@NotNull PsiElement from, @Nullable PsiElement upUntil, @NotNull PsiFile hostFile, @Nullable InjectionResult result) {
    Getter<InjectionResult> cachedRef = result == null || result.isEmpty() ? getEmptyInjectionResult(hostFile) : new SoftReference<>(result);
    for (PsiElement e = from; e != upUntil && e != null; e = e.getParent()) {
      ProgressManager.checkCanceled();
      e.putUserData(INJECTED_PSI, cachedRef);
    }
  }

  private static final Key<InjectionResult> INJECTION_HOLDER_BACK_REFERENCE = Key.create("INJECTION_HOLDER_BACK_REFERENCE");

  /**
   * Prevents InjectionResult from being GC-ed while there are references to the PSI inside,
   * to avoid new injected PSI being created when there's one alive already.
   */
  private static void preventResultFromGCWhileInjectedPsiIsReachable(@Nullable InjectionResult result) {
    if (result != null && result.files != null) {
      for (PsiFile injectedPsiFile : result.files) {
        injectedPsiFile.getViewProvider().putUserData(INJECTION_HOLDER_BACK_REFERENCE, result);
      }
    }
  }

  @NotNull
  private static InjectionResult getEmptyInjectionResult(@NotNull PsiFile host) {
    return CachedValuesManager.getCachedValue(host, () ->
      CachedValueProvider.Result.createSingleDependency(new InjectionResult(host, null, null),
                                                        PsiModificationTracker.MODIFICATION_COUNT));
  }

  /**
   * We can only inject into injection hosts or their ancestors, so if we're sure there are no PsiLanguageInjectionHost descendants,
   * we can skip that PSI safely.
   */
  @Nullable
  private static PsiElement skipNonInjectablePsi(@NotNull PsiElement element, boolean probeUp) {
    if (!stopLookingForInjection(element) && element.getFirstChild() == null) {
      if (!probeUp) return null;

      element = element.getParent();
      while (element != null && !stopLookingForInjection(element) && element.getFirstChild() == element.getLastChild()) {
        element = element.getParent();
      }
    }
    return element;
  }

  private static boolean stopLookingForInjection(@NotNull PsiElement element) {
    return element instanceof PsiFileSystemItem || element instanceof PsiLanguageInjectionHost;
  }

  private static boolean intersects(@NotNull PsiElement hostElement, @NotNull Place place) {
    TextRange hostElementRange = hostElement.getTextRange();
    boolean intersects = false;
    for (PsiLanguageInjectionHost.Shred shred : place) {
      PsiLanguageInjectionHost shredHost = shred.getHost();
      if (shredHost != null && shredHost.getTextRange().intersects(hostElementRange)) {
        intersects = true;
        break;
      }
    }
    return intersects;
  }

  /**
   * Invocation of this method on uncommitted {@code hostFile} can lead to unexpected results, including throwing an exception!
   */
  static PsiElement findInjectedElementNoCommit(@NotNull PsiFile hostFile, final int offset) {
    if (hostFile instanceof PsiCompiledElement) return null;
    Project project = hostFile.getProject();
    if (InjectedLanguageManager.getInstance(project).isInjectedFragment(hostFile)) return null;
    final PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
    Trinity<PsiElement, PsiElement, Language> result = tryOffset(hostFile, offset, documentManager);
    return result.first;
  }

  // returns (injected psi, leaf element at the offset, language of the leaf element)
  // since findElementAt() is expensive, we trying to reuse its result
  @NotNull
  private static Trinity<PsiElement, PsiElement, Language> tryOffset(@NotNull PsiFile hostFile,
                                                                     final int offset,
                                                                     @NotNull PsiDocumentManager documentManager) {
    FileViewProvider provider = hostFile.getViewProvider();
    Language leafLanguage = null;
    PsiElement leafElement = null;
    for (Language language : provider.getLanguages()) {
      PsiElement element = provider.findElementAt(offset, language);
      if (element != null) {
        if (leafLanguage == null) {
          leafLanguage = language;
          leafElement = element;
        }
        PsiElement injected = findInside(element, hostFile, offset, documentManager);
        if (injected != null) return Trinity.create(injected, element, language);
      }
      // maybe we are at the border between two psi elements, then try to find injection at the end of the left element
      if (offset != 0 && (element == null || element.getTextRange().getStartOffset() == offset)) {
        PsiElement leftElement = provider.findElementAt(offset - 1, language);
        if (leftElement != null && leftElement.getTextRange().getEndOffset() == offset) {
          PsiElement injected = findInside(leftElement, hostFile, offset, documentManager);
          if (injected != null) return Trinity.create(injected, element, language);
        }
      }
    }

    return Trinity.create(null, leafElement, leafLanguage);
  }

  private static PsiElement findInside(@NotNull PsiElement element,
                                       @NotNull PsiFile hostFile,
                                       final int hostOffset,
                                       @NotNull final PsiDocumentManager documentManager) {
    final Ref<PsiElement> out = new Ref<>();
    enumerate(element, hostFile, true, (injectedPsi, places) -> {
      for (PsiLanguageInjectionHost.Shred place : places) {
        TextRange hostRange = place.getHost().getTextRange();
        if (hostRange.cutOut(place.getRangeInsideHost()).grown(1).contains(hostOffset)) {
          DocumentWindowImpl document = (DocumentWindowImpl)documentManager.getCachedDocument(injectedPsi);
          if (document == null) return;
          int injectedOffset = document.hostToInjected(hostOffset);
          PsiElement injElement = injectedPsi.findElementAt(injectedOffset);
          out.set(injElement == null ? injectedPsi : injElement);
        }
      }
    });
    return out.get();
  }

  private static final Key<List<DocumentWindow>> INJECTED_DOCS_KEY = Key.create("INJECTED_DOCS_KEY");

  /**
   * @deprecated use {@link InjectedLanguageManager#getCachedInjectedDocumentsInRange(PsiFile, TextRange)} instead
   */
  @NotNull
  @Deprecated
  public static ConcurrentList<DocumentWindow> getCachedInjectedDocuments(@NotNull PsiFile hostPsiFile) {
    // modification of cachedInjectedDocuments must be under InjectedLanguageManagerImpl.ourInjectionPsiLock only
    List<DocumentWindow> injected = hostPsiFile.getUserData(INJECTED_DOCS_KEY);
    if (injected == null) {
      injected = ((UserDataHolderEx)hostPsiFile).putUserDataIfAbsent(INJECTED_DOCS_KEY, ContainerUtil.createConcurrentList());
    }
    return (ConcurrentList<DocumentWindow>)injected;
  }

  @NotNull
  static List<DocumentWindow> getCachedInjectedDocumentsInRange(@NotNull PsiFile hostPsiFile, @NotNull TextRange range) {
    List<DocumentWindow> injected = getCachedInjectedDocuments(hostPsiFile);

    return ContainerUtil.filter(injected, inj-> Arrays.stream(inj.getHostRanges()).anyMatch(range::intersects));
  }

  static void clearCachedInjectedFragmentsForFile(@NotNull PsiFile file) {
    file.putUserData(INJECTED_DOCS_KEY, null);
  }

  static void clearCaches(@NotNull PsiFile injected, @NotNull DocumentWindowImpl documentWindow) {
    VirtualFileWindowImpl virtualFile = (VirtualFileWindowImpl)injected.getVirtualFile();
    PsiManagerEx psiManagerEx = (PsiManagerEx)injected.getManager();
    if (psiManagerEx.getProject().isDisposed()) return;

    DebugUtil.performPsiModification("injected clearCaches", () ->
      psiManagerEx.getFileManager().setViewProvider(virtualFile, null));

    VirtualFile delegate = virtualFile.getDelegate();
    if (!delegate.isValid()) return;

    FileViewProvider viewProvider = psiManagerEx.getFileManager().findCachedViewProvider(delegate);
    if (viewProvider == null) return;

    for (PsiFile hostFile : ((AbstractFileViewProvider)viewProvider).getCachedPsiFiles()) {
      // modification of cachedInjectedDocuments must be under InjectedLanguageManagerImpl.ourInjectionPsiLock
      synchronized (InjectedLanguageManagerImpl.ourInjectionPsiLock) {
        List<DocumentWindow> cachedInjectedDocuments = getCachedInjectedDocuments(hostFile);
        for (int i = cachedInjectedDocuments.size() - 1; i >= 0; i--) {
          DocumentWindow cachedInjectedDocument = cachedInjectedDocuments.get(i);
          if (cachedInjectedDocument == documentWindow) {
            cachedInjectedDocuments.remove(i);
          }
        }
      }
    }
  }


  public static Editor openEditorFor(@NotNull PsiFile file, @NotNull Project project) {
    Document document = PsiDocumentManager.getInstance(project).getDocument(file);
    // may return editor injected in current selection in the host editor, not for the file passed as argument
    VirtualFile virtualFile = file.getVirtualFile();
    if (virtualFile == null) {
      return null;
    }
    if (virtualFile instanceof VirtualFileWindow) {
      virtualFile = ((VirtualFileWindow)virtualFile).getDelegate();
    }
    Editor editor = FileEditorManager.getInstance(project).openTextEditor(new OpenFileDescriptor(project, virtualFile, -1), false);
    if (editor == null || editor instanceof EditorWindow || editor.isDisposed()) return editor;
    if (document instanceof DocumentWindowImpl) {
      return EditorWindowImpl.create((DocumentWindowImpl)document, (EditorImpl)editor, file);
    }
    return editor;
  }

  /**
   * @deprecated use {@link InjectedLanguageManager#getTopLevelFile(PsiElement)} instead
   */
  @Deprecated
  public static PsiFile getTopLevelFile(@NotNull PsiElement element) {
    PsiFile containingFile = element.getContainingFile();
    if (containingFile == null) return null;
    if (containingFile.getViewProvider() instanceof InjectedFileViewProvider) {
      PsiElement host = InjectedLanguageManager.getInstance(containingFile.getProject()).getInjectionHost(containingFile);
      if (host != null) containingFile = host.getContainingFile();
    }
    return containingFile;
  }

  @NotNull
  public static Editor getTopLevelEditor(@NotNull Editor editor) {
    return editor instanceof EditorWindow ? ((EditorWindow)editor).getDelegate() : editor;
  }

  public static boolean isInInjectedLanguagePrefixSuffix(@NotNull final PsiElement element) {
    PsiFile injectedFile = element.getContainingFile();
    if (injectedFile == null) return false;
    Project project = injectedFile.getProject();
    InjectedLanguageManager languageManager = InjectedLanguageManager.getInstance(project);
    if (!languageManager.isInjectedFragment(injectedFile)) return false;
    TextRange elementRange = element.getTextRange();
    List<TextRange> edibles = languageManager.intersectWithAllEditableFragments(injectedFile, elementRange);
    int combinedEdiblesLength = edibles.stream().mapToInt(TextRange::getLength).sum();

    return combinedEdiblesLength != elementRange.getLength();
  }


  public static int hostToInjectedUnescaped(DocumentWindow window, int hostOffset) {
    Place shreds = ((DocumentWindowImpl)window).getShreds();
    Segment hostRangeMarker = shreds.get(0).getHostRangeMarker();
    if (hostRangeMarker == null || hostOffset < hostRangeMarker.getStartOffset()) {
      return shreds.get(0).getPrefix().length();
    }
    StringBuilder chars = new StringBuilder();
    int unescaped = 0;
    for (int i = 0; i < shreds.size(); i++, chars.setLength(0)) {
      PsiLanguageInjectionHost.Shred shred = shreds.get(i);
      int prefixLength = shred.getPrefix().length();
      int suffixLength = shred.getSuffix().length();
      PsiLanguageInjectionHost host = shred.getHost();
      TextRange rangeInsideHost = shred.getRangeInsideHost();
      LiteralTextEscaper<? extends PsiLanguageInjectionHost> escaper = host == null ? null : host.createLiteralTextEscaper();
      unescaped += prefixLength;
      Segment currentRange = shred.getHostRangeMarker();
      if (currentRange == null) continue;
      Segment nextRange = i == shreds.size() - 1 ? null : shreds.get(i + 1).getHostRangeMarker();
      if (nextRange == null || hostOffset < nextRange.getStartOffset()) {
        hostOffset = Math.min(hostOffset, currentRange.getEndOffset());
        int inHost = hostOffset - currentRange.getStartOffset();
        if (escaper != null && escaper.decode(rangeInsideHost, chars)) {
          int found = ObjectUtils.binarySearch(
            0, inHost, index -> Comparing.compare(escaper.getOffsetInHost(index, TextRange.create(0, host.getTextLength())), inHost));
          return unescaped + (found >= 0 ? found : -found - 1);
        }
        return unescaped + inHost;
      }
      if (escaper != null && escaper.decode(rangeInsideHost, chars)) {
        unescaped += chars.length();
      }
      else {
        unescaped += currentRange.getEndOffset() - currentRange.getStartOffset();
      }
      unescaped += suffixLength;
    }
    return unescaped - shreds.get(shreds.size() - 1).getSuffix().length();
  }

  /**
   * @deprecated Use {@link InjectedLanguageManager#getInjectedPsiFiles(PsiElement)} != null instead
   */
  @Deprecated
  public static boolean hasInjections(@NotNull PsiLanguageInjectionHost host) {
    if (!host.isPhysical()) return false;
    final Ref<Boolean> result = Ref.create(false);
    enumerate(host, (injectedPsi, places) -> result.set(true));
    return result.get().booleanValue();
  }

  public static String getUnescapedText(@NotNull PsiFile file, @Nullable final PsiElement startElement, @Nullable final PsiElement endElement) {
    final InjectedLanguageManager manager = InjectedLanguageManager.getInstance(file.getProject());
    if (manager.getInjectionHost(file) == null) {
      return file.getText().substring(startElement == null ? 0 : startElement.getTextRange().getStartOffset(),
                                      endElement == null ? file.getTextLength() : endElement.getTextRange().getStartOffset());
    }
    final StringBuilder sb = new StringBuilder();
    file.accept(new PsiRecursiveElementWalkingVisitor() {

      Boolean myState = startElement == null ? Boolean.TRUE : null;

      @Override
      public void visitElement(@NotNull PsiElement element) {
        if (element == startElement) myState = Boolean.TRUE;
        if (element == endElement) myState = Boolean.FALSE;
        if (Boolean.FALSE == myState) return;
        if (Boolean.TRUE == myState && element.getFirstChild() == null) {
          sb.append(getUnescapedLeafText(element, false));
        }
        else {
          super.visitElement(element);
        }
      }
    });
    return sb.toString();
  }

  @Nullable
  public static String getUnescapedLeafText(PsiElement element, boolean strict) {
    String unescaped = element.getCopyableUserData(LeafPatcher.UNESCAPED_TEXT);
    if (unescaped != null) {
      return unescaped;
    }
    if (!strict && element.getFirstChild() == null) {
      return element.getText();
    }
    return null;
  }

  @Nullable
  public static DocumentWindow getDocumentWindow(@NotNull PsiElement element) {
    PsiFile file = element.getContainingFile();
    if (file == null) return null;
    VirtualFile virtualFile = file.getVirtualFile();
    if (virtualFile instanceof VirtualFileWindow) return ((VirtualFileWindow)virtualFile).getDocumentWindow();
    return null;
  }

  public static boolean isHighlightInjectionBackground(@Nullable PsiLanguageInjectionHost host) {
    return !(host instanceof InjectionBackgroundSuppressor);
  }

  public static int getInjectedStart(@NotNull List<? extends PsiLanguageInjectionHost.Shred> places) {
    PsiLanguageInjectionHost.Shred shred = places.get(0);
    PsiLanguageInjectionHost host = shred.getHost();
    assert host != null;
    return shred.getRangeInsideHost().getStartOffset() + host.getTextRange().getStartOffset();
  }

  @Nullable
  public static PsiElement findElementInInjected(@NotNull PsiLanguageInjectionHost injectionHost, final int offset) {
    final Ref<PsiElement> ref = Ref.create();
    enumerate(injectionHost, (injectedPsi, places) -> ref.set(injectedPsi.findElementAt(offset - getInjectedStart(places))));
    return ref.get();
  }

  @Nullable
  public static PsiLanguageInjectionHost findInjectionHost(@Nullable PsiElement psi) {
    if (psi == null) return null;
    PsiFile containingFile = psi.getContainingFile().getOriginalFile();              // * formatting
    PsiElement fileContext = containingFile.getContext();                            // * quick-edit-handler
    if (fileContext instanceof PsiLanguageInjectionHost) return (PsiLanguageInjectionHost)fileContext;
    Place shreds = getShreds(containingFile.getViewProvider()); // * injection-registrar
    if (shreds == null) {
      VirtualFile virtualFile = PsiUtilCore.getVirtualFile(containingFile);
      if (virtualFile instanceof LightVirtualFile) {
        virtualFile = ((LightVirtualFile)virtualFile).getOriginalFile();             // * dynamic files-from-text
      }
      if (virtualFile instanceof VirtualFileWindow) {
        shreds = getShreds(((VirtualFileWindow)virtualFile).getDocumentWindow());
      }
    }
    return shreds != null ? shreds.getHostPointer().getElement() : null;
  }

  @Nullable
  public static PsiLanguageInjectionHost findInjectionHost(@Nullable VirtualFile virtualFile) {
    return virtualFile instanceof VirtualFileWindow ?
           getShreds(((VirtualFileWindow)virtualFile).getDocumentWindow()).getHostPointer().getElement() : null;
  }

  public static <T> void putInjectedFileUserData(@NotNull PsiElement element,
                                                 @NotNull Language language,
                                                 @NotNull Key<T> key,
                                                 @Nullable T value) {
    PsiFile file = getCachedInjectedFileWithLanguage(element, language);
    if (file != null) {
      file.putUserData(key, value);
    }
  }

  /**
   * @deprecated use {@link #putInjectedFileUserData(PsiElement, Language, Key, Object)} instead
   */
  @Deprecated
  public static <T> void putInjectedFileUserData(MultiHostRegistrar registrar, Key<T> key, T value) {
    DeprecatedMethodException.report("use putInjectedFileUserData(PsiElement, Language, Key, Object)} instead");
    InjectionResult result = ((InjectionRegistrarImpl)registrar).getInjectedResult();
    if (result != null && result.files != null) {
      List<? extends PsiFile> files = result.files;
      PsiFile file = files.get(files.size() - 1);
      file.putUserData(key, value);
    }
  }

  @Nullable
  public static PsiFile getCachedInjectedFileWithLanguage(@NotNull PsiElement element, @NotNull Language language) {
    if (!element.isValid()) return null;
    PsiFile containingFile = element.getContainingFile();
    if (containingFile == null || !containingFile.isValid()) return null;
    return InjectedLanguageManager.getInstance(containingFile.getProject())
      .getCachedInjectedDocumentsInRange(containingFile, element.getTextRange())
      .stream()
      .map(documentWindow -> PsiDocumentManager.getInstance(containingFile.getProject()).getPsiFile(documentWindow))
      .filter(file -> file != null && file.getLanguage() == LanguageSubstitutors.getInstance()
        .substituteLanguage(language, file.getVirtualFile(), file.getProject()))
      .max(Comparator.comparingInt(PsiElement::getTextLength))
      .orElse(null);
  }

  /**
   * Start injecting the reference in {@code language} in this place.
   * Unlike {@link MultiHostRegistrar#startInjecting(Language)} this method doesn't inject the full blown file in the other language.
   * Instead, it just marks some range as a reference in some language.
   * For example, you can inject file reference into string literal.
   * After that, it won't be highlighted as an injected fragment but still can be subject to e.g. "Goto declaraion" action.
   */
  public static void injectReference(@NotNull MultiHostRegistrar registrar,
                                     @NotNull Language language,
                                     @NotNull String prefix,
                                     @NotNull String suffix,
                                     @NotNull PsiLanguageInjectionHost host,
                                     @NotNull TextRange rangeInsideHost) {
    ((InjectionRegistrarImpl)registrar).injectReference(language, prefix, suffix, host, rangeInsideHost);
  }

  // null means failed to reparse
  public static BooleanRunnable reparse(@NotNull PsiFile injectedPsiFile,
                                        @NotNull DocumentWindow injectedDocument,
                                        @NotNull PsiFile hostPsiFile,
                                        @NotNull Document hostDocument,
                                        @NotNull FileViewProvider hostViewProvider,
                                        @NotNull ProgressIndicator indicator,
                                        @NotNull ASTNode oldRoot,
                                        @NotNull ASTNode newRoot,
                                        @NotNull PsiDocumentManagerBase documentManager) {
    Language language = injectedPsiFile.getLanguage();
    InjectedFileViewProvider provider = (InjectedFileViewProvider)injectedPsiFile.getViewProvider();
    VirtualFile oldInjectedVFile = provider.getVirtualFile();
    VirtualFile hostVirtualFile = hostViewProvider.getVirtualFile();
    BooleanRunnable runnable = InjectionRegistrarImpl
      .reparse(language, (DocumentWindowImpl)injectedDocument, injectedPsiFile, (VirtualFileWindow)oldInjectedVFile, hostVirtualFile, hostPsiFile,
               (DocumentEx)hostDocument,
               indicator, oldRoot, newRoot, documentManager);
    if (runnable == null) {
      EditorWindowImpl.disposeEditorFor(injectedDocument);
    }
    return runnable;
  }
}
