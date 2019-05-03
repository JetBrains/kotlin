// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.templateLanguages;

import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageExtension;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.SingleRootFileViewProvider;
import com.intellij.psi.impl.DebugUtil;
import com.intellij.psi.impl.source.PsiFileImpl;
import com.intellij.psi.impl.source.tree.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.CharTable;
import com.intellij.util.LocalTimeCounter;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author peter
 */
public class TemplateDataElementType extends IFileElementType implements ITemplateDataElementType {
  public static final LanguageExtension<TreePatcher> TREE_PATCHER =
    new LanguageExtension<>("com.intellij.lang.treePatcher", new SimpleTreePatcher());

  @NotNull private final IElementType myTemplateElementType;
  @NotNull private final IElementType myOuterElementType;

  public TemplateDataElementType(@NonNls String debugName,
                                 Language language,
                                 @NotNull IElementType templateElementType,
                                 @NotNull IElementType outerElementType) {
    super(debugName, language);
    myTemplateElementType = templateElementType;
    myOuterElementType = outerElementType;
  }

  protected Lexer createBaseLexer(TemplateLanguageFileViewProvider viewProvider) {
    return LanguageParserDefinitions.INSTANCE.forLanguage(viewProvider.getBaseLanguage())
                                             .createLexer(viewProvider.getManager().getProject());
  }

  protected LanguageFileType createTemplateFakeFileType(final Language language) {
    return new TemplateFileType(language);
  }

  @Override
  public ASTNode parseContents(@NotNull ASTNode chameleon) {
    final CharTable charTable = SharedImplUtil.findCharTableByTree(chameleon);
    final FileElement fileElement = TreeUtil.getFileElement((TreeElement)chameleon);
    final PsiFile psiFile = (PsiFile)fileElement.getPsi();
    PsiFile originalPsiFile = psiFile.getOriginalFile();

    final TemplateLanguageFileViewProvider viewProvider = (TemplateLanguageFileViewProvider)originalPsiFile.getViewProvider();

    final Language templateLanguage = getTemplateFileLanguage(viewProvider);
    final CharSequence sourceCode = chameleon.getChars();

    RangeCollector collector = new RangeCollector();
    final PsiFile templatePsiFile = createTemplateFile(psiFile, templateLanguage, sourceCode, viewProvider, collector);

    final FileElement templateFileElement = ((PsiFileImpl)templatePsiFile).calcTreeElement();

    return DebugUtil.performPsiModification("template language parsing", () -> {
      prepareParsedTemplateFile(templateFileElement);
      insertOuterElementsAndRemoveRanges(templateFileElement, sourceCode, collector, charTable);

      TreeElement childNode = templateFileElement.getFirstChildNode();

      DebugUtil.checkTreeStructure(templateFileElement);
      DebugUtil.checkTreeStructure(chameleon);
      if (fileElement != chameleon) {
        DebugUtil.checkTreeStructure(psiFile.getNode());
        DebugUtil.checkTreeStructure(originalPsiFile.getNode());
      }

      return childNode;
    });
  }

  protected void prepareParsedTemplateFile(@NotNull FileElement root) {
  }

  protected Language getTemplateFileLanguage(TemplateLanguageFileViewProvider viewProvider) {
    return viewProvider.getTemplateDataLanguage();
  }

  /**
   * Creates psi tree without template tokens. The result PsiFile can contain additional elements.
   * Ranges of the removed tokens/additional elements should be stored in the rangeCollector
   *
   * @param psiFile          chameleon's psi file
   * @param templateLanguage template language to parse
   * @param sourceCode       source code: base language with template language
   * @param rangeCollector   collector for ranges with non-template/additional elements
   * @return template psiFile
   */
  protected PsiFile createTemplateFile(final PsiFile psiFile,
                                       final Language templateLanguage,
                                       final CharSequence sourceCode,
                                       final TemplateLanguageFileViewProvider viewProvider,
                                       @NotNull RangeCollector rangeCollector) {
    final CharSequence templateSourceCode = createTemplateText(sourceCode, createBaseLexer(viewProvider), rangeCollector);
    return createPsiFileFromSource(templateLanguage, templateSourceCode, psiFile.getManager());
  }

