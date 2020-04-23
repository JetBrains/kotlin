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

import java.awt.*;
import java.awt.event.MouseEvent;

class DocRenderMouseEventBridge implements EditorMouseListener, EditorMouseMotionListener {
  private DocRenderer.EditorPane myCurrentPane;

  @Override
  public void mouseMoved(@NotNull EditorMouseEvent event) {
    if (event.getArea() != EditorMouseEventArea.EDITING_AREA) return;

    DocRenderer.EditorPane currentPane = redispatchEvent(event, MouseEvent.MOUSE_MOVED);
    if (currentPane == null) {
      restoreCursor();
    }
    else {
      ((EditorEx)event.getEditor()).setCustomCursor(DocRenderMouseEventBridge.class, currentPane.getCursor());
      if (currentPane != myCurrentPane) {
        if (myCurrentPane != null) {
          dispatchMouseExitEvent(myCurrentPane);
        }
        myCurrentPane = currentPane;
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
    if (myCurrentPane != null) {
      dispatchMouseExitEvent(myCurrentPane);
      ((EditorEx)myCurrentPane.getEditor()).setCustomCursor(DocRenderMouseEventBridge.class, null);
      myCurrentPane = null;
    }
  }

  @Nullable
  private static DocRenderer.EditorPane redispatchEvent(@NotNull EditorMouseEvent event, int eventId) {
    MouseEvent mouseEvent = event.getMouseEvent();
    Point mousePoint = mouseEvent.getPoint();
    Inlay inlay = event.getInlay();
    if (inlay != null) {
      EditorCustomElementRenderer renderer = inlay.getRenderer();
      if (renderer instanceof DocRenderer) {
        Rectangle relativeBounds = ((DocRenderer)renderer).getEditorPaneBoundsWithinInlay(inlay);
        Rectangle inlayBounds = inlay.getBounds();
        assert inlayBounds != null;
        int x = mousePoint.x - inlayBounds.x - relativeBounds.x;
        int y = mousePoint.y - inlayBounds.y - relativeBounds.y;
        if (x >= 0 && x < relativeBounds.width && y >= 0 && y < relativeBounds.height) {
          DocRenderer.EditorPane editorPane = ((DocRenderer)renderer).getRendererComponent(inlay, relativeBounds.width);
          int button = mouseEvent.getButton();
          dispatchEvent(editorPane, new MouseEvent(editorPane, eventId, 0, 0, x, y, mouseEvent.getClickCount(), false,
                                              // hack to process middle-button clicks (JEditorPane ignores them)
                                              button == MouseEvent.BUTTON2 ? MouseEvent.BUTTON1 : button));
          return editorPane;
        }
      }
    }
    return null;
  }

  private static void dispatchEvent(@NotNull DocRenderer.EditorPane editorPane, @NotNull MouseEvent event) {
    editorPane.doWithRepaintTracking(() -> AWTAccessor.getComponentAccessor().processEvent(editorPane, event));
  }

  private static void dispatchMouseExitEvent(@NotNull DocRenderer.EditorPane editorPane) {
    dispatchEvent(editorPane, new MouseEvent(editorPane, MouseEvent.MOUSE_EXITED, 0, 0, 0, 0, 0, false));
  }
}
