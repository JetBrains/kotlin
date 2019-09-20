// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

/*
 * @author max
 */
package com.intellij.ide.bookmarks;

import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;

public interface BookmarksListener {
  Topic<BookmarksListener> TOPIC = Topic.create("Bookmarks", BookmarksListener.class);

  default void bookmarkAdded(@NotNull Bookmark b) { }

  default void bookmarkRemoved(@NotNull Bookmark b) { }

  default void bookmarkChanged(@NotNull Bookmark b) { }

  default void bookmarksOrderChanged() { }
}