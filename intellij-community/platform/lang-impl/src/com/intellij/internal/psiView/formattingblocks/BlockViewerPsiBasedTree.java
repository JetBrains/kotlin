// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.internal.psiView.formattingblocks;

import com.intellij.application.options.CodeStyle;
import com.intellij.diagnostic.AttachmentFactory;
import com.intellij.formatting.ASTBlock;
import com.intellij.formatting.Block;
import com.intellij.formatting.FormattingModel;
import com.intellij.formatting.FormattingModelBuilder;
import com.intellij.internal.psiView.PsiViewerDialog;
import com.intellij.internal.psiView.ViewerPsiBasedTree;
import com.intellij.lang.ASTNode;
import com.intellij.lang.LanguageFormatting;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.tree.AsyncTreeModel;
import com.intellij.ui.tree.StructureTreeModel;
import com.intellij.ui.tree.TreeVisitor;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.JBTreeTraverser;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.HashMap;
import java.util.Set;

import static com.intellij.internal.psiView.PsiViewerDialog.initTree;

public class BlockViewerPsiBasedTree implements ViewerPsiBasedTree {

  @NotNull
  private final JPanel myBlockStructurePanel;
  @NotNull
  private final Tree myBlockTree;
  @NotNull
  private final Project myProject;
  @NotNull
  private final PsiTreeUpdater myUpdater;
  @Nullable
  private volatile HashMap<PsiElement, BlockTreeNode> myPsiToBlockMap;
  private AsyncTreeModel myTreeModel;
  private Disposable myTreeModelDisposable = Disposer.newDisposable();

  public BlockViewerPsiBasedTree(@NotNull Project project, @NotNull PsiTreeUpdater updater) {
    myProject = project;
    myUpdater = updater;
    myBlockTree = new Tree(new DefaultTreeModel(new DefaultMutableTreeNode()));
    myBlockStructurePanel = new JPanel(new BorderLayout());
    myBlockStructurePanel.add(ScrollPaneFactory.createScrollPane(myBlockTree));
    myBlockStructurePanel.setBorder(IdeBorderFactory.createBorder());
    initTree(myBlockTree);
  }

  @Override
  public void reloadTree(@Nullable PsiElement rootRootElement, @NotNull String text) {
    resetBlockTree();
    buildBlockTree(rootRootElement);
  }

  @Override
  public void selectNodeFromPsi(@Nullable PsiElement element) {
    if (myTreeModel != null && element != null) {
      BlockTreeNode currentBlockNode = findBlockNode(element);
      if (currentBlockNode != null) {
        selectBlockNode(currentBlockNode);
      }
    }
  }

  @NotNull
  @Override
  public JComponent getComponent() {
    return myBlockStructurePanel;
  }

  @Override
  public boolean isFocusOwner() {
    return myBlockTree.isFocusOwner();
  }

  @Override
  public void focusTree() {
    IdeFocusManager.getInstance(myProject).requestFocus(myBlockTree, true);
  }

  @Override
  public void dispose() {
    resetBlockTree();
  }

  private void resetBlockTree() {
    myBlockTree.removeAll();
    if (myTreeModel != null) {
      Disposer.dispose(myTreeModelDisposable);
      myTreeModel = null;
      myTreeModelDisposable = Disposer.newDisposable();
    }
    myPsiToBlockMap = null;
    ViewerPsiBasedTree.removeListenerOfClass(myBlockTree, BlockTreeSelectionListener.class);
  }


