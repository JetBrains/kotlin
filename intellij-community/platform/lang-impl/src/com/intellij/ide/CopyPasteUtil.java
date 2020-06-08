// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide;

import com.intellij.ide.util.treeView.AbstractTreeUpdater;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.Transferable;
import java.util.function.Consumer;

import static com.intellij.openapi.application.ApplicationManager.getApplication;

public final class CopyPasteUtil {
  private CopyPasteUtil() { }

  public static PsiElement[] getElementsInTransferable(Transferable t) {
    final PsiElement[] elts = PsiCopyPasteManager.getElements(t);
    return elts != null ? elts : PsiElement.EMPTY_ARRAY;
  }

  public static void addDefaultListener(@NotNull Disposable parent, @NotNull Consumer<? super PsiElement> consumer) {
    CopyPasteManager.getInstance().addContentChangedListener(new DefaultCopyPasteListener(consumer), parent);
  }

  public static class DefaultCopyPasteListener implements CopyPasteManager.ContentChangedListener {
    private final Consumer<? super PsiElement> consumer;

    /**
     * @deprecated use {@link #DefaultCopyPasteListener(Consumer)}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval(inVersion = "2020.2")
    public DefaultCopyPasteListener(AbstractTreeUpdater updater) {
      this(element -> updater.addSubtreeToUpdateByElement(element));
    }

    private DefaultCopyPasteListener(@NotNull Consumer<? super PsiElement> consumer) {
      this.consumer = consumer;
    }

    @Override
    public void contentChanged(final Transferable oldTransferable, final Transferable newTransferable) {
      Application application = getApplication();
      if (application == null || application.isReadAccessAllowed()) {
        updateByTransferable(oldTransferable);
        updateByTransferable(newTransferable);
      }
      else {
        application.runReadAction(() -> {
          updateByTransferable(oldTransferable);
          updateByTransferable(newTransferable);
        });
      }
    }

    private void updateByTransferable(final Transferable t) {
      PsiElement[] psiElements = getElementsInTransferable(t);
      for (PsiElement psiElement : psiElements) {
        if (!psiElement.getProject().isDisposed()) {
          consumer.accept(psiElement);
        }
      }
    }
  }
}
