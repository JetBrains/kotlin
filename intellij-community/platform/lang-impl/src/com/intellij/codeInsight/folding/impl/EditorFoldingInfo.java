// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.folding.impl;

import com.intellij.injected.editor.EditorWindow;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.FoldRegion;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.impl.source.tree.injected.FoldingRegionWindow;
import com.intellij.util.ObjectUtils;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;

/**
 * Holds {@code 'fold region -> PSI element'} mappings.
 */
public class EditorFoldingInfo {
  private static final Key<EditorFoldingInfo> KEY = Key.create("EditorFoldingInfo.KEY");
  private static final Object ourLock = ObjectUtils.sentinel("lock");

  private final Map<FoldRegion, SmartPsiElementPointer<?>> myFoldRegionToSmartPointerMap = Collections.synchronizedMap(new THashMap<>());

  @NotNull
  public static EditorFoldingInfo get(@NotNull Editor editor) {
    if (editor instanceof EditorWindow) return new EditorFoldingInfoWindow(get(((EditorWindow)editor).getDelegate()));

    synchronized (ourLock) {
      EditorFoldingInfo info = editor.getUserData(KEY);
      if (info == null){
        info = new EditorFoldingInfo();
        editor.putUserData(KEY, info);
      }
      return info;
    }
  }

  @Nullable
  public PsiElement getPsiElement(@NotNull FoldRegion region) {
    final SmartPsiElementPointer<?> pointer = myFoldRegionToSmartPointerMap.get(region);
    if (pointer == null) {
      return null;
    }
    PsiElement element = pointer.getElement();
    return element != null && element.isValid() ? element : null;
  }

  @Nullable
  TextRange getPsiElementRange(@NotNull FoldRegion region) {
    PsiElement element = getPsiElement(region);
    if (element == null) return null;
    PsiFile containingFile = element.getContainingFile();
    InjectedLanguageManager injectedManager = InjectedLanguageManager.getInstance(containingFile.getProject());
    boolean isInjected = injectedManager.isInjectedFragment(containingFile);
    TextRange range = element.getTextRange();
    if (isInjected) {
      range = injectedManager.injectedToHost(element, range);
    }
    return range;
  }

  void addRegion(@NotNull FoldRegion region, @NotNull SmartPsiElementPointer<?> pointer){
    myFoldRegionToSmartPointerMap.put(region, pointer);
  }

  public void removeRegion(@NotNull FoldRegion region){
    myFoldRegionToSmartPointerMap.remove(region);
  }

  public void dispose() {
    myFoldRegionToSmartPointerMap.clear();
  }

  private static class EditorFoldingInfoWindow extends EditorFoldingInfo {
    private final EditorFoldingInfo myDelegate;

    private EditorFoldingInfoWindow(EditorFoldingInfo delegate) {
      myDelegate = delegate;
    }

    @Nullable
    @Override
    public PsiElement getPsiElement(@NotNull FoldRegion region) {
      return myDelegate.getPsiElement(getHostRegion(region));
    }

    @Override
    void addRegion(@NotNull FoldRegion region, @NotNull SmartPsiElementPointer<?> pointer) {
      myDelegate.addRegion(getHostRegion(region), pointer);
    }

    @Override
    public void removeRegion(@NotNull FoldRegion region) {
      myDelegate.removeRegion(getHostRegion(region));
    }

    @Override
    public void dispose() {
      myDelegate.dispose();
    }

    private static FoldRegion getHostRegion(@NotNull FoldRegion injectedRegion) {
      return injectedRegion instanceof FoldingRegionWindow ? ((FoldingRegionWindow) injectedRegion).getDelegate() : injectedRegion;
    }
  }
}
