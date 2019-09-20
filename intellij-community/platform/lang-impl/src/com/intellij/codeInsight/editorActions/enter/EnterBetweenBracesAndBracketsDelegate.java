// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.editorActions.enter;

public class EnterBetweenBracesAndBracketsDelegate extends EnterBetweenBracesDelegate {
  @Override
  protected boolean isBracePair(char lBrace, char rBrace) {
    return super.isBracePair(lBrace, rBrace ) || (lBrace == '[' && rBrace == ']');
  }
}
