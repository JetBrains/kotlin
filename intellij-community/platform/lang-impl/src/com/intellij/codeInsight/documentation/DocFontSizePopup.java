// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.documentation;

import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.options.FontSize;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.reference.SoftReference;
import com.intellij.ui.JBColor;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.lang.ref.WeakReference;

public final class DocFontSizePopup {
  private static WeakReference<JSlider> ourCurrentSlider;

  public static void show(@NotNull Runnable changeCallback, @NotNull Component parentComponent) {
    JSlider slider = new JSlider(SwingConstants.HORIZONTAL, 0, FontSize.values().length - 1, 3);
    slider.setOpaque(true);
    slider.setMinorTickSpacing(1);
    slider.setPaintTicks(true);
    slider.setPaintTrack(true);
    slider.setSnapToTicks(true);
    UIUtil.setSliderIsFilled(slider, true);
    updateSliderPosition(slider);
    slider.addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        DocumentationComponent.setQuickDocFontSize(FontSize.values()[slider.getValue()]);
        changeCallback.run();
      }
    });
    ourCurrentSlider = new WeakReference<>(slider);

    JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
    panel.setOpaque(true);
    panel.add(new JLabel(ApplicationBundle.message("label.font.size")));
    panel.add(slider);
    panel.setBorder(BorderFactory.createLineBorder(JBColor.border(), 1));

    JBPopup popup = JBPopupFactory.getInstance().createComponentPopupBuilder(panel, slider).createPopup();
    Point location = MouseInfo.getPointerInfo().getLocation();
    popup.show(new RelativePoint(new Point(location.x - panel.getPreferredSize().width / 2,
                                           location.y - panel.getPreferredSize().height / 2)).getPointOn(parentComponent));
  }

  public static void update() {
    JSlider slider = SoftReference.dereference(ourCurrentSlider);
    if (slider != null && slider.isShowing()) updateSliderPosition(slider);
  }

  private static void updateSliderPosition(@NotNull JSlider slider) {
    slider.setValue(DocumentationComponent.getQuickDocFontSize().ordinal());
  }
}
