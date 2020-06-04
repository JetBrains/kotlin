/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.psi.impl.source.tree.injected;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.diagnostic.Attachment;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.LiteralTextEscaper;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.DebugUtil;
import com.intellij.psi.impl.source.tree.*;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

class LeafPatcher extends RecursiveTreeElementWalkingVisitor {
  private static final Logger LOG = Logger.getInstance(LeafPatcher.class);
  private int shredNo;
  private String hostText;
  private LiteralTextEscaper currentTextEscaper;
  private TextRange rangeInHost;
  private final Map<LeafElement, String> newTexts = new THashMap<>();
  @NotNull
  private final List<? extends PlaceInfo> myPlaceInfos;
  private final StringBuilder catLeafs;
  private final StringBuilder tempLeafBuffer = new StringBuilder();

  LeafPatcher(@NotNull List<? extends PlaceInfo> placeInfos, int approxTextLength) {
    myPlaceInfos = placeInfos;
    catLeafs = new StringBuilder(approxTextLength);
  }

  @Override
  public void visitLeaf(LeafElement leaf) {
    String leafText = leaf instanceof ForeignLeafPsiElement ? "" : leaf.getText();
    catLeafs.append(leafText);
    final TextRange leafRange = leaf.getTextRange();

    StringBuilder leafEncodedText = constructTextFromHostPSI(leafRange.getStartOffset(), leafRange.getEndOffset());

    if (!Comparing.equal(leafText, leafEncodedText)) {
      newTexts.put(leaf, leafEncodedText.toString());
      storeUnescapedTextFor(leaf, leafText);
    }
  }

  private StringBuilder constructTextFromHostPSI(int startOffset, int endOffset) {
    boolean firstTimer = false;
    PlaceInfo currentPlace = myPlaceInfos.get(shredNo);
    if (hostText == null) {
      hostText = currentPlace.myHostText;
      rangeInHost = currentPlace.getRelevantRangeInsideHost();
      currentTextEscaper = currentPlace.myEscaper;
      firstTimer = true;
    }

    StringBuilder text = tempLeafBuffer;
    text.setLength(0);
    while (startOffset < endOffset) {
      TextRange shredRange = currentPlace.rangeInDecodedPSI;
      String prefix = currentPlace.prefix;
      if (startOffset >= shredRange.getEndOffset()) {
        currentPlace = myPlaceInfos.get(++shredNo);
        hostText = currentPlace.myHostText;
        currentTextEscaper = currentPlace.myEscaper;
        rangeInHost = currentPlace.getRelevantRangeInsideHost();
        firstTimer = true;
        continue;
      }
      assert startOffset >= shredRange.getStartOffset();
      if (startOffset - shredRange.getStartOffset() < prefix.length()) {
        // inside prefix
        TextRange rangeInPrefix = new TextRange(startOffset - shredRange.getStartOffset(), Math.min(prefix.length(), endOffset - shredRange.getStartOffset()));
        text.append(prefix, rangeInPrefix.getStartOffset(), rangeInPrefix.getEndOffset());
        startOffset += rangeInPrefix.getLength();
        continue;
      }

      String suffix = currentPlace.suffix;
      if (startOffset < shredRange.getEndOffset() - suffix.length()) {
        // inside host body, cut out from the host text
        int startOffsetInHost = currentTextEscaper.getOffsetInHost(startOffset - shredRange.getStartOffset() - prefix.length(), rangeInHost);
        int endOffsetCut = Math.min(endOffset, shredRange.getEndOffset() - suffix.length());
        int endOffsetInHost = currentTextEscaper.getOffsetInHost(endOffsetCut - shredRange.getStartOffset() - prefix.length(), rangeInHost);
        if (endOffsetInHost != -1) {
          if (firstTimer) text.append(hostText, rangeInHost.getStartOffset(), startOffsetInHost);
          text.append(hostText, startOffsetInHost, endOffsetInHost);
          startOffset = endOffsetCut;
          // todo what about lastTimer?
        }
        else {
          LOG.error("Text escaper " + currentTextEscaper +" ("+currentTextEscaper.getClass()+") " +
                    "returned -1 in 'getOffsetInHost("+(endOffsetCut - shredRange.getStartOffset() - prefix.length())+", new TextRange"+rangeInHost+")' " +
                    "for "+currentPlace.host.getClass(),
                    new Attachment("host", StringUtil.first(currentPlace.host.getText(), 100, true)));
        }

        continue;
      }

      // inside suffix
      TextRange rangeInSuffix = new TextRange(suffix.length() - shredRange.getEndOffset() + startOffset, Math.min(suffix.length(), endOffset + suffix.length() - shredRange.getEndOffset()));
      text.append(suffix, rangeInSuffix.getStartOffset(), rangeInSuffix.getEndOffset());
      startOffset += rangeInSuffix.getLength();
    }

    return text;
  }

  static final Key<String> UNESCAPED_TEXT = Key.create("INJECTED_UNESCAPED_TEXT");
  private static void storeUnescapedTextFor(@NotNull LeafElement leaf, @NotNull String leafText) {
    PsiElement psi = leaf.getPsi();
    if (psi != null) {
      psi.putCopyableUserData(UNESCAPED_TEXT, leafText);
    }
  }

  void patch(@NotNull ASTNode parsedNode, @NotNull List<? extends PlaceInfo> placeInfos) {
    ((TreeElement)parsedNode).acceptTree(this);

    assert ((TreeElement)parsedNode).textMatches(catLeafs) :
      "Malformed PSI structure: leaf texts do not add up to the whole file text." +
      "\nFile text (from tree)  :'" + parsedNode.getText() + "'" +
      "\nFile text (from PSI)   :'" + parsedNode.getPsi().getText() + "'" +
      "\nLeaf texts concatenated:'" + catLeafs + "';" +
      "\nFile root: " + parsedNode +
      "\nLanguage: " + parsedNode.getPsi().getLanguage() +
      "\nHost file: " + placeInfos.get(0).host.getContainingFile().getVirtualFile();
    DebugUtil.performPsiModification("injection leaf patching", () -> {
      for (Map.Entry<LeafElement, String> entry : newTexts.entrySet()) {
        LeafElement leaf = entry.getKey();
        String newText = entry.getValue();
        leaf.rawReplaceWithText(newText);
      }
    });

    TreeUtil.clearCaches((TreeElement)parsedNode);
  }
}
