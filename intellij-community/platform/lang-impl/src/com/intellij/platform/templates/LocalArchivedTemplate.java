// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.platform.templates;

import com.intellij.facet.ui.ValidationResult;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author Dmitry Avdeev
 */
public class LocalArchivedTemplate extends ArchivedProjectTemplate {
  public static final String DESCRIPTION_PATH = Project.DIRECTORY_STORE_FOLDER + "/description.html";
  static final String TEMPLATE_DESCRIPTOR = Project.DIRECTORY_STORE_FOLDER + "/project-template.xml";
  static final String TEMPLATE_META_XML = "template-meta.xml";
  static final String META_TEMPLATE_DESCRIPTOR_PATH = Project.DIRECTORY_STORE_FOLDER + "/"+TEMPLATE_META_XML;
  public static final String UNENCODED_ATTRIBUTE = "unencoded";
  static final String ROOT_FILE_NAME = "root";

  private final URL myArchivePath;
  private final ModuleType myModuleType;
  @Nullable private final List<RootDescription> myModuleDescriptions;
  private boolean myEscaped = true;
  private Icon myIcon;

  public LocalArchivedTemplate(@NotNull URL archivePath,
                               @NotNull ClassLoader classLoader) {
    super(getTemplateName(archivePath), null);

    myArchivePath = archivePath;
    myModuleType = computeModuleType(this);
    String s = readEntry(TEMPLATE_DESCRIPTOR);
    if (s != null) {
      try {
        Element templateElement = JDOMUtil.load(s);
        populateFromElement(templateElement);
        String iconPath = templateElement.getChildText("icon-path");
        if (iconPath != null) {
          myIcon = IconLoader.findIcon(iconPath, classLoader);
        }
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    String meta = readEntry(META_TEMPLATE_DESCRIPTOR_PATH);
    if (meta != null) {
      try {
        Element templateElement = JDOMUtil.load(meta);
        String unencoded = templateElement.getAttributeValue(UNENCODED_ATTRIBUTE);
        if (unencoded != null) {
          myEscaped = !Boolean.valueOf(unencoded);
        }

        myModuleDescriptions = RootDescription.readRoots(templateElement);
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    else {
      myModuleDescriptions = null;
    }
  }

  public ValidationResult validate(@NotNull String baseDirPath) {
    if (myModuleDescriptions != null && !myModuleDescriptions.isEmpty()) {
      File baseDirFile = new File(baseDirPath);
      for (RootDescription description : myModuleDescriptions) {
        File rootFile = new File(baseDirFile + "/" + description.myRelativePath);
        try {
          rootFile = rootFile.getCanonicalFile();
          if (rootFile.exists()) {
            String[] list = rootFile.list();
            if (list == null) {
              return new ValidationResult("<html>File '" + rootFile.getAbsolutePath() + "' already exists," +
                                          " so project root can't be created</html>");
            }
            if (list.length > 0) {
              return new ValidationResult("<html>Directory '" + rootFile.getAbsolutePath() + "' already exists and is not empty, " +
                                          "so project root can't be created</html>");
            }
          }
        }
        catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }
    return null;
  }

  private static String getTemplateName(URL url) {
    String fileName = new File(url.getPath()).getName();
    return fileName.substring(0, fileName.length() - ArchivedTemplatesFactory.ZIP.length()).replace('_', ' ');
  }

  @Override
  public String getDescription() {
    return readEntry(DESCRIPTION_PATH);
  }

  @Override
  public Icon getIcon() {
    return myIcon == null ? super.getIcon() : myIcon;
  }

  public boolean isEscaped(){
    return myEscaped;
  }

  @Nullable
  String readEntry(@NotNull final String endsWith) {
    try {
      return processStream(new StreamProcessor<String>() {
        @Override
        public String consume(@NotNull ZipInputStream stream) throws IOException {
          ZipEntry entry;
          while ((entry = stream.getNextEntry()) != null) {
            if (entry.getName().endsWith(endsWith)) {
              return StreamUtil.readText(stream, StandardCharsets.UTF_8);
            }
          }
          return null;
        }
      });
    }
    catch (IOException ignored) {
      return null;
    }
  }

  @NotNull
  private static ModuleType computeModuleType(LocalArchivedTemplate template) {
    String iml = template.readEntry(".iml");
    if (iml == null) return ModuleType.EMPTY;
    try {
      String type = JDOMUtil.load(iml).getAttributeValue(Module.ELEMENT_TYPE);
      return ModuleTypeManager.getInstance().findByID(type);
    }
    catch (Exception e) {
      return ModuleType.EMPTY;
    }
  }

  @Override
  protected ModuleType getModuleType() {
    return myModuleType;
  }

  @Override
  public <T> T processStream(@NotNull StreamProcessor<T> consumer) throws IOException {
    return consumeZipStream(consumer, new ZipInputStream(myArchivePath.openStream()));
  }

  public URL getArchivePath() {
    return myArchivePath;
  }

  @Override
  public void handleUnzippedDirectories(@NotNull File dir, @NotNull List<? super File> filesToRefresh) throws IOException {
    if (myModuleDescriptions == null) {
      filesToRefresh.add(dir);
      return;
    }

    for (RootDescription description : myModuleDescriptions) {
      File root = new File(dir, ROOT_FILE_NAME + description.myIndex);
      File target = new File(dir.getAbsolutePath() + "/" + description.myRelativePath);
      //noinspection ResultOfMethodCallIgnored
      target.mkdirs();
      FileUtil.copyDir(root, target);
      FileUtil.delete(root);
      filesToRefresh.add(target);
    }
  }

  static class RootDescription {
    private static final String ROOTS_ELEMENT = "roots";
    private static final String ROOT_ELEMENT = "root";
    private static final String INDEX_ATTRIBUTE = "index";
    private static final String PATH_ATTRIBUTE = "path";
    final VirtualFile myFile;
    final String myRelativePath;
    final int myIndex;

    RootDescription(VirtualFile file, String path, int index) {
      myFile = file;
      myRelativePath = path;
      myIndex = index;
    }

    private void write(Element parent) {
      Element rootChild = new Element(ROOT_ELEMENT);
      rootChild.setAttribute(INDEX_ATTRIBUTE, String.valueOf(myIndex));
      rootChild.setAttribute(PATH_ATTRIBUTE, myRelativePath);
      parent.addContent(rootChild);
    }

    private static List<RootDescription> read(Element parent) {
      List<Element> children = parent.getChildren(ROOT_ELEMENT);
      List<RootDescription> result = new ArrayList<>(children.size());
      for (Element child : children) {
        int index = Integer.parseInt(child.getAttributeValue(INDEX_ATTRIBUTE));
        String path = child.getAttributeValue(PATH_ATTRIBUTE);
        result.add(index, new RootDescription(null, path, index));
      }
      return result;
    }

    static void writeRoots(Element element, List<? extends RootDescription> rootDescriptions) {
      Element rootsElement = new Element(ROOTS_ELEMENT);
      for (LocalArchivedTemplate.RootDescription description : rootDescriptions) {
        description.write(rootsElement);
      }
      element.addContent(rootsElement);
    }

    @Nullable
    static List<RootDescription> readRoots(Element element) {
      Element modulesElement = element.getChild(ROOTS_ELEMENT);
      if (modulesElement != null) {
        return read(modulesElement);
      }
      else {
        return null;
      }
    }
  }
}
