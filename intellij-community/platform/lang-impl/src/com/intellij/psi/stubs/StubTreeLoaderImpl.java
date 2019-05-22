/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.psi.stubs;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.NoAccessDuringPsiEvents;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.RecursionManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.psi.impl.source.PsiFileImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

/**
 * @author yole
 */
public class StubTreeLoaderImpl extends StubTreeLoader {
  private static final Logger LOG = Logger.getInstance("#com.intellij.psi.stubs.StubTreeLoaderImpl");
  private static volatile boolean ourStubReloadingProhibited;

  @Override
  @Nullable
  public ObjectStubTree readOrBuild(Project project, final VirtualFile vFile, @Nullable PsiFile psiFile) {
    final ObjectStubTree fromIndices = readFromVFile(project, vFile);
    if (fromIndices != null) {
      return fromIndices;
    }

    try {
      byte[] content = vFile.contentsToByteArray();
      vFile.setPreloadedContentHint(content);
      try {
        final FileContent fc = new FileContentImpl(vFile, content);
        fc.putUserData(IndexingDataKeys.PROJECT, project);
        if (psiFile != null && !vFile.getFileType().isBinary()) {
          fc.putUserData(IndexingDataKeys.FILE_TEXT_CONTENT_KEY, psiFile.getViewProvider().getContents());
          // but don't reuse psiFile itself to avoid loading its contents. If we load AST, the stub will be thrown out anyway.
        }

        Stub element = RecursionManager.doPreventingRecursion(vFile, false, () -> StubTreeBuilder.buildStubTree(fc));
        ObjectStubTree tree = element instanceof PsiFileStub ? new StubTree((PsiFileStub)element) :
                              element instanceof ObjectStubBase ? new ObjectStubTree((ObjectStubBase)element, true) :
                              null;
        if (tree != null) {
          tree.setDebugInfo("created from file content");
          return tree;
        }
      }
      finally {
        vFile.setPreloadedContentHint(null);
      }
    }
    catch (IOException e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug(e);
      } else {
        // content can be not cached yet, and the file can be deleted on disk already, without refresh
        LOG.info("Can't load file content for stub building: " + e.getMessage());
      }
    }

