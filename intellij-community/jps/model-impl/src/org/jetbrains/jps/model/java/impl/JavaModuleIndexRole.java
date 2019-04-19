// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.jps.model.java.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.JpsElementParameterizedCreator;
import org.jetbrains.jps.model.ex.JpsElementChildRoleBase;
import org.jetbrains.jps.model.java.JavaModuleIndex;
import org.jetbrains.jps.model.java.compiler.JpsCompilerExcludes;

/**
 * @author Eugene Zhuravlev
 */
public class JavaModuleIndexRole extends JpsElementChildRoleBase<JavaModuleIndex> implements JpsElementParameterizedCreator<JavaModuleIndex, JpsCompilerExcludes>{
  public static final JavaModuleIndexRole INSTANCE = new JavaModuleIndexRole();

  public JavaModuleIndexRole() {
    super("java module index");
  }

  @Override
  @NotNull
  public JavaModuleIndex create(@NotNull JpsCompilerExcludes excludes) {
    return new JavaModuleIndexImpl(excludes);
  }
}