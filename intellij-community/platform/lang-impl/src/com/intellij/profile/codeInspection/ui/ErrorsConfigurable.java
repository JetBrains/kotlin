/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package com.intellij.profile.codeInspection.ui;

import com.intellij.codeInspection.ex.InspectionProfileImpl;
import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nullable;

/**
 * Marker interface for the configurable which is used to configure the current inspection profile.
 *
 * @author yole
 */
public interface ErrorsConfigurable extends Configurable {
  void selectProfile(InspectionProfileImpl profile);

  void selectInspectionTool(final String selectedToolShortName);

  void selectInspectionGroup(final String[] groupPath);

  @Nullable
  Object getSelectedObject();
}