  /**
   * Creates source code without template tokens. May add additional pieces of code.
   * Ranges of such additions should be added in rangeCollector using {@link RangeCollector#addRangeToRemove(TextRange)}for later removal from the resulting tree
   *
   * @param sourceCode     source code with base and template languages
   * @param baseLexer      base language lexer
   * @param rangeCollector collector for ranges with non-template/additional symbols
   * @return template source code
   */
  protected CharSequence createTemplateText(@NotNull CharSequence sourceCode,
                                            @NotNull Lexer baseLexer,
                                            @NotNull RangeCollector rangeCollector) {
    StringBuilder result = new StringBuilder(sourceCode.length());
    baseLexer.start(sourceCode);

    TextRange currentRange = TextRange.EMPTY_RANGE;
    while (baseLexer.getTokenType() != null) {
      TextRange newRange = TextRange.create(baseLexer.getTokenStart(), baseLexer.getTokenEnd());
      assert currentRange.getEndOffset() == newRange.getStartOffset() :
        "Inconsistent tokens stream from " + baseLexer +
        ": " + getRangeDump(currentRange, sourceCode) + " followed by " + getRangeDump(newRange, sourceCode);
      currentRange = newRange;
      if (baseLexer.getTokenType() == myTemplateElementType) {
        appendCurrentTemplateToken(result, sourceCode, baseLexer, rangeCollector);
      }
      else {
        rangeCollector.addOuterRange(currentRange);
      }
      baseLexer.advance();
    }

    return result;
  }

  @NotNull
  private static String getRangeDump(@NotNull TextRange range, @NotNull CharSequence sequence) {
    return "'" + StringUtil.escapeLineBreak(range.subSequence(sequence).toString()) + "' " + range;
  }

  protected void appendCurrentTemplateToken(@NotNull StringBuilder result,
                                            @NotNull CharSequence buf,
                                            @NotNull Lexer lexer,
                                            @NotNull RangeCollector collector) {
    result.append(buf, lexer.getTokenStart(), lexer.getTokenEnd());
  }


  /**
   * Builds the merged tree with inserting outer language elements and removing additional elements according to the ranges from rangeCollector
   *
   * @param templateFileElement parsed template data language file without outer elements and with possible custom additions
   * @param sourceCode          original source code (include template data language and template language)
   * @param rangeCollector      collector for ranges with non-template/additional elements
   */
  private void insertOuterElementsAndRemoveRanges(@NotNull TreeElement templateFileElement,
                                                  @NotNull CharSequence sourceCode,
                                                  @NotNull RangeCollector rangeCollector,
                                                  @NotNull CharTable charTable) {
    TreePatcher templateTreePatcher = TREE_PATCHER.forLanguage(templateFileElement.getPsi().getLanguage());

    LeafElement currentLeaf = TreeUtil.findFirstLeaf(templateFileElement);

    //we use manual offset counter because node.getStartOffset() is expensive here
    int currentLeafOffset = 0;

    for (TextRange rangeToProcess: rangeCollector.myOuterAndRemoveRanges) {
      int rangeStartOffset = rangeToProcess.getStartOffset();

      while (currentLeaf != null &&
             currentLeafOffset < rangeStartOffset &&
             !shouldRemoveRangeInsideLeaf(currentLeaf, currentLeafOffset, rangeToProcess)) {
        currentLeafOffset += currentLeaf.getTextLength();

        if (currentLeafOffset > rangeStartOffset) {
          int splitOffset = currentLeaf.getTextLength() - (currentLeafOffset - rangeStartOffset);
          currentLeaf = templateTreePatcher.split(currentLeaf, splitOffset, charTable);
          currentLeafOffset = rangeStartOffset;
        }
        currentLeaf = TreeUtil.nextLeaf(currentLeaf);
      }

      if (rangeToProcess instanceof RangeToRemove) {
        assert currentLeaf != null;
        currentLeaf = removeElementsForRange(currentLeaf, currentLeafOffset, rangeToProcess, templateTreePatcher, charTable);
      }
      else {
        if (currentLeaf == null) {
          insertLastOuterElementForRange((CompositeElement)templateFileElement, rangeToProcess, sourceCode, rangeCollector, charTable);
        }
        else {
          currentLeaf = insertOuterElementFromRange(currentLeaf, rangeToProcess, sourceCode, templateTreePatcher, charTable);
        }
      }
    }

    if (ApplicationManager.getApplication().isUnitTestMode()) {
      String after = templateFileElement.getText();
      assert after.contentEquals(sourceCode) :
        "Text presentation for the new tree must be the same: \nbefore: " + sourceCode + "\nafter: " + after;
    }
  }

  private static boolean shouldRemoveRangeInsideLeaf(LeafElement currentLeaf,
                                                     int currentLeafOffset,
                                                     TextRange rangeToProcess) {
    return rangeToProcess instanceof RangeToRemove && currentLeafOffset + currentLeaf.getTextLength() > rangeToProcess.getEndOffset();
  }

