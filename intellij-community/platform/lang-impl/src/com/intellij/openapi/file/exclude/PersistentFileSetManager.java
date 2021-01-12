// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.file.exclude;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileWithId;
import gnu.trove.THashSet;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author Rustam Vishnyakov
 */
class PersistentFileSetManager implements PersistentStateComponent<Element> {
  private static final String FILE_ELEMENT = "file";
  private static final String PATH_ATTR = "url";

  private final Set<VirtualFile> myFiles = new THashSet<>();

  protected boolean addFile(@NotNull VirtualFile file) {
    if (!(file instanceof VirtualFileWithId) || file.isDirectory()) return false;
    myFiles.add(file);
    onFileAdded(file);
    return true;
  }

  protected boolean containsFile(@NotNull VirtualFile file) {
    return myFiles.contains(file);
  }

  protected boolean removeFile(@NotNull VirtualFile file) {
    boolean isRemoved = myFiles.remove(file);
    if (isRemoved) {
      onFileRemoved(file);
    }
    return isRemoved;
  }

  protected void onFileAdded(@NotNull VirtualFile file) {

  }

  protected void onFileRemoved(@NotNull VirtualFile file) {

  }

  @NotNull
  public Collection<VirtualFile> getFiles() {
    return myFiles;
  }

  @NotNull
  private Collection<VirtualFile> getSortedFiles() {
    List<VirtualFile> sortedFiles = new ArrayList<>(myFiles);
    sortedFiles.sort(Comparator.comparing(file -> StringUtil.toLowerCase(file.getPath())));
    return sortedFiles;
  }

  @Override
  public Element getState() {
    final Element root = new Element("root");
    for (VirtualFile vf : getSortedFiles()) {
      final Element vfElement = new Element(FILE_ELEMENT);
      final Attribute filePathAttr = new Attribute(PATH_ATTR, VfsUtilCore.pathToUrl(vf.getPath()));
      vfElement.setAttribute(filePathAttr);
      root.addContent(vfElement);
    }
    return root;
  }

  @Override
  public void loadState(@NotNull Element state) {
    Set<VirtualFile> oldFiles = new THashSet<>(myFiles);
    myFiles.clear();
    final VirtualFileManager vfManager = VirtualFileManager.getInstance();
    for (Object child : state.getChildren(FILE_ELEMENT)) {
      if (child instanceof Element) {
        final Element fileElement = (Element)child;
        final Attribute filePathAttr = fileElement.getAttribute(PATH_ATTR);
        if (filePathAttr != null) {
          final String filePath = filePathAttr.getValue();
          VirtualFile vf = vfManager.findFileByUrl(filePath);
          if (vf != null) {
            myFiles.add(vf);
          }
        }
      }
    }

    for (VirtualFile file : myFiles) {
      if (!oldFiles.contains(file)) {
        onFileAdded(file);
      }
    }

    for (VirtualFile file : oldFiles) {
      if (!myFiles.contains(file)) {
        onFileRemoved(file);
      }
    }
  }
}
