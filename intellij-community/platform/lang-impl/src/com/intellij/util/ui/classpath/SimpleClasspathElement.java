package com.intellij.util.ui.classpath;

import com.intellij.openapi.roots.libraries.Library;
import org.jdom.Element;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

/**
 * @author nik
 */
public interface SimpleClasspathElement {

  String getPresentableName();

  List<String> getClassesRootUrls();

  @Nullable
  Library getLibrary();

  @Nullable
  String getLibraryName();

  void serialize(Element element) throws IOException;
}
