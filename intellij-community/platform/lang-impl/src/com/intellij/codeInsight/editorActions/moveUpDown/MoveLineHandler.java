/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.codeInsight.editorActions.moveUpDown;

import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Dennis.Ushakov
 */
class MoveLineHandler extends BaseMoveHandler {
  MoveLineHandler(boolean down) {
    super(down);
  }

  @Override
  @Nullable
  protected MoverWrapper getSuitableMover(@NotNull final Editor editor, @Nullable final PsiFile file) {
    final StatementUpDownMover.MoveInfo info = new StatementUpDownMover.MoveInfo();
    info.indentTarget = false;
    if (LineMover.checkLineMoverAvailable(editor, info, isDown)) {
      final StatementUpDownMover mover = new LineMover();
      return new MoverWrapper(mover, info, isDown);
    }
    return null;
  }
}
