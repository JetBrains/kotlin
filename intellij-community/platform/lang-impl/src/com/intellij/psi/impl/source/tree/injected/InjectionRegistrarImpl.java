// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.psi.impl.source.tree.injected;

import com.intellij.codeInsight.daemon.impl.DaemonProgressIndicator;
import com.intellij.injected.editor.DocumentWindow;
import com.intellij.injected.editor.VirtualFileWindow;
import com.intellij.lang.*;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.impl.FileDocumentManagerImpl;
import com.intellij.openapi.fileTypes.EditorHighlighterProvider;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeEditorHighlighterProviders;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.*;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.*;
import com.intellij.psi.impl.smartPointers.Identikit;
import com.intellij.psi.impl.smartPointers.SelfElementInfo;
import com.intellij.psi.impl.smartPointers.SmartPointerManagerImpl;
import com.intellij.psi.impl.source.PsiFileImpl;
import com.intellij.psi.impl.source.resolve.FileContextUtil;
import com.intellij.psi.impl.source.tree.FileElement;
import com.intellij.psi.impl.source.tree.TreeUtil;
import com.intellij.psi.injection.ReferenceInjector;
import com.intellij.psi.templateLanguages.TemplateLanguageFileViewProvider;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.ObjectUtils;
import com.intellij.util.PathUtil;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@SuppressWarnings("deprecation")
class InjectionRegistrarImpl extends MultiHostRegistrarImpl implements MultiHostRegistrar {
  private final PsiDocumentManagerBase myDocumentManagerBase;
  private List<PsiFile> resultFiles;
  private List<Pair<ReferenceInjector, Place>> resultReferences;
  private Language myLanguage;
  private List<PlaceInfo> placeInfos;
  private boolean cleared = true;
  private String fileExtension;
  private final Project myProject;
  private final DocumentEx myHostDocument;
  private final VirtualFile myHostVirtualFile;
  private final PsiElement myContextElement;
  private final PsiFile myHostPsiFile;
  private Thread currentThread;

  InjectionRegistrarImpl(@NotNull Project project,
                         @NotNull PsiFile hostPsiFile,
                         @NotNull PsiElement contextElement,
                         @NotNull PsiDocumentManager docManager) {
    myProject = project;
    myContextElement = contextElement;
    myHostPsiFile = PsiUtilCore.getTemplateLanguageFile(hostPsiFile);
    FileViewProvider viewProvider = myHostPsiFile.getViewProvider();
    if (viewProvider instanceof InjectedFileViewProvider) throw new IllegalArgumentException(viewProvider +" must not be injected");
    myHostVirtualFile = viewProvider.getVirtualFile();
    myDocumentManagerBase = (PsiDocumentManagerBase)docManager;
    myHostDocument = (DocumentEx)viewProvider.getDocument();
  }

  @Nullable
  InjectionResult getInjectedResult() {
    return resultFiles == null && resultReferences == null ? null : new InjectionResult(myHostPsiFile, resultFiles, resultReferences);
  }

  @NotNull
  @Override
  public MultiHostRegistrar startInjecting(@NotNull Language language) {
    return startInjecting(language, null);
  }

  @NotNull
  @Override
  public MultiHostRegistrar startInjecting(@NotNull Language language, @Nullable String extension) {
    fileExtension = extension;
    placeInfos = new SmartList<>();

    if (!cleared) {
      clear();
      throw new IllegalStateException("Seems you haven't called doneInjecting()");
    }
    currentThread = Thread.currentThread();

    if (LanguageParserDefinitions.INSTANCE.forLanguage(language) == null) {
      throw new UnsupportedOperationException("Cannot inject language '" + language + "' because it has no ParserDefinition");
    }
    myLanguage = language;
    return this;
  }

  private void clear() {
    fileExtension = null;
    myLanguage = null;

    cleared = true;
    placeInfos = null;
    currentThread = null;
  }

  @Override
  @NotNull
  public MultiHostRegistrar addPlace(@NonNls @Nullable String prefix,
                                     @NonNls @Nullable String suffix,
                                     @NotNull PsiLanguageInjectionHost host,
                                     @NotNull TextRange rangeInsideHost) {
    checkThreading();
    if (myLanguage == null) {
      clear();
      throw new IllegalStateException("Seems you haven't called startInjecting()");
    }
    if (!host.isValidHost()) {
      throw new IllegalArgumentException(host + ".isValidHost() in " + host.getClass() + " returned false so you mustn't inject here.");
    }
    PsiFile containingFile = PsiUtilCore.getTemplateLanguageFile(host);
    assert containingFile == myHostPsiFile : exceptionContext("Trying to inject into foreign file: "+containingFile, myLanguage,
                                                              myHostPsiFile, myHostVirtualFile, myHostDocument, placeInfos, myDocumentManagerBase);
    TextRange hostTextRange = host.getTextRange();
    if (!hostTextRange.contains(rangeInsideHost.shiftRight(hostTextRange.getStartOffset()))) {
      clear();
      throw new IllegalArgumentException("rangeInsideHost must lie within host text range. rangeInsideHost:"+rangeInsideHost+"; host textRange:"+
                                         hostTextRange);
    }

    cleared = false;
    String nnPrefix = StringUtil.isEmpty(prefix) ? "" : prefix; // intern empty strings too to reduce gc
    String nnSuffix = StringUtil.isEmpty(suffix) ? "" : suffix; // intern empty strings too to reduce gc
    PlaceInfo info = new PlaceInfo(nnPrefix, nnSuffix, host, rangeInsideHost);
    placeInfos.add(info);

    return this;
  }