  private void insertLastOuterElementForRange(@NotNull CompositeElement templateFileElement, @NotNull TextRange outerElementRange,
                                              @NotNull CharSequence sourceCode,
                                              @NotNull RangeCollector collector,
                                              @NotNull CharTable charTable) {
    assert isLastRange(collector.myOuterAndRemoveRanges, outerElementRange) :
      "This should only happen for the last inserted range. Got " + collector.myOuterAndRemoveRanges.lastIndexOf(outerElementRange) +
      " of " + (collector.myOuterAndRemoveRanges.size() - 1);
    templateFileElement.rawAddChildren(
      createOuterLanguageElement(charTable.intern(outerElementRange.subSequence(sourceCode)), myOuterElementType)
    );
  }

  @NotNull
  private LeafElement insertOuterElementFromRange(@NotNull LeafElement currentLeaf, @NotNull TextRange outerElementRange,
                                                  @NotNull CharSequence sourceCode,
                                                  @NotNull TreePatcher templateTreePatcher,
                                                  @NotNull CharTable charTable) {
    final OuterLanguageElementImpl newLeaf =
      createOuterLanguageElement(charTable.intern(outerElementRange.subSequence(sourceCode)), myOuterElementType);
    CompositeElement parent = currentLeaf.getTreeParent();
    templateTreePatcher.insert(parent, currentLeaf, newLeaf);
    return newLeaf;
  }

  @Nullable
  private static LeafElement removeElementsForRange(@NotNull LeafElement startLeaf,
                                                    int startLeafOffset,
                                                    @NotNull TextRange rangeToRemove,
                                                    @NotNull TreePatcher templateTreePatcher,
                                                    @NotNull CharTable charTable) {
    @Nullable LeafElement nextLeaf = startLeaf;
    int nextLeafStartOffset = startLeafOffset;
    Collection<TreeElement> leavesToRemove = new ArrayList<>();
    while (nextLeaf != null && rangeToRemove.containsRange(nextLeafStartOffset, nextLeafStartOffset + nextLeaf.getTextLength())) {
      leavesToRemove.add(nextLeaf);
      nextLeafStartOffset += nextLeaf.getTextLength();
      nextLeaf = TreeUtil.nextLeaf(nextLeaf);
    }

    nextLeaf = splitOrRemoveRangeInsideLeafIfOverlap(nextLeaf, nextLeafStartOffset, rangeToRemove, templateTreePatcher, charTable);

    for (TreeElement element: leavesToRemove) {
      element.rawRemove();
    }
    return nextLeaf;
  }

  /**
   * Removes part the nextLeaf that intersects rangeToRemove.
   * If nextLeaf doesn't intersect rangeToRemove the method returns the nextLeaf without changes
   *
   * @return new leaf after removing the range or original nextLeaf if nothing changed
   */
  @Nullable
  private static LeafElement splitOrRemoveRangeInsideLeafIfOverlap(@Nullable LeafElement nextLeaf,
                                                                   int nextLeafStartOffset,
                                                                   @NotNull TextRange rangeToRemove,
                                                                   @NotNull TreePatcher templateTreePatcher,
                                                                   @NotNull CharTable charTable) {
    if (nextLeaf == null) return null;
    if (nextLeafStartOffset >= rangeToRemove.getEndOffset()) return nextLeaf;

    if (rangeToRemove.getStartOffset() > nextLeafStartOffset) {
      return templateTreePatcher.removeRange(nextLeaf, rangeToRemove.shiftLeft(nextLeafStartOffset), charTable);
    }

    int offsetToSplit = rangeToRemove.getEndOffset() - nextLeafStartOffset;
    return removeLeftPartOfLeaf(nextLeaf, offsetToSplit, templateTreePatcher, charTable);
  }

  /**
   * Splits the node according to the offsetToSplit and remove left leaf
   *
   * @return right part of the split node
   */
  @NotNull
  private static LeafElement removeLeftPartOfLeaf(@NotNull LeafElement nextLeaf,
                                                  int offsetToSplit,
                                                  @NotNull TreePatcher templateTreePatcher,
                                                  @NotNull CharTable charTable) {
    LeafElement lLeaf = templateTreePatcher.split(nextLeaf, offsetToSplit, charTable);
    LeafElement rLeaf = TreeUtil.nextLeaf(lLeaf);
    assert rLeaf != null;
    lLeaf.rawRemove();
    return rLeaf;
  }