  private void buildBlockTree(@Nullable PsiElement rootElement) {
    Block rootBlock = rootElement == null ? null : buildBlocks(rootElement);
    if (rootBlock == null) {
      myTreeModel = null;
      myBlockTree.setRootVisible(false);
      myBlockTree.setVisible(false);
      return;
    }

    myBlockTree.setVisible(true);
    BlockTreeStructure blockTreeStructure = new BlockTreeStructure();
    BlockTreeNode rootNode = new BlockTreeNode(rootBlock, null);
    StructureTreeModel treeModel = new StructureTreeModel(blockTreeStructure);
    initMap(rootNode, rootElement);
    assert myPsiToBlockMap != null;

    PsiElement rootPsi = rootNode.getBlock() instanceof ASTBlock ?
                         ((ASTBlock)rootNode.getBlock()).getNode().getPsi() : rootElement;
    BlockTreeNode blockNode = myPsiToBlockMap.get(rootPsi);

    if (blockNode == null) {
      PsiViewerDialog.LOG.error("PsiViewer: rootNode not found\nCurrent language: " + rootElement.getContainingFile().getLanguage(),
                                (Throwable)null,
                                AttachmentFactory.createAttachment(rootElement.getContainingFile().getOriginalFile().getVirtualFile()));
      blockNode = findBlockNode(rootPsi);
    }

    blockTreeStructure.setRoot(blockNode);
    myTreeModel = new AsyncTreeModel(treeModel, myTreeModelDisposable);
    myBlockTree.setModel(myTreeModel);

    myBlockTree.addTreeSelectionListener(new BlockTreeSelectionListener(rootElement));
    myBlockTree.setRootVisible(true);
    myBlockTree.expandRow(0);

    treeModel.invalidate();
  }


  @Nullable
  private BlockTreeNode findBlockNode(PsiElement element) {
    HashMap<PsiElement, BlockTreeNode> psiToBlockMap = myPsiToBlockMap;

    BlockTreeNode result = psiToBlockMap == null ? null : psiToBlockMap.get(element);
    if (result == null) {
      TextRange rangeInHostFile = InjectedLanguageManager.getInstance(myProject).injectedToHost(element, element.getTextRange());
      result = findBlockNode(rangeInHostFile);
    }
    return result;
  }


  @NotNull
  private TreeVisitor createVisitor(@NotNull BlockTreeNode currentBlockNode) {
    Function<Object, BlockTreeNode> converter = el -> el instanceof DefaultMutableTreeNode ?
                                                      (BlockTreeNode)((DefaultMutableTreeNode)el).getUserObject() :
                                                      null;
    Set<SimpleNode> parents = ContainerUtil.newHashSet();
    SimpleNode parent = currentBlockNode.getParent();
    while (parent != null) {
      parents.add(parent);
      parent = parent.getParent();
    }

    DefaultMutableTreeNode root = getRoot();
    if (root != null) {
      parents.add((SimpleNode)root.getUserObject());
    }

    return new TreeVisitor.ByComponent<BlockTreeNode, BlockTreeNode>(currentBlockNode, converter) {

      @Override
      protected boolean contains(@NotNull BlockTreeNode pathComponent, @NotNull BlockTreeNode thisComponent) {
        return parents.contains(pathComponent);
      }
    };
  }

  private void selectBlockNode(@Nullable BlockTreeNode currentBlockNode) {
    if (myTreeModel == null) return;

    if (currentBlockNode != null) {
      TreeUtil.promiseSelect(myBlockTree, createVisitor(currentBlockNode));
    }
    else {
      myBlockTree.getSelectionModel().clearSelection();
    }
  }

  public class BlockTreeSelectionListener implements TreeSelectionListener {
    @NotNull
    private final PsiElement myRootElement;

    public BlockTreeSelectionListener(@NotNull PsiElement rootElement) {
      myRootElement = rootElement;
    }