  private void checkThreading() {
    if (currentThread != Thread.currentThread()) {
      throw new IllegalStateException("Wow, you must not start injecting in one thread ("+currentThread+") and finish in the other ("+Thread.currentThread()+")");
    }
  }

  private static void decode(@NotNull PlaceInfo info, @NotNull StringBuilder outChars) {
    int startOffset = outChars.length();
    outChars.append(info.prefix);
    LiteralTextEscaper<? extends PsiLanguageInjectionHost> textEscaper = info.myEscaper;

    TextRange relevantRange = info.getRelevantRangeInsideHost();
    if (relevantRange == null) {
      relevantRange = TextRange.from(textEscaper.getRelevantTextRange().getStartOffset(), 0);
    }
    else {
      int before = outChars.length();
      boolean decodeSuccessful = textEscaper.decode(relevantRange, outChars);
      int after = outChars.length();
      assert after >= before : "Escaper " + textEscaper + "("+textEscaper.getClass()+") must not mangle char buffer";
      if (!decodeSuccessful) {
        // if there are invalid chars, adjust the range
        int charsDecodedSuccessfully = outChars.length() - before;
        int startOffsetInHost = textEscaper.getOffsetInHost(0, info.registeredRangeInsideHost);
        assert relevantRange.containsOffset(startOffsetInHost) : textEscaper.getClass() +" is inconsistent: its.getOffsetInHost(0) = "+startOffsetInHost+" while its relevantRange="+relevantRange;
        int endOffsetInHost = textEscaper.getOffsetInHost(charsDecodedSuccessfully, info.registeredRangeInsideHost);
        assert relevantRange.containsOffset(endOffsetInHost) : textEscaper.getClass() +" is inconsistent: its.getOffsetInHost(" + charsDecodedSuccessfully +
                                                                 ") = "+startOffsetInHost+" while its relevantRange="+relevantRange;
        ProperTextRange successfulHostRange = new ProperTextRange(startOffsetInHost, endOffsetInHost);
        relevantRange = relevantRange.intersection(successfulHostRange);
      }
    }
    outChars.append(info.suffix);
    int endOffset = outChars.length();
    info.rangeInDecodedPSI = new ProperTextRange(startOffset, endOffset);
    info.rangeInHostElement = relevantRange;
  }

  @NotNull
  private static ShredImpl createShred(@NotNull PlaceInfo info, @NotNull StringBuilder outChars, @NotNull PsiFile hostPsiFile) {
    decode(info, outChars);

    TextRange relevantRange = info.rangeInHostElement;

    TextRange hostTextRange = info.host.getTextRange();
    TextRange relevantRangeInHostFile = relevantRange.shiftRight(hostTextRange.getStartOffset());
    SmartPointerManagerImpl manager = (SmartPointerManagerImpl)SmartPointerManager.getInstance(hostPsiFile.getProject());
    return new ShredImpl(manager.createSmartPsiFileRangePointer(hostPsiFile, relevantRangeInHostFile, true),
                         manager.createSmartPsiElementPointer(info.host, hostPsiFile, true),
                         info.prefix, info.suffix, info.rangeInDecodedPSI, false, info.myEscaper.isOneLine());
  }

  @Override
  public void doneInjecting() {
    checkThreading();
    try {
      if (myLanguage == null) {
        throw new IllegalStateException("Seems you haven't called startInjecting()");
      }
      if (placeInfos.isEmpty()) {
        throw new IllegalStateException("Seems you haven't called addPlace()");
      }
      Language forcedLanguage = myContextElement.getUserData(SingleRootInjectedFileViewProvider.LANGUAGE_FOR_INJECTED_COPY_KEY);
      checkForCorrectContextElement(placeInfos, myContextElement, myLanguage, myHostPsiFile, myHostVirtualFile, myHostDocument,
                                    myDocumentManagerBase);

      createAndRegisterInjected(forcedLanguage);
    }
    finally {
      clear();
    }
  }

  private static void checkForCorrectContextElement(@NotNull List<PlaceInfo> placeInfos,
                                                    @NotNull PsiElement contextElement,
                                                    @NotNull Language language,
                                                    @NotNull PsiFile hostPsiFile,
                                                    @NotNull VirtualFile hostVirtualFile,
                                                    @NotNull DocumentEx hostDocument,
                                                    @NotNull PsiDocumentManagerBase documentManager) {
    boolean isAncestor = false;
    for (PlaceInfo info : placeInfos) {
      isAncestor |= PsiTreeUtil.isAncestor(contextElement, info.host, false);
    }
    assert isAncestor : exceptionContext("Context element " + contextElement.getTextRange() + ": '" + contextElement + "' (" + contextElement.getClass() + "); " +
                                         " must be the parent of at least one of injection hosts", language,
                                         hostPsiFile, hostVirtualFile, hostDocument, placeInfos, documentManager);
  }

