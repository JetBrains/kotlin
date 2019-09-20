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

package com.intellij.ide.util.gotoByName;

import com.intellij.ide.util.PlatformModuleRendererFactory;
import com.intellij.ide.util.PsiElementListCellRenderer;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ObjectUtils;
import com.intellij.util.ui.FilePathSplittingPolicy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class GotoFileCellRenderer extends PsiElementListCellRenderer<PsiFileSystemItem> {
  private final int myMaxWidth;

  public GotoFileCellRenderer(int maxSize) {
    myMaxWidth = maxSize;
  }

  @Override
  public String getElementText(PsiFileSystemItem element) {
    return element.getName();
  }

  @Override
  protected String getContainerText(PsiFileSystemItem element, String name) {
    PsiFileSystemItem parent = element.getParent();
    final PsiDirectory psiDirectory = parent instanceof PsiDirectory ? (PsiDirectory)parent : null;
    if (psiDirectory == null) return null;
    final VirtualFile virtualFile = psiDirectory.getVirtualFile();
    final String relativePath = getRelativePath(virtualFile, element.getProject());
    if (relativePath == null) return "( " + File.separator + " )";
    String path =
      FilePathSplittingPolicy.SPLIT_BY_SEPARATOR.getOptimalTextForComponent(name + "          ", new File(relativePath), this, myMaxWidth);
    return "(" + path + ")";
  }

  @Nullable
  public static String getRelativePath(final VirtualFile virtualFile, final Project project) {
    if (project == null) {
      return virtualFile.getPresentableUrl();
    }
    VirtualFile root = getAnyRoot(virtualFile, project);
    if (root != null) {
      return getRelativePathFromRoot(virtualFile, root);
    }

    String url = virtualFile.getPresentableUrl();
    final VirtualFile baseDir = project.getBaseDir();
    if (baseDir != null) {
      final String projectHomeUrl = baseDir.getPresentableUrl();
      if (url.startsWith(projectHomeUrl)) {
        final String cont = url.substring(projectHomeUrl.length());
        if (cont.isEmpty()) return null;
        url = "..." + cont;
      }
    }
    return url;
  }

  @Nullable
  public static VirtualFile getAnyRoot(@NotNull VirtualFile virtualFile, @NotNull Project project) {
    ProjectFileIndex index = ProjectFileIndex.SERVICE.getInstance(project);
    VirtualFile root = index.getContentRootForFile(virtualFile);
    if (root == null) root = index.getClassRootForFile(virtualFile);
    if (root == null) root = index.getSourceRootForFile(virtualFile);
    return root;
  }

  @NotNull
  static String getRelativePathFromRoot(@NotNull VirtualFile file, @NotNull VirtualFile root) {
    return root.getName() + File.separatorChar + VfsUtilCore.getRelativePath(file, root, File.separatorChar);
  }

  @Override
  protected boolean customizeNonPsiElementLeftRenderer(ColoredListCellRenderer renderer,
                                                       JList list,
                                                       Object value,
                                                       int index,
                                                       boolean selected,
                                                       boolean hasFocus) {
    return doCustomizeNonPsiElementLeftRenderer(renderer, list, value, getNavigationItemAttributes(value));
  }

  public static boolean doCustomizeNonPsiElementLeftRenderer(ColoredListCellRenderer renderer,
                                                             JList list,
                                                             Object value,
                                                             TextAttributes attributes) {
    if (!(value instanceof NavigationItem)) return false;

    NavigationItem item = (NavigationItem)value;

    SimpleTextAttributes nameAttributes = attributes != null ? SimpleTextAttributes.fromTextAttributes(attributes) : null;

    Color color = list.getForeground();
    if (nameAttributes == null) nameAttributes = new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, color);

    ItemPresentation presentation = ObjectUtils.notNull(item.getPresentation());
    renderer.append(presentation.getPresentableText() + " ", nameAttributes);
    renderer.setIcon(presentation.getIcon(true));

    String locationString = presentation.getLocationString();
    if (!StringUtil.isEmpty(locationString)) {
      renderer.append(locationString, new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.GRAY));
    }
    return true;
  }

  @Override
  protected DefaultListCellRenderer getRightCellRenderer(final Object value) {
    final DefaultListCellRenderer rightRenderer = super.getRightCellRenderer(value);
    if (rightRenderer instanceof PlatformModuleRendererFactory.PlatformModuleRenderer) {
      // that renderer will display file path, but we're showing it ourselves - no need to show twice
      return null;
    }
    return rightRenderer;
  }

  @Override
  protected int getIconFlags() {
    return Iconable.ICON_FLAG_READ_STATUS;
  }
}
