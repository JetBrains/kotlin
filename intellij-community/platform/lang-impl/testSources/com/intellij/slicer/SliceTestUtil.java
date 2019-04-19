/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.slicer;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.CommonProcessors;
import gnu.trove.THashMap;

import java.util.*;

import static junit.framework.TestCase.*;

public class SliceTestUtil {

  private SliceTestUtil() {
  }

  public static class Node {
    public final int myOffset;
    public final List<Node> myChildren;

    public Node(int offset, List<Node> children) {
      myOffset = offset;
      myChildren = children;
      myChildren.sort(Comparator.comparingInt(n -> n.myOffset));
    }

    @Override
    public String toString() {
      return myOffset + (myChildren.isEmpty() ? "" : " -> " + myChildren);
    }
  }

  public static Node buildTree(PsiElement startElement, Map<String, RangeMarker> sliceUsageName2Offset) {
    return buildNode("", startElement.getTextOffset(), sliceUsageName2Offset);
  }

  private static Node buildNode(String name, int offset, Map<String, RangeMarker> sliceUsageName2Offset) {
    List<Node> children = new ArrayList<>();
    for (int i = 1; i < 9; i++) {
      String newName = name + i;
      RangeMarker marker = sliceUsageName2Offset.get(newName);
      if (marker == null) break;
      children.add(buildNode(newName, marker.getStartOffset(), sliceUsageName2Offset));
    }
    return new Node(offset, children);
  }

  public static Map<String, RangeMarker> extractSliceOffsetsFromDocument(final Document document) {
    return extractSliceOffsetsFromDocuments(Collections.singletonList(document));
  }

  public static Map<String, RangeMarker> extractSliceOffsetsFromDocuments(final List<Document> documents) {
    Map<String, RangeMarker> sliceUsageName2Offset = new THashMap<>();

    extract(documents, sliceUsageName2Offset, "");

    for (Document document : documents) {
      int index = document.getText().indexOf("<flown");
      if(index!=-1) {
        fail(document.getText().substring(index, Math.min(document.getText().length(), index+50)));
      }
    }

    assertTrue(!sliceUsageName2Offset.isEmpty());
    return sliceUsageName2Offset;
  }

  private static void extract(final List<Document> documents, final Map<String, RangeMarker> sliceUsageName2Offset, final String name) {
    WriteCommandAction.runWriteCommandAction(null, () -> {
      for (int i = 1; i < 9; i++) {
        String newName = name + i;
        String s = "<flown" + newName + ">";

        boolean continueExtraction = false;
        for (Document document : documents) {
          if (!document.getText().contains(s)) continue;

          int off = document.getText().indexOf(s);

          document.deleteString(off, off + s.length());
          RangeMarker prev = sliceUsageName2Offset.put(newName, document.createRangeMarker(off, off));
          assertNull(prev);

          continueExtraction = true;
        }

        if (continueExtraction) {
          extract(documents, sliceUsageName2Offset, newName);
        }
      }
    });
  }

  public static void checkUsages(final SliceUsage usage, final Node tree) {
    final List<SliceUsage> children = new ArrayList<>();
    boolean b = ProgressManager.getInstance().runProcessWithProgressSynchronously(
      () -> usage.processChildren(new CommonProcessors.CollectProcessor<>(children)), "Expanding", true, usage.getElement().getProject());
    assertTrue(b);
    int startOffset = usage.getElement().getTextOffset();
    assertEquals(message(startOffset, usage), tree.myOffset, startOffset);
    List<Node> expectedChildren = tree.myChildren;

    int size = expectedChildren.size();
    assertEquals(message(startOffset, usage), size, children.size());
    children.sort(Comparator.naturalOrder());

    for (int i = 0; i < children.size(); i++) {
      checkUsages(children.get(i), expectedChildren.get(i));
    }
  }

  private static String message(int startOffset, SliceUsage usage) {
    PsiFile file = usage.getElement().getContainingFile();
    Editor editor = FileEditorManager.getInstance(file.getProject()).getSelectedTextEditor();
    LogicalPosition position = editor.offsetToLogicalPosition(startOffset);
    return position + ": '" + StringUtil.first(file.getText().substring(startOffset), 100, true) + "'";
  }
}
