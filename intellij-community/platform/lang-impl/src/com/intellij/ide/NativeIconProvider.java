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

package com.intellij.ide;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.INativeFileType;
import com.intellij.openapi.fileTypes.UnknownFileType;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.impl.ElementBase;
import com.intellij.ui.DeferredIconImpl;
import com.intellij.util.Function;
import com.intellij.util.ui.update.ComparableObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.util.*;

/**
 * @author yole
 */
public class NativeIconProvider extends IconProvider implements DumbAware {
  private final Map<Ext, Icon> myIconCache = new HashMap<>();
  // on Windows .exe and .ico files provide their own icons which can differ for each file, cache them by full file path
  private final Set<Ext> myCustomIconExtensions =
    SystemInfo.isWindows ? new HashSet<>(Arrays.asList(new Ext("exe"), new Ext("ico"))) : new HashSet<>();
  private final Map<String, Icon> myCustomIconCache = new HashMap<>();

  private static final Ext NO_EXT = new Ext(null);

  private static final Ext CLOSED_DIR = new Ext(null, 0);

  @Nullable
  @Override
  public Icon getIcon(@NotNull PsiElement element, @Iconable.IconFlags int flags) {
    if (element instanceof PsiFileSystemItem) {
      VirtualFile file = ((PsiFileSystemItem)element).getVirtualFile();
      if (file != null) return doGetIcon(file, flags);
    }
    return null;
  }

  @Nullable
  private Icon doGetIcon(@NotNull VirtualFile file, final int flags) {
    if (!isNativeFileType(file)) return null;

    final Ext ext = getExtension(file, flags);
    final String filePath = file.getPath();

    Icon icon;
    synchronized (myIconCache) {
      if (!myCustomIconExtensions.contains(ext)) {
        icon = ext != null ? myIconCache.get(ext) : null;
      }
      else {
        icon = filePath != null ? myCustomIconCache.get(filePath) : null;
      }
    }
    if (icon != null) {
      return icon;
    }
    return new DeferredIconImpl<>(ElementBase.ICON_PLACEHOLDER.getValue(), file, false, virtualFile -> {
      final File f = new File(filePath);
      if (!f.exists()) {
        return null;
      }
      Icon icon1;
      try { // VM will ensure lock to init -static final field--, note we should have no read access here, to avoid deadlock with EDT needed to init component
        assert SwingComponentHolder.ourFileChooser != null || !ApplicationManager.getApplication().isReadAccessAllowed();
        icon1 = getNativeIcon(f);
      }
      catch (Exception e) {      // see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4854174
        return null;
      }
      if (ext != null) {
        synchronized (myIconCache) {
          if (!myCustomIconExtensions.contains(ext)) {
            myIconCache.put(ext, icon1);
          }
          else if (filePath != null) {
            myCustomIconCache.put(filePath, icon1);
          }
        }
      }
      return icon1;
    });
  }

  @Nullable
  public static Icon getNativeIcon(@Nullable File file) {
    return file == null ? null : SwingComponentHolder.ourFileChooser.getIcon(file);
  }

  private static Ext getExtension(final VirtualFile file, final int flags) {
    if (file.isDirectory()) {
      if (file.getExtension() == null) {
        return CLOSED_DIR;
      } else {
        return new Ext(file.getExtension(), flags);
      }
    }

    return file.getExtension() != null ? new Ext(file.getExtension()) : NO_EXT;
  }

  static class SwingComponentHolder {
    private static final JFileChooser ourFileChooser = new JFileChooser();
  }

  protected boolean isNativeFileType(VirtualFile file) {
    FileType type = file.getFileType();

    if (type instanceof INativeFileType) return ((INativeFileType)type).useNativeIcon();
    return type instanceof UnknownFileType && !file.isDirectory();
  }

  private static class Ext extends ComparableObject.Impl {
    private final Object[] myText;

    private Ext(@Nullable String text) {
      myText = new Object[] {text};
    }

    private Ext(@Nullable String text, final int flags) {
      myText = new Object[] {text, flags};
    }

    @Override
    @NotNull
    public Object[] getEqualityObjects() {
      return myText;
    }

    @Override
    public String toString() {
      return myText[0] != null ? myText[0].toString() : null;
    }
  }
}
