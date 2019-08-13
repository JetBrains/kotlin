// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.codeInspection;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.jetbrains.plugins.groovy.codeInspection.BaseInspectionVisitor;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrCallExpression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public class MultipleRepositoryUrlsInspection extends GradleBaseInspection {

  @NotNull
  @Override
  protected BaseInspectionVisitor buildVisitor() {
    return new MyVisitor();
  }

  @Nls
  @NotNull
  @Override
  public String getGroupDisplayName() {
    return PROBABLE_BUGS;
  }


  @Override
  protected String buildErrorString(Object... args) {
    return GradleInspectionBundle.message("multiple.repository.urls", args);
  }

  @Nls
  @NotNull
  @Override
  public String getDisplayName() {
    return GradleInspectionBundle.message("multiple.repository.urls");
  }

  private static class MyVisitor extends BaseInspectionVisitor {
    @Override
    public void visitClosure(@NotNull GrClosableBlock closure) {
      PsiFile file = closure.getContainingFile();
      if (file == null || !FileUtilRt.extensionEquals(file.getName(), GradleConstants.EXTENSION)) return;

      super.visitClosure(closure);
      GrMethodCall mavenMethodCall = PsiTreeUtil.getParentOfType(closure, GrMethodCall.class);
      if (mavenMethodCall == null) return;
      GrExpression mavenMethodExpression = mavenMethodCall.getInvokedExpression();
      if (!ArrayUtil.contains(mavenMethodExpression.getText(), "maven", "ivy")) {
        return;
      }

      GrMethodCall repositoryMethodCall = PsiTreeUtil.getParentOfType(mavenMethodCall, GrMethodCall.class);
      if (repositoryMethodCall == null) return;
      GrExpression repositoryMethodExpression = repositoryMethodCall.getInvokedExpression();
      if (!repositoryMethodExpression.getText().equals("repositories")) return;

      List<GrCallExpression> statements = findUrlCallExpressions(closure);
      if (statements.size() > 1) {
        registerError(closure);

        registerError(closure, GradleInspectionBundle.message("multiple.repository.urls"),
                      new LocalQuickFix[]{new MultipleRepositoryUrlsFix(closure, mavenMethodExpression.getText())},
                      ProblemHighlightType.GENERIC_ERROR);
      }
    }
  }

  @NotNull
  static List<GrCallExpression> findUrlCallExpressions(@NotNull GrClosableBlock closure) {
    GrCallExpression[] applicationStatements = PsiTreeUtil.getChildrenOfType(closure, GrCallExpression.class);
    if (applicationStatements == null) return Collections.emptyList();

    List<GrCallExpression> statements = new ArrayList<>();
    for (GrCallExpression statement : applicationStatements) {
      GrReferenceExpression[] referenceExpressions = PsiTreeUtil.getChildrenOfType(statement, GrReferenceExpression.class);
      if (referenceExpressions == null) continue;
      for (GrReferenceExpression expression : referenceExpressions) {
        String expressionText = expression.getText();
        if ("url".equals(expressionText) || "setUrl".equals(expressionText)) {
          statements.add(statement);
        }
      }
    }
    return statements;
  }
}