  private void createAndRegisterInjected(Language forcedLanguage) {
    StringBuilder decodedChars = new StringBuilder();
    Place place = new Place();
    for (PlaceInfo info : placeInfos) {
      ShredImpl shred = createShred(info, decodedChars, myHostPsiFile);
      place.add(shred);
      info.newInjectionHostRange = shred.getSmartPointer().getRange();
    }
    DocumentWindowImpl documentWindow = new DocumentWindowImpl(myHostDocument, place);
    String fileName = PathUtil.makeFileName(myHostVirtualFile.getName(), fileExtension);

    ASTNode[] parsedNodes =
      parseFile(myLanguage, forcedLanguage, documentWindow, myHostVirtualFile, myHostDocument, myHostPsiFile, myProject, documentWindow.getText(),
                placeInfos, decodedChars, fileName, myDocumentManagerBase);
    for (ASTNode node : parsedNodes) {
      PsiFile psiFile = (PsiFile)node.getPsi();
      InjectedFileViewProvider viewProvider = (InjectedFileViewProvider)psiFile.getViewProvider();
      synchronized (InjectedLanguageManagerImpl.ourInjectionPsiLock) {
        if (psiFile.getLanguage() == viewProvider.getBaseLanguage()) {
          psiFile = createOrMergeInjectedFile(myHostPsiFile, myDocumentManagerBase, place, documentWindow, psiFile, viewProvider);
          addFileToResults(psiFile);
        } else {
          cacheEverything(place, documentWindow, viewProvider, psiFile);
          InjectedLanguageUtil.setHighlightTokens(psiFile, InjectedLanguageUtil.getHighlightTokens(psiFile));
        }

        DocumentWindowImpl retrieved = (DocumentWindowImpl)myDocumentManagerBase.getDocument(psiFile);
        assertEverythingIsAllright(myDocumentManagerBase, retrieved, psiFile);
      }
    }
  }

  @NotNull
  private static PsiFile createOrMergeInjectedFile(@NotNull PsiFile hostPsiFile,
                                                   @NotNull PsiDocumentManagerBase documentManager,
                                                   @NotNull Place place,
                                                   @NotNull DocumentWindowImpl documentWindow,
                                                   @NotNull PsiFile psiFile,
                                                   @NotNull InjectedFileViewProvider viewProvider) {
    cacheEverything(place, documentWindow, viewProvider, psiFile);

    final VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(documentWindow);
    PsiFile cachedPsiFile = ((PsiManagerEx)psiFile.getManager()).getFileManager().findCachedViewProvider(virtualFile).getPsi(psiFile.getLanguage());
    assert cachedPsiFile == psiFile : "Cached psi :"+ cachedPsiFile +" instead of "+psiFile;

    assert place.isValid();
    assert viewProvider.isValid();

    List<InjectedLanguageUtil.TokenInfo> newTokens = InjectedLanguageUtil.getHighlightTokens(psiFile);
    PsiFile newFile = registerDocument(documentWindow, psiFile, place, hostPsiFile, documentManager);
    boolean mergeHappened = newFile != psiFile;
    Place mergedPlace = place;
    if (mergeHappened) {
      InjectedLanguageUtil.clearCaches(psiFile, documentWindow);
      psiFile = newFile;
      viewProvider = (InjectedFileViewProvider)psiFile.getViewProvider();
      documentWindow = viewProvider.getDocument();
      boolean shredsReused = !cacheEverything(place, documentWindow, viewProvider, psiFile);
      if (shredsReused) {
        place.dispose();
        mergedPlace = documentWindow.getShreds();
      }
      InjectedLanguageUtil.setHighlightTokens(psiFile, newTokens);
    }

    assert psiFile.isValid();
    assert mergedPlace.isValid();
    assert viewProvider.isValid();
    return psiFile;
  }

  private static class PatchException extends Exception {
    PatchException(String message) {
      super(message);
    }
  }
  private static void patchLeaves(@NotNull List<? extends PlaceInfo> placeInfos,
                                  @NotNull InjectedFileViewProvider viewProvider,
                                  @NotNull ASTNode parsedNode,
                                  @NotNull CharSequence documentText) throws PatchException {
    Runnable patch = () -> {
      LeafPatcher patcher = new LeafPatcher(placeInfos, parsedNode.getTextLength());
      patcher.patch(parsedNode, placeInfos);
    };
    if (viewProvider instanceof SingleRootInjectedFileViewProvider) {
      ((SingleRootInjectedFileViewProvider)viewProvider).doNotInterruptMeWhileImPatchingLeaves(patch);
    } else if (viewProvider instanceof MultipleRootsInjectedFileViewProvider) {
      ((MultipleRootsInjectedFileViewProvider)viewProvider).doNotInterruptMeWhileImPatchingLeaves(patch);
    }
    if (!((FileElement)parsedNode).textMatches(documentText)) {
      throw new PatchException("After patch: doc:\n'" + documentText + "'\n---PSI:\n'" + parsedNode.getText());
    }
  }

