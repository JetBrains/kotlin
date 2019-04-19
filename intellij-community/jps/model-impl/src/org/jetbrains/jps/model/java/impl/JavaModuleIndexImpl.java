// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.jps.model.java.impl;

import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.jps.model.ex.JpsElementBase;
import org.jetbrains.jps.model.java.JavaModuleIndex;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.jetbrains.jps.model.java.compiler.JpsCompilerExcludes;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.model.module.JpsModuleSourceRoot;
import org.jetbrains.jps.util.JpsPathUtil;

import java.io.File;
import java.util.Map;

public class JavaModuleIndexImpl extends JpsElementBase<JavaModuleIndexImpl> implements JavaModuleIndex {
  private static final String SOURCE_SUFFIX = ":S";
  private static final String TEST_SUFFIX = ":T";
  private static final String MODULE_INFO_FILE = "module-info.java";

  private final Map<String, File> myMapping;
  private final JpsCompilerExcludes myExcludes;

  public JavaModuleIndexImpl(@NotNull JpsCompilerExcludes excludes) {
    myMapping = ContainerUtil.newHashMap();
    myExcludes = excludes;
  }

  @Override
  @NotNull
  public JavaModuleIndexImpl createCopy() {
    JavaModuleIndexImpl copy = new JavaModuleIndexImpl(myExcludes);
    copy.myMapping.putAll(myMapping);
    return copy;
  }

  @Override
  public void applyChanges(@NotNull JavaModuleIndexImpl modified) {
    // not supported
  }

  @Nullable
  @Override
  public File getModuleInfoFile(@NotNull JpsModule module, boolean forTests) {
    String key = module.getName() + (forTests ? TEST_SUFFIX : SOURCE_SUFFIX);

    if (myMapping.containsKey(key)) {
      return myMapping.get(key);
    }

    File file = findModuleInfoFile(module, forTests ? JavaSourceRootType.TEST_SOURCE : JavaSourceRootType.SOURCE);
    myMapping.put(key, file);
    return file;
  }

  private File findModuleInfoFile(JpsModule module, JavaSourceRootType rootType) {
    for (JpsModuleSourceRoot root : module.getSourceRoots()) {
      if (rootType.equals(root.getRootType())) {
        File file = new File(JpsPathUtil.urlToOsPath(root.getUrl()), MODULE_INFO_FILE);
        if (file.isFile() && !myExcludes.isExcluded(file)) {
          return file;
        }
      }
    }

    return null;
  }

  @TestOnly
  public void dropCache() {
    myMapping.clear();
  }
}