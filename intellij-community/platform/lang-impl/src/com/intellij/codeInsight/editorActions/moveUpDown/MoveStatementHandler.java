// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.editorActions.moveUpDown;

import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class MoveStatementHandler extends BaseMoveHandler {

  MoveStatementHandler(boolean down) {
    super(down);
  }

  @Override
  @Nullable
  protected MoverWrapper getSuitableMover(@NotNull final Editor editor, @NotNull final PsiFile file) {
    // order is important!
    final StatementUpDownMover.MoveInfo info = new StatementUpDownMover.MoveInfo();
    for (final StatementUpDownMover mover : StatementUpDownMover.STATEMENT_UP_DOWN_MOVER_EP.getExtensionList()) {
      if (mover.checkAvailable(editor, file, info, isDown)) {
        return new MoverWrapper(mover, info, isDown);
      }
    }

    // order is important
    //Mover[] movers = new Mover[]{new StatementMover(isDown), new DeclarationMover(isDown), new XmlMover(isDown), new LineMover(isDown)};
    return null;
  }

}

