/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.formatting.engine;

import com.intellij.formatting.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.TextChange;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.editor.impl.BulkChangesMerger;
import com.intellij.openapi.editor.impl.TextChangeImpl;
import com.intellij.util.DocumentUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ApplyChangesState extends State {

  /**
   * There is a possible case that formatting introduced big number of changes to the underlying document. That number may be
   * big enough for that their subsequent appliance is much slower than direct replacing of the whole document text.
   * <p/>
   * Current constant holds minimum number of changes that should trigger such {@code 'replace whole text'} optimization.
   */
  private static final int BULK_REPLACE_OPTIMIZATION_CRITERIA = 3000;


  private final FormattingModel myModel;
  
  private final FormattingProgressCallback myProgressCallback;
  private final WrapBlocksState myWrapState;

  private List<LeafBlockWrapper> myBlocksToModify;
  private int myShift;
  private int myIndex;
  private boolean myResetBulkUpdateState;

  private final BlockIndentOptions myBlockIndentOptions;

  public ApplyChangesState(FormattingModel model, WrapBlocksState state, FormattingProgressCallback callback) {
    myModel = model;
    myWrapState = state;
    myProgressCallback = callback;
    myBlockIndentOptions = state.getBlockIndentOptions();
  }

  /**
   * Performs formatter changes in a series of blocks, for each block a new contents of document is calculated
   * and whole document is replaced in one operation.
   *
   * @param blocksToModify changes introduced by formatter
   * @param model          current formatting model
   */
  @SuppressWarnings({"deprecation"})
  private void applyChangesAtRewriteMode(@NotNull final List<LeafBlockWrapper> blocksToModify,
                                         @NotNull final FormattingModel model) {
    FormattingDocumentModel documentModel = model.getDocumentModel();
    Document document = documentModel.getDocument();
    CaretOffsetUpdater caretOffsetUpdater = new CaretOffsetUpdater(document);

    DocumentUtil.executeInBulk(document, true, ()->{
      List<TextChange> changes = new ArrayList<>();
      int shift = 0;
      int currentIterationShift = 0;
      for (LeafBlockWrapper block : blocksToModify) {
        WhiteSpace whiteSpace = block.getWhiteSpace();
        CharSequence newWs = documentModel.adjustWhiteSpaceIfNecessary(
          whiteSpace.generateWhiteSpace(myBlockIndentOptions.getIndentOptions(block)), whiteSpace.getStartOffset(),
          whiteSpace.getEndOffset(), block.getNode(), false
        );
        if (changes.size() > 10000) {
          caretOffsetUpdater.update(changes);
          CharSequence mergeResult =
            BulkChangesMerger.INSTANCE.mergeToCharSequence(document.getChars(), document.getTextLength(), changes);
          document.replaceString(0, document.getTextLength(), mergeResult);
          shift += currentIterationShift;
          currentIterationShift = 0;
          changes.clear();
        }
        TextChangeImpl change = new TextChangeImpl(newWs, whiteSpace.getStartOffset() + shift, whiteSpace.getEndOffset() + shift);
        currentIterationShift += change.getDiff();
        changes.add(change);
      }
      caretOffsetUpdater.update(changes);
      CharSequence mergeResult = BulkChangesMerger.INSTANCE.mergeToCharSequence(document.getChars(), document.getTextLength(), changes);
      document.replaceString(0, document.getTextLength(), mergeResult);
    });

    caretOffsetUpdater.restoreCaretLocations();
    cleanupBlocks(blocksToModify);
  }

  private static void cleanupBlocks(List<? extends LeafBlockWrapper> blocks) {
    for (LeafBlockWrapper block : blocks) {
      block.getParent().dispose();
      block.dispose();
    }
    blocks.clear();
  }

  @Nullable
  private static DocumentEx getAffectedDocument(final FormattingModel model) {
    final Document document = model.getDocumentModel().getDocument();
    if (document instanceof DocumentEx) {
      return (DocumentEx)document;
    }
    else {
      return null;
    }
  }

  private List<LeafBlockWrapper> collectBlocksToModify() {
    List<LeafBlockWrapper> blocksToModify = new ArrayList<>();
    LeafBlockWrapper firstBlock = myWrapState.getFirstBlock();
    for (LeafBlockWrapper block = firstBlock; block != null; block = block.getNextBlock()) {
      final WhiteSpace whiteSpace = block.getWhiteSpace();
      if (!whiteSpace.isReadOnly()) {
        final String newWhiteSpace = whiteSpace.generateWhiteSpace(myBlockIndentOptions.getIndentOptions(block));
        if (!whiteSpace.equalsToString(newWhiteSpace)) {
          blocksToModify.add(block);
        }
      }
    }
    return blocksToModify;
  }

  @Override
  public void prepare() {
    myBlocksToModify = collectBlocksToModify();
    // call doModifications static method to ensure no access to state
    // thus we may clear formatting state

    //reset();
    //myDisposed = true;

    if (myBlocksToModify.isEmpty()) {
      setDone(true);
      return;
    }

    myProgressCallback.beforeApplyingFormatChanges(myBlocksToModify);

    final int blocksToModifyCount = myBlocksToModify.size();
    if (blocksToModifyCount > BULK_REPLACE_OPTIMIZATION_CRITERIA) {
      applyChangesAtRewriteMode(myBlocksToModify, myModel);
      setDone(true);
    }
    else if (blocksToModifyCount > 50) {
      DocumentEx updatedDocument = getAffectedDocument(myModel);
      if (updatedDocument != null) {
        updatedDocument.setInBulkUpdate(true);
        myResetBulkUpdateState = true;
      }
    }
  }

  @Override
  protected void doIteration() {
    LeafBlockWrapper blockWrapper = myBlocksToModify.get(myIndex);
    myShift = FormatProcessorUtils.replaceWhiteSpace(
      myModel,
      blockWrapper,
      myShift,
      blockWrapper.getWhiteSpace().generateWhiteSpace(myBlockIndentOptions.getIndentOptions(blockWrapper)),
      myBlockIndentOptions.getIndentOptions()
    );
    myProgressCallback.afterApplyingChange(blockWrapper);
    // block could be gc'd
    blockWrapper.getParent().dispose();
    blockWrapper.dispose();
    myBlocksToModify.set(myIndex, null);
    myIndex++;

    if (myIndex >= myBlocksToModify.size()) {
      setDone(true);
    }
  }

  @Override
  protected void setDone(boolean done) {
    super.setDone(done);

    if (myResetBulkUpdateState) {
      DocumentEx document = getAffectedDocument(myModel);
      if (document != null) {
        document.setInBulkUpdate(false);
        myResetBulkUpdateState = false;
      }
    }

    if (done) {
      myModel.commitChanges();
    }
  }

  @Override
  public void stop() {
    if (myIndex > 0) {
      ApplicationManager.getApplication().invokeAndWait(() -> myModel.commitChanges());
    }
  }
}