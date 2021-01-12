// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.patterns.compiler;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ConcurrentFactoryMap;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author Gregory.Shrago
 */
final class PatternCompilerFactoryImpl extends PatternCompilerFactory {
  public static final ExtensionPointName<PatternClassBean> PATTERN_CLASS_EP =
    new ExtensionPointName<>("com.intellij.patterns.patternClass");

  private final Map<String, Class[]> myClasses = ConcurrentFactoryMap.createMap(key-> {
      final ArrayList<Class> result = new ArrayList<>(1);
      final List<String> typeList = key == null? null : Arrays.asList(key.split(",|\\s"));
      for (PatternClassBean bean : PATTERN_CLASS_EP.getExtensions()) {
        if (typeList == null || typeList.contains(bean.getAlias())) result.add(bean.getPatternClass());
      }
      return result.isEmpty()? ArrayUtil.EMPTY_CLASS_ARRAY : result.toArray(ArrayUtil.EMPTY_CLASS_ARRAY);
    }
  );
  private final Map<List<Class<?>>, PatternCompiler> myCompilers = ConcurrentFactoryMap.createMap(PatternCompilerImpl::new);

  public PatternCompilerFactoryImpl() {
    PATTERN_CLASS_EP.addChangeListener(() -> {
      myClasses.clear();
      myCompilers.clear();
    }, null);
  }

  @Override
  public Class<?> @NotNull [] getPatternClasses(String alias) {
    return myClasses.get(alias);
  }

  @NotNull
  @Override
  public <T> PatternCompiler<T> getPatternCompiler(Class @NotNull [] patternClasses) {
    return myCompilers.get(Arrays.asList(patternClasses));
  }
}
