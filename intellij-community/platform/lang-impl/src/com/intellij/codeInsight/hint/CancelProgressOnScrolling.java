// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hint;

import com.intellij.openapi.editor.event.VisibleAreaEvent;
import com.intellij.openapi.editor.event.VisibleAreaListener;
import com.intellij.openapi.progress.ProgressIndicator;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

class CancelProgressOnScrolling implements VisibleAreaListener {
    private final ProgressIndicator myProgressIndicator;

    CancelProgressOnScrolling(ProgressIndicator indicator) {
        myProgressIndicator = indicator;
    }

    @Override
    public void visibleAreaChanged(@NotNull VisibleAreaEvent e) {
        Rectangle oldRect = e.getOldRectangle();
        Rectangle newRect = e.getNewRectangle();
        if (oldRect != null && (oldRect.x != newRect.x || oldRect.y != newRect.y)) {
            myProgressIndicator.cancel();
        }
    }
}
