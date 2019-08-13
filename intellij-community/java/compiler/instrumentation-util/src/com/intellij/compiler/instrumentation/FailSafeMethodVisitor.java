// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.instrumentation;

import org.jetbrains.org.objectweb.asm.AnnotationVisitor;
import org.jetbrains.org.objectweb.asm.Label;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.TypePath;

/**
 * To be used together with FailSafeClassReader: adds null checks for labels describing annotation visibility range.
 * For incorrectly generated annotations FailSafeClassReader returns null labels. Local variables annotations with null labels
 * will be ignored by this visitor.
 */
public class FailSafeMethodVisitor extends MethodVisitor {
  public FailSafeMethodVisitor(int api, MethodVisitor mv) {
    super(api, mv);
  }

  @Override
  public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index, String desc, boolean visible) {
    for (Label aStart : start) {
      if (aStart == null) {
        return null;
      }
    }
    for (Label anEnd : end) {
      if (anEnd == null) {
        return null;
      }
    }
    return super.visitLocalVariableAnnotation(typeRef, typePath, start, end, index, desc, visible);
  }
}