  void injectReference(@NotNull Language language,
                       @NotNull String prefix,
                       @NotNull String suffix,
                       @NotNull PsiLanguageInjectionHost host,
                       @NotNull TextRange rangeInsideHost) {
    ParserDefinition parser = LanguageParserDefinitions.INSTANCE.forLanguage(language);
    if (parser != null) {
      throw new IllegalArgumentException("Language "+language+" being injected as reference must not have ParserDefinition and yet - "+parser);
    }
    ReferenceInjector injector = ReferenceInjector.findById(language.getID());
    if (injector == null) {
      throw new IllegalArgumentException("Language "+language+" being injected as reference must register reference injector");
    }
    placeInfos = new SmartList<>();

    if (!cleared) {
      clear();
      throw new IllegalStateException("Seems you haven't called doneInjecting()");
    }

    myLanguage = language;
    currentThread = Thread.currentThread();

    addPlace(prefix, suffix, host, rangeInsideHost);
    Place place = new Place();
    StringBuilder decodedChars = new StringBuilder();
    ShredImpl shred = createShred(placeInfos.get(0), decodedChars, myHostPsiFile);
    place.add(shred);
    addReferenceToResults(Pair.create(injector, place));
    clear();
  }

  // returns true if shreds were set, false if old ones were reused
  private static boolean cacheEverything(@NotNull Place place,
                                         @NotNull DocumentWindowImpl documentWindow,
                                         @NotNull InjectedFileViewProvider viewProvider,
                                         @NotNull PsiFile psiFile) {
    FileDocumentManagerImpl.registerDocument(documentWindow, viewProvider.getVirtualFile());

    DebugUtil.performPsiModification("MultiHostRegistrar cacheEverything", () -> viewProvider.forceCachedPsi(psiFile));

    SmartPsiElementPointer<PsiLanguageInjectionHost> pointer = ((ShredImpl)place.get(0)).getSmartPointer();
    psiFile.putUserData(FileContextUtil.INJECTED_IN_ELEMENT, pointer);

    keepTreeFromChameleoningBack(psiFile);

    return viewProvider.setShreds(place);
  }


  @NonNls
  private static String exceptionContext(@NonNls @NotNull String msg,
                                         @NotNull Language language,
                                         @NotNull PsiFile hostPsiFile,
                                         @NotNull VirtualFile hostVirtualFile,
                                         @NotNull DocumentEx hostDocument,
                                         @NotNull List<PlaceInfo> placeInfos, @NotNull PsiDocumentManagerBase documentManager) {
    return msg + ".\n" +
           "OK let's see. Host file: " + hostPsiFile + " in '" + hostVirtualFile.getPresentableUrl() + "' (" + hostPsiFile.getLanguage()+") " +
           (documentManager.isUncommited(hostDocument) ? " (uncommitted)" : "") + "\n" +
           "Was injected " + language +
           " at ranges: " + placeInfos;
  }

  private static final Key<ASTNode> TREE_HARD_REF = Key.create("TREE_HARD_REF");
  private static ASTNode keepTreeFromChameleoningBack(PsiFile psiFile) {
    // need to keep tree reachable to avoid being garbage-collected (via WeakReference in PsiFileImpl)
    // and then being reparsed from wrong (escaped) document content
    ASTNode node = psiFile.getNode();
    // expand chameleons
    ASTNode child = node.getFirstChildNode();

    assert !TreeUtil.isCollapsedChameleon(node) : "Chameleon "+node+" is collapsed; file: "+psiFile+"; language: "+psiFile.getLanguage();
    psiFile.putUserData(TREE_HARD_REF, node);

    // just to use child variable
    if (child == null) {
      assert node != null;
    }
    return node;
  }

  private static void assertEverythingIsAllright(@NotNull PsiDocumentManagerBase documentManager,
                                                 @NotNull DocumentWindowImpl documentWindow,
                                                 @NotNull PsiFile psiFile) {
    InjectedFileViewProvider injectedFileViewProvider = (InjectedFileViewProvider)psiFile.getViewProvider();
    assert injectedFileViewProvider.isValid() : "Invalid view provider: "+injectedFileViewProvider;
    DocumentEx frozenWindow = documentManager.getLastCommittedDocument(documentWindow);
    assert psiFile.textMatches(frozenWindow.getText()) : "Document window text mismatch";
    assert injectedFileViewProvider.getDocument() == documentWindow : "Provider document mismatch";
    assert documentManager.getCachedDocument(psiFile) == documentWindow : "Cached document mismatch";
    assert Comparing.equal(psiFile.getVirtualFile(), injectedFileViewProvider.getVirtualFile()) : "Virtual file mismatch: " +
                                                                                                  psiFile.getVirtualFile() +
                                                                                                  "; " +
                                                                                                  injectedFileViewProvider.getVirtualFile();
    PsiDocumentManagerBase.checkConsistency(psiFile, frozenWindow);
  }

  void addToResults(@NotNull InjectionResult result) {
    if (result.files != null) {
      for (PsiFile file : result.files) {
        addFileToResults(file);
      }
    }
    if (result.references != null) {
      for (Pair<ReferenceInjector, Place> pair : result.references) {
        addReferenceToResults(pair);
      }
    }
  }

  private void addFileToResults(@NotNull PsiFile psiFile) {
    if (resultFiles == null) {
      resultFiles = new SmartList<>();
    }
    resultFiles.add(psiFile);
  }
  private void addReferenceToResults(@NotNull Pair<ReferenceInjector, Place> pair) {
    if (resultReferences == null) {
      resultReferences = new SmartList<>();
    }
    resultReferences.add(pair);
  }


