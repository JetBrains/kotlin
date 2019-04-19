/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.pom.wrappers;

import com.intellij.injected.editor.VirtualFileWindow;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.PomModel;
import com.intellij.pom.PomModelAspect;
import com.intellij.pom.event.PomModelEvent;
import com.intellij.pom.tree.TreeAspect;
import com.intellij.pom.tree.events.TreeChangeEvent;
import com.intellij.pom.tree.events.impl.TreeChangeEventImpl;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.PsiDocumentManagerImpl;
import com.intellij.psi.impl.PsiManagerImpl;
import com.intellij.psi.impl.PsiToDocumentSynchronizer;
import com.intellij.psi.impl.source.DummyHolder;
import com.intellij.testFramework.LightVirtualFile;

import java.util.Collections;

public class PsiEventWrapperAspect implements PomModelAspect{
  private final TreeAspect myTreeAspect;

  public PsiEventWrapperAspect(PomModel model, TreeAspect aspect) {
    myTreeAspect = aspect;
    model.registerAspect(PsiEventWrapperAspect.class, this, Collections.singleton(aspect));
  }

  @Override
  public void update(PomModelEvent event) {
    final TreeChangeEvent changeSet = (TreeChangeEvent)event.getChangeSet(myTreeAspect);
    if(changeSet == null) return;
    sendAfterEvents(changeSet);
  }

  private static void sendAfterEvents(TreeChangeEvent changeSet) {
    ASTNode rootElement = changeSet.getRootElement();
    PsiFile file = (PsiFile)rootElement.getPsi();
    if (!file.isPhysical()) {
      promoteNonPhysicalChangesToDocument(rootElement, file);
      ((PsiManagerImpl)file.getManager()).afterChange(false);
      return;
    }

    ((TreeChangeEventImpl)changeSet).fireEvents();
  }

  private static void promoteNonPhysicalChangesToDocument(ASTNode rootElement, PsiFile file) {
    if (file instanceof DummyHolder) return;
    if (((PsiDocumentManagerImpl)PsiDocumentManager.getInstance(file.getProject())).isCommitInProgress()) return;
    
    VirtualFile vFile = file.getViewProvider().getVirtualFile();
    if (vFile instanceof LightVirtualFile && !(vFile instanceof VirtualFileWindow)) {
      Document document = FileDocumentManager.getInstance().getCachedDocument(vFile);
      if (document != null) {
        CharSequence text = rootElement.getChars();
        PsiToDocumentSynchronizer.performAtomically(file, () -> document.replaceString(0, document.getTextLength(), text));
      }
    }
  }

}
