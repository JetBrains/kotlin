package com.intellij.util.ui.classpath;

import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.JarFileSystem;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
public class SingleRootClasspathElement implements SimpleClasspathElement {
  @NonNls public static final String URL_ELEMENT = "url";
  private final String myUrl;

  public SingleRootClasspathElement(@NotNull String url) {
    myUrl = url;
  }

  @Override
  public String getPresentableName() {
    String url;
    if (myUrl.endsWith(JarFileSystem.JAR_SEPARATOR)) {
      url = myUrl.substring(0, myUrl.length() - JarFileSystem.JAR_SEPARATOR.length());
    }
    else {
      url = myUrl;
    }
    int startIndex = Math.min(url.lastIndexOf('/') + 1, url.length() - 1);
    return url.substring(startIndex);
  }

  @Override
  public Library getLibrary() {
    return null;
  }

  @Override
  public String getLibraryName() {
    return null;
  }

  @Override
  public void serialize(Element element) throws IOException {
    element.addContent(new Element(URL_ELEMENT).setText(myUrl));
  }

  @Override
  public List<String> getClassesRootUrls() {
    return Collections.singletonList(myUrl);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SingleRootClasspathElement element = (SingleRootClasspathElement)o;

    if (!myUrl.equals(element.myUrl)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return myUrl.hashCode();
  }
}
