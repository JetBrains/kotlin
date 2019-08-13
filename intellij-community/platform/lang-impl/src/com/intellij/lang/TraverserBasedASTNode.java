/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.lang;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.CharTableImpl;
import com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.ILazyParseableElementType;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.CharTable;
import com.intellij.util.Function;
import com.intellij.util.concurrency.AtomicFieldUpdater;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * PsiBuilder-based read-only ASTNode implementation.
 * <p>
 * <h1>Memory consumption comparison with ordinary PSI API</h1>
 * <pre>
 * PSI memory consumption stays the same, so let's compare AST memory consumption only.
 * Both read-write and read-only PSI require a single PsiBuilder instance to parse a file.
 * The latter then keeps it while the first copies all the needed data and discards it.
 *
 * Here're the numbers for a sample 700K SQL schema:
 *
 *     PsiBuilderImpl (one instance): shallow  ~ 0, retained 2M (700K text & 1.5M member arrays)
 *   PsiBuilderImpl.ProductionMarker: shallow 4.6M, retained same, 101K instances
 *   TraverserBasedASTNode (r/o AST): shallow 4.4M, retained  11M, 115K instances   (1)
 *             TreeElement (r/w AST): shallow 6.3M, retained 8.1M, 133K instances   (2)
 *
 * (1) allocates 4.6M (markers) & 2M (builder) during parsing and 4.4M (nodes) afterwards, hence 11M.
 * (2) allocates same 6.6M for parsing, then copies it to a new 8.1M data structure, GC'ing the original.
 * </pre>
 *
 * @author gregsh
 *
 * @see com.intellij.lang.impl.PsiBuilderImpl
 * @see com.intellij.ide.util.treeView.smartTree.TreeElement
 */
public class TraverserBasedASTNode<N> extends ReadOnlyASTNode {

  @NotNull
  public static <N> FileASTNode createFileNode(@NotNull SyntaxTraverser<N> traverser, @NotNull N node, @NotNull PsiFile psiFile) {
    return new FileNode<>(traverser, node, psiFile);
  }

  @NotNull
  public static FileViewProvider createViewProvider(@NotNull String fileName, @NotNull Language language, @NotNull PsiManager psiManager) {
    return new ReadOnlyViewProvider(fileName, language, psiManager);
  }


  protected final N myNode;
  protected final SyntaxTraverser<N> myTraverser;

  /** @noinspection unused */
  private volatile PsiElement myPsi;
  /** @noinspection unused */
  private volatile ASTNode[] myKids;

  private static final AtomicFieldUpdater<TraverserBasedASTNode, PsiElement> ourPsiUpdater =
    AtomicFieldUpdater.forFieldOfType(TraverserBasedASTNode.class, PsiElement.class);
  private static final AtomicFieldUpdater<TraverserBasedASTNode, ASTNode[]> ourKidsUpdater =
    AtomicFieldUpdater.forFieldOfType(TraverserBasedASTNode.class, ASTNode[].class);


  public TraverserBasedASTNode(@NotNull N node, int index,
                               @Nullable TraverserBasedASTNode<?> parent,
                               @NotNull SyntaxTraverser<N> traverser) {
    super(parent, index);
    myTraverser = traverser;
    myNode = node;
  }

  @NotNull
  @Override
  public IElementType getElementType() {
    return myTraverser.api.typeOf(myNode);
  }

  @NotNull
  @Override
  public CharSequence getChars() {
    return myTraverser.api.textOf(myNode);
  }

  @Override
  public TextRange getTextRange() {
    return myTraverser.api.rangeOf(myNode);
  }

  @Override
  protected ASTNode[] getChildArray() {
    ASTNode[] kids = myKids;
    if (kids != null) return kids;
    return ourKidsUpdater.compareAndSet(this, null, kids = childrenImpl()) ? kids : myKids;
  }

  private ASTNode[] childrenImpl() {
    List<ASTNode> children = myTraverser.children(myNode).transform(CHILD_TRANSFORM(myTraverser, 0)).toList();
    if (!children.isEmpty() || getTreeParent() == null) {
      return children.isEmpty() ? EMPTY_ARRAY : children.toArray(ASTNode.EMPTY_ARRAY);
    }

    // expand (parse) non-file lazy-parseable nodes
    IElementType type = myTraverser.api.typeOf(myNode);
    if (!(type instanceof ILazyParseableElementType)) return EMPTY_ARRAY;
    PsiBuilder builder = ((ILazyParseableElementType)type).parseLight(this);
    SyntaxTraverser<LighterASTNode> s = SyntaxTraverser.lightTraverser(builder);
    // avoid ShiftedNode double-shifting
    int shift = myTraverser.api.rangeOf(myNode).getStartOffset();
    List<ASTNode> childrenLazy = s.api.children(s.getRoot()).transform(CHILD_TRANSFORM(s, shift)).toList();
    return childrenLazy.toArray(ASTNode.EMPTY_ARRAY);
  }

  @NotNull
  protected <NN> TraverserBasedASTNode<NN> createChildNode(int index, NN input, SyntaxTraverser<NN> s, int shift) {
    return shift == 0 ? new TraverserBasedASTNode<>(input, index, this, s) :
           new ShiftedNode<>(this, index, shift, s, input);
  }