  private static boolean isLastRange(@NotNull List<TextRange> outerElementsRanges, @NotNull TextRange outerElementRange) {
    return outerElementsRanges.get(outerElementsRanges.size() - 1) == outerElementRange;
  }

  protected OuterLanguageElementImpl createOuterLanguageElement(@NotNull CharSequence internedTokenText,
                                                                @NotNull IElementType outerElementType) {
    return new OuterLanguageElementImpl(outerElementType, internedTokenText);
  }

  protected PsiFile createPsiFileFromSource(final Language language, CharSequence sourceCode, PsiManager manager) {
    @NonNls final LightVirtualFile virtualFile =
      new LightVirtualFile("foo", createTemplateFakeFileType(language), sourceCode, LocalTimeCounter.currentTime());

    FileViewProvider viewProvider = new SingleRootFileViewProvider(manager, virtualFile, false) {
      @Override
      @NotNull
      public Language getBaseLanguage() {
        return language;
      }
    };

    // Since we're already inside a template language PSI that was built regardless of the file size (for whatever reason),
    // there should also be no file size checks for template data files.
    SingleRootFileViewProvider.doNotCheckFileSizeLimit(virtualFile);

    return viewProvider.getPsi(language);
  }

  protected static class TemplateFileType extends LanguageFileType {
    private final Language myLanguage;

    protected TemplateFileType(final Language language) {
      super(language);
      myLanguage = language;
    }

    @Override
    @NotNull
    public String getDefaultExtension() {
      return "";
    }

    @Override
    @NotNull
    @NonNls
    public String getDescription() {
      return "fake for language" + myLanguage.getID();
    }

    @Override
    @Nullable
    public Icon getIcon() {
      return null;
    }

    @Override
    @NotNull
    @NonNls
    public String getName() {
      return myLanguage.getID();
    }
  }

  /**
   * This collector is used for storing ranges of outer elements and ranges of artificial elements, that should be stripped from the resulting tree
   * At the time of creating source code for the data language we need to memorize positions with template language elements.
   * For such positions we use {@link RangeCollector#addOuterRange}
   * Sometimes to build a correct tree we need to insert additional symbols into resulting source:
   * e.g. put an identifier instead of the base language fragment: {@code something={% $var %}} => {@code something=dummyidentifier}
   * that must be removed after building the tree.
   * For such additional symbols {@link RangeCollector#addRangeToRemove} must be used
   *
   * @apiNote Please note that all start offsets for the ranges must be in terms of "original source code"
   */
  protected static class RangeCollector {
    private final List<TextRange> myOuterAndRemoveRanges = new ArrayList<>();

    /**
     * Adds range corresponding to the outer element inside original source code.
     * After building the data template tree these ranges will be used for inserting outer language elements
     */
    public void addOuterRange(@NotNull TextRange newRange) {
      if (newRange.isEmpty()) {
        return;
      }
      assertRangeOrder(newRange);

      if (!myOuterAndRemoveRanges.isEmpty()) {
        int lastItemIndex = myOuterAndRemoveRanges.size() - 1;
        TextRange lastRange = myOuterAndRemoveRanges.get(lastItemIndex);
        if (lastRange.getEndOffset() == newRange.getStartOffset() && !(lastRange instanceof RangeToRemove)) {
          myOuterAndRemoveRanges.set(lastItemIndex, TextRange.create(lastRange.getStartOffset(), newRange.getEndOffset()));
          return;
        }
      }
      myOuterAndRemoveRanges.add(newRange);
    }

    /**
     * Adds the range that must be removed from the tree on the stage inserting outer elements.
     * This method should be called after adding "fake" symbols inside the data language text for building syntax correct tree
     */
    public void addRangeToRemove(@NotNull TextRange rangeToRemove) {
      if (rangeToRemove.isEmpty()) {
        return;
      }
      assertRangeOrder(rangeToRemove);

      myOuterAndRemoveRanges.add(new RangeToRemove(rangeToRemove.getStartOffset(), rangeToRemove.getEndOffset()));
    }

    private void assertRangeOrder(@NotNull TextRange newRange) {
      TextRange range = ContainerUtil.getLastItem(myOuterAndRemoveRanges);
      assert range == null || newRange.getStartOffset() >= range.getStartOffset();
    }
  }

  private final static class RangeToRemove extends TextRange {
    private RangeToRemove(int startOffset, int endOffset) {
      super(startOffset, endOffset);
    }
  }
}