    return null;
  }

  @Override
  @Nullable
  public ObjectStubTree readFromVFile(Project project, final VirtualFile vFile) {
    if (DumbService.getInstance(project).isDumb() || NoAccessDuringPsiEvents.isInsideEventProcessing()) {
      return null;
    }

    final int id = SingleEntryFileBasedIndexExtension.getFileKey(vFile);
    if (id <= 0) {
      return null;
    }

    boolean wasIndexedAlready = ((FileBasedIndexImpl)FileBasedIndex.getInstance()).isFileUpToDate(vFile);

    Document document = FileDocumentManager.getInstance().getCachedDocument(vFile);
    boolean saved = document == null || !FileDocumentManager.getInstance().isDocumentUnsaved(document);

    final List<SerializedStubTree> datas = FileBasedIndex.getInstance().getValues(StubUpdatingIndex.INDEX_ID, id, GlobalSearchScope
        .fileScope(project, vFile));
    final int size = datas.size();

    if (size == 1) {
      SerializedStubTree stubTree = datas.get(0);

      if (!checkLengthMatch(project, vFile, wasIndexedAlready, document, saved)) {
        return null;
      }

      Stub stub;
      try {
        stub = stubTree.getStub(false);
      }
      catch (SerializerNotFoundException e) {
        return processError(vFile, "No stub serializer: " + vFile.getPresentableUrl() + ": " + e.getMessage(), e);
      }
      ObjectStubTree<?> tree = stub instanceof PsiFileStub ? new StubTree((PsiFileStub)stub) : new ObjectStubTree((ObjectStubBase)stub, true);
      tree.setDebugInfo("created from index");
      checkDeserializationCreatesNoPsi(tree);
      return tree;
    }
    if (size != 0) {
      return processError(vFile, "Twin stubs: " + vFile.getPresentableUrl() + " has " + size + " stub versions. Should only have one. id=" + id,
                          null);
    }

    return null;
  }

  private boolean checkLengthMatch(Project project,
                                   VirtualFile vFile,
                                   boolean wasIndexedAlready,
                                   Document document,
                                   boolean saved) {
    PsiFile cachedPsi = PsiManagerEx.getInstanceEx(project).getFileManager().getCachedPsiFile(vFile);
    IndexingStampInfo indexingStampInfo = getIndexingStampInfo(vFile);
    if (indexingStampInfo != null && !indexingStampInfo.contentLengthMatches(vFile.getLength(), getCurrentTextContentLength(project, vFile, document, cachedPsi))) {
      diagnoseLengthMismatch(vFile, wasIndexedAlready, document, saved, cachedPsi);
      return false;
    }
    return true;
  }

  private void diagnoseLengthMismatch(VirtualFile vFile,
                                      boolean wasIndexedAlready,
                                      @Nullable Document document,
                                      boolean saved,
                                      @Nullable PsiFile cachedPsi) {
    String message = "Outdated stub in index: " + vFile + " " + getIndexingStampInfo(vFile) +
                     ", doc=" + document +
                     ", docSaved=" + saved +
                     ", wasIndexedAlready=" + wasIndexedAlready +
                     ", queried at " + vFile.getTimeStamp();
    message += "\ndoc length=" + (document == null ? -1 : document.getTextLength()) +
               "\nfile length=" + vFile.getLength();
    if (cachedPsi != null) {
      message += "\ncached PSI " + cachedPsi.getClass();
      if (cachedPsi instanceof PsiFileImpl && ((PsiFileImpl)cachedPsi).isContentsLoaded()) {
        message += "\nPSI length=" + cachedPsi.getTextLength();
      }
      List<Project> projects = ContainerUtil.findAll(ProjectManager.getInstance().getOpenProjects(),
                                                p -> PsiManagerEx.getInstanceEx(p).getFileManager().findCachedViewProvider(vFile) != null);
      message += "\nprojects with file: " + (LOG.isDebugEnabled() ? projects.toString() : projects.size());
    }

    processError(vFile, message, new Exception());
  }

  private static void checkDeserializationCreatesNoPsi(ObjectStubTree<?> tree) {
    if (ourStubReloadingProhibited || !(tree instanceof StubTree)) return;

    for (PsiFileStub root : ((PsiFileStubImpl<?>)tree.getRoot()).getStubRoots()) {
      if (root instanceof StubBase) {
        StubList stubList = ((StubBase)root).myStubList;
        for (int i = 0; i < stubList.size(); i++) {
          StubBase<?> each = stubList.getCachedStub(i);
          PsiElement cachedPsi = each == null ? null : ((StubBase)each).getCachedPsi();
          if (cachedPsi != null) {
            ourStubReloadingProhibited = true;
            throw new AssertionError("Stub deserialization shouldn't create PSI: " + cachedPsi + "; " + each);
          }
        }
      }
    }
  }

  private static int getCurrentTextContentLength(Project project, VirtualFile vFile, Document document, PsiFile psiFile) {
    if (vFile.getFileType().isBinary()) {
      return -1;
    }
    if (psiFile instanceof PsiFileImpl && ((PsiFileImpl)psiFile).isContentsLoaded()) {
      return psiFile.getTextLength();
    }
    
    if (document != null) {
      return PsiDocumentManager.getInstance(project).getLastCommittedText(document).length();
    }
    return -1;
  }

  private static ObjectStubTree processError(final VirtualFile vFile, String message, @Nullable Exception e) {
    LOG.error(message, e);

    ApplicationManager.getApplication().invokeLater(() -> {
      final Document doc = FileDocumentManager.getInstance().getCachedDocument(vFile);
      if (doc != null) {
        FileDocumentManager.getInstance().saveDocument(doc);
      }
      
      // avoid deadlock by requesting reindex later. 
      // processError may be invoked under stub index's read action and requestReindex in EDT starts dumb mode in writeAction (IDEA-197296)
      FileBasedIndex.getInstance().requestReindex(vFile);
    }, ModalityState.NON_MODAL);

    return null;
  }

  @Override
  public void rebuildStubTree(VirtualFile virtualFile) {
    FileBasedIndex.getInstance().requestReindex(virtualFile);
  }

  @Override
  public boolean canHaveStub(VirtualFile file) {
    return StubUpdatingIndex.canHaveStub(file);
  }

  @Override
  protected boolean hasPsiInManyProjects(@NotNull final VirtualFile virtualFile) {
    int count = 0;
    for (Project project : ProjectManager.getInstance().getOpenProjects()) {
      if (PsiManagerEx.getInstanceEx(project).getFileManager().findCachedViewProvider(virtualFile) != null) {
        count++;
      }
    }
    return count > 1;
  }

  @Override
  protected IndexingStampInfo getIndexingStampInfo(@NotNull VirtualFile file) {
    return StubUpdatingIndex.getIndexingStampInfo(file);
  }

  @Override
  protected boolean isPrebuilt(@NotNull VirtualFile virtualFile) {
      boolean canBePrebuilt = false;
      try {
        final PrebuiltStubsProvider prebuiltStubsProvider =
          PrebuiltStubsProviders.INSTANCE.forFileType(virtualFile.getFileType());
        if (prebuiltStubsProvider != null) {
          canBePrebuilt = null !=
                          prebuiltStubsProvider
                            .findStub(new FileContentImpl(virtualFile, virtualFile.contentsToByteArray()));
        }
      }
      catch (Exception e) {
        // pass
      }
      return canBePrebuilt;
    }
}
