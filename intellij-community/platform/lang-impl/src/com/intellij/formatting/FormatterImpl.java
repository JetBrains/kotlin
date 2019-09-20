// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.formatting;

import com.intellij.formatting.engine.ExpandableIndent;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.formatter.FormatterUtil;
import com.intellij.psi.formatter.FormattingDocumentModelImpl;
import com.intellij.psi.formatter.PsiBasedFormattingModel;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.SequentialTask;
import com.intellij.util.text.CharArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.intellij.formatting.FormatProcessor.FormatOptions;

public class FormatterImpl extends FormatterEx
  implements IndentFactory,
             WrapFactory,
             AlignmentFactory,
             SpacingFactory,
             FormattingModelFactory {
  private static final Logger LOG = Logger.getInstance(FormatterImpl.class);

  private final AtomicReference<FormattingProgressTask> myProgressTask = new AtomicReference<>();

  private final AtomicInteger myIsDisabledCount = new AtomicInteger();
  private final IndentImpl NONE_INDENT = new IndentImpl(Indent.Type.NONE, false, false);
  private final IndentImpl myAbsoluteNoneIndent = new IndentImpl(Indent.Type.NONE, true, false);
  private final IndentImpl myLabelIndent = new IndentImpl(Indent.Type.LABEL, false, false);
  private final IndentImpl myContinuationIndentRelativeToDirectParent = new IndentImpl(Indent.Type.CONTINUATION, false, true);
  private final IndentImpl myContinuationIndentNotRelativeToDirectParent = new IndentImpl(Indent.Type.CONTINUATION, false, false);
  private final IndentImpl myContinuationWithoutFirstIndentRelativeToDirectParent
    = new IndentImpl(Indent.Type.CONTINUATION_WITHOUT_FIRST, false, true);
  private final IndentImpl myContinuationWithoutFirstIndentNotRelativeToDirectParent
    = new IndentImpl(Indent.Type.CONTINUATION_WITHOUT_FIRST, false, false);
  private final IndentImpl myAbsoluteLabelIndent = new IndentImpl(Indent.Type.LABEL, true, false);
  private final IndentImpl myNormalIndentRelativeToDirectParent = new IndentImpl(Indent.Type.NORMAL, false, true);
  private final IndentImpl myNormalIndentNotRelativeToDirectParent = new IndentImpl(Indent.Type.NORMAL, false, false);
  private final SpacingImpl myReadOnlySpacing = new SpacingImpl(0, 0, 0, true, false, true, 0, false, 0);

  @Override
  public Alignment createAlignment(boolean applyToNonFirstBlocksOnLine, @NotNull Alignment.Anchor anchor) {
    return new AlignmentImpl(applyToNonFirstBlocksOnLine, anchor);
  }

  @Override
  public Alignment createChildAlignment(final Alignment base) {
    AlignmentImpl result = new AlignmentImpl();
    result.setParent(base);
    return result;
  }

  @Override
  public Indent getNormalIndent(boolean relative) {
    return relative ? myNormalIndentRelativeToDirectParent : myNormalIndentNotRelativeToDirectParent;
  }

  @Override
  public Indent getNoneIndent() {
    return NONE_INDENT;
  }

  @Override
  public void setProgressTask(@NotNull FormattingProgressTask progressIndicator) {
    if (!FormatterUtil.isFormatterCalledExplicitly()) {
      return;
    }
    myProgressTask.set(progressIndicator);
  }

  @Override
  public int getSpacingForBlockAtOffset(FormattingModel model, int offset) {
    SpacingImpl spacing = getSpacingBeforeBlockAtOffset(model, offset);
    if (spacing != null) {
      int minSpaces = spacing.getMinSpaces();
      if (minSpaces >= 0) {
        return minSpaces;
      }
    }
    return -1;
  }

  @Override
  public int getMinLineFeedsBeforeBlockAtOffset(FormattingModel model, int offset) {
    SpacingImpl spacing = getSpacingBeforeBlockAtOffset(model, offset);
    if (spacing != null) {
      int minLineFeeds = spacing.getMinLineFeeds();
      if (minLineFeeds >= 0) {
        return minLineFeeds;
      }
    }
    return -1;
  }

  private static SpacingImpl getSpacingBeforeBlockAtOffset(FormattingModel model, int offset) {
    Couple<Block> blockWithParent = getBlockAtOffset(null, model.getRootBlock(), offset);
    if (blockWithParent != null) {
      Block parentBlock = blockWithParent.first;
      Block targetBlock = blockWithParent.second;
      if (parentBlock != null && targetBlock != null) {
        Block prevBlock = findPreviousSibling(parentBlock, targetBlock);
        if (prevBlock != null) return (SpacingImpl)parentBlock.getSpacing(prevBlock, targetBlock);
      }
    }
    return null;
  }

  @Nullable
  private static Couple<Block> getBlockAtOffset(@Nullable Block parent, @NotNull Block block, int offset) {
    TextRange textRange = block.getTextRange();
    int startOffset = textRange.getStartOffset();
    int endOffset = textRange.getEndOffset();
    if (startOffset == offset) {
      return Couple.of(parent, block);
    }
    if (startOffset > offset || endOffset < offset || block.isLeaf()) {
      return null;
    }
    for (Block subBlock : block.getSubBlocks()) {
      Couple<Block> result = getBlockAtOffset(block, subBlock, offset);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  @Nullable
  private static Block findPreviousSibling(@NotNull Block parent, Block block) {
    Block result = null;
    for (Block subBlock : parent.getSubBlocks()) {
      if (subBlock == block) {
        return result;
      }
      result = subBlock;
    }
    return null;
  }

  @Override
  public Wrap createWrap(WrapType type, boolean wrapFirstElement) {
    return new WrapImpl(type, wrapFirstElement);
  }

  @Override
  public Wrap createChildWrap(final Wrap parentWrap, final WrapType wrapType, final boolean wrapFirstElement) {
    final WrapImpl result = new WrapImpl(wrapType, wrapFirstElement);
    result.registerParent((WrapImpl)parentWrap);
    return result;
  }

  @Override
  @NotNull
  public Spacing createSpacing(int minOffset,
                               int maxOffset,
                               int minLineFeeds,
                               final boolean keepLineBreaks,
                               final int keepBlankLines) {
    return getSpacingImpl(minOffset, maxOffset, minLineFeeds, false, false, keepLineBreaks, keepBlankLines,false, 0);
  }

  @Override
  @NotNull
  public Spacing getReadOnlySpacing() {
    return myReadOnlySpacing;
  }

  @NotNull
  @Override
  public Spacing createDependentLFSpacing(int minSpaces,
                                          int maxSpaces,
                                          @NotNull TextRange dependencyRange,
                                          boolean keepLineBreaks,
                                          int keepBlankLines,
                                          @NotNull DependentSpacingRule rule)
  {
    return new DependantSpacingImpl(minSpaces, maxSpaces, dependencyRange, keepLineBreaks, keepBlankLines, rule);
  }

  @NotNull
  @Override
  public Spacing createDependentLFSpacing(int minSpaces,
                                          int maxSpaces,
                                          @NotNull List<TextRange> dependentRegion,
                                          boolean keepLineBreaks,
                                          int keepBlankLines,
                                          @NotNull DependentSpacingRule rule)
  {
    return new DependantSpacingImpl(minSpaces, maxSpaces, dependentRegion, keepLineBreaks, keepBlankLines, rule);
  }

  @NotNull
  private FormattingProgressCallback getProgressCallback() {
    FormattingProgressCallback result = myProgressTask.get();
    return result == null ? FormattingProgressCallback.EMPTY : result;
  }

  @Override
  public void format(final FormattingModel model,
                     final CodeStyleSettings settings,
                     final CommonCodeStyleSettings.IndentOptions indentOptions,
                     final FormatTextRanges affectedRanges) throws IncorrectOperationException {
      try {
      validateModel(model);
      SequentialTask task = new MyFormattingTask() {
        @NotNull
        @Override
        protected FormatProcessor buildProcessor() {
          FormatOptions options = new FormatOptions(settings, indentOptions, affectedRanges);
          FormatProcessor processor = new FormatProcessor(
            model.getDocumentModel(), model.getRootBlock(), options, getProgressCallback()
          );
          processor.format(model, true);
          return processor;
        }
      };
      execute(task);
    }
    catch (FormattingModelInconsistencyException e) {
      LOG.error(e);
    }
  }

  public void formatWithoutModifications(final FormattingDocumentModel model,
                                         final Block rootBlock,
                                         final CodeStyleSettings settings,
                                         final CommonCodeStyleSettings.IndentOptions indentOptions,
                                         final TextRange affectedRange) throws IncorrectOperationException
  {
    SequentialTask task = new MyFormattingTask() {
      @NotNull
      @Override
      protected FormatProcessor buildProcessor() {
        FormatProcessor result = new FormatProcessor(
          model, rootBlock, settings, indentOptions, new FormatTextRanges(affectedRange, true), FormattingProgressCallback.EMPTY
        );
        result.formatWithoutRealModifications();
        return result;
      }
    };
    execute(task);
  }

  private void execute(@NotNull SequentialTask task) {
    disableFormatting();
    Application application = ApplicationManager.getApplication();
    FormattingProgressTask progressTask = myProgressTask.getAndSet(null);
    if (progressTask == null || !application.isDispatchThread() || application.isUnitTestMode()) {
      try {
        task.prepare();
        while (!task.isDone()) {
          task.iteration();
        }
      }
      finally {
        enableFormatting();
      }
    }
    else {
      progressTask.setTask(task);
      Runnable callback = () -> enableFormatting();
      for (FormattingProgressCallback.EventType eventType : FormattingProgressCallback.EventType.values()) {
        progressTask.addCallback(eventType, callback);
      }
      ProgressManager.getInstance().run(progressTask);
    }
  }

  @Override
  public void adjustLineIndentsForRange(final FormattingModel model,
                                        final CodeStyleSettings settings,
                                        final CommonCodeStyleSettings.IndentOptions indentOptions,
                                        final TextRange rangeToAdjust) {
    disableFormatting();
    try {
      validateModel(model);
      final FormattingDocumentModel documentModel = model.getDocumentModel();
      final Block block = model.getRootBlock();
      final FormatProcessor processor = buildProcessorAndWrapBlocks(
        documentModel, block, settings, indentOptions, new FormatTextRanges(rangeToAdjust, true)
      );
      LeafBlockWrapper tokenBlock = processor.getFirstTokenBlock();
      while (tokenBlock != null) {
        final WhiteSpace whiteSpace = tokenBlock.getWhiteSpace();
        whiteSpace.setLineFeedsAreReadOnly(true);
        if (!whiteSpace.containsLineFeeds()) {
          whiteSpace.setIsReadOnly(true);
        }
        tokenBlock = tokenBlock.getNextBlock();
      }
      processor.formatWithoutRealModifications();
      processor.performModifications(model);
    }
    catch (FormattingModelInconsistencyException e) {
      LOG.error(e);
    }
    finally {
      enableFormatting();
    }
  }

  @Override
  public void formatAroundRange(FormattingModel model,
                                CodeStyleSettings settings,
                                PsiFile file,
                                TextRange textRange) {
    disableFormatting();
    try {
      validateModel(model);
      final FormattingDocumentModel documentModel = model.getDocumentModel();
      final Block block = model.getRootBlock();
      final FormatProcessor processor = buildProcessorAndWrapBlocks(
        documentModel, block, settings, settings.getIndentOptionsByFile(file), null
      );
      LeafBlockWrapper tokenBlock = processor.getFirstTokenBlock();
      while (tokenBlock != null) {
        final WhiteSpace whiteSpace = tokenBlock.getWhiteSpace();

        if (whiteSpace.getEndOffset() < textRange.getStartOffset() || whiteSpace.getEndOffset() > textRange.getEndOffset() + 1) {
          whiteSpace.setIsReadOnly(true);
        } else if (whiteSpace.getStartOffset() > textRange.getStartOffset() &&
                   whiteSpace.getEndOffset() < textRange.getEndOffset())
        {
          if (whiteSpace.containsLineFeeds()) {
            whiteSpace.setLineFeedsAreReadOnly(true);
          } else {
            whiteSpace.setIsReadOnly(true);
          }
        }

        tokenBlock = tokenBlock.getNextBlock();
      }
      processor.formatWithoutRealModifications();
      processor.performModifications(model);
    }
    catch (FormattingModelInconsistencyException e) {
      LOG.error(e);
    }
    finally{
      enableFormatting();
    }
  }

  @Override
  public int adjustLineIndent(final FormattingModel model,
                              final CodeStyleSettings settings,
                              final CommonCodeStyleSettings.IndentOptions indentOptions,
                              final int offset,
                              final TextRange affectedRange) throws IncorrectOperationException {
    disableFormatting();
    try {
      validateModel(model);
      if (model instanceof PsiBasedFormattingModel) {
        ((PsiBasedFormattingModel)model).canModifyAllWhiteSpaces();
      }
      final FormattingDocumentModel documentModel = model.getDocumentModel();
      final FormatProcessor processor = buildProcessorAndWrapBlocks(model, settings, indentOptions, affectedRange, offset);
      final LeafBlockWrapper blockAfterOffset = processor.getBlockRangesMap().getBlockAtOrAfter(offset);
      if (blockAfterOffset != null && blockAfterOffset.contains(offset)) {
        return offset;
      }
      WhiteSpace whiteSpace = blockAfterOffset != null ? blockAfterOffset.getWhiteSpace() : processor.getLastWhiteSpace();
      return adjustLineIndent(offset, documentModel, processor, indentOptions, model, whiteSpace,
                              blockAfterOffset != null ? blockAfterOffset.getNode() : null);
    }
    catch (FormattingModelInconsistencyException e) {
      LOG.error(e);
    }
    finally {
      enableFormatting();
    }
    return offset;
  }

  @NotNull
  private static FormatProcessor buildProcessorAndWrapBlocks(final FormattingModel model,
                                                             CodeStyleSettings settings,
                                                             CommonCodeStyleSettings.IndentOptions indentOptions,
                                                             @Nullable TextRange affectedRange,
                                                             int offset) {
    FormattingDocumentModel docModel = model.getDocumentModel();
    Block rootBlock = model.getRootBlock();
    return buildProcessorAndWrapBlocks(docModel, rootBlock, settings, indentOptions, new FormatTextRanges(affectedRange, true), offset);
  }

  private static FormatProcessor buildProcessorAndWrapBlocks(final FormattingDocumentModel docModel,
                                                             Block rootBlock,
                                                             CodeStyleSettings settings,
                                                             CommonCodeStyleSettings.IndentOptions indentOptions,
                                                             @Nullable FormatTextRanges affectedRanges) {
    return buildProcessorAndWrapBlocks(docModel, rootBlock, settings, indentOptions, affectedRanges, -1);
  }

  private static FormatProcessor buildProcessorAndWrapBlocks(final FormattingDocumentModel docModel,
                                                             Block rootBlock,
                                                             CodeStyleSettings settings,
                                                             CommonCodeStyleSettings.IndentOptions indentOptions,
                                                             @Nullable FormatTextRanges affectedRanges,
                                                             int interestingOffset)
  {
    FormatOptions options = new FormatOptions(settings, indentOptions, affectedRanges, interestingOffset);
    FormatProcessor processor = new FormatProcessor(
      docModel, rootBlock, options, FormattingProgressCallback.EMPTY
    );
    //noinspection StatementWithEmptyBody
    while (!processor.iteration()) ;
    return processor;
  }

  private static int adjustLineIndent(
    final int offset,
    final FormattingDocumentModel documentModel,
    final FormatProcessor processor,
    final CommonCodeStyleSettings.IndentOptions indentOptions,
    final FormattingModel model,
    final WhiteSpace whiteSpace,
    ASTNode nodeAfter)
  {
    boolean wsContainsCaret = whiteSpace.getStartOffset() <= offset && offset < whiteSpace.getEndOffset();

    int lineStartOffset = getLineStartOffset(offset, whiteSpace, documentModel);

    final IndentInfo indent = calcIndent(offset, documentModel, processor, whiteSpace);

    final String newWS = whiteSpace.generateWhiteSpace(indentOptions, lineStartOffset, indent).toString();
    if (!whiteSpace.equalsToString(newWS)) {
      try {
        if (model instanceof FormattingModelEx) {
          ((FormattingModelEx) model).replaceWhiteSpace(whiteSpace.getTextRange(), nodeAfter, newWS);
        }
        else {
          model.replaceWhiteSpace(whiteSpace.getTextRange(), newWS);
        }
      }
      finally {
        model.commitChanges();
      }
    }

    final int defaultOffset = offset - whiteSpace.getLength() + newWS.length();

    if (wsContainsCaret) {
      final int ws = whiteSpace.getStartOffset()
                     + CharArrayUtil.shiftForward(newWS, Math.max(0, lineStartOffset - whiteSpace.getStartOffset()), " \t");
      return Math.max(defaultOffset, ws);
    } else {
      return defaultOffset;
    }
  }

  private static boolean hasContentAfterLineBreak(final FormattingDocumentModel documentModel, final int offset, final WhiteSpace whiteSpace) {
    return documentModel.getLineNumber(offset) == documentModel.getLineNumber(whiteSpace.getEndOffset()) &&
           documentModel.getTextLength() != whiteSpace.getEndOffset();
  }

  @Override
  public String getLineIndent(final FormattingModel model,
                              final CodeStyleSettings settings,
                              final CommonCodeStyleSettings.IndentOptions indentOptions,
                              final int offset,
                              final TextRange affectedRange) {
    final FormattingDocumentModel documentModel = model.getDocumentModel();
    final Block block = model.getRootBlock();
    if (block.getTextRange().isEmpty()) return null; // handing empty document case
    final FormatProcessor processor = buildProcessorAndWrapBlocks(model, settings, indentOptions, affectedRange, offset);
    WhiteSpace whiteSpace = getWhiteSpaceAtOffset(offset, processor);
    if (whiteSpace != null) {
      final IndentInfo indent = calcIndent(offset, documentModel, processor, whiteSpace);
      return indent.generateNewWhiteSpace(indentOptions);
    }
    return null;
  }

  @Nullable
  private static WhiteSpace getWhiteSpaceAtOffset(int offset,
                                                  @NotNull FormatProcessor formatProcessor) {
    final LeafBlockWrapper blockAfterOffset = formatProcessor.getBlockRangesMap().getBlockAtOrAfter(offset);
    if (blockAfterOffset != null) {
      if (!blockAfterOffset.contains(offset)) return blockAfterOffset.getWhiteSpace();
    }
    else {
      if (offset >= formatProcessor.getLastWhiteSpace().getStartOffset()) {
        return formatProcessor.getLastWhiteSpace();
      }
    }
    return null;
  }

  private static IndentInfo calcIndent(int offset, FormattingDocumentModel documentModel, FormatProcessor processor, WhiteSpace whiteSpace) {
    processor.setAllWhiteSpacesAreReadOnly();
    whiteSpace.setLineFeedsAreReadOnly(true);
    final IndentInfo indent;
    if (hasContentAfterLineBreak(documentModel, offset, whiteSpace)) {
      whiteSpace.setReadOnly(false);
      processor.formatWithoutRealModifications();
      indent = new IndentInfo(0, whiteSpace.getIndentOffset(), whiteSpace.getSpaces());
    }
    else {
      indent = processor.getIndentAt(offset);
    }
    return indent;
  }

  public static String getText(final FormattingDocumentModel documentModel) {
    return getCharSequence(documentModel).toString();
  }

  private static CharSequence getCharSequence(final FormattingDocumentModel documentModel) {
    return documentModel.getText(new TextRange(0, documentModel.getTextLength()));
  }

  private static int getLineStartOffset(final int offset,
                                        final WhiteSpace whiteSpace,
                                        final FormattingDocumentModel documentModel) {
    int lineStartOffset = offset;

    CharSequence text = getCharSequence(documentModel);
    lineStartOffset = CharArrayUtil.shiftBackwardUntil(text, lineStartOffset, " \t\n");
    if (lineStartOffset > whiteSpace.getStartOffset()) {
      if (lineStartOffset >= text.length()) lineStartOffset = text.length() - 1;
      final int wsStart = whiteSpace.getStartOffset();
      int prevEnd;

      if (text.charAt(lineStartOffset) == '\n'
          && wsStart <= (prevEnd = documentModel.getLineStartOffset(documentModel.getLineNumber(lineStartOffset - 1))) &&
          documentModel.getText(new TextRange(prevEnd, lineStartOffset)).toString().trim().length() == 0 // ws consists of space only, it is not true for <![CDATA[
         ) {
        lineStartOffset--;
      }
      lineStartOffset = CharArrayUtil.shiftBackward(text, lineStartOffset, "\t ");
      if (lineStartOffset < 0) lineStartOffset = 0;
      if (lineStartOffset != offset && text.charAt(lineStartOffset) == '\n') {
        lineStartOffset++;
      }
    }
    return lineStartOffset;
  }


  @Override
  public FormattingModel createFormattingModelForPsiFile(@NotNull final PsiFile file,
                                                         @NotNull final Block rootBlock,
                                                         final CodeStyleSettings settings) {
    return new PsiBasedFormattingModel(file, rootBlock, FormattingDocumentModelImpl.createOn(file));
  }

  @Override
  public Indent getSpaceIndent(final int spaces, final boolean relative) {
    return getIndent(Indent.Type.SPACES, spaces, relative, false);
  }

  @Override
  public Indent getIndent(@NotNull Indent.Type type, boolean relativeToDirectParent, boolean enforceIndentToChildren) {
    return getIndent(type, 0, relativeToDirectParent, enforceIndentToChildren);
  }

  @Override
  public Indent getSmartIndent(@NotNull Indent.Type type) {
    return new ExpandableIndent(type);
  }

  @Override
  public Indent getIndent(@NotNull Indent.Type type, int spaces, boolean relativeToDirectParent, boolean enforceIndentToChildren) {
    return new IndentImpl(type, false, spaces, relativeToDirectParent, enforceIndentToChildren);
  }

  @Override
  public Indent getAbsoluteLabelIndent() {
    return myAbsoluteLabelIndent;
  }

  @Override
  @NotNull
  public Spacing createSafeSpacing(final boolean shouldKeepLineBreaks, final int keepBlankLines) {
    return getSpacingImpl(0, 0, 0, false, true, shouldKeepLineBreaks, keepBlankLines, false, 0);
  }

  @Override
  @NotNull
  public Spacing createKeepingFirstColumnSpacing(final int minSpace,
                                                 final int maxSpace,
                                                 final boolean keepLineBreaks,
                                                 final int keepBlankLines) {
    return getSpacingImpl(minSpace, maxSpace, -1, false, false, keepLineBreaks, keepBlankLines, true, 0);
  }

  @Override
  @NotNull
  public Spacing createSpacing(final int minSpaces, final int maxSpaces, final int minLineFeeds, final boolean keepLineBreaks, final int keepBlankLines,
                               final int prefLineFeeds) {
    return getSpacingImpl(minSpaces, maxSpaces, minLineFeeds, false, false, keepLineBreaks, keepBlankLines, false, prefLineFeeds);
  }

  private final Map<SpacingImpl,SpacingImpl> ourSharedProperties = new HashMap<>();
  private final SpacingImpl ourSharedSpacing = new SpacingImpl(-1,-1,-1,false,false,false,-1,false,0);

  private SpacingImpl getSpacingImpl(final int minSpaces,
                                     final int maxSpaces,
                                     final int minLineFeeds,
                                     final boolean readOnly,
                                     final boolean safe,
                                     final boolean keepLineBreaksFlag,
                                     final int keepLineBreaks,
                                     final boolean keepFirstColumn,
                                     int prefLineFeeds)
  {
    synchronized(ourSharedSpacing) {
      ourSharedSpacing.init(minSpaces, maxSpaces, minLineFeeds, readOnly, safe, keepLineBreaksFlag, keepLineBreaks, keepFirstColumn, prefLineFeeds);
      SpacingImpl spacing = ourSharedProperties.get(ourSharedSpacing);

      if (spacing == null) {
        spacing = new SpacingImpl(minSpaces, maxSpaces, minLineFeeds, readOnly, safe, keepLineBreaksFlag, keepLineBreaks, keepFirstColumn, prefLineFeeds);
        ourSharedProperties.put(spacing, spacing);
      }
      return spacing;
    }
  }

  @Override
  public Indent getAbsoluteNoneIndent() {
    return myAbsoluteNoneIndent;
  }

  @Override
  public Indent getLabelIndent() {
    return myLabelIndent;
  }

  @Override
  public Indent getContinuationIndent(boolean relative) {
    return relative ? myContinuationIndentRelativeToDirectParent : myContinuationIndentNotRelativeToDirectParent;
  }

  //is default
  @Override
  public Indent getContinuationWithoutFirstIndent(boolean relative) {
    return relative ? myContinuationWithoutFirstIndentRelativeToDirectParent : myContinuationWithoutFirstIndentNotRelativeToDirectParent;
  }

  @Override
  public boolean isDisabled() {
    return myIsDisabledCount.get() > 0;
  }

  private void disableFormatting() {
    myIsDisabledCount.incrementAndGet();
  }

  private void enableFormatting() {
    int old = myIsDisabledCount.getAndDecrement();
    if (old <= 0) {
      LOG.error("enableFormatting()/disableFormatting() not paired. DisabledLevel = " + old);
    }
  }

  @Nullable
  public <T> T runWithFormattingDisabled(@NotNull Computable<T> runnable) {
    disableFormatting();
    try {
      return runnable.compute();
    }
    finally {
      enableFormatting();
    }
  }

  private abstract static class MyFormattingTask implements SequentialTask {
    private FormatProcessor myProcessor;
    private boolean         myDone;

    @Override
    public void prepare() {
      myProcessor = buildProcessor();
    }

    @Override
    public boolean isDone() {
      return myDone;
    }

    @Override
    public boolean iteration() {
      return myDone = myProcessor.iteration();
    }

    @Override
    public void stop() {
      myProcessor.stopSequentialProcessing();
      myDone = true;
    }

    @NotNull
    protected abstract FormatProcessor buildProcessor();
  }

  private static void validateModel(FormattingModel model) throws FormattingModelInconsistencyException {
    FormattingDocumentModel documentModel = model.getDocumentModel();
    Document document = documentModel.getDocument();
    Block rootBlock = model.getRootBlock();
    if (rootBlock instanceof ASTBlock) {
      PsiElement rootElement = ((ASTBlock)rootBlock).getNode().getPsi();
      if (!rootElement.isValid()) {
        throw new FormattingModelInconsistencyException("Invalid root block PSI element");
      }
      PsiFile file = rootElement.getContainingFile();
      Project project = file.getProject();
      PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
      if (documentManager.isUncommited(document)) {
        throw new FormattingModelInconsistencyException("Uncommitted document");
      }
      if (document.getTextLength() != file.getTextLength()) {
        throw new FormattingModelInconsistencyException(
          "Document length " + document.getTextLength() +
          " doesn't match PSI file length " + file.getTextLength() + ", language: " + file.getLanguage()
        );
      }
    }
  }

  private static class FormattingModelInconsistencyException extends Exception {
    FormattingModelInconsistencyException(String message) {
      super(message);
    }
  }
}
