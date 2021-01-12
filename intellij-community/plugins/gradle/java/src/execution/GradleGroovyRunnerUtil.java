// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.execution;

import com.intellij.execution.Location;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.service.resolve.GradleResolverUtil;
import org.jetbrains.plugins.gradle.settings.GradleExtensionsSettings;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrApplicationStatement;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrLiteral;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrMethodCallExpression;
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil;

import java.util.Collections;
import java.util.List;

public class GradleGroovyRunnerUtil {
  @NotNull
  public static List<String> getTasksTarget(@NotNull PsiElement element, @Nullable Module module) {
    PsiElement parent = element;
    while (parent.getParent() != null && !(parent.getParent() instanceof PsiFile)) {
      parent = parent.getParent();
    }

    if (isCreateTaskMethod(parent)) {
      final GrExpression[] arguments = ((GrMethodCallExpression)parent).getExpressionArguments();
      if (arguments.length > 0 && arguments[0] instanceof GrLiteral && ((GrLiteral)arguments[0]).getValue() instanceof String) {
        return Collections.singletonList((String)((GrLiteral)arguments[0]).getValue());
      }
    }
    else if (parent instanceof GrApplicationStatement) {
      PsiElement shiftExpression = parent.getChildren()[1].getChildren()[0];
      if (GradleResolverUtil.isLShiftElement(shiftExpression)) {
        PsiElement shiftiesChild = shiftExpression.getChildren()[0];
        if (shiftiesChild instanceof GrReferenceExpression) {
          return Collections.singletonList(shiftiesChild.getText());
        }
        else if (shiftiesChild instanceof GrMethodCallExpression) {
          return Collections.singletonList(shiftiesChild.getChildren()[0].getText());
        }
      }
      else if (shiftExpression instanceof GrMethodCallExpression) {
        return Collections.singletonList(shiftExpression.getChildren()[0].getText());
      }
    }
    GrMethodCallExpression methodCallExpression = PsiTreeUtil.getParentOfType(element, GrMethodCallExpression.class);
    if (methodCallExpression != null) {
      String taskNameCandidate = methodCallExpression.getChildren()[0].getText();
      Project project = element.getProject();
      if (module == null) {
        module = getModule(element, project);
      }
      GradleExtensionsSettings.GradleExtensionsData extensionsData = GradleExtensionsSettings.getInstance(project).getExtensionsFor(module);
      if (extensionsData != null) {
        GradleExtensionsSettings.GradleTask gradleTask = extensionsData.tasksMap.get(taskNameCandidate);
        if (gradleTask != null) {
          return Collections.singletonList(taskNameCandidate);
        }
      }
    }

    return Collections.emptyList();
  }

  @Nullable
  private static Module getModule(@NotNull PsiElement element, @NotNull Project project) {
    PsiFile containingFile = element.getContainingFile();
    if (containingFile != null) {
      VirtualFile virtualFile = containingFile.getVirtualFile();
      if (virtualFile != null) {
        return ProjectFileIndex.SERVICE.getInstance(project).getModuleForFile(virtualFile);
      }
    }
    return null;
  }

  private static boolean isCreateTaskMethod(PsiElement parent) {
    return parent instanceof GrMethodCallExpression && PsiUtil.isMethodCall((GrMethodCallExpression)parent, "createTask");
  }

  @NotNull
  public static List<String> getTasksTarget(@Nullable Location location) {
    if (location == null) return Collections.emptyList();
    if (location instanceof GradleTaskLocation) {
      return ((GradleTaskLocation)location).getTasks();
    }

    Module module = location.getModule();
    return getTasksTarget(location.getPsiElement(), module);
  }

  @NotNull
  public static List<String> getTasksTarget(@NotNull PsiElement element) {
    return getTasksTarget(element, null);
  }
}
