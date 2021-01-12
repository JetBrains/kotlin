/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package com.intellij.codeInsight.editorActions;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.Transferable;
import java.util.Collections;
import java.util.List;

/**
 * An extension to collect and apply additional transferable data when performing copy-paste in editors.<p/>
 * 
 * @author yole
 */
public abstract class CopyPastePostProcessor<T extends TextBlockTransferableData> {
  public static final ExtensionPointName<CopyPastePostProcessor<? extends TextBlockTransferableData>> EP_NAME = ExtensionPointName.create("com.intellij.copyPastePostProcessor");

  /**
   * This method will be run in the dispatch thread with alternative resolve enabled
   */
  @NotNull
  public abstract List<T> collectTransferableData(final PsiFile file, final Editor editor, final int[] startOffsets, final int[] endOffsets);

  @NotNull
  public List<T> extractTransferableData(final Transferable content) {
    return Collections.emptyList();
  }

  public void processTransferableData(final Project project, final Editor editor, final RangeMarker bounds, int caretOffset,
                                      Ref<Boolean> indented, final List<T> values) {
  }
}
