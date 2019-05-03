// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.resolve;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.resolve.NonCodeMembersContributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Denis Zhdanov
 */
public class GradleScriptContributor extends NonCodeMembersContributor {

  public static final Set<String> BUILD_PROJECT_SCRIPT_BLOCKS = ContainerUtil.newHashSet(
    "project",
    "configure",
    "subprojects",
    "allprojects",
    "buildscript"
  );


  @Override
  public void processDynamicElements(@NotNull PsiType qualifierType,
                                     @Nullable PsiClass aClass,
                                     @NotNull PsiScopeProcessor processor,
                                     @NotNull PsiElement place,
                                     @NotNull ResolveState state) {
    if (!UtilKt.isResolvedInGradleScript(aClass)) return;

    List<String> methodInfo = new ArrayList<>();
    for (GrMethodCall current = PsiTreeUtil.getParentOfType(place, GrMethodCall.class);
         current != null;
         current = PsiTreeUtil.getParentOfType(current, GrMethodCall.class)) {
      GrExpression expression = current.getInvokedExpression();
      String text = expression.getText();
      if (text != null) {
        methodInfo.add(text);
      }
    }

    final String methodCall = ContainerUtil.getLastItem(methodInfo);
    if (methodInfo.size() > 1 && BUILD_PROJECT_SCRIPT_BLOCKS.contains(methodCall)) {
      methodInfo.remove(methodInfo.size() - 1);
    }

    for (GradleMethodContextContributor contributor : GradleMethodContextContributor.EP_NAME.getExtensions()) {
      if (!contributor.process(methodInfo, processor, state, place)) return;
    }
  }
}
