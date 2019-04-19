/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

/*
 * Author: max
 */

package com.intellij.codeInspection.ex;

import com.intellij.codeInspection.*;
import com.intellij.icons.AllIcons;
import com.intellij.ide.impl.ContentManagerWatcher;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiElement;
import com.intellij.ui.content.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.HashSet;
import java.util.Set;

public class InspectionManagerEx extends InspectionManagerBase {
  private final NotNullLazyValue<ContentManager> myContentManager;
  private final Set<GlobalInspectionContextImpl> myRunningContexts = new HashSet<>();

  public InspectionManagerEx(final Project project) {
    super(project);
    if (ApplicationManager.getApplication().isHeadlessEnvironment()) {
      myContentManager = new NotNullLazyValue<ContentManager>() {
        @NotNull
        @Override
        protected ContentManager compute() {
          ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
          toolWindowManager.registerToolWindow(ToolWindowId.INSPECTION, true, ToolWindowAnchor.BOTTOM, project);
          return ContentFactory.SERVICE.getInstance().createContentManager(new TabbedPaneContentUI(), true, project);
        }
      };
    }
    else {
      myContentManager = new NotNullLazyValue<ContentManager>() {
        @NotNull
        @Override
        protected ContentManager compute() {
          ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
          ToolWindow toolWindow = toolWindowManager.registerToolWindow(ToolWindowId.INSPECTION, true, ToolWindowAnchor.BOTTOM, project);
          ContentManager contentManager = toolWindow.getContentManager();
          toolWindow.setIcon(AllIcons.Toolwindows.ToolWindowInspection);
          new ContentManagerWatcher(toolWindow, contentManager);
          contentManager.addContentManagerListener(new ContentManagerAdapter() {
            private static final String PREFIX = "of ";

            @Override
            public void contentAdded(@NotNull ContentManagerEvent event) {
              handleContentSizeChanged();
            }

            @Override
            public void contentRemoved(@NotNull ContentManagerEvent event) {
              handleContentSizeChanged();
            }

            private void handleContentSizeChanged() {
              final int count = contentManager.getContentCount();
              if (count == 1) {
                final Content content = contentManager.getContent(0);
                final String displayName = content.getDisplayName();
                if (!content.getDisplayName().startsWith(PREFIX)) {
                  content.setDisplayName(PREFIX + displayName);
                }
              }
              else if (count > 1) {
                for (Content content : contentManager.getContents()) {
                  if (content.getDisplayName().startsWith(PREFIX)) {
                    content.setDisplayName(content.getDisplayName().substring(PREFIX.length()));
                  }
                }
              }
            }
          });
          return contentManager;
        }
      };
    }
  }

  @NotNull
  public ProblemDescriptor createProblemDescriptor(@NotNull final PsiElement psiElement,
                                                   @NotNull final String descriptionTemplate,
                                                   @NotNull final ProblemHighlightType highlightType,
                                                   @Nullable final HintAction hintAction,
                                                   boolean onTheFly,
                                                   @Nullable LocalQuickFix... fixes) {
    return new ProblemDescriptorImpl(psiElement, psiElement, descriptionTemplate, fixes, highlightType, false, null, hintAction, onTheFly);
  }

  @Override
  @NotNull
  public GlobalInspectionContextImpl createNewGlobalContext(boolean reuse) {
    return createNewGlobalContext();
  }

  @NotNull
  @Override
  public GlobalInspectionContextImpl createNewGlobalContext() {
    final GlobalInspectionContextImpl inspectionContext = new GlobalInspectionContextImpl(getProject(), myContentManager);
    myRunningContexts.add(inspectionContext);
    return inspectionContext;
  }

  public void setProfile(@NotNull String name) {
    myCurrentProfileName = name;
  }

  void closeRunningContext(@NotNull GlobalInspectionContextImpl globalInspectionContext){
    myRunningContexts.remove(globalInspectionContext);
  }

  @NotNull
  public Set<GlobalInspectionContextImpl> getRunningContexts() {
    return myRunningContexts;
  }

  @TestOnly
  @NotNull
  public NotNullLazyValue<ContentManager> getContentManager() {
    return myContentManager;
  }
}
