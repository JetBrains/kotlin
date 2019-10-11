// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.daemon.impl;

import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.xml.util.XmlStringUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

class HighlightInfoComposite extends HighlightInfo {
  @NonNls private static final String LINE_BREAK = "<hr size=1 noshade>";

  static HighlightInfoComposite create(@NotNull List<? extends HighlightInfo> infos) {
    // derive composite's offsets from an info with tooltip, if present
    HighlightInfo anchorInfo = ContainerUtil.find(infos, info -> info.getToolTip() != null);
    if (anchorInfo == null) anchorInfo = infos.get(0);
    return new HighlightInfoComposite(infos, anchorInfo);
  }

  private HighlightInfoComposite(@NotNull List<? extends HighlightInfo> infos, @NotNull HighlightInfo anchorInfo) {
    super(null, null, anchorInfo.type, anchorInfo.startOffset, anchorInfo.endOffset,
          createCompositeDescription(infos), createCompositeTooltip(infos), anchorInfo.type.getSeverity(null), false, null, false, 0,
          anchorInfo.getProblemGroup(), null, anchorInfo.getGutterIconRenderer(), anchorInfo.getGroup());
    highlighter = anchorInfo.getHighlighter();
    List<Pair<IntentionActionDescriptor, RangeMarker>> markers = ContainerUtil.emptyList();
    List<Pair<IntentionActionDescriptor, TextRange>> ranges = ContainerUtil.emptyList();
    for (HighlightInfo info : infos) {
      if (info.quickFixActionMarkers != null) {
        if (markers == ContainerUtil.<Pair<IntentionActionDescriptor, RangeMarker>>emptyList()) markers = new ArrayList<>();
        markers.addAll(info.quickFixActionMarkers);
      }
      if (info.quickFixActionRanges != null) {
        if (ranges == ContainerUtil.<Pair<IntentionActionDescriptor, TextRange>>emptyList()) ranges = new ArrayList<>();
        ranges.addAll(info.quickFixActionRanges);
      }
    }
    quickFixActionMarkers = ContainerUtil.createLockFreeCopyOnWriteList(markers);
    quickFixActionRanges = ContainerUtil.createLockFreeCopyOnWriteList(ranges);
  }

  @Nullable
  private static String createCompositeDescription(List<? extends HighlightInfo> infos) {
    StringBuilder description = new StringBuilder();
    boolean isNull = true;
    for (HighlightInfo info : infos) {
      String itemDescription = info.getDescription();
      if (itemDescription != null) {
        itemDescription = itemDescription.trim();
        description.append(itemDescription);
        if (!itemDescription.endsWith(".")) {
          description.append('.');
        }
        description.append(' ');

        isNull = false;
      }
    }
    return isNull ? null : description.toString();
  }

  @Nullable
  private static String createCompositeTooltip(@NotNull List<? extends HighlightInfo> infos) {
    StringBuilder result = new StringBuilder();
    for (HighlightInfo info : infos) {
      String toolTip = info.getToolTip();
      if (toolTip != null) {
        if (result.length() != 0) {
          result.append(LINE_BREAK);
        }
        toolTip = XmlStringUtil.stripHtml(toolTip);
        result.append(toolTip);
      }
    }
    if (result.length() == 0) {
      return null;
    }
    return XmlStringUtil.wrapInHtml(result);
  }
}
