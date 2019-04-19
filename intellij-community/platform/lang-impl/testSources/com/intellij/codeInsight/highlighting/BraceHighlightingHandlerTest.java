// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.highlighting;

import com.intellij.injected.editor.EditorWindow;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.FileBasedTestCaseHelper;
import com.intellij.testFramework.LightPlatformCodeInsightTestCase;
import com.intellij.testFramework.TestDataPath;
import com.intellij.util.Alarm;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.intellij.testFramework.EdtTestUtil.runInEdtAndWait;

@RunWith(com.intellij.testFramework.Parameterized.class)
@TestDataPath("/testData/../../../platform/lang-impl/testData/editor/braceHighlighter/")
public class BraceHighlightingHandlerTest extends LightPlatformCodeInsightTestCase implements FileBasedTestCaseHelper {
  private static final String PAIR_MARKER = "<pair>";

  @Test
  public void testAction() {
    runInEdtAndWait(() -> {
      configureByFile(myFileSuffix);
      doTest(getProject(), getFile(), getEditor());
    });
  }

  public static void doTest(@NotNull Project project, @NotNull PsiFile psiFile, @NotNull Editor editor) {
    Editor hostEditor = editor instanceof EditorWindow ? ((EditorWindow)editor).getDelegate() : editor;
    final Document document = hostEditor.getDocument();
    int first = document.getText().indexOf(PAIR_MARKER);
    if (first >= 0) {
      WriteCommandAction.runWriteCommandAction(null, () -> document.replaceString(first, first + PAIR_MARKER.length(), ""));
    }
    int second;
    int secondCandidate = document.getText().indexOf(PAIR_MARKER);
    if (secondCandidate >= 0) {
      WriteCommandAction.runWriteCommandAction(null, () -> document.replaceString(secondCandidate, secondCandidate + PAIR_MARKER.length(), ""));
      second = secondCandidate;
    } else {
      second = hostEditor.getCaretModel().getOffset();
    }

    Alarm alarm = new Alarm();
    try {
      new BraceHighlightingHandler(project, (EditorEx)editor, alarm, psiFile).updateBraces();
      RangeHighlighter[] highlighters = editor.getMarkupModel().getAllHighlighters();
      int braceHighlighters = 0;
      for (RangeHighlighter highlighter : highlighters) {
        if (highlighter.getLayer() == BraceHighlightingHandler.LAYER) {
          braceHighlighters++;
          assertTrue(first == highlighter.getStartOffset() || second == highlighter.getStartOffset());
        }
      }
      assertEquals(first >= 0 ? 2 : 0, braceHighlighters);
    } finally {
      Disposer.dispose(alarm);
    }
  }

  @Nullable
  @Override
  public String getFileSuffix(String fileName) {
    return fileName;
  }
}