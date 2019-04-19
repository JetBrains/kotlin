/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.codeInspection.ui;

import com.intellij.codeInspection.reference.RefElement;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.util.Alarm;
import com.intellij.util.ObjectUtils;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

class InspectionViewChangeAdapter extends PsiTreeChangeAdapter {
  private final InspectionResultsView myView;
  private final Alarm myAlarm;
  private final Set<VirtualFile> myUnPresentEditedFiles = Collections.synchronizedSet(ContainerUtil.createWeakSet());

  private final Set<VirtualFile> myFilesToProcess = new THashSet<>(); // guarded by myFilesToProcess
  private final AtomicBoolean myNeedReValidate = new AtomicBoolean(false);
  private final Alarm myUpdateQueue;

  InspectionViewChangeAdapter(@NotNull InspectionResultsView view) {
    myView = view;
    myAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, view);
    myUpdateQueue = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, view);
  }

  @Override
  public void childAdded(@NotNull PsiTreeChangeEvent event) {
    processFileEvent(event);
  }

  @Override
  public void childRemoved(@NotNull PsiTreeChangeEvent event) {
    processFileOrDirEvent(event);
  }


  @Override
  public void childReplaced(@NotNull PsiTreeChangeEvent event) {
    processFileOrDirEvent(event);
  }

  @Override
  public void childMoved(@NotNull PsiTreeChangeEvent event) {
    if (!processFileEvent(event)) {
      PsiElement oldParent = event.getOldParent();
      PsiElement newParent = event.getNewParent();
      if (oldParent instanceof PsiDirectory || newParent instanceof PsiDirectory) {
        needReValidateTree();
      }
    }
  }

  @Override
  public void childrenChanged(@NotNull PsiTreeChangeEvent event) {
    processFileOrDirEvent(event);
  }

  @Override
  public void propertyChanged(@NotNull PsiTreeChangeEvent event) {
    processFileEvent(event);
  }

  private void needReValidateTree() {
    myNeedReValidate.set(true);
    invokeQueue();
  }

  private void processFileOrDirEvent(@NotNull PsiTreeChangeEvent event) {
    if (!processFileEvent(event)) {
      PsiElement parent = event.getParent();
      if (parent instanceof PsiDirectory) {
        needReValidateTree();
      }
    }
  }

  private boolean processFileEvent(@NotNull PsiTreeChangeEvent event) {
    PsiFile file = event.getFile();
    if (file != null) {
      VirtualFile vFile = file.getVirtualFile();
      if (!myUnPresentEditedFiles.contains(vFile)) {
        synchronized (myFilesToProcess) {
          myFilesToProcess.add(vFile);
        }
        invokeQueue();
      }
      return true;
    }
    return false;
  }

  private void invokeQueue() {
    myUpdateQueue.cancelAllRequests();
    myUpdateQueue.addRequest(() -> {
      boolean[] needUpdateUI = {false};
      Processor<InspectionTreeNode> nodeProcessor = null;

      if (myNeedReValidate.compareAndSet(true, false)) {
        nodeProcessor = (node) -> {
          if (myView.isDisposed()) {
            return false;
          }

          if (node instanceof SuppressableInspectionTreeNode) {
            RefElement element = ObjectUtils.tryCast(((SuppressableInspectionTreeNode)node).getElement(), RefElement.class);
            if (element != null) {
              SmartPsiElementPointer pointer = element.getPointer();
              if (pointer != null) {
                VirtualFile vFile = pointer.getVirtualFile();
                if (vFile == null || !vFile.isValid()) {
                  ((SuppressableInspectionTreeNode)node).dropCache();
                  if (!needUpdateUI[0]) {
                    needUpdateUI[0] = true;
                  }
                }
              }
            }
          }

          return true;
        };
      }

      Set<VirtualFile> filesToCheck;
      synchronized (myFilesToProcess) {
        filesToCheck = new THashSet<>(myFilesToProcess);
        myFilesToProcess.clear();
      }
      Set<VirtualFile> unPresentFiles = new THashSet<>(filesToCheck);
      if (!filesToCheck.isEmpty()) {
        Processor<InspectionTreeNode> fileCheckProcessor = (node) -> {
          if (myView.isDisposed()) {
            return false;
          }

          if (node instanceof SuppressableInspectionTreeNode) {
            RefElement element = ObjectUtils.tryCast(((SuppressableInspectionTreeNode)node).getElement(), RefElement.class);
            if (element != null) {
              SmartPsiElementPointer pointer = element.getPointer();
              if (pointer != null) {
                VirtualFile vFile = ReadAction.compute(() -> pointer.getVirtualFile());
                if (filesToCheck.contains(vFile)) {
                  unPresentFiles.remove(vFile);
                  ((SuppressableInspectionTreeNode)node).dropCache();
                  if (!needUpdateUI[0]) {
                    needUpdateUI[0] = true;
                  }
                }
              }
            }
          }

          return true;
        };
        nodeProcessor = CompositeProcessor.combine(fileCheckProcessor, nodeProcessor);
      }

      myView.getTree().getInspectionTreeModel().traverse(myView.getTree().getInspectionTreeModel().getRoot()).processEach(nodeProcessor);

      if (!unPresentFiles.isEmpty()) {
        myUnPresentEditedFiles.addAll(unPresentFiles);
      }

      if (needUpdateUI[0] && !myAlarm.isDisposed()) {
        myAlarm.cancelAllRequests();
        //TODO replace with more accurate
        myAlarm.addRequest(() -> myView.getTree().getInspectionTreeModel().reload(), 100, ModalityState.NON_MODAL);
      }
    }, 200);
  }

  private static class CompositeProcessor<X> implements Processor<X> {
    private final Processor<? super X> myFirstProcessor;
    private boolean myFirstFinished;
    private final Processor<? super X> mySecondProcessor;
    private boolean mySecondFinished;

    private CompositeProcessor(@NotNull Processor<? super X> firstProcessor, @NotNull Processor<? super X> secondProcessor) {
      myFirstProcessor = firstProcessor;
      mySecondProcessor = secondProcessor;
    }

    @Override
    public boolean process(X x) {
      if (!myFirstFinished && !myFirstProcessor.process(x)) {
        myFirstFinished = true;
      }
      if (!mySecondFinished && !mySecondProcessor.process(x)) {
        mySecondFinished = true;
      }
      return !myFirstFinished || !mySecondFinished;
    }

    @NotNull
    public static <X> Processor<X> combine(@NotNull Processor<X> processor1, @Nullable Processor<? super X> processor2) {
      return processor2 == null ? processor1 : new CompositeProcessor<>(processor1, processor2);
    }
  }

}
