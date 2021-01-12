// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.model.psi.impl;

import com.intellij.model.Pointer;
import com.intellij.model.Symbol;
import com.intellij.psi.PsiElement;
import com.intellij.psi.SmartPointerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class Psi2Symbol implements Symbol {

  private final @NotNull PsiElement myElement;
  private final @NotNull Pointer<Psi2Symbol> myPointer;

  Psi2Symbol(@NotNull PsiElement element) {
    this(element, new MyPointer(element));
  }

  Psi2Symbol(@NotNull PsiElement element, @NotNull Pointer<Psi2Symbol> pointer) {
    myElement = element;
    myPointer = pointer;
  }

  @NotNull
  PsiElement getElement() {
    return myElement;
  }

  @NotNull
  @Override
  public Pointer<Psi2Symbol> createPointer() {
    return myPointer;
  }

  private static final class MyPointer implements Pointer<Psi2Symbol> {

    private final @NotNull Pointer<? extends PsiElement> myPointer;

    private MyPointer(@NotNull PsiElement element) {
      myPointer = SmartPointerManager.createPointer(element);
    }

    @Nullable
    @Override
    public Psi2Symbol dereference() {
      PsiElement element = myPointer.dereference();
      if (element == null) {
        return null;
      }
      else {
        return new Psi2Symbol(element, this);
      }
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      MyPointer pointer = (MyPointer)o;

      if (!myPointer.equals(pointer.myPointer)) return false;

      return true;
    }

    @Override
    public int hashCode() {
      return myPointer.hashCode();
    }
  }
}
