// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.impl;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementDecorator;
import com.intellij.codeInsight.template.TemplateLookupSelectionHandler;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
* @author peter
*/
class TemplateExpressionLookupElement extends LookupElementDecorator<LookupElement> {
  private final TemplateState myState;

  TemplateExpressionLookupElement(final TemplateState state, LookupElement element, int index) {
    super(PrioritizedLookupElement.withPriority(element, Integer.MAX_VALUE - 10 - index));
    myState = state;
  }

  private static InsertionContext createInsertionContext(LookupElement item,
                                                         PsiFile psiFile,
                                                         List<? extends LookupElement> elements,
                                                         Editor editor, final char completionChar) {
    OffsetMap offsetMap = new OffsetMap(editor.getDocument());
    InsertionContext context = new InsertionContext(offsetMap, completionChar, elements.toArray(LookupElement.EMPTY_ARRAY), psiFile, editor,
                                                    InsertionContext.shouldAddCompletionChar(completionChar));
    context.setTailOffset(editor.getCaretModel().getOffset());
    offsetMap.addOffset(CompletionInitializationContext.START_OFFSET, context.getTailOffset() - item.getLookupString().length());
    offsetMap.addOffset(CompletionInitializationContext.SELECTION_END_OFFSET, context.getTailOffset());
    offsetMap.addOffset(CompletionInitializationContext.IDENTIFIER_END_OFFSET, context.getTailOffset());
    return context;
  }

  void handleTemplateInsert(List<? extends LookupElement> elements, final char completionChar) {
    InsertionContext context = createInsertionContext(this, myState.getPsiFile(), elements, myState.getEditor(), completionChar);
    WriteAction.run(() -> {
      doHandleInsert(context);
      if (context.shouldAddCompletionChar()) {
        CodeCompletionHandlerBase.addCompletionChar(context, this);
      }
    });
    Disposer.dispose(context.getOffsetMap());

    if (handleCompletionChar(context) && !myState.isFinished()) {
      myState.calcResults(true);
      myState.considerNextTabOnLookupItemSelected(getDelegate());
    }
  }

  @Override
  public void handleInsert(@NotNull final InsertionContext context) {
    doHandleInsert(context);
    handleCompletionChar(context);
  }

  private void doHandleInsert(InsertionContext context) {
    LookupElement item = getDelegate();
    PsiDocumentManager.getInstance(context.getProject()).commitAllDocuments();

    TextRange range = myState.getCurrentVariableRange();
    final TemplateLookupSelectionHandler handler = item.getUserData(TemplateLookupSelectionHandler.KEY_IN_LOOKUP_ITEM);
    if (handler != null && range != null) {
      handler.itemSelected(item, context.getFile(), context.getDocument(), range.getStartOffset(), range.getEndOffset());
    }
    else {
      super.handleInsert(context);
    }
  }

  private static boolean handleCompletionChar(InsertionContext context) {
    if (context.getCompletionChar() == '.') {
      AutoPopupController.getInstance(context.getProject()).autoPopupMemberLookup(context.getEditor(), null);
      return false;
    }
    return true;
  }
}