  // under InjectedLanguageManagerImpl.ourInjectionPsiLock
  @NotNull
  private static PsiFile registerDocument(@NotNull DocumentWindowImpl newDocumentWindow,
                                          @NotNull PsiFile newInjectedPsi,
                                          @NotNull Place shreds,
                                          @NotNull PsiFile hostPsiFile,
                                          @NotNull PsiDocumentManager documentManager) {
    List<DocumentWindow> injected = InjectedLanguageUtil.getCachedInjectedDocuments(hostPsiFile);

    for (int i = injected.size()-1; i>=0; i--) {
      DocumentWindowImpl oldDocument = (DocumentWindowImpl)injected.get(i);
      final PsiFileImpl oldFile = (PsiFileImpl)documentManager.getCachedPsiFile(oldDocument);
      FileViewProvider viewProvider;
      if (oldFile == null ||
          !oldFile.isValid() ||
          !((viewProvider = oldFile.getViewProvider()) instanceof InjectedFileViewProvider) ||
          ((InjectedFileViewProvider)viewProvider).isDisposed()
        ) {
        injected.remove(i);
        Disposer.dispose(oldDocument);
        continue;
      }

      final ASTNode newInjectedNode = newInjectedPsi.getNode();
      final ASTNode oldFileNode = oldFile.getNode();
      assert newInjectedNode != null : "New node is null";
      if (oldDocument.areRangesEqual(newDocumentWindow)) {
        if (oldFile.getFileType() != newInjectedPsi.getFileType() || oldFile.getLanguage() != newInjectedPsi.getLanguage()) {
          injected.remove(i);
          Disposer.dispose(oldDocument);
          continue;
        }
        oldFile.putUserData(FileContextUtil.INJECTED_IN_ELEMENT, newInjectedPsi.getUserData(FileContextUtil.INJECTED_IN_ELEMENT));

        assert shreds.isValid();
        mergePsi(oldFile, oldFileNode, newInjectedPsi, newInjectedNode);
        assert shreds.isValid();

        return oldFile;
      }
      else if (intersect(oldDocument, newDocumentWindow)) {
        injected.remove(i); // injected fragments should not overlap. In the End, there can be only one.
      }
    }
    injected.add(newDocumentWindow);

    return newInjectedPsi;
  }

  private static void mergePsi(@NotNull PsiFile oldFile,
                               @NotNull ASTNode oldFileNode,
                               @NotNull PsiFile injectedPsi,
                               @NotNull ASTNode injectedNode) {
    if (!oldFile.textMatches(injectedPsi)) {
      InjectedFileViewProvider oldViewProvider = (InjectedFileViewProvider)oldFile.getViewProvider();
      oldViewProvider.performNonPhysically(() -> DebugUtil.performPsiModification("injected tree diff", () -> {
        final DiffLog diffLog = BlockSupportImpl.mergeTrees((PsiFileImpl)oldFile, oldFileNode, injectedNode, new DaemonProgressIndicator(),
                                                            oldFileNode.getText());
        diffLog.doActualPsiChange(oldFile);
      }));
    }
  }

