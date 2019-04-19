/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package org.jetbrains.plugins.gradle.integrations.maven.codeInsight.actions;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.FileModificationService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.indices.MavenArtifactSearchDialog;
import org.jetbrains.idea.maven.model.MavenId;
import org.jetbrains.plugins.gradle.util.GradleBundle;
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;

import java.util.Collections;
import java.util.List;

/**
 * @author Vladislav.Soroka
 */
class AddGradleDslDependencyActionHandler implements CodeInsightActionHandler {

  @Override
  public void invoke(@NotNull final Project project, @NotNull final Editor editor, @NotNull final PsiFile file) {
    if (!EditorModificationUtil.checkModificationAllowed(editor) ||
        !FileModificationService.getInstance().preparePsiElementsForWrite(file)) return;

    final List<MavenId> ids;
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      ids = AddGradleDslDependencyAction.TEST_THREAD_LOCAL.get();
    }
    else {
      ids = MavenArtifactSearchDialog.searchForArtifact(project, Collections.emptyList());
    }

    if (ids.isEmpty()) return;

    WriteCommandAction.writeCommandAction(project, file)
                      .withName(GradleBundle.message("gradle.codeInsight.action.add_maven_dependency.text")).run(() -> {
      GroovyPsiElementFactory factory = GroovyPsiElementFactory.getInstance(project);
      List<GrMethodCall> closableBlocks = PsiTreeUtil.getChildrenOfTypeAsList(file, GrMethodCall.class);
      GrCall dependenciesBlock = ContainerUtil.find(closableBlocks, call -> {
        GrExpression expression = call.getInvokedExpression();
        return "dependencies".equals(expression.getText());
      });

      if (dependenciesBlock == null) {
        StringBuilder buf = new StringBuilder();
        for (MavenId mavenId : ids) {
          buf.append(String.format("compile '%s'\n", getMavenArtifactKey(mavenId)));
        }
        dependenciesBlock = (GrCall)factory.createStatementFromText("dependencies{\n" + buf + "}");
        file.add(dependenciesBlock);
      }
      else {
        GrClosableBlock closableBlock = ArrayUtil.getFirstElement(dependenciesBlock.getClosureArguments());
        if (closableBlock != null) {
          for (MavenId mavenId : ids) {
            closableBlock.addStatementBefore(
              factory.createStatementFromText(String.format("compile '%s'\n", getMavenArtifactKey(mavenId))), null);
          }
        }
      }
    });
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }


  @NotNull
  private static String getMavenArtifactKey(MavenId mavenId) {
    StringBuilder builder = new StringBuilder();
    append(builder, mavenId.getGroupId());
    append(builder, mavenId.getArtifactId());
    append(builder, mavenId.getVersion());

    return builder.toString();
  }

  private static void append(StringBuilder builder, String part) {
    if (builder.length() != 0) builder.append(':');
    builder.append(part == null ? "" : part);
  }
}
