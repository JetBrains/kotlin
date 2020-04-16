// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.util.proximity;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.ProximityLocation;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.JavaResourceRootType;
import org.jetbrains.jps.model.java.JavaSourceRootType;

import java.util.Collections;

public class DirectoryTypeWeigher extends ProximityWeigher {

  @Override
  public Comparable weigh(@NotNull PsiElement element, @NotNull ProximityLocation location) {
    Project project = location.getProject();
    if (project == null) return 0;

    VirtualFile file = PsiUtilCore.getVirtualFile(element);
    if (file == null) return 0;

    ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
    if (fileIndex.isUnderSourceRootOfType(file, Collections.singleton(JavaSourceRootType.SOURCE))) return 3;
    if (fileIndex.isUnderSourceRootOfType(file, Collections.singleton(JavaSourceRootType.TEST_SOURCE))) return 2;
    if (fileIndex.isUnderSourceRootOfType(file, ContainerUtil.newHashSet(JavaResourceRootType.RESOURCE, JavaResourceRootType.TEST_RESOURCE))) return 1;

    return 0;
  }
}
