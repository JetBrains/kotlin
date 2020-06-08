// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.model;

import com.intellij.notification.NotificationGroup;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.externalSystem.ExternalSystemUiAware;
import com.intellij.openapi.externalSystem.view.ExternalProjectsView;
import com.intellij.openapi.externalSystem.view.ExternalSystemNode;
import com.intellij.openapi.externalSystem.view.ProjectNode;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

/**
 * @author Denis Zhdanov
 */
public final class ExternalSystemDataKeys {

  @NotNull public static final DataKey<ProjectSystemId> EXTERNAL_SYSTEM_ID = DataKey.create("external.system.id");
  @NotNull public static final DataKey<NotificationGroup> NOTIFICATION_GROUP = DataKey.create("external.system.notification");
  @NotNull public static final DataKey<ExternalProjectsView> VIEW = DataKey.create("external.system.view");
  @NotNull public static final DataKey<ProjectNode> SELECTED_PROJECT_NODE = DataKey.create("external.system.selected.project.node");
  @NotNull public static final DataKey<List<ExternalSystemNode>> SELECTED_NODES = DataKey.create("external.system.selected.nodes");
  @NotNull public static final DataKey<ExternalSystemUiAware> UI_AWARE = DataKey.create("external.system.ui.aware");
  @NotNull public static final DataKey<JTree> PROJECTS_TREE = DataKey.create("external.system.tree");

  @NotNull public static final Key<Boolean> NEWLY_IMPORTED_PROJECT = new Key<>("external.system.newly.imported");
  @NotNull public static final Key<Boolean> NEWLY_CREATED_PROJECT = new Key<>("external.system.newly.created");

  private ExternalSystemDataKeys() {
  }
}