    @Override
    public void valueChanged(@NotNull TreeSelectionEvent e) {
      if (myTreeModel == null) {
        return;
      }

      TreePath path = myBlockTree.getSelectionModel().getSelectionPath();
      if (path == null) return;
      DefaultMutableTreeNode component = (DefaultMutableTreeNode)path.getLastPathComponent();
      if (component == null) return;
      Object item = component.getUserObject();

      if (!(item instanceof BlockTreeNode)) return;
      BlockTreeNode descriptor = (BlockTreeNode)item;

      int blockStart = descriptor.getBlock().getTextRange().getStartOffset();
      PsiFile file = myRootElement.getContainingFile();
      PsiElement currentPsiEl = InjectedLanguageUtil.findElementAtNoCommit(file, blockStart);
      if (currentPsiEl == null) currentPsiEl = file;
      int blockLength = descriptor.getBlock().getTextRange().getLength();
      while (currentPsiEl.getParent() != null &&
             currentPsiEl.getTextRange().getStartOffset() == blockStart &&
             currentPsiEl.getTextLength() != blockLength) {
        currentPsiEl = currentPsiEl.getParent();
      }
      BlockTreeNode rootBlockNode = (BlockTreeNode)getRoot().getUserObject();
      int baseOffset = 0;
      if (rootBlockNode != null) {
        baseOffset = rootBlockNode.getBlock().getTextRange().getStartOffset();
      }

      TextRange range = descriptor.getBlock().getTextRange();
      range = range.shiftRight(-baseOffset);
      myUpdater.updatePsiTree(currentPsiEl, myBlockTree.hasFocus() ? range : null);
    }
  }

  @Nullable
  private BlockTreeNode findBlockNode(TextRange range) {
    if (myTreeModel == null || !myBlockStructurePanel.isVisible()) {
      return null;
    }

    DefaultMutableTreeNode root = getRoot();
    if (root == null) return null;
    
    BlockTreeNode node = (BlockTreeNode)root.getUserObject();
    main_loop:
    while (true) {
      if (node.getBlock().getTextRange().equals(range)) {
        return node;
      }

      for (BlockTreeNode child : node.getChildren()) {
        if (child.getBlock().getTextRange().contains(range)) {
          node = child;
          continue main_loop;
        }
      }
      return node;
    }
  }

  @Nullable
  private static Block buildBlocks(@NotNull PsiElement rootElement) {
    FormattingModelBuilder formattingModelBuilder = LanguageFormatting.INSTANCE.forContext(rootElement);
    CodeStyleSettings settings = CodeStyle.getSettings(rootElement.getContainingFile());
    if (formattingModelBuilder != null) {
      FormattingModel formattingModel = formattingModelBuilder.createModel(rootElement, settings);
      return formattingModel.getRootBlock();
    }
    else {
      return null;
    }
  }

  private void initMap(BlockTreeNode rootBlockNode, PsiElement psiEl) {
    myPsiToBlockMap = new HashMap<>();
    JBTreeTraverser<BlockTreeNode> traverser = JBTreeTraverser.of(BlockTreeNode::getChildren);
    for (BlockTreeNode block : traverser.withRoot(rootBlockNode)) {
      PsiElement currentElem = null;
      if (block.getBlock() instanceof ASTBlock) {
        ASTNode node = ((ASTBlock)block.getBlock()).getNode();
        if (node != null) {
          currentElem = node.getPsi();
        }
      }
      if (currentElem == null) {
        currentElem =
          InjectedLanguageUtil
            .findElementAtNoCommit(psiEl.getContainingFile(), block.getBlock().getTextRange().getStartOffset());
      }
      myPsiToBlockMap.put(currentElem, block);

      //nested PSI elements with same ranges will be mapped to one blockNode
      //    assert currentElem != null;      //for Scala-language plugin etc it can be null, because formatterBlocks is not instance of ASTBlock
      TextRange curTextRange = currentElem.getTextRange();
      PsiElement parentElem = currentElem.getParent();
      while (parentElem != null && parentElem.getTextRange().equals(curTextRange)) {
        myPsiToBlockMap.put(parentElem, block);
        parentElem = parentElem.getParent();
      }
    }
  }

  @Nullable
  private DefaultMutableTreeNode getRoot() {
    return (DefaultMutableTreeNode)myBlockTree.getModel().getRoot();
  }
}