  /**
   * {@code language} was injected in {@code hostPsiFile} (located in {@code hostVirtualFile})
   * and corresponding injected {@code oldInjectedPsi} (along with {@code oldDocumentWindow} and {@code oldInjectedVirtualFile}) were created.
   * Then the user came along and changed the host document in {@code hostVirtualFile}.
   * Document commit started and produced PSI diff {@code oldRoot} -> {@code newRoot} in the host PSI.
   *
   * Now we try to produce similar diff for the injected fragment PSI.
   * To do that, we:
   * <pre>
   * - calculate where injected shreds from oldRoot will be located in the newRoot
   *   -- get old range markers for shreds
   *   -- calculate new ranges by applying doc changes in {@link SelfElementInfo#calcActualRangeAfterDocumentEvents(PsiFile, Document, Segment, boolean)}
   *      (have to do all that manually because smart pointers are not yet updated)
   * - find similar PsiLanguageInjectionHost in the {@code newRoot} at these ranges
   *   (since newRoot is a non-physical copy of hostPsiFile with PSI diff applied, we have to do that semi-manually too)
   * - create fake injection using this new injection host (along with characters decoding/leaf patching)
   *   (see call to {@link #parseFile(Language, Language, DocumentWindowImpl, VirtualFile, DocumentEx, PsiFile, Project, CharSequence, List, StringBuilder, String, PsiDocumentManagerBase)} )
   * - feed two injections, the old and the new created fake to the standard tree diff
   *   (see call to {@link BlockSupportImpl#mergeTrees(PsiFileImpl, ASTNode, ASTNode, ProgressIndicator, CharSequence)} )
   * - return continuation which performs actual PSI replace, just like {@link DocumentCommitThread#doCommit(DocumentCommitThread.CommitTask, PsiFile, FileASTNode, ProperTextRange, List)} does
   *   {@code null} means we failed to reparse and will have to kill the injection.
   * </pre>
   */
  static BooleanRunnable reparse(@NotNull Language language,
                                 @NotNull DocumentWindowImpl oldDocumentWindow,
                                 @NotNull PsiFile oldInjectedPsi,
                                 @NotNull VirtualFileWindow oldInjectedVirtualFile,
                                 @NotNull VirtualFile hostVirtualFile,
                                 @NotNull PsiFile hostPsiFile,
                                 @NotNull DocumentEx hostDocument,
                                 @NotNull ProgressIndicator indicator,
                                 @NotNull ASTNode oldRoot,
                                 @NotNull ASTNode newRoot,
                                 @NotNull PsiDocumentManagerBase documentManager) {
    Project project = hostPsiFile.getProject();
    String newText = oldDocumentWindow.getText();
    FileASTNode oldNode = oldInjectedPsi.getNode();
    InjectedFileViewProvider oldInjectedPsiViewProvider = (InjectedFileViewProvider)oldInjectedPsi.getViewProvider();
    String oldPsiText = oldNode.getText();
    if (newText.equals(oldPsiText)) return ()->true;
    if (oldDocumentWindow.isOneLine() && newText.contains("\n") != oldPsiText.contains("\n")) {
      // one-lineness changed, e.g. when enter pressed in the middle of a string literal
      return null;
    }
    Place oldPlace = oldDocumentWindow.getShreds();
    // can be different from newText if decode fails in the middle and we'll have to shrink the document
    StringBuilder newDocumentText = new StringBuilder(newText.length());
    // we need escaper but it only works with committed PSI,
    // so we get the committed (but not yet applied) PSI from the commit-document-in-the-background process
    // and find the corresponding injection host there
    // and create literal escaper from that new (dummy) psi
    List<PlaceInfo> placeInfos = new SmartList<>();
    StringBuilder chars = new StringBuilder();
    for (PsiLanguageInjectionHost.Shred shred : oldPlace) {
      PsiLanguageInjectionHost oldHost = shred.getHost();
      if (oldHost == null) return null;
      SmartPsiElementPointer<PsiLanguageInjectionHost> hostPointer = ((ShredImpl)shred).getSmartPointer();
      Segment newInjectionHostRange = calcActualRange(hostPsiFile, hostDocument, hostPointer.getPsiRange());
      if (newInjectionHostRange == null) return null;
      PsiLanguageInjectionHost newDummyInjectionHost = findNewInjectionHost(hostPsiFile, oldRoot, newRoot, oldHost, newInjectionHostRange);
      if (newDummyInjectionHost == null) {
        return null;
      }
      newInjectionHostRange = newDummyInjectionHost.getTextRange().shiftRight(oldRoot.getTextRange().getStartOffset());
      Segment hostInjectionRange = shred.getHostRangeMarker(); // in the new document
      if (hostInjectionRange == null) return null;
      TextRange rangeInsideHost = TextRange.create(hostInjectionRange).shiftLeft(newInjectionHostRange.getStartOffset());

      PlaceInfo info = new PlaceInfo(shred.getPrefix(), shred.getSuffix(), newDummyInjectionHost, rangeInsideHost);
      placeInfos.add(info);
      info.newInjectionHostRange = newInjectionHostRange;

      decode(info, chars);

      // pass the old pointers because their offsets will be adjusted automatically (SmartPsiElementPointer does that)
      TextRange rangeInHostElementPSI = info.rangeInHostElement;

      newDocumentText.append(shred.getPrefix());
      newDocumentText.append(newDummyInjectionHost.getText(), rangeInHostElementPSI.getStartOffset(), rangeInHostElementPSI.getEndOffset());
      newDocumentText.append(shred.getSuffix());
    }
    // newDocumentText can be shorter if decode failed
    //assert newText.equals(newDocumentText.toString()) : "-\n"+newText+"\n--\n"+newDocumentText+"\n---\n";

    assert documentManager.isUncommited(hostDocument);
    String fileName = ((VirtualFileWindowImpl)oldInjectedVirtualFile).getName();
    ASTNode[] parsedNodes = parseFile(language, language, oldDocumentWindow,
                                   hostVirtualFile, hostDocument, hostPsiFile, project, newDocumentText, placeInfos, chars,
                                   fileName, documentManager);
    List<PsiFile> oldFiles = ((AbstractFileViewProvider)oldInjectedPsiViewProvider).getCachedPsiFiles();
    synchronized (InjectedLanguageManagerImpl.ourInjectionPsiLock) {
      DiffLog[] diffLogs = new DiffLog[parsedNodes.length];
      for (int i = 0; i < parsedNodes.length; i++) {
        ASTNode parsedNode = parsedNodes[i];
        PsiFile oldFile = oldFiles.get(i);
        diffLogs[i] = BlockSupportImpl.mergeTrees((PsiFileImpl)oldFile, oldFile.getNode(), parsedNode, indicator, oldPsiText);
      }

      return () -> {
        oldInjectedPsiViewProvider.performNonPhysically(() ->
          DebugUtil.performPsiModification("injected tree diff", () -> {
            for (int i = 0; i < diffLogs.length; i++) {
              DiffLog diffLog = diffLogs[i];
              diffLog.doActualPsiChange(oldFiles.get(i));

              // create new shreds after commit is complete because otherwise the range markers will be changed in MarkerCache.updateMarkers
              Place newPlace = new Place();
              for (int j = 0; j < oldPlace.size(); j++) {
                PsiLanguageInjectionHost.Shred shred = oldPlace.get(j);
                PlaceInfo info = placeInfos.get(j);
                TextRange rangeInDecodedPSI = info.rangeInDecodedPSI;
                TextRange rangeInHostElementPSI = info.rangeInHostElement;
                // now find the injection host in the newly committed file
                FileASTNode root = hostPsiFile.getNode();
                PsiLanguageInjectionHost newHost = findNewInjectionHost(hostPsiFile, root, root, info.host, info.newInjectionHostRange);
                ShredImpl newShred = ((ShredImpl)shred).withRange(rangeInDecodedPSI, rangeInHostElementPSI, newHost);
                newPlace.add(newShred);
              }

              cacheEverything(newPlace, oldDocumentWindow, oldInjectedPsiViewProvider, oldFiles.get(i));
              String docText = oldDocumentWindow.getText();
              assert docText.equals(newText) : "=\n" + docText + "\n==\n" + newDocumentText + "\n===\n";
            }
          })
        );
        return true;
      };
    }
  }

