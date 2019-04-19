/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
public class ExternalSystemDataKeys {

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
