// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.editor;

import com.intellij.util.messages.Topic;

/**
 * Common interface of listeners that get notified on applying changes from editor-related settings pages.
 * !!!Important!!!
 * This is not a settings' listener, but the listener of a page these can be modified from,
 * so it doesn't respond to external storage changes or changes made from code.
 */
public interface EditorOptionsListener {
  Topic<EditorOptionsListener> FOLDING_CONFIGURABLE_TOPIC = Topic.create("CodeFoldingConfigurable changes applied", EditorOptionsListener.class);
  Topic<EditorOptionsListener> APPEARANCE_CONFIGURABLE_TOPIC = Topic.create("EditorAppearanceConfigurable changes applied", EditorOptionsListener.class);
  Topic<EditorOptionsListener> OPTIONS_PANEL_TOPIC = Topic.create("EditorOptionsPanel changes applied", EditorOptionsListener.class);
  Topic<EditorOptionsListener> SMART_KEYS_CONFIGURABLE_TOPIC = Topic.create("EditorSmartKeysConfigurable changes applied", EditorOptionsListener.class);
  Topic<EditorOptionsListener> GUTTER_ICONS_CONFIGURABLE_TOPIC = Topic.create("GutterIconsConfigurable changes applied", EditorOptionsListener.class);

  void changesApplied();
}
