/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.mock;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.SingleRootFileViewProvider;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.psi.impl.file.impl.FileManager;
import com.intellij.util.containers.ConcurrentFactoryMap;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * @author peter
 */
public class MockFileManager implements FileManager {
  private final PsiManagerEx myManager;
  // in mock tests it's LightVirtualFile, they're only alive when they're referenced, 
  // and there can not be several instances representing the same file
  private final Map<VirtualFile, FileViewProvider> myViewProviders;

  @Override
  @NotNull
  public FileViewProvider createFileViewProvider(@NotNull VirtualFile file, boolean eventSystemEnabled) {
    return new SingleRootFileViewProvider(myManager, file, eventSystemEnabled);
  }

  public MockFileManager(PsiManagerEx manager) {
    myManager = manager;
    myViewProviders = ConcurrentFactoryMap.createMap(key->new SingleRootFileViewProvider(myManager, key), ContainerUtil::createConcurrentWeakKeyWeakValueMap);
  }

  @Override
  public void dispose() {
    throw new UnsupportedOperationException("Method dispose is not yet implemented in " + getClass().getName());
  }

  @Override
  @Nullable
  public PsiFile findFile(@NotNull VirtualFile vFile) {
    return getCachedPsiFile(vFile);
  }

  @Override
  @Nullable
  public PsiDirectory findDirectory(@NotNull VirtualFile vFile) {
    throw new UnsupportedOperationException("Method findDirectory is not yet implemented in " + getClass().getName());
  }

  @Override
  public void reloadFromDisk(@NotNull PsiFile file) //Q: move to PsiFile(Impl)?
  {
    throw new UnsupportedOperationException("Method reloadFromDisk is not yet implemented in " + getClass().getName());
  }

  @Override
  @Nullable
  public PsiFile getCachedPsiFile(@NotNull VirtualFile vFile) {
    FileViewProvider provider = findCachedViewProvider(vFile);
    return provider.getPsi(provider.getBaseLanguage());
  }

  @Override
  public void cleanupForNextTest() {
    myViewProviders.clear();
  }

  @Override
  public FileViewProvider findViewProvider(@NotNull VirtualFile file) {
    throw new UnsupportedOperationException("Method findViewProvider is not yet implemented in " + getClass().getName());
  }

  @Override
  public FileViewProvider findCachedViewProvider(@NotNull VirtualFile file) {
    return myViewProviders.get(file);
  }

  @Override
  public void setViewProvider(@NotNull VirtualFile virtualFile, FileViewProvider fileViewProvider) {
    myViewProviders.put(virtualFile, fileViewProvider);
  }

  @Override
  @NotNull
  public List<PsiFile> getAllCachedFiles() {
    throw new UnsupportedOperationException("Method getAllCachedFiles is not yet implemented in " + getClass().getName());
  }
}