  private static Segment calcActualRange(@NotNull PsiFile containingFile,
                                         @NotNull Document document,
                                         @NotNull Segment range) {
    return SelfElementInfo.calcActualRangeAfterDocumentEvents(containingFile, document, range, true);
  }

  private static ASTNode @NotNull [] parseFile(@NotNull Language language, Language forcedLanguage,
                                               @NotNull DocumentWindowImpl documentWindow,
                                               @NotNull VirtualFile hostVirtualFile,
                                               @NotNull DocumentEx hostDocument, @NotNull PsiFile hostPsiFile,
                                               @NotNull Project project,
                                               @NotNull CharSequence documentText,
                                               @NotNull List<PlaceInfo> placeInfos,
                                               @NotNull StringBuilder decodedChars,
                                               @NotNull String fileName, @NotNull PsiDocumentManagerBase documentManager) {
    VirtualFileWindowImpl virtualFile = new VirtualFileWindowImpl(fileName, hostVirtualFile, documentWindow, language, decodedChars);
    Language finalLanguage = forcedLanguage == null ? LanguageSubstitutors.getInstance().substituteLanguage(language, virtualFile, project) : forcedLanguage;
    InjectedFileViewProvider viewProvider = InjectedFileViewProvider.create(PsiManagerEx.getInstanceEx(project), virtualFile, documentWindow, finalLanguage);
    Set<Language> languages = viewProvider.getLanguages();
    ASTNode[] parsedNodes = new ASTNode[languages.size()];

    int i = 0;
    for (Language lang : languages) {
      ParserDefinition parserDefinition = LanguageParserDefinitions.INSTANCE.forLanguage(lang);
      assert parserDefinition != null : "Parser definition for language " + finalLanguage + " is null";
      PsiFileImpl psiFile = (PsiFileImpl)parserDefinition.createFile(viewProvider);
      if (viewProvider instanceof TemplateLanguageFileViewProvider) {
        IElementType elementType = ((TemplateLanguageFileViewProvider)viewProvider).getContentElementType(lang);
        if (elementType != null) {
          psiFile.setContentElementType(elementType);
        }
      }
      final ASTNode parsedNode = keepTreeFromChameleoningBack(psiFile);

      assert parsedNode instanceof FileElement : "Parsed to " + parsedNode + " instead of FileElement";

      assert ((FileElement)parsedNode).textMatches(decodedChars) : exceptionContext("Before patch: doc:\n'" + documentText + "'\n---PSI:\n'" + parsedNode.getText() + "'\n---chars:\n'" +
                                                                                    decodedChars + "'",
                                                                                    finalLanguage, hostPsiFile, hostVirtualFile,
                                                                                    hostDocument, placeInfos, documentManager);
      try {
        patchLeaves(placeInfos, viewProvider, parsedNode, documentText);
      }
      catch (PatchException e) {
        throw new RuntimeException(exceptionContext(e.getMessage()+"'\n---chars:\n'" + decodedChars + "'", finalLanguage, hostPsiFile, hostVirtualFile, hostDocument, placeInfos,
                                                    documentManager));
      }

      try {
        List<InjectedLanguageUtil.TokenInfo> tokens = obtainHighlightTokensFromLexer(decodedChars, virtualFile, project, placeInfos);
        InjectedLanguageUtil.setHighlightTokens(psiFile, tokens);
      }
      catch (ProcessCanceledException e) {
        throw e;
      }
      catch (RuntimeException e) {
        throw new RuntimeException(exceptionContext("Obtaining tokens error", language, hostPsiFile, hostVirtualFile, hostDocument, placeInfos,
                                                    documentManager), e);
      }
      parsedNodes[i++] = parsedNode;
    }

    return parsedNodes;
  }

