/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

/*
 * @author max
 */
package com.intellij.tools;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import org.jetbrains.annotations.NotNull;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class ScanSourceCommentsAction extends AnAction {
  private static final Logger LOG = Logger.getInstance("#com.intellij.tools.ScanSourceCommentsAction");

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {

    final Project p = e.getProject();
    final String file =
      Messages.showInputDialog(p, "Enter path to the file comments will be extracted to", "Comments File Path", Messages.getQuestionIcon());

    try (final PrintStream stream = new PrintStream(file)){
      stream.println("Comments in " + p.getName());

      ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
        final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
        ProjectRootManager.getInstance(p).getFileIndex().iterateContent(fileOrDir -> {
          if (fileOrDir.isDirectory()) {
            indicator.setText("Extracting comments");
            indicator.setText2(fileOrDir.getPresentableUrl());
          }
          scanCommentsInFile(p, fileOrDir);
          return true;
        });

        indicator.setText2("");
        int count = 1;
        for (CommentDescriptor descriptor : myComments.values()) {
          stream.println("#" + count + " ---------------------------------------------------------------");
          descriptor.print(stream);
          stream.println();
          count++;
        }

      }, "Generating Comments", true, p);
    }
    catch (Throwable e1) {
      LOG.error(e1);
      Messages.showErrorDialog(p, "Error writing? " + e1.getMessage(), "Problem writing");
    }
  }

  private final Map<String, CommentDescriptor> myComments = new HashMap<>();

  private void commentFound(VirtualFile file, String text) {
    String reduced = text.replaceAll("\\s", "");
    CommentDescriptor descriptor = myComments.get(reduced);
    if (descriptor == null) {
      descriptor = new CommentDescriptor(text);
      myComments.put(reduced, descriptor);
    }
    descriptor.addFile(file);
  }

  private void scanCommentsInFile(final Project project, final VirtualFile vFile) {
    if (!vFile.isDirectory() && vFile.getFileType() instanceof LanguageFileType) {
      ApplicationManager.getApplication().runReadAction(() -> {
        PsiFile psiFile = PsiManager.getInstance(project).findFile(vFile);
        if (psiFile == null) return;

        for (PsiFile root : psiFile.getViewProvider().getAllFiles()) {
          root.accept(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitComment(PsiComment comment) {
              commentFound(vFile, comment.getText());
            }
          });
        }
      });
    }
  }


  private static class CommentDescriptor {
    private final String myText;
    private final Set<VirtualFile> myFiles = new LinkedHashSet<>();

    CommentDescriptor(String text) {
      myText = text;
    }

    public void addFile(VirtualFile file) {
      myFiles.add(file);
    }

    public void print(PrintStream out) {
      out.println(myText);
      int count = myFiles.size();
      int i = 0;

      for (VirtualFile file : myFiles) {
        out.println(file.getPresentableUrl());
        count--;
        i++;
        if (i > 5 && count > 2) break;
      }

      if (count > 0) {
        out.println("And " + count + " more files");
      }
    }
  }
}
