// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.codeInspection;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.codeInspection.GroovyFix;
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrCallExpression;
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public class MultipleRepositoryUrlsFix extends GroovyFix {
  private final SmartPsiElementPointer<GrClosableBlock> myClosure;
  private final String myRepoType;

  public MultipleRepositoryUrlsFix(@NotNull GrClosableBlock closure, @NotNull String repoType) {
    myClosure = SmartPointerManager.getInstance(closure.getProject()).createSmartPsiElementPointer(closure);
    myRepoType = repoType;
  }

  @Override
  protected void doFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) throws IncorrectOperationException {
    GrClosableBlock closure = myClosure.getElement();
    if (closure == null) return;
    List<GrCallExpression> statements = MultipleRepositoryUrlsInspection.findUrlCallExpressions(closure);
    if (statements.size() <= 1) return;
    statements.remove(0);

    List<PsiElement> elements = new ArrayList<>(statements);
    for (GrCallExpression statement : statements) {
      PsiElement newLineCandidate = statement.getNextSibling();
      if (PsiUtil.isNewLine(newLineCandidate)) {
        elements.add(newLineCandidate);
      }
    }

    closure.removeElements(elements.toArray(PsiElement.EMPTY_ARRAY));
    GrClosableBlock closableBlock = PsiTreeUtil.getParentOfType(closure, GrClosableBlock.class);
    if (closableBlock == null) return;

    GroovyPsiElementFactory elementFactory = GroovyPsiElementFactory.getInstance(project);
    for (GrCallExpression statement : statements) {
      closableBlock.addStatementBefore(elementFactory.createStatementFromText(myRepoType + '{' + statement.getText() + '}'), null);
    }
  }

  @NotNull
  @Override
  public String getFamilyName() {
    return GradleInspectionBundle.message("multiple.repository.urls.fix.name");
  }
}
