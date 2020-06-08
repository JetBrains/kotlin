// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.ui.classpath;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class SimpleClasspathElementFactory {
  private SimpleClasspathElementFactory() {
  }


  public static List<SimpleClasspathElement> createElements(@Nullable Project project, @NotNull Element element) {
    final String name = element.getAttributeValue(GlobalLibraryReferenceElement.NAME_ATTRIBUTE);
    final String level = element.getAttributeValue(GlobalLibraryReferenceElement.LEVEL_ATTRIBUTE);
    final String url = element.getChildText(SingleRootClasspathElement.URL_ELEMENT);
    if (!StringUtil.isEmpty(url)) {
      return Collections.singletonList(new SingleRootClasspathElement(url));
    }
    if (name == null || level == null) {
      return Collections.emptyList();
    }
    if (LibraryTablesRegistrar.APPLICATION_LEVEL.equals(level)) {
      return Collections.singletonList(new GlobalLibraryReferenceElement(name));
    }
    //this is needed only for backward compatibility with version before 8
    if (project != null) {
      final LibraryTable libraryTable = LibraryTablesRegistrar.getInstance().getLibraryTableByLevel(level, project);
      if (libraryTable != null) {
        final Library library = libraryTable.getLibraryByName(name);
        if (library != null) {
          return createElements(library);
        }
      }
    }
    return Collections.emptyList();
  }

  public static List<SimpleClasspathElement> createElements(@NotNull Library library) {
    final LibraryTable table = library.getTable();
    if (table != null && LibraryTablesRegistrar.APPLICATION_LEVEL.equals(table.getTableLevel())) {
      return Collections.singletonList(new GlobalLibraryReferenceElement(library.getName()));
    }
    final List<SimpleClasspathElement> elements = new ArrayList<>();
    for (VirtualFile file : library.getFiles(OrderRootType.CLASSES)) {
      elements.add(new SingleRootClasspathElement(file.getUrl()));
    }
    return elements;
  }

  public static List<SimpleClasspathElement> createElements(String... urls) {
    final List<SimpleClasspathElement> list = new ArrayList<>();
    for (String url : urls) {
      list.add(new SingleRootClasspathElement(url));
    }
    return list;
  }

  public static List<VirtualFile> convertToFiles(Collection<? extends SimpleClasspathElement> cpeList)
  {
    VirtualFileManager fileManager = VirtualFileManager.getInstance();
    List<VirtualFile> files = new ArrayList<>();
    for (SimpleClasspathElement cpe : cpeList) {
      for (String fileUrl : cpe.getClassesRootUrls()) {
        VirtualFile file = fileManager.findFileByUrl(fileUrl);
        if (file != null) {
          files.add(file);
        }
      }
    }
    return files;
  }

}
