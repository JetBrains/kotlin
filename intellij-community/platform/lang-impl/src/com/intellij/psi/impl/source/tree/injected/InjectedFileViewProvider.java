/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package com.intellij.psi.impl.source.tree.injected;

import com.intellij.injected.editor.DocumentWindow;
import com.intellij.injected.editor.VirtualFileWindow;
import com.intellij.lang.Language;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.Segment;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.FreeThreadedFileViewProvider;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class InjectedFileViewProvider extends SingleRootFileViewProvider implements FreeThreadedFileViewProvider {
  private final Object myLock = new Object();
  private final DocumentWindowImpl myDocumentWindow;
  private static final ThreadLocal<Boolean> disabledTemporarily = ThreadLocal.withInitial(() -> false);
  private boolean myPatchingLeaves;

  InjectedFileViewProvider(@NotNull PsiManager psiManager,
                           @NotNull VirtualFileWindow virtualFile,
                           @NotNull DocumentWindowImpl documentWindow,
                           @NotNull Language language) {
    super(psiManager, (VirtualFile)virtualFile, true, language);
    myDocumentWindow = documentWindow;
  }

  @Override
  public void rootChanged(@NotNull PsiFile psiFile) {
    super.rootChanged(psiFile);
    if (!isPhysical()) return; // injected PSI change happened inside reparse; ignore
    if (myPatchingLeaves) return;

    DocumentWindowImpl documentWindow = myDocumentWindow;
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

  @Override
  public FileViewProvider clone() {
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
      current.putUserData(LANGUAGE_FOR_INJECTED_COPY_KEY, language);
      try {
        InjectedLanguageManager.getInstance(hostPsiFileCopy.getProject()).enumerateEx(current, hostPsiFileCopy, false, visitor);
      }
      finally {
        current.putUserData(LANGUAGE_FOR_INJECTED_COPY_KEY, null);
      }
      if (provider.get() != null) break;
    }
    return provider.get();
  }

  static Key<Language> LANGUAGE_FOR_INJECTED_COPY_KEY = Key.create("LANGUAGE_FOR_INJECTED_COPY_KEY");
  // returns true if shreds were set, false if old ones were reused
  boolean setShreds(@NotNull Place newShreds) {
    synchronized (myLock) {
      Place oldShreds = myDocumentWindow.getShreds();
      // try to reuse shreds, otherwise there are too many range markers disposals/re-creations
      if (same(oldShreds, newShreds)) {
        return false;
      }
      myDocumentWindow.setShreds(newShreds);
      return true;
    }
  }

  @NotNull
  private Project getProject() {
    return getManager().getProject();
  }

  private static boolean same(Place oldShreds, Place newShreds) {
    if (oldShreds == newShreds) return true;
    if (oldShreds.size() != newShreds.size()) return false;
    for (int i = 0; i < oldShreds.size(); i++) {
      PsiLanguageInjectionHost.Shred oldShred = oldShreds.get(i);
      PsiLanguageInjectionHost.Shred newShred = newShreds.get(i);
      if (!oldShred.equals(newShred)) return false;
    }
    return true;
  }

  boolean isValid() {
    return getShreds().isValid();
  }

  boolean isDisposed() {
    return getProject().isDisposed();
  }

  Place getShreds() {
    return myDocumentWindow.getShreds();
  }

  @Override
  @NotNull
  public DocumentWindow getDocument() {
    return myDocumentWindow;
  }

  @Override
  public boolean isEventSystemEnabled() {
    return !disabledTemporarily.get();
  }

  @Override
  public boolean isPhysical() {
    return isEventSystemEnabled();
  }

  void performNonPhysically(Runnable runnable) {
    synchronized (myLock) {
      disabledTemporarily.set(true);
      try {
        runnable.run();
      }
      finally {
        disabledTemporarily.set(false);
      }
    }
  }

  @NonNls
  @Override
  public String toString() {
    return "Injected file '"+getVirtualFile().getName()+"' " + (isValid() ? "" : " invalid") + (isPhysical() ? "" : " nonphysical");
  }

  void doNotInterruptMeWhileImPatchingLeaves(@NotNull Runnable runnable) {
    myPatchingLeaves = true;
    try {
      runnable.run();
    }
    finally {
      myPatchingLeaves = false;
    }
  }
}
