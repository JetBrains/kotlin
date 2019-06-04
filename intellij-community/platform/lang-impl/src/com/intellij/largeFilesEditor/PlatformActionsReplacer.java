// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor;

import com.intellij.largeFilesEditor.actions.*;
import com.intellij.largeFilesEditor.editor.actions.LfeEditorActionTextEndHandler;
import com.intellij.largeFilesEditor.editor.actions.LfeEditorActionTextStartHandler;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.editor.actionSystem.EditorActionManager;

public class PlatformActionsReplacer {

  private static final Logger logger = Logger.getInstance(PlatformActionsReplacer.class);
  private static boolean isPlatformActionsWereAdapted = false;

  public static void makeAdaptingOfPlatformActionsIfNeed() {
    if (!isPlatformActionsWereAdapted) {
      isPlatformActionsWereAdapted = true;
      makeAdaptingOfPlatformActions();
    }
  }

  private static void makeAdaptingOfPlatformActions() {
    logger.info("[Large File Editor Subsystem] Performing adapting of platform actions...");

    disableActionForLfe(IdeActions.ACTION_HIGHLIGHT_USAGES_IN_FILE);
    disableActionForLfe("GotoLine");

    addEditorActionHandler(IdeActions.ACTION_FIND_NEXT, LfeEditorActionSearchAgainHandler::new);
    addEditorActionHandler(IdeActions.ACTION_FIND_PREVIOUS, LfeEditorActionSearchBackHandler::new);
    addEditorActionHandler(IdeActions.ACTION_EDITOR_TEXT_START, LfeEditorActionTextStartHandler::new);
    addEditorActionHandler(IdeActions.ACTION_EDITOR_TEXT_END, LfeEditorActionTextEndHandler::new);
    addEditorActionHandler(IdeActions.ACTION_EDITOR_ESCAPE, LfeEditorActionHandlerEscape::new);
    addEditorActionHandler(IdeActions.ACTION_FIND, LfeEditorActionHandlerFind::new);
    addDisablingEditorActionHandler(IdeActions.ACTION_REPLACE);
    addDisablingEditorActionHandler(IdeActions.ACTION_FIND_WORD_AT_CARET);
    addDisablingEditorActionHandler(IdeActions.ACTION_SELECT_ALL_OCCURRENCES);
    addDisablingEditorActionHandler(IdeActions.ACTION_SELECT_NEXT_OCCURENCE);
    addDisablingEditorActionHandler(IdeActions.ACTION_UNSELECT_PREVIOUS_OCCURENCE);
  }

  private static void addDisablingEditorActionHandler(String actionId) {
    addEditorActionHandler(actionId, LfeEditorActionHandlerDisabled::new);
  }

  private static void addEditorActionHandler(String actionId, MyEditorActionHandlerFactory lfeEditorActionHandlerFactory) {
    try {
      EditorActionManager editorActionManager = EditorActionManager.getInstance();
      EditorActionHandler originalHandler = editorActionManager.getActionHandler(actionId);
      EditorActionHandler newHandler = lfeEditorActionHandlerFactory.create(originalHandler);
      editorActionManager.setActionHandler(actionId, newHandler);
    }
    catch (ClassCastException e) {
      logger.warn(e);
    }
  }

  private static void disableActionForLfe(String actionId) {
    replaceActionByProxy(actionId, LfeActionDisabled::new);
  }

  private static void replaceActionByProxy(String actionId, MyActionFactory actionFactory) {
    ActionManager actionManager = ActionManager.getInstance();
    AnAction originalAction = actionManager.getAction(actionId);
    if (originalAction == null) {
      logger.warn("[Large File Editor Subsystem] Can't replace action with id=\""
                  + actionId + "\". Action with this id doesn't exist");
      return;
    }
    AnAction proxyAction = actionFactory.create(originalAction);
    actionManager.replaceAction(actionId, proxyAction);
  }

  private interface MyActionFactory<T extends LfeBaseProxyAction> {
    T create(AnAction originalAction);
  }

  private interface MyEditorActionHandlerFactory<T extends LfeBaseEditorActionHandler> {
    T create(EditorActionHandler originalEditorActionHandler);
  }
}
