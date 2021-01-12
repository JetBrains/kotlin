// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hint;

import com.intellij.openapi.editor.event.VisibleAreaEvent;
import com.intellij.openapi.editor.event.VisibleAreaListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.concurrency.CancellablePromise;

import java.awt.*;
import java.util.concurrent.atomic.AtomicReference;

class CancelProgressOnScrolling implements VisibleAreaListener {
    private final AtomicReference<CancellablePromise<?>> myCancellablePromiseRef;

    CancelProgressOnScrolling(AtomicReference<CancellablePromise<?>> cancellablePromiseRef) {
        myCancellablePromiseRef = cancellablePromiseRef;
    }

    @Override
    public void visibleAreaChanged(@NotNull VisibleAreaEvent e) {
        Rectangle oldRect = e.getOldRectangle();
        Rectangle newRect = e.getNewRectangle();
        CancellablePromise<?> promise = myCancellablePromiseRef.get();
        if (oldRect != null && (oldRect.x != newRect.x || oldRect.y != newRect.y) && promise != null) {
            promise.cancel();
        }
    }
}
