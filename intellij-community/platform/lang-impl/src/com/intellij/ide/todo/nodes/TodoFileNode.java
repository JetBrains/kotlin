// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.todo.nodes;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.PsiFileNode;
import com.intellij.ide.todo.SmartTodoItemPointer;
import com.intellij.ide.todo.SmartTodoItemPointerComparator;
import com.intellij.ide.todo.TodoFilter;
import com.intellij.ide.todo.TodoTreeBuilder;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.injected.editor.DocumentWindow;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.impl.search.TodoItemImpl;
import com.intellij.psi.search.PsiTodoSearchHelper;
import com.intellij.psi.search.TodoItem;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class TodoFileNode extends PsiFileNode {
  private final TodoTreeBuilder myBuilder;
  private final boolean mySingleFileMode;

  public TodoFileNode(Project project,
                      @NotNull PsiFile file,
                      TodoTreeBuilder treeBuilder,
                      boolean singleFileMode){
    super(project,file,ViewSettings.DEFAULT);
    myBuilder=treeBuilder;
    mySingleFileMode=singleFileMode;
  }

  @Override
  public Collection<AbstractTreeNode> getChildrenImpl() {
    try {
      if (!mySingleFileMode) {
        return (Collection<AbstractTreeNode>)createGeneralList();
      }
      return (Collection<AbstractTreeNode>)createListForSingleFile();
    }
    catch (IndexNotReadyException e) {
      return Collections.emptyList();
    }
  }

  private Collection<? extends AbstractTreeNode> createListForSingleFile() {
    PsiFile psiFile = getValue();
    TodoItem[] items= findAllTodos(psiFile, myBuilder.getTodoTreeStructure().getSearchHelper());
    List<TodoItemNode> children= new ArrayList<>(items.length);
    Document document = PsiDocumentManager.getInstance(getProject()).getDocument(psiFile);
    if (document != null) {
      for (TodoItem todoItem : items) {
        if (todoItem.getTextRange().getEndOffset() < document.getTextLength() + 1) {
          SmartTodoItemPointer pointer = new SmartTodoItemPointer(todoItem, document);
          TodoFilter toDoFilter = getToDoFilter();
          if (toDoFilter != null) {
            TodoItemNode itemNode = new TodoItemNode(getProject(), pointer, myBuilder);
            if (toDoFilter.contains(todoItem.getPattern())) {
              children.add(itemNode);
            }
          } else {
            children.add(new TodoItemNode(getProject(), pointer, myBuilder));
          }
        }
      }
    }
    children.sort(SmartTodoItemPointerComparator.ourInstance);
    return children;
  }

  public static TodoItem[] findAllTodos(final PsiFile psiFile, final PsiTodoSearchHelper helper) {
    final List<TodoItem> todoItems = new ArrayList<>(Arrays.asList(helper.findTodoItems(psiFile)));

    psiFile.accept(new PsiRecursiveElementWalkingVisitor() {
      @Override
      public void visitElement(PsiElement element) {
        if (element instanceof PsiLanguageInjectionHost) {
          InjectedLanguageManager.getInstance(psiFile.getProject()).enumerate(element, (injectedPsi, places) -> {
            if (places.size() == 1) {
              Document document = PsiDocumentManager.getInstance(injectedPsi.getProject()).getCachedDocument(injectedPsi);
              if (!(document instanceof DocumentWindow)) return;
              for (TodoItem item : helper.findTodoItems(injectedPsi)) {
                TextRange rangeInHost = ((DocumentWindow)document).injectedToHost(item.getTextRange());
                List<TextRange> additionalRanges = ContainerUtil.map(item.getAdditionalTextRanges(),
                                                                     ((DocumentWindow)document)::injectedToHost);
                TodoItemImpl hostItem = new TodoItemImpl(psiFile, rangeInHost.getStartOffset(), rangeInHost.getEndOffset(),
                                                         item.getPattern(), additionalRanges);
                todoItems.add(hostItem);
              }
            }
          });
        }
        super.visitElement(element);
      }
    });
    return todoItems.toArray(new TodoItem[0]);
  }

  private Collection<? extends AbstractTreeNode> createGeneralList() {
    List<TodoItemNode> children = new ArrayList<>();

    PsiFile psiFile = getValue();
    final TodoItem[] items = findAllTodos(psiFile, myBuilder.getTodoTreeStructure().getSearchHelper());
    final Document document = PsiDocumentManager.getInstance(getProject()).getDocument(psiFile);

    if (document != null) {
      for (final TodoItem todoItem : items) {
        if (todoItem.getTextRange().getEndOffset() < document.getTextLength() + 1) {
          final SmartTodoItemPointer pointer = new SmartTodoItemPointer(todoItem, document);
          TodoFilter todoFilter = getToDoFilter();
          if (todoFilter != null) {
            if (todoFilter.contains(todoItem.getPattern())) {
              children.add(new TodoItemNode(getProject(), pointer, myBuilder));
            }
          } else {
            children.add(new TodoItemNode(getProject(), pointer, myBuilder));
          }
        }
      }
    }
    children.sort(SmartTodoItemPointerComparator.ourInstance);
    return children;
  }

  private TodoFilter getToDoFilter() {
    return myBuilder.getTodoTreeStructure().getTodoFilter();
  }

  @Override
  protected void updateImpl(@NotNull PresentationData data) {
    super.updateImpl(data);
    String newName;
    if(myBuilder.getTodoTreeStructure().isPackagesShown()){
      newName=getValue().getName();
    }else{
      newName=mySingleFileMode ? getValue().getName() : getValue().getVirtualFile().getPresentableUrl();
    }

    data.setPresentableText(newName);
    int todoItemCount;
    try {
      todoItemCount = myBuilder.getTodoTreeStructure().getTodoItemCount(getValue());
    }
    catch (IndexNotReadyException e) {
      return;
    }
    if (todoItemCount > 0) {
      data.setLocationString(IdeBundle.message("node.todo.items", todoItemCount));
    }
  }

  @Override
  public int getWeight() {
    return 4;
  }
}
