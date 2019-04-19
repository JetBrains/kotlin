/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package com.intellij.ide.macro;

import com.intellij.ide.IdeBundle;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.ex.PathUtilEx;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public final class JdkPathMacro extends Macro {
  @Override
  public String getName() {
    return "JDKPath";
  }

  @Override
  public String getDescription() {
    return IdeBundle.message("macro.jdk.path");
  }

  @Override
  public String expand(DataContext dataContext) {
    Project project = CommonDataKeys.PROJECT.getData(dataContext);
    if (project == null) {
      return null;
    }
    final Sdk anyJdk = PathUtilEx.getAnyJdk(project);
    return sdkPath(anyJdk);
  }

  @Nullable
  static String sdkPath(@Nullable Sdk anyJdk) {
    if (anyJdk == null) {
      return null;
    }
    String jdkHomePath = PathUtil.getLocalPath(anyJdk.getHomeDirectory());
    if (jdkHomePath != null) {
      jdkHomePath = jdkHomePath.replace('/', File.separatorChar);
    }
    return jdkHomePath;
  }
}
