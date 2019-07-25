// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.execution;

import com.intellij.codeInsight.TestFrameworks;
import com.intellij.execution.Location;
import com.intellij.execution.junit2.PsiMemberParameterizedLocation;
import com.intellij.execution.junit2.info.MethodLocation;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.model.data.BuildParticipant;
import org.jetbrains.plugins.gradle.settings.CompositeDefinitionSource;
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings;
import org.jetbrains.plugins.gradle.settings.GradleSettings;
import org.jetbrains.plugins.gradle.util.GradleConstants;

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
}
