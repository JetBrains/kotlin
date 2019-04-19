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
package com.intellij.compiler;

import com.intellij.compiler.progress.CompilerTask;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompilerMessage;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.UUID;

/**
 * @author Eugene Zhuravlev
 */
public abstract class ProblemsView {

  protected final Project myProject;

  public static class SERVICE {
    private SERVICE() {
    }

    public static ProblemsView getInstance(Project project) {
      return ServiceManager.getService(project, ProblemsView.class);
    }
  }

  protected ProblemsView(Project project) {
    myProject = project;
  }

  public abstract void clearOldMessages(CompileScope scope, UUID currentSessionId);

  public abstract void addMessage(int type, @NotNull String[] text, @Nullable String groupName, @Nullable Navigatable navigatable, @Nullable String exportTextPrefix, @Nullable String rendererTextPrefix, @NotNull UUID sessionId);

  public final void addMessage(CompilerMessage message, @NotNull UUID sessionId) {
    final VirtualFile file = message.getVirtualFile();
    Navigatable navigatable = message.getNavigatable();
    if (navigatable == null && file != null && !file.getFileType().isBinary()) {
      navigatable = new OpenFileDescriptor(myProject, file, -1, -1);
    }
    final CompilerMessageCategory category = message.getCategory();
    final int type = CompilerTask.translateCategory(category);
    final String[] text = convertMessage(message);
    final String groupName = file != null? file.getPresentableUrl() : category.getPresentableText();
    addMessage(type, text, groupName, navigatable, message.getExportTextPrefix(), message.getRenderTextPrefix(), sessionId);
  }

  public abstract void setProgress(String text, float fraction);
  
  public abstract void setProgress(String text);

  public abstract void clearProgress();

  private static String[] convertMessage(final CompilerMessage message) {
    String text = message.getMessage();
    if (!text.contains("\n")) {
      return new String[]{text};
    }
    final List<String> lines = new ArrayList<>();
    StringTokenizer tokenizer = new StringTokenizer(text, "\n", false);
    while (tokenizer.hasMoreTokens()) {
      lines.add(tokenizer.nextToken());
    }
    return ArrayUtil.toStringArray(lines);
  }
  
}
