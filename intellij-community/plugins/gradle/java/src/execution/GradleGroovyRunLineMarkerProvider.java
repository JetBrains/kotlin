// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.execution;

import com.intellij.execution.lineMarker.ExecutorAction;
import com.intellij.execution.lineMarker.RunLineMarkerContributor;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.impl.source.tree.LeafElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrMethodCallExpression;

import java.util.List;

import static com.intellij.openapi.util.text.StringUtil.join;
import static com.intellij.util.containers.ContainerUtil.mapNotNull;
import static org.jetbrains.plugins.gradle.execution.GradleRunnerUtil.isFromGroovyGradleScript;

/**
 * @author Vladislav.Soroka
 */
public class GradleGroovyRunLineMarkerProvider extends RunLineMarkerContributor {
  @Nullable
  @Override
  public Info getInfo(@NotNull final PsiElement element) {
    if (isFromGroovyGradleScript(element)) {
      if (element instanceof LeafElement && !(element instanceof PsiWhiteSpace) && !(element instanceof PsiComment)
          && element.getParent() instanceof GrReferenceExpression && element.getParent().getParent() instanceof GrMethodCallExpression) {
        List<String> tasks = GradleRunnerUtil.getTasksTarget(element);
        if (!tasks.isEmpty() && tasks.contains(element.getText().trim())) {
          final AnAction[] actions = ExecutorAction.getActions();
          return new Info(AllIcons.RunConfigurations.TestState.Run, actions,
                          e -> join(mapNotNull(actions, action -> getText(action, e)), "\n"));
        }
      }
    }
    return null;
  }
}