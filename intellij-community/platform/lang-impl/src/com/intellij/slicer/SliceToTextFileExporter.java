/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.slicer;

import com.intellij.ide.ExporterToTextFile;
import com.intellij.usages.UsageViewSettings;
import com.intellij.util.SystemProperties;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author nik
 */
public class SliceToTextFileExporter implements ExporterToTextFile {
  private final SliceTreeBuilder myBuilder;
  @NotNull private final UsageViewSettings myUsageViewSettings;
  private final String myLineSeparator = SystemProperties.getLineSeparator();

  public SliceToTextFileExporter(@NotNull SliceTreeBuilder builder, @NotNull UsageViewSettings usageViewSettings) {
    myBuilder = builder;
    myUsageViewSettings = usageViewSettings;
  }

  @NotNull
  @Override
  public String getReportText() {
    StringBuilder buffer = new StringBuilder();
    appendChildren(buffer, myBuilder.getRootSliceNode(), "");
    return buffer.toString();
  }

  private void appendNode(StringBuilder buffer, SliceNode node, String indent) {
    buffer.append(indent).append(node.getNodeText()).append(myLineSeparator);

    appendChildren(buffer, node, indent + "    ");
  }

  private void appendChildren(StringBuilder buffer, SliceNode node, String indent) {
    List<SliceNode> cachedChildren = node.getCachedChildren();
    if (cachedChildren != null) {
      for (SliceNode child : cachedChildren) {
        appendNode(buffer, child, indent);
      }
    }
    else {
      buffer.append(indent).append("...").append(myLineSeparator);
    }
  }

  @NotNull
  @Override
  public String getDefaultFilePath() {
    return myUsageViewSettings.getExportFileName();
  }

  @Override
  public void exportedTo(@NotNull String filePath) {
    myUsageViewSettings.setExportFileName(filePath);
  }

  @Override
  public boolean canExport() {
    return !myBuilder.analysisInProgress;
  }
}
