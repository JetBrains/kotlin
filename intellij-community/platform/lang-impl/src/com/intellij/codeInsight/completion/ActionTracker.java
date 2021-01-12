// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.completion;

import com.intellij.injected.editor.EditorWindow;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import com.intellij.openapi.command.CommandEvent;
import com.intellij.openapi.command.CommandListener;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * @author peter
 */
final class ActionTracker {
  private final @NotNull MessageBusConnection myConnection;
  private @NotNull List<Integer> myCaretOffsets;
  private long myStartDocStamp;
  private boolean myActionsHappened;
  private final Editor myEditor;
  private final Project myProject;
  private final boolean myIsDumb;

  ActionTracker(@NotNull Editor editor, @NotNull Disposable parentDisposable) {
    myEditor = editor;
    myProject = Objects.requireNonNull(editor.getProject());
    myIsDumb = DumbService.getInstance(myProject).isDumb();

    myConnection = myProject.getMessageBus().connect(parentDisposable);
    myConnection.subscribe(AnActionListener.TOPIC, new AnActionListener() {
      @Override
      public void beforeActionPerformed(@NotNull AnAction action, @NotNull DataContext dataContext, @NotNull AnActionEvent event) {
        myActionsHappened = true;
      }
    });
    myStartDocStamp = docStamp();
    myCaretOffsets = caretOffsets();
  }

  private List<Integer> caretOffsets() {
    return ContainerUtil.map(myEditor.getCaretModel().getAllCarets(), Caret::getOffset);
  }

  private long docStamp() {
    return myEditor.getDocument().getModificationStamp();
  }

  void ignoreCurrentDocumentChange() {
    if (CommandProcessor.getInstance().getCurrentCommand() == null) {
      return;
    }

    myConnection.subscribe(CommandListener.TOPIC, new CommandListener() {
      boolean insideCommand = true;
      @Override
      public void commandFinished(@NotNull CommandEvent event) {
        if (insideCommand) {
          insideCommand = false;
          myStartDocStamp = docStamp();
          myCaretOffsets = caretOffsets();
        }
      }
    });
  }

  boolean hasAnythingHappened() {
    return myActionsHappened || myIsDumb != DumbService.getInstance(myProject).isDumb() ||
           myEditor.isDisposed() ||
           (myEditor instanceof EditorWindow && !((EditorWindow)myEditor).isValid()) ||
           myStartDocStamp != docStamp() ||
           !myCaretOffsets.equals(caretOffsets());
  }
}
