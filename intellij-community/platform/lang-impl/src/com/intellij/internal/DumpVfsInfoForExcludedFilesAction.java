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
package com.intellij.internal;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.impl.DirectoryIndexExcludePolicy;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.NewVirtualFile;
import com.intellij.openapi.vfs.newvfs.persistent.PersistentFS;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author nik
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class DumpVfsInfoForExcludedFilesAction extends DumbAwareAction {
  public DumpVfsInfoForExcludedFilesAction() {
    super("Dump VFS content for files under excluded roots");
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) return;

    Set<String> excludeRoots = new HashSet<>();
    for (Module module : ModuleManager.getInstance(project).getModules()) {
      Collections.addAll(excludeRoots, ModuleRootManager.getInstance(module).getExcludeRootUrls());
    }
    for (DirectoryIndexExcludePolicy policy : DirectoryIndexExcludePolicy.EP_NAME.getExtensions(project)) {
      ContainerUtil.addAll(excludeRoots, policy.getExcludeUrlsForProject());
    }

    if (excludeRoots.isEmpty()) {
      System.out.println("No excluded roots found in project.");
    }

    for (String root : excludeRoots) {
      VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(root);
      if (file == null) {
        System.out.println(root + " not in VFS");
        continue;
      }
      dumpChildrenInDbRecursively(file, 0);
    }
  }

  private static void dumpChildrenInDbRecursively(VirtualFile dir, int depth) {
    if (!(dir instanceof NewVirtualFile)) {
      System.out.println(dir.getPresentableUrl() + ": not in db (" + dir.getClass().getName() + ")");
      return;
    }

    List<VirtualFile> dirs = new ArrayList<>();
    int inDb = 0, contentInDb = 0, nullChildren = 0;
    PersistentFS persistentFS = PersistentFS.getInstance();
    if (persistentFS.wereChildrenAccessed(dir)) {
      for (String name : persistentFS.listPersisted(dir)) {
        inDb++;
        NewVirtualFile child = ((NewVirtualFile)dir).refreshAndFindChild(name);
        if (child == null) {
          nullChildren++;
          continue;
        }
        if (child.isDirectory()) {
          dirs.add(child);
        }
        else if (PersistentFS.getInstance().getCurrentContentId(child) != 0) {
          contentInDb++;
        }
      }
    }
    System.out.print(dir.getPresentableUrl() + ": " + inDb + " children in db");
    if (contentInDb > 0) {
      System.out.print(", content of " + contentInDb + " files in db");
    }
    if (nullChildren > 0) {
      System.out.print(", " + nullChildren + " invalid files in db");
    }
    System.out.println();

    if (depth > 10) {
      System.out.println("too deep, skipping children");
    }
    else {
      for (VirtualFile childDir : dirs) {
        dumpChildrenInDbRecursively(childDir, depth+1);
      }
    }
  }
}