  @Override
  public PsiElement getPsi() {
    PsiElement psi = myPsi;
    if (psi != null) return psi;
    return ourPsiUpdater.compareAndSet(this, null, psi = getPsiImpl()) ? psi : myPsi;
  }

  public PsiElement getPsiImpl() {
    IElementType type = getElementType();
    ParserDefinition pd = LanguageParserDefinitions.INSTANCE.forLanguage(type.getLanguage());
    if (pd != null && pd.getWhitespaceTokens().contains(type) ||
        pd == null && type == TokenType.WHITE_SPACE) {
      return new PsiWhiteSpaceImpl(getChars());
    }
    else if (pd != null && pd.getCommentTokens().contains(type)) {
      return new ASTWrapperPsiComment(this);
    }
    else if (pd == null || myNode instanceof LighterASTTokenNode) {
      return new ASTWrapperPsiElement(this);
    }
    else {
      return pd.createElement(this);
    }
  }

  @NotNull
  private <NN> Function<NN, ASTNode> CHILD_TRANSFORM(final SyntaxTraverser<NN> s, final int shift) {
    return new Function<NN, ASTNode>() {
      int index = 0;

      @Override
      public ASTNode fun(@Nullable NN input) {
        return createChildNode(index++, input, s, shift);
      }
    };
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof TraverserBasedASTNode)) return false;
    return Comparing.equal(myNode, ((TraverserBasedASTNode)obj).myNode);
  }

  @Override
  public int hashCode() {
    return myNode.hashCode();
  }

  @NotNull
  public static PsiElement[] getChildrenAsPsiArray(@NotNull ASTNode node) {
    ASTNode[] kids = ((ReadOnlyASTNode)node).getChildArray();
    if (kids.length == 0) return PsiElement.EMPTY_ARRAY;
    PsiElement[] result = new PsiElement[kids.length];
    int idx = 0;
    for (ASTNode o : kids) {
      PsiElement psi = o.getPsi();
      if (psi != null) {
        result[idx++] = psi;
      }
    }
    return result;
  }

  @Nullable
  public static PsiElement getFirstPsiChild(@NotNull ASTNode node) {
    ASTNode[] kids = ((ReadOnlyASTNode)node).getChildArray();
    for (ASTNode o : kids) {
      PsiElement psi = o.getPsi();
      if (psi != null) return psi;
    }
    return null;
  }

  @Nullable
  public static PsiElement getLastPsiChild(@NotNull ASTNode node) {
    ASTNode[] kids = ((ReadOnlyASTNode)node).getChildArray();
    for (int i = kids.length - 1; i >= 0; i--) {
      ASTNode o = kids[i];
      PsiElement psi = o.getPsi();
      if (psi != null) return psi;
    }
    return null;
  }

  private static class FileNode<N> extends TraverserBasedASTNode<N> implements FileASTNode {
    private final PsiFile myPsiFile;
    private final CharTableImpl myCharTable = new CharTableImpl();

    FileNode(@NotNull SyntaxTraverser<N> traverser, @NotNull N node, @NotNull PsiFile psiFile) {
      super(node, -1, null, traverser);
      myPsiFile = psiFile;
      ((ReadOnlyViewProvider)psiFile.getViewProvider()).forceCachedPsi(psiFile);
    }

    @Override
    public PsiElement getPsi() {
      return myPsiFile;
    }

    @NotNull
    @Override
    public CharTable getCharTable() {
      return myCharTable;
    }

    @Override
    public boolean isParsed() {
      return true;
    }

    @NotNull
    @Override
    public LighterAST getLighterAST() {
      return new TreeBackedLighterAST(this);
    }
  }

  private static class ShiftedNode<N> extends TraverserBasedASTNode<N> {

    private final int myShift;

    ShiftedNode(TraverserBasedASTNode<?> parent, int index, int shift, @NotNull SyntaxTraverser<N> traverser, @NotNull N node) {
      super(node, index, parent, traverser);
      myShift = shift;
    }

    @Override
    public TextRange getTextRange() {
      return super.getTextRange().shiftRight(myShift);
    }

    @NotNull
    @Override
    protected <NN> TraverserBasedASTNode<NN> createChildNode(int index, NN input, SyntaxTraverser<NN> s, int shift) {
      return new ShiftedNode<>(this, index, myShift + shift, s, input);
    }
  }

  private static class ASTWrapperPsiComment extends ASTWrapperPsiElement implements PsiComment {
    ASTWrapperPsiComment(@NotNull ASTNode node) {
      super(node);
    }

    @Override
    public IElementType getTokenType() {
      return getNode().getElementType();
    }
  }

  private static class ReadOnlyViewProvider extends SingleRootFileViewProvider {

    ReadOnlyViewProvider(@NotNull String name, @NotNull Language language, @NotNull PsiManager psiManager) {
      super(psiManager, new LightVirtualFile(name, language, ""), false);
    }

    @Nullable
    @Override
    protected PsiFile createFile(@NotNull Project project, @NotNull VirtualFile file, @NotNull FileType fileType) {
      throw new UnsupportedOperationException("Should not try to create mutable PSI");
    }

    @Nullable
    @Override
    protected PsiFile createFile(@NotNull Language lang) {
      throw new UnsupportedOperationException("Should not try to create mutable PSI");
    }
  }
}
