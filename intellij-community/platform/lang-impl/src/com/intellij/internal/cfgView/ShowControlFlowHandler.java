// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.internal.cfgView;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.controlflow.ConditionalInstruction;
import com.intellij.codeInsight.controlflow.ControlFlow;
import com.intellij.codeInsight.controlflow.ControlFlowProvider;
import com.intellij.codeInsight.controlflow.Instruction;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.PathEnvironmentVariableUtil;
import com.intellij.execution.util.ExecUtil;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class ShowControlFlowHandler implements CodeInsightActionHandler {

  private static final Logger LOGGER = Logger.getInstance(ShowControlFlowHandler.class);

  private static final NotificationGroup NOTIFICATION_GROUP = NotificationGroup.balloonGroup("Show control flow group");
  private static final String NO_GRAPHVIZ_HELP = "Probably graphviz is missing." +
                                                 "You could install graphviz using `apt install graphviz` or `brew install graphviz`";


  @Override
  public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
    final int offset = editor.getCaretModel().getOffset();
    final PsiElement position = file.findElementAt(offset);
    if (position == null) {
      return;
    }
    try {
      final File svgFile = FileUtil.createTempFile("control-flow", ".svg", true);
      final String path = svgFile.getAbsolutePath();
      final boolean success = toSvgFile(path, position);
      if (success) {
        ApplicationManager.getApplication().invokeLater(() -> {
          boolean exists = svgFile.exists();
          final VirtualFile fileByUrl = VirtualFileManager.getInstance().refreshAndFindFileByUrl(VfsUtilCore.pathToUrl(path));
          if (fileByUrl != null) {
            final AnAction openInBrowser = ActionManager.getInstance().getAction("OpenInBrowser");
            DataContext dataContext = dataId -> {
              if (CommonDataKeys.VIRTUAL_FILE.is(dataId)) {
                return fileByUrl;
              }
              if (CommonDataKeys.PROJECT.is(dataId)) {
                return project;
              }
              return null;
            };
            final AnActionEvent action = AnActionEvent.createFromDataContext("ShowControlFlow", null, dataContext);
            openInBrowser.actionPerformed(action);
          }
          else {
            LOGGER.error("cannot find file by URL: " + path + " " + exists);
          }
        });
      }
    }
    catch (FileNotFoundException e) {
      NOTIFICATION_GROUP.createNotification("Show CFG:", e.getMessage(), NO_GRAPHVIZ_HELP, NotificationType.ERROR).notify(project);
      LOGGER.warn(e);
    }
    catch (IOException | ExecutionException e) {
      LOGGER.warn(e);
    }
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }

  public static boolean toSvgFile(@NotNull final String outSvgFile, @NotNull final PsiElement target) throws IOException, ExecutionException {
    String dotUtilName = SystemInfo.isUnix ? "dot" : "dot.exe";
    File dotFullPath = PathEnvironmentVariableUtil.findInPath(dotUtilName);
    if (dotFullPath == null) {
      throw new FileNotFoundException("Cannot find dot utility in path");
    }
    ControlFlow controlFlow = null;
    ControlFlowProvider provider = null;
    for (ControlFlowProvider extension : ControlFlowProvider.EP_NAME.getExtensions()) {
      controlFlow = extension.getControlFlow(target);
      if (controlFlow != null) {
        provider = extension;
        break;
      }
    }
    if (controlFlow == null) {
      return false;
    }

    File tmpFile = FileUtil.createTempFile("control-flow", ".dot", true);
    try {
      FileUtil.writeToFile(tmpFile, toDot(controlFlow, provider));
      GeneralCommandLine generalCommandLine = new GeneralCommandLine(dotFullPath.getAbsolutePath()).withInput(tmpFile.getAbsoluteFile())
        .withParameters("-Tsvg", "-o" + outSvgFile, tmpFile.getAbsolutePath()).withRedirectErrorStream(true);
      ExecUtil.execAndGetOutput(generalCommandLine);
    }
    finally {
      if (!tmpFile.delete()) {
        LOGGER.warn("Cannot delete tmp file: " + tmpFile);
      }
    }

    return true;
  }

  @NotNull
  private static String toDot(@NotNull final ControlFlow flow, @NotNull final ControlFlowProvider provider) {
    StringBuilder builder = new StringBuilder();
    builder.append("digraph {");
    for (Instruction instruction : flow.getInstructions()) {
      printInstruction(builder, instruction, provider);

      if (instruction instanceof ConditionalInstruction) {
        ConditionalInstruction conditionalInstruction = (ConditionalInstruction)instruction;
        builder.append("\n").append("Its ").append(conditionalInstruction.getResult()).
          append(" branch, condition: ").append(conditionalInstruction.getCondition().getText());
      }
      builder.append("\"").append("]");

      builder.append(System.lineSeparator());
      if (instruction.allPred().isEmpty()) {
        builder.append("Entry -> Instruction").append(instruction.num()).append(System.lineSeparator());
      }
      if (instruction.allSucc().isEmpty()) {
        builder.append("Instruction").append(instruction.num()).append(" -> Exit").append(System.lineSeparator());
      }
      for (Instruction succ : instruction.allSucc()) {
        builder.append("Instruction").append(instruction.num()).append(" -> ")
          .append("Instruction").append(succ.num()).append(System.lineSeparator());
      }
    }
    builder.append("}");
    return builder.toString();
  }

  private static void printInstruction(@NotNull StringBuilder builder,
                                       @NotNull Instruction instruction,
                                       @NotNull ControlFlowProvider provider) {
    PsiElement element = instruction.getElement();
    Class<? extends Instruction> instructionClass = instruction.getClass();
    builder.append("Instruction").append(instruction.num()).append("[font=\"Courier\", label=\"")
      .append(element != null ? escape(element.getText()) : "").append(" \\n(").append(instruction.num()).append(")[")
      .append(element != null ? element.getClass().getSimpleName() : "null").append("]").append(System.lineSeparator())
      .append("{").append(instructionClass.getSimpleName().isEmpty()
                          ? instructionClass.getSuperclass().getSimpleName()
                          : instructionClass.getSimpleName() ).append("}");
    String additionalInfo = provider.getAdditionalInfo(instruction);

    if (additionalInfo != null) {
      builder.append("\n{").append(additionalInfo).append("}");
    }
  }

  @NotNull
  private static String escape(@NotNull String text) {
    return StringUtil.replace(StringUtil.escapeChars(text, '"'), "\n", "\\n");
  }
}
