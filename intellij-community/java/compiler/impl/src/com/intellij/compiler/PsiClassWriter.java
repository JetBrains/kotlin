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
package com.intellij.compiler;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.JavaSdkVersion;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.ClassUtil;
import org.jetbrains.org.objectweb.asm.ClassWriter;

/**
 * @author yole
 */
public class PsiClassWriter extends ClassWriter {
  private final Project myProject;

  public PsiClassWriter(final Project project, boolean isJava6) {
    super(isJava6 ? ClassWriter.COMPUTE_FRAMES : ClassWriter.COMPUTE_MAXS);
    myProject = project;
  }

  public PsiClassWriter(final Module module) {
    this(module.getProject(), isJdk6(module));
  }

  private static boolean isJdk6(final Module module) {
    final Sdk projectJdk = ModuleRootManager.getInstance(module).getSdk();
    if (projectJdk == null) return false;
    return JavaSdk.getInstance().isOfVersionOrHigher(projectJdk, JavaSdkVersion.JDK_1_6);
  }

  @Override
  protected String getCommonSuperClass(final String type1, final String type2) {
    //PsiManager.getInstance(myProject).findClass(type1.replace('/', '.').replace('$', '.'), myProject.getAllScope());
    return ReadAction.compute(() -> {
      final PsiManager manager = PsiManager.getInstance(myProject);
      PsiClass c = ClassUtil.findPsiClassByJVMName(manager, type1);
      if (c == null) {
        return "java/lang/Object";
      }
      PsiClass d = ClassUtil.findPsiClassByJVMName(manager, type2);
      if (d == null) {
        return "java/lang/Object";
      }
      if (c.isInheritor(d, true)) {
        return ClassUtil.getJVMClassName(d).replace('.', '/');
      }
      if (d.isInheritor(c, true)) {
        return ClassUtil.getJVMClassName(c).replace('.', '/');
      }
      if (c.isInterface() || d.isInterface()) {
        return "java/lang/Object";
      }
      do {
        c = c.getSuperClass();
      }
      while (c != null && !d.isInheritor(c, true));
      if (c == null) {
        return "java/lang/Object";
      }
      return ClassUtil.getJVMClassName(c).replace('.', '/');
    });
  }

}