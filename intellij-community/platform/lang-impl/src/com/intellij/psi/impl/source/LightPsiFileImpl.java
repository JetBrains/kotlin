// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.psi.impl.source;

import com.intellij.ide.util.PsiNavigationSupport;
import com.intellij.lang.FileASTNode;
import com.intellij.lang.Language;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.*;
import com.intellij.psi.impl.file.PsiFileImplUtil;
import com.intellij.psi.impl.source.resolve.FileContextUtil;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.text.CharArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class LightPsiFileImpl extends PsiElementBase implements PsiFileEx {

  private static final Logger LOG = Logger.getInstance(LightPsiFileImpl.class);
  private PsiFile myOriginalFile = null;
  private boolean myExplicitlySetAsValid = false;
  private boolean myInvalidated = false;
  private final FileViewProvider myViewProvider;
  private final PsiManagerImpl myManager;
  private final Language myLanguage;

  public LightPsiFileImpl(@NotNull FileViewProvider provider, @NotNull Language language) {
    myViewProvider = provider;
    myManager = (PsiManagerImpl)provider.getManager();
    myLanguage = language;
  }

  @Override
  public VirtualFile getVirtualFile() {
    return getViewProvider().isEventSystemEnabled() ? getViewProvider().getVirtualFile() : null;
  }

  @Override
  public boolean processChildren(@NotNull final PsiElementProcessor<PsiFileSystemItem> processor) {
    return true;
  }

  @Override
  public boolean isValid() {
    if (myInvalidated) return false;
    if (!getViewProvider().isPhysical() || myExplicitlySetAsValid) return true; // "dummy" file
    return getViewProvider().getVirtualFile().isValid();
  }

  public void setIsValidExplicitly(boolean b) {
    LOG.assertTrue(ApplicationManager.getApplication().isUnitTestMode());
    myExplicitlySetAsValid = b;
  }

  @Override
  public String getText() {
    return getViewProvider().getContents().toString();
  }

  @Override
  public long getModificationStamp() {
    return getViewProvider().getModificationStamp();
  }

  @Override
  public void subtreeChanged() {
    clearCaches();
    getViewProvider().rootChanged(this);
  }

  @Override
  public abstract void clearCaches();

  @Override
  protected LightPsiFileImpl clone() {
    final FileViewProvider provider = getViewProvider().clone();
    final LightPsiFileImpl clone = (LightPsiFileImpl)provider.getPsi(getLanguage());

    copyCopyableDataTo(clone);

    if (getViewProvider().isEventSystemEnabled()) {
      clone.myOriginalFile = this;
    }
    else if (myOriginalFile != null) {
      clone.myOriginalFile = myOriginalFile;
    }
    return clone;
  }

  @Override
  @NotNull
  public String getName() {
    return getViewProvider().getVirtualFile().getName();
  }

  @Override
  public PsiElement setName(@NotNull String name) throws IncorrectOperationException {
    checkSetName(name);
    subtreeChanged();
    return PsiFileImplUtil.setName(this, name);
  }

  @Override
  public void checkSetName(String name) throws IncorrectOperationException {
    if (!getViewProvider().isEventSystemEnabled()) return;
    PsiFileImplUtil.checkSetName(this, name);
  }

  @Override
  public PsiDirectory getParent() {
    return getContainingDirectory();
  }

  @Override
  public PsiDirectory getContainingDirectory() {
    final VirtualFile parentFile = getViewProvider().getVirtualFile().getParent();
    if (parentFile == null) return null;
    return getManager().findDirectory(parentFile);
  }

  @Nullable
  public PsiDirectory getParentDirectory() {
    return getContainingDirectory();
  }

  @Override
  public PsiFile getContainingFile() {
    return this;
  }

  @Override
  public void delete() throws IncorrectOperationException {
    throw new IncorrectOperationException("Not implemented");
  }

  @Override
  public void checkDelete() throws IncorrectOperationException {
    if (!getViewProvider().isEventSystemEnabled()) {
      throw new IncorrectOperationException();
    }
    CheckUtil.checkWritable(this);
  }

  @Override
  @NotNull
  public PsiFile getOriginalFile() {
    return myOriginalFile == null ? this : myOriginalFile;
  }

  public void setOriginalFile(final PsiFile originalFile) {
    myOriginalFile = originalFile.getOriginalFile();
  }

  @Override
  @NotNull
  public PsiFile[] getPsiRoots() {
    return new PsiFile[]{this};
  }

  @Override
  public boolean isPhysical() {
    return getViewProvider().isEventSystemEnabled();
  }

  @Override
  @NotNull
  public Language getLanguage() {
    return myLanguage;
  }

  @Override
  @NotNull
  public FileViewProvider getViewProvider() {
    return myViewProvider;
  }

  @Override
  public PsiManager getManager() {
    return myManager;
  }

  @Override
  @NotNull
  public Project getProject() {
    final PsiManager manager = getManager();
    if (manager == null) throw new PsiInvalidElementAccessException(this);

    return manager.getProject();
  }

  @Override
  public void acceptChildren(@NotNull PsiElementVisitor visitor) {
    PsiElement child = getFirstChild();
    while (child != null) {
      final PsiElement nextSibling = child.getNextSibling();
      child.accept(visitor);
      child = nextSibling;
    }
  }

  @Override
  public final synchronized PsiElement copy() {
    return clone();
  }

  @Override
  public final void checkAdd(@NotNull PsiElement element) throws IncorrectOperationException {
    CheckUtil.checkWritable(this);
  }

  @Override
  @NotNull
  public synchronized PsiReference[] getReferences() {
    return SharedPsiElementImplUtil.getReferences(this);
  }

  @Override
  public void navigate(boolean requestFocus) {
    PsiNavigationSupport.getInstance().getDescriptor(this).navigate(requestFocus);
  }

  @Override
  public synchronized PsiElement findElementAt(int offset) {
    return getViewProvider().findElementAt(offset);
  }

  @Override
  public synchronized PsiReference findReferenceAt(int offset) {
    return getViewProvider().findReferenceAt(offset);
  }

  @Override
  @NotNull
  public char[] textToCharArray() {
    return CharArrayUtil.fromSequence(getViewProvider().getContents());
  }

  @Override
  public boolean isContentsLoaded() {
    return true;
  }

  @Override
  public void onContentReload() {
  }

  @Override
  public boolean isWritable() {
    return getViewProvider().getVirtualFile().isWritable();
  }

  @Override
  @NotNull
  public abstract PsiElement[] getChildren();

  @Override
  public PsiElement getFirstChild() {
    final PsiElement[] children = getChildren();
    return children.length == 0 ? null : children[0];
  }

  @Override
  public PsiElement getLastChild() {
    final PsiElement[] children = getChildren();
    return children.length == 0 ? null : children[children.length - 1];
  }

  @Override
  public TextRange getTextRange() {
    return new TextRange(0, getTextLength());
  }

  @Override
  public int getStartOffsetInParent() {
    return 0;
  }

  @Override
  public int getTextLength() {
    return getViewProvider().getContents().length();
  }

  @Override
  public int getTextOffset() {
    return 0;
  }

  @Override
  public boolean textMatches(@NotNull PsiElement element) {
    return textMatches(element.getText());
  }

  @Override
  public boolean textMatches(@NotNull CharSequence text) {
    return text.equals(getViewProvider().getContents());
  }

  @Override
  public boolean textContains(char c) {
    return getText().indexOf(c) >= 0;
  }

  @Override
  public PsiElement add(@NotNull PsiElement element) throws IncorrectOperationException {
    throw new IncorrectOperationException("Not implemented");
  }

  @Override
  public PsiElement addBefore(@NotNull PsiElement element, PsiElement anchor) throws IncorrectOperationException {
    throw new IncorrectOperationException("Not implemented");
  }

  @Override
  public PsiElement addAfter(@NotNull PsiElement element, PsiElement anchor) throws IncorrectOperationException {
    throw new IncorrectOperationException("Not implemented");
  }

  @Override
  public PsiElement addRange(PsiElement first, PsiElement last) throws IncorrectOperationException {
    throw new IncorrectOperationException("Not implemented");
  }

  @Override
  public final PsiElement addRangeBefore(@NotNull PsiElement first, @NotNull PsiElement last, PsiElement anchor)
    throws IncorrectOperationException {
    throw new IncorrectOperationException("Not implemented");
  }

  @Override
  public final PsiElement addRangeAfter(PsiElement first, PsiElement last, PsiElement anchor)
    throws IncorrectOperationException {
    throw new IncorrectOperationException("Not implemented");
  }

  @Override
  public void deleteChildRange(PsiElement first, PsiElement last) throws IncorrectOperationException {
    throw new IncorrectOperationException("Not implemented");
  }

  @Override
  public PsiElement replace(@NotNull PsiElement newElement) throws IncorrectOperationException {
    throw new IncorrectOperationException("Not implemented");
  }

  @Override
  public FileASTNode getNode() {
    return null;
  }

  public abstract LightPsiFileImpl copyLight(final FileViewProvider viewProvider);

  @Override
  public PsiElement getContext() {
    return FileContextUtil.getFileContext(this);
  }

  @Override
  public void markInvalidated() {
    myInvalidated = true;
  }
}
