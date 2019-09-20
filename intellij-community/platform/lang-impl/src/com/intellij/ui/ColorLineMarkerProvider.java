// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ui;

import com.intellij.codeHighlighting.Pass;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor;
import com.intellij.codeInsight.daemon.MergeableLineMarkerInfo;
import com.intellij.codeInsight.daemon.NavigateAction;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ElementColorProvider;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.util.Function;
import com.intellij.util.FunctionUtil;
import com.intellij.util.ui.ColorIcon;
import com.intellij.util.ui.ColorsIcon;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
public final class ColorLineMarkerProvider extends LineMarkerProviderDescriptor {
  public static final ColorLineMarkerProvider INSTANCE = new ColorLineMarkerProvider();

  @Override
  public LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement element) {
    return ElementColorProvider.EP_NAME.computeSafeIfAny(provider -> {
      Color color = provider.getColorFrom(element);
      if (color == null) {
        return null;
      }

      MyInfo info = new MyInfo(element, color, provider);
      NavigateAction.setNavigateAction(info, "Choose color", null);
      return info;
    });
  }

  @Override
  public String getName() {
    return "Color preview";
  }

  @NotNull
  @Override
  public Icon getIcon() {
    return AllIcons.Gutter.Colors;
  }

  private static class MyInfo extends MergeableLineMarkerInfo<PsiElement> {

    private final Color myColor;

    MyInfo(@NotNull final PsiElement element, final Color color, final ElementColorProvider colorProvider) {
      super(element,
            element.getTextRange(),
            JBUI.scale(new ColorIcon(12, color)),
            Pass.LINE_MARKERS,
            FunctionUtil.<Object, String>nullConstant(),
            (e, elt) -> {
              if (!elt.isWritable()) return;

              final Editor editor = PsiUtilBase.findEditor(elt);
              assert editor != null;

              if (Registry.is("ide.new.color.picker")) {
                ColorPicker.showColorPickerPopup(element.getProject(), color, (c, l) -> WriteAction.run(() -> colorProvider.setColorTo(elt, c)));
              } else {
                final Color c = ColorChooser.chooseColor(editor.getProject(), editor.getComponent(), "Choose Color", color, true);
                if (c != null) {
                  WriteAction.run(() -> colorProvider.setColorTo(elt, c));
                }
              }
            },
            GutterIconRenderer.Alignment.LEFT);
      myColor = color;
    }

    @Override
    public boolean canMergeWith(@NotNull MergeableLineMarkerInfo<?> info) {
      return info instanceof MyInfo;
    }

    @Override
    public Icon getCommonIcon(@NotNull List<MergeableLineMarkerInfo> infos) {
      return JBUI.scale(new ColorsIcon(12, infos.stream().map(_info -> ((MyInfo)_info).myColor).toArray(Color[]::new)));
    }

    @NotNull
    @Override
    public Function<? super PsiElement, String> getCommonTooltip(@NotNull List<MergeableLineMarkerInfo> infos) {
      return FunctionUtil.nullConstant();
    }
  }
}
