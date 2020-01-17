// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.impl.source.tree.injected;

import com.intellij.injected.editor.DocumentWindow;
import com.intellij.injected.editor.VirtualFileWindow;
import com.intellij.lang.Language;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.Segment;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.impl.FreeThreadedFileViewProvider;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.psi.templateLanguages.TemplateLanguageFileViewProvider;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @deprecated Use methods from {@link InjectedLanguageManager} instead
 */
@Deprecated
public interface InjectedFileViewProvider extends FileViewProvider, FreeThreadedFileViewProvider {
  default void rootChangedImpl(@NotNull PsiFile psiFile) {
    if (!isPhysical()) return; // injected PSI change happened inside reparse; ignore
    if (getPatchingLeaves()) return;

    DocumentWindowImpl documentWindow = getDocument();
    List<PsiLanguageInjectionHost.Shred> shreds = documentWindow.getShreds();
    assert documentWindow.getHostRanges().length == shreds.size();
    String[] changes = documentWindow.calculateMinEditSequence(psiFile.getNode().getText());
    assert changes.length == shreds.size();
    for (int i = 0; i < changes.length; i++) {
      String change = changes[i];
      if (change != null) {
        PsiLanguageInjectionHost.Shred shred = shreds.get(i);
        PsiLanguageInjectionHost host = shred.getHost();
        TextRange rangeInsideHost = shred.getRangeInsideHost();
        String newHostText = StringUtil.replaceSubstring(host.getText(), rangeInsideHost, change);
        //shred.host =
          host.updateText(newHostText);
      }
    }
  }

  default FileViewProvider cloneImpl() {
    final DocumentWindow oldDocumentWindow = ((VirtualFileWindow)getVirtualFile()).getDocumentWindow();
    Document hostDocument = oldDocumentWindow.getDelegate();
    final PsiDocumentManager documentManager = PsiDocumentManager.getInstance(getManager().getProject());
    PsiFile hostFile = documentManager.getPsiFile(hostDocument);
    Language language = getBaseLanguage();
    PsiFile file = getPsi(language);
    final Language hostFileLanguage = InjectedLanguageManager.getInstance(file.getProject()).getTopLevelFile(file).getLanguage();
    PsiFile hostPsiFileCopy = (PsiFile)hostFile.copy();
    Segment firstTextRange = oldDocumentWindow.getHostRanges()[0];
    PsiElement hostElementCopy = hostPsiFileCopy.getViewProvider().findElementAt(firstTextRange.getStartOffset(), hostFileLanguage);
    assert hostElementCopy != null;
    final Ref<FileViewProvider> provider = new Ref<>();
    PsiLanguageInjectionHost.InjectedPsiVisitor visitor = (injectedPsi, places) -> {
      Document document = documentManager.getCachedDocument(injectedPsi);
      if (document instanceof DocumentWindowImpl && oldDocumentWindow.areRangesEqual((DocumentWindowImpl)document)) {
        provider.set(injectedPsi.getViewProvider());
      }
    };
    for (PsiElement current = hostElementCopy; current != null && current != hostPsiFileCopy; current = current.getParent()) {
      current.putUserData(SingleRootInjectedFileViewProvider.LANGUAGE_FOR_INJECTED_COPY_KEY, language);
      try {
        InjectedLanguageManager.getInstance(hostPsiFileCopy.getProject()).enumerateEx(current, hostPsiFileCopy, false, visitor);
      }
      finally {
        current.putUserData(SingleRootInjectedFileViewProvider.LANGUAGE_FOR_INJECTED_COPY_KEY, null);
      }
      if (provider.get() != null) break;
    }
    return provider.get();
  }

  // returns true if shreds were set, false if old ones were reused
  default boolean setShreds(@NotNull Place newShreds) {
    synchronized (getLock()) {
      Place oldShreds = getDocument().getShreds();
      // try to reuse shreds, otherwise there are too many range markers disposals/re-creations
      if (same(oldShreds, newShreds)) {
        return false;
      }
      getDocument().setShreds(newShreds);
      return true;
    }
  }

  static boolean same(Place oldShreds, Place newShreds) {
    if (oldShreds == newShreds) return true;
    if (oldShreds.size() != newShreds.size()) return false;
    for (int i = 0; i < oldShreds.size(); i++) {
      PsiLanguageInjectionHost.Shred oldShred = oldShreds.get(i);
      PsiLanguageInjectionHost.Shred newShred = newShreds.get(i);
      if (!oldShred.equals(newShred)) return false;
    }
    return true;
  }

  default boolean isPhysicalImpl() {
    return isEventSystemEnabled();
  }

  default void performNonPhysically(Runnable runnable) {
    synchronized (getLock()) {
      SingleRootInjectedFileViewProvider.disabledTemporarily.set(true);
      try {
        runnable.run();
      }
      finally {
        SingleRootInjectedFileViewProvider.disabledTemporarily.set(false);
      }
    }
  }

  boolean getPatchingLeaves();
  void forceCachedPsi(@NotNull PsiFile file);
  Object getLock();

  default boolean isValid() {
    return getShreds().isValid();
  }

  default boolean isDisposed() {
    return getManager().getProject().isDisposed();
  }

  default Place getShreds() {
    return getDocument().getShreds();
  }

  default boolean isEventSystemEnabledImpl() {
    return !SingleRootInjectedFileViewProvider.disabledTemporarily.get();
  }

  @Override
  @NotNull
  DocumentWindowImpl getDocument();

  static InjectedFileViewProvider create(@NotNull PsiManagerEx manager,
                                         @NotNull VirtualFileWindowImpl file,
                                         @NotNull DocumentWindowImpl window,
                                         @NotNull Language language) {
    AbstractFileViewProvider original = (AbstractFileViewProvider)manager.getFileManager().createFileViewProvider(file, false);
    return original instanceof TemplateLanguageFileViewProvider ?
             new MultipleRootsInjectedFileViewProvider.Template(manager, file, window, language, original) :
           original instanceof MultiplePsiFilesPerDocumentFileViewProvider ?
             new MultipleRootsInjectedFileViewProvider(manager, file, window, language, original) :
             new SingleRootInjectedFileViewProvider(manager, file, window, language);
  }
}