  // find PsiLanguageInjectionHost in range "newInjectionHostRange" in the file which is almost "hostPsiFile" but where "oldRoot" replaced with "newRoot"
  private static PsiLanguageInjectionHost findNewInjectionHost(@NotNull PsiFile hostPsiFile,
                                                               @NotNull ASTNode oldRoot,
                                                               @NotNull ASTNode newRoot,
                                                               @NotNull PsiLanguageInjectionHost oldInjectionHost,
                                                               @NotNull Segment newInjectionHostRange) {
    TextRange oldRootRange = oldRoot.getTextRange();
    TextRange newRootRange = newRoot.getTextRange();
    PsiElement toLookIn;
    int startToLook;
    int endToLook;
    if (newInjectionHostRange.getEndOffset() <= oldRootRange.getStartOffset()) {
      // find left of the change
      toLookIn = hostPsiFile;
      startToLook = newInjectionHostRange.getStartOffset();
      endToLook = newInjectionHostRange.getEndOffset();
    }
    else if (newInjectionHostRange.getStartOffset() >= oldRootRange.getStartOffset() + newRootRange.getLength()) {
      // find right of the change
      toLookIn = hostPsiFile;
      startToLook = newInjectionHostRange.getStartOffset() + newRootRange.getLength() - oldRootRange.getLength();
      endToLook = newInjectionHostRange.getEndOffset() + newRootRange.getLength() - oldRootRange.getLength();
    }
    else {
      // inside
      toLookIn = newRoot.getPsi();
      if (toLookIn instanceof PsiFile) {
        FileViewProvider viewProvider = ((PsiFile)toLookIn).getViewProvider();
        toLookIn = ObjectUtils.notNull(viewProvider.getPsi(hostPsiFile.getLanguage()), viewProvider.getPsi(viewProvider.getBaseLanguage()));
      }
      startToLook = newInjectionHostRange.getStartOffset() - oldRootRange.getStartOffset();
      endToLook = newInjectionHostRange.getEndOffset() - oldRootRange.getStartOffset();
    }
    try {
      Identikit.ByType kit = Identikit.fromPsi(oldInjectionHost, hostPsiFile.getLanguage());
      return (PsiLanguageInjectionHost)kit.findInside(toLookIn, startToLook, endToLook);
    }
    // JavaParserDefinition.create() throws this.
    // DO not over-generalize this exception type to avoid swallowing meaningful exceptions
    catch (IllegalStateException e) {
      return null;
    }
  }


  static boolean intersect(DocumentWindowImpl doc1, DocumentWindowImpl doc2) {
    Segment[] hostRanges1 = doc1.getHostRanges();
    Segment[] hostRanges2 = doc2.getHostRanges();
    // DocumentWindowImpl.getHostRanges() may theoretically return non-sorted ranges
    for (Segment segment1 : hostRanges1) {
      for (Segment segment2 : hostRanges2) {
        if (Math.max(segment1.getStartOffset(), segment2.getStartOffset()) < Math.min(segment1.getEndOffset(), segment2.getEndOffset())) {
          return true;
        }
      }
    }
    return false;
  }

  // returns lexer element types with corresponding ranges in encoded (injection host based) PSI
  @NotNull
  private static List<InjectedLanguageUtil.TokenInfo>
          obtainHighlightTokensFromLexer(@NotNull CharSequence outChars,
                                         @NotNull VirtualFileWindow virtualFile,
                                         @NotNull Project project,
                                         @NotNull List<? extends PlaceInfo> placeInfos) {
    VirtualFile file = (VirtualFile)virtualFile;
    FileType fileType = file.getFileType();
    EditorHighlighterProvider provider = FileTypeEditorHighlighterProviders.INSTANCE.forFileType(fileType);
    EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
    EditorHighlighter highlighter = provider.getEditorHighlighter(project, fileType, file, scheme);
    highlighter.setText(outChars);
    HighlighterIterator iterator = highlighter.createIterator(0);
    int hostNum = -1;
    int prevHostEndOffset = 0;
    LiteralTextEscaper<?> escaper = null;
    int prefixLength = 0;
    int suffixLength = 0;
    TextRange rangeInsideHost = null;
    int shredEndOffset = -1;
    List<InjectedLanguageUtil.TokenInfo> tokens = new ArrayList<>(outChars.length()/5); // avg. token per 5 chars
    while (!iterator.atEnd()) {
      IElementType tokenType = iterator.getTokenType();
      TextRange range = new ProperTextRange(iterator.getStart(), iterator.getEnd());
      while (range != null && !range.isEmpty()) {
        if (range.getStartOffset() >= shredEndOffset) {
          hostNum++;
          PlaceInfo info = placeInfos.get(hostNum);
          shredEndOffset = info.rangeInDecodedPSI.getEndOffset();
          prevHostEndOffset = range.getStartOffset();
          escaper = info.myEscaper;
          rangeInsideHost = info.rangeInHostElement;
          prefixLength = info.prefix.length();
          suffixLength = info.suffix.length();
        }
        //in prefix/suffix or spills over to next fragment
        if (range.getStartOffset() < prevHostEndOffset + prefixLength) {
          range = new UnfairTextRange(prevHostEndOffset + prefixLength, range.getEndOffset());
        }
        TextRange spilled = null;
        if (range.getEndOffset() > shredEndOffset - suffixLength) {
          spilled = new UnfairTextRange(shredEndOffset, range.getEndOffset());
          range = new UnfairTextRange(range.getStartOffset(), shredEndOffset-suffixLength);
        }
        if (!range.isEmpty()) {
          int start = escaper.getOffsetInHost(range.getStartOffset() - prevHostEndOffset - prefixLength, rangeInsideHost);
          if (start == -1) start = rangeInsideHost.getStartOffset();
          int end = escaper.getOffsetInHost(range.getEndOffset() - prevHostEndOffset - prefixLength, rangeInsideHost);
          if (end == -1) {
            end = rangeInsideHost.getEndOffset();
            prevHostEndOffset = shredEndOffset;
          }
          ProperTextRange rangeInHost = new ProperTextRange(start, end);
          tokens.add(new InjectedLanguageUtil.TokenInfo(tokenType, rangeInHost, hostNum, iterator.getTextAttributes()));
        }
        range = spilled;
      }
      iterator.advance();
    }
    return tokens;
  }

  @Override
  public String toString() {
    return String.valueOf(resultFiles);
  }

  // performance: avoid context.getContainingFile()
  @NotNull
  PsiFile getHostPsiFile() {
    return myHostPsiFile;
  }
}
