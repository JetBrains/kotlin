/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
public class PatternCompilerFactoryImpl extends PatternCompilerFactory {
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
  private final Map<List<Class>, PatternCompiler> myCompilers =
    ConcurrentFactoryMap.createMap(key -> new PatternCompilerImpl(key));

  @NotNull
  @Override
  public Class[] getPatternClasses(String alias) {
    return myClasses.get(alias);
  }

  @NotNull
  @Override
  public <T> PatternCompiler<T> getPatternCompiler(@NotNull Class[] patternClasses) {
    return myCompilers.get(Arrays.asList(patternClasses));
  }
}
