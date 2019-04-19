// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.execution;

import com.intellij.codeInsight.TestFrameworks;
import com.intellij.execution.Location;
import com.intellij.execution.junit2.PsiMemberParameterizedLocation;
import com.intellij.execution.junit2.info.MethodLocation;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.model.data.BuildParticipant;
import org.jetbrains.plugins.gradle.service.resolve.GradleResolverUtil;
import org.jetbrains.plugins.gradle.settings.CompositeDefinitionSource;
import org.jetbrains.plugins.gradle.settings.GradleExtensionsSettings;
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings;
import org.jetbrains.plugins.gradle.settings.GradleSettings;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrApplicationStatement;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrLiteral;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrMethodCallExpression;
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public class GradleRunnerUtil {

  public static boolean isGradleModule(@NotNull Module module) {
    return ExternalSystemApiUtil.isExternalSystemAwareModule(GradleConstants.SYSTEM_ID, module);
  }

  @Nullable
  public static Location<PsiMethod> getMethodLocation(@NotNull Location contextLocation) {
    Location<PsiMethod> methodLocation = getTestMethod(contextLocation);
    if (methodLocation == null) return null;

    if (contextLocation instanceof PsiMemberParameterizedLocation) {
      PsiClass containingClass = ((PsiMemberParameterizedLocation)contextLocation).getContainingClass();
      if (containingClass != null) {
        methodLocation = MethodLocation.elementInClass(methodLocation.getPsiElement(), containingClass);
      }
    }
    return methodLocation;
  }

  @Nullable
  public static Location<PsiMethod> getTestMethod(final Location<?> location) {
    for (Iterator<Location<PsiMethod>> iterator = location.getAncestors(PsiMethod.class, false); iterator.hasNext(); ) {
      final Location<PsiMethod> methodLocation = iterator.next();
      if (TestFrameworks.getInstance().isTestMethod(methodLocation.getPsiElement(), false)) return methodLocation;
    }
    return null;
  }

  @Nullable
  public static String resolveProjectPath(@NotNull Module module) {
    final String rootProjectPath = ExternalSystemApiUtil.getExternalRootProjectPath(module);
    final String projectPath = ExternalSystemApiUtil.getExternalProjectPath(module);
    if (rootProjectPath == null || projectPath == null) return null;

    GradleProjectSettings projectSettings = GradleSettings.getInstance(module.getProject()).getLinkedProjectSettings(rootProjectPath);
    if (projectSettings != null &&
        projectSettings.getCompositeBuild() != null &&
        projectSettings.getCompositeBuild().getCompositeDefinitionSource() == CompositeDefinitionSource.SCRIPT) {
      List<BuildParticipant> buildParticipants = projectSettings.getCompositeBuild().getCompositeParticipants();
      String compositeProjectPath = buildParticipants.stream()
                                                     .filter(participant -> participant.getProjects().contains(projectPath))
                                                     .findFirst()
                                                     .map(BuildParticipant::getRootPath)
                                                     .orElse(null);
      if (compositeProjectPath != null) {
        return compositeProjectPath;
      }
    }
    return rootProjectPath;
  }

  public static boolean isFromGroovyGradleScript(@Nullable Location location) {
    if (location == null) return false;
    return isFromGroovyGradleScript(location.getPsiElement());
  }

  public static boolean isFromGroovyGradleScript(@NotNull PsiElement element) {
    PsiFile psiFile = element.getContainingFile();
    if (psiFile == null) return false;
    VirtualFile virtualFile = psiFile.getVirtualFile();
    if (virtualFile == null) return false;
    return GradleConstants.EXTENSION.equals(virtualFile.getExtension());
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

  @NotNull
  public static List<String> getTasksTarget(@NotNull PsiElement element) {
    return getTasksTarget(element, null);
  }


  private static boolean isCreateTaskMethod(PsiElement parent) {
    return parent instanceof GrMethodCallExpression && PsiUtil.isMethodCall((GrMethodCallExpression)parent, "createTask");
  }
}
