// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.documentation.render;

import com.intellij.openapi.editor.EditorCustomElementRenderer;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseEventArea;
import com.intellij.openapi.editor.event.EditorMouseListener;
import com.intellij.openapi.editor.event.EditorMouseMotionListener;
import com.intellij.openapi.editor.ex.EditorEx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sun.awt.AWTAccessor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

class DocRenderMouseEventBridge implements EditorMouseListener, EditorMouseMotionListener {
  private Inlay<? extends DocRenderer> myCurrentInlay;

  @Override
  public void mouseMoved(@NotNull EditorMouseEvent event) {
    if (event.getArea() != EditorMouseEventArea.EDITING_AREA) return;

    Inlay<? extends DocRenderer> currentInlay = redispatchEvent(event, MouseEvent.MOUSE_MOVED);
    if (currentInlay == null) {
      restoreCursor();
    }
    else {
      ((EditorEx)event.getEditor()).setCustomCursor(DocRenderMouseEventBridge.class, currentInlay.getRenderer().myPane.getCursor());
      if (currentInlay != myCurrentInlay) {
        if (myCurrentInlay != null) {
          dispatchMouseExitEvent(myCurrentInlay);
        }
        myCurrentInlay = currentInlay;
      }
    }
  }

  @Override
  public void mouseExited(@NotNull EditorMouseEvent event) {
    if (event.getArea() != EditorMouseEventArea.EDITING_AREA) return;

    restoreCursor();
  }

  @Override
  public void mouseClicked(@NotNull EditorMouseEvent event) {
    if (event.getArea() != EditorMouseEventArea.EDITING_AREA) return;

    redispatchEvent(event, MouseEvent.MOUSE_CLICKED);
  }

  private void restoreCursor() {
    if (myCurrentInlay != null) {
      dispatchMouseExitEvent(myCurrentInlay);
      ((EditorEx)myCurrentInlay.getEditor()).setCustomCursor(DocRenderMouseEventBridge.class, null);
      myCurrentInlay = null;
    }
  }

  @SuppressWarnings("unchecked")
  @Nullable
  private static Inlay<? extends DocRenderer> redispatchEvent(@NotNull EditorMouseEvent event, int eventId) {
    MouseEvent mouseEvent = event.getMouseEvent();
    Point mousePoint = mouseEvent.getPoint();
    Inlay inlay = event.getInlay();
    if (inlay != null) {
      EditorCustomElementRenderer renderer = inlay.getRenderer();
      if (renderer instanceof DocRenderer) {
        Point relativeLocation = ((DocRenderer)renderer).getEditorPaneLocationWithinInlay();
        Rectangle inlayBounds = inlay.getBounds();
        assert inlayBounds != null;
        int x = mousePoint.x - inlayBounds.x - relativeLocation.x;
        int y = mousePoint.y - inlayBounds.y - relativeLocation.y;
        JEditorPane editorPane = ((DocRenderer)renderer).myPane;
        if (x >= 0 && x < editorPane.getWidth() && y >= 0 && y < editorPane.getHeight()) {
          int button = mouseEvent.getButton();
          dispatchEvent(inlay, new MouseEvent(editorPane, eventId, 0, 0, x, y, mouseEvent.getClickCount(), false,
                                              // hack to process middle-button clicks (JEditorPane ignores them)
                                              button == MouseEvent.BUTTON2 ? MouseEvent.BUTTON1 : button));
          return inlay;
        }
      }
    }
    return null;
  }

  private static void dispatchEvent(@NotNull Inlay<? extends DocRenderer> inlay, @NotNull MouseEvent event) {
    DocRenderer renderer = inlay.getRenderer();
    renderer.doWithRepaintTracking(() -> AWTAccessor.getComponentAccessor().processEvent(renderer.myPane, event));
  }

  private static void dispatchMouseExitEvent(@NotNull Inlay<? extends DocRenderer> inlay) {
    dispatchEvent(inlay, new MouseEvent(inlay.getRenderer().myPane, MouseEvent.MOUSE_EXITED, 0, 0, 0, 0, 0, false));
  }
}
