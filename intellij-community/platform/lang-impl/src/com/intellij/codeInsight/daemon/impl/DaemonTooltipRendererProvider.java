// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

/*
 * @author max
 */
package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeInsight.daemon.impl.tooltips.TooltipActionProvider;
import com.intellij.codeInsight.hint.LineTooltipRenderer;
import com.intellij.codeInsight.hint.TooltipRenderer;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.ErrorStripTooltipRendererProvider;
import com.intellij.openapi.editor.ex.TooltipAction;
import com.intellij.openapi.editor.impl.TrafficTooltipRenderer;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class DaemonTooltipRendererProvider implements ErrorStripTooltipRendererProvider {
  private final Project myProject;
  private final Editor myEditor;

  DaemonTooltipRendererProvider(final Project project, Editor editor) {
    myProject = project;
    myEditor = editor;
  }

  @Override
  public TooltipRenderer calcTooltipRenderer(@NotNull final Collection<? extends RangeHighlighter> highlighters) {
    LineTooltipRenderer bigRenderer = null;
    List<HighlightInfo> infos = new SmartList<>();
    Collection<String> tooltips = new THashSet<>(); //do not show same tooltip twice
    for (RangeHighlighter marker : highlighters) {
      final Object tooltipObject = marker.getErrorStripeTooltip();
      if (tooltipObject == null) continue;
      if (tooltipObject instanceof HighlightInfo) {
        HighlightInfo info = (HighlightInfo)tooltipObject;
        if (info.getToolTip() != null && tooltips.add(info.getToolTip())) {
          infos.add(info);
        }
      }
      else {
        final String text = tooltipObject.toString();
        if (tooltips.add(text)) {
          if (bigRenderer == null) {
            bigRenderer = new DaemonTooltipRenderer(text, new Object[]{highlighters});
          }
          else {
            bigRenderer.addBelow(text);
          }
        }
      }
    }
    if (!infos.isEmpty()) {
      // show errors first
      ContainerUtil.quickSort(infos, (o1, o2) -> {
        int i = SeverityRegistrar.getSeverityRegistrar(myProject).compare(o2.getSeverity(), o1.getSeverity());
        if (i != 0) return i;
        return o1.getToolTip().compareTo(o2.getToolTip());
      });
      final HighlightInfoComposite composite = HighlightInfoComposite.create(infos);
      String toolTip = composite.getToolTip();
      DaemonTooltipRenderer myRenderer;
      if (Registry.is("ide.tooltip.show.with.actions")) {
        TooltipAction action = TooltipActionProvider.calcTooltipAction(composite, myEditor);
        myRenderer = new DaemonTooltipWithActionRenderer(toolTip, action, 0,
                                                         action == null ? new Object[]{toolTip} : new Object[]{toolTip, action});
      }
      else {
        myRenderer = new DaemonTooltipRenderer(toolTip, new Object[]{highlighters});
      }
      if (bigRenderer != null) {
        myRenderer.addBelow(bigRenderer.getText());
      }
      bigRenderer = myRenderer;
    }
    return bigRenderer;
  }

  @NotNull
  @Override
  public TooltipRenderer calcTooltipRenderer(@NotNull final String text) {
    return new DaemonTooltipRenderer(text, new Object[]{text});
  }

  @NotNull
  @Override
  public TooltipRenderer calcTooltipRenderer(@NotNull final String text, final int width) {
    return new DaemonTooltipRenderer(text, width, new Object[]{text});
  }

  @NotNull
  @Override
  public TooltipRenderer calcTooltipRenderer(@NotNull String text, @Nullable TooltipAction action, int width) {
    if (action != null || Registry.is("ide.tooltip.show.with.actions")) {
      return new DaemonTooltipWithActionRenderer(text, action, width, action == null ? new Object[]{text} : new Object[]{text, action});
    }
    
    return ErrorStripTooltipRendererProvider.super.calcTooltipRenderer(text, action, width);
  }

  @NotNull
  @Override
  public TrafficTooltipRenderer createTrafficTooltipRenderer(@NotNull Runnable onHide, @NotNull Editor editor) {
    return new TrafficTooltipRendererImpl(onHide, editor);
  }
}