/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.codeInspection.ui.actions.suppress;

import com.intellij.codeInspection.*;
import com.intellij.codeInspection.ex.GlobalInspectionContextImpl;
import com.intellij.codeInspection.ex.InspectionManagerEx;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.codeInspection.ui.SuppressableInspectionTreeNode;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.SequentialTask;
import com.intellij.util.ThrowableRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * @author Dmitry Batkovich
 */
public class SuppressActionSequentialTask implements SequentialTask {
  private static final Logger LOG = Logger.getInstance(SuppressActionSequentialTask.class);

  private final SuppressableInspectionTreeNode[] myNodesToSuppress;
  @NotNull private final SuppressIntentionAction mySuppressAction;
  @NotNull private final InspectionToolWrapper myWrapper;
  private int myCount = 0;

  public SuppressActionSequentialTask(SuppressableInspectionTreeNode @NotNull [] nodesToSuppress,
                                      @NotNull SuppressIntentionAction suppressAction,
                                      @NotNull InspectionToolWrapper wrapper) {
    myNodesToSuppress = nodesToSuppress;
    mySuppressAction = suppressAction;
    myWrapper = wrapper;
  }

  @Override
  public boolean iteration() {
    return true;
  }

  @Override
  public boolean iteration(@NotNull ProgressIndicator indicator) {
    final SuppressableInspectionTreeNode node = myNodesToSuppress[myCount++];
    indicator.setFraction((double)myCount / myNodesToSuppress.length);

    final Pair<PsiElement, CommonProblemDescriptor> content = node.getSuppressContent();
    if (content.first != null) {
      suppress(content.first, content.second, mySuppressAction, myWrapper, node);
    }

    return isDone();
  }

  @Override
  public boolean isDone() {
    return myCount > myNodesToSuppress.length - 1;
  }

  @Override
  public void prepare() {
    final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    if (indicator != null) {
      indicator.setText(InspectionsBundle.message("inspection.action.suppress", myWrapper.getDisplayName()));
    }
  }

  private void suppress(@NotNull final PsiElement element,
                        @Nullable final CommonProblemDescriptor descriptor,
                        @NotNull final SuppressIntentionAction action,
                        @NotNull InspectionToolWrapper wrapper,
                        @NotNull final SuppressableInspectionTreeNode node) {
    if (action instanceof SuppressIntentionActionFromFix && !(descriptor instanceof ProblemDescriptor)) {
      LOG.info("local suppression fix for specific problem descriptor:  " + wrapper.getTool().getClass().getName());
    }

    final Project project = element.getProject();
    try {

      PsiElement container = null;
      if (action instanceof SuppressIntentionActionFromFix) {
        container = ((SuppressIntentionActionFromFix)action).getContainer(element);
      }
      if (container == null) {
        container = element;
      }

      if (action.isAvailable(project, null, element)) {
        ThrowableRunnable<RuntimeException> runnable = () -> action.invoke(project, null, element);
        if (action.startInWriteAction()) {
          WriteAction.run(runnable);
        }
        else {
          runnable.run();
        }
      }
      final Set<GlobalInspectionContextImpl> globalInspectionContexts =
        ((InspectionManagerEx)InspectionManager.getInstance(element.getProject())).getRunningContexts();
      for (GlobalInspectionContextImpl context : globalInspectionContexts) {
        context.resolveElement(wrapper.getTool(), container);
        if (descriptor != null) {
          context.getPresentation(wrapper).suppressProblem(descriptor);
        }
      }
    }
    catch (IncorrectOperationException e1) {
      LOG.error(e1);
    }

    node.removeSuppressActionFromAvailable(mySuppressAction);
  }
}