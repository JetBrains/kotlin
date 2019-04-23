// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.formatting;

/**
 * Marker interface for blocks which may not be modified by a formatter operation.
 * @deprecated The interface is not needed anymore, model blocks are not removed by formatter.
 */
@Deprecated
public interface ReadOnlyBlockContainer {
}
