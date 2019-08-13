package com.intellij.util.ui.classpath;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.NonNls;
import org.jdom.Element;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.List;
import java.util.Collections;
import java.util.ArrayList;
import java.io.IOException;

/**
 * @author nik
 */
public class GlobalLibraryReferenceElement implements SimpleClasspathElement {
  @NonNls public static final String NAME_ATTRIBUTE = "name";
  @NonNls public static final String LEVEL_ATTRIBUTE = "level";
  private final String myLibraryName;

  public GlobalLibraryReferenceElement(@NotNull String libraryName) {
    myLibraryName = libraryName;
  }

  public GlobalLibraryReferenceElement(@NotNull Element element) {
    myLibraryName = element.getAttributeValue(NAME_ATTRIBUTE);
  }

  @Override
  public String getPresentableName() {
    return myLibraryName;
  }

  public void writeExternal(Element element) {
    element.setAttribute(NAME_ATTRIBUTE, myLibraryName);
    //todo[nik,greg] remote later. this is needed only for forward compatibility with version before 8
    element.setAttribute(LEVEL_ATTRIBUTE, LibraryTablesRegistrar.APPLICATION_LEVEL);
  }

  @Override
  public Library getLibrary() {
    final LibraryTable libraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable();
    return libraryTable.getLibraryByName(myLibraryName);
  }

  @Override
  public String getLibraryName() {
    return myLibraryName;
  }

  @Override
  public void serialize(Element element) throws IOException {
    element.setAttribute(NAME_ATTRIBUTE, myLibraryName);
    //todo[nik,greg] remote later. this is needed only for forward compatibility with version before 8
    element.setAttribute(LEVEL_ATTRIBUTE, LibraryTablesRegistrar.APPLICATION_LEVEL);
  }

  @Override
  public List<String> getClassesRootUrls() {
    final Library library = getLibrary();
    if (library != null) {
      final List<String> list = new ArrayList<>();
      for (VirtualFile file : library.getFiles(OrderRootType.CLASSES)) {
        list.add(file.getUrl());
      }
      return list;
    }
    return Collections.emptyList();
  }
}
