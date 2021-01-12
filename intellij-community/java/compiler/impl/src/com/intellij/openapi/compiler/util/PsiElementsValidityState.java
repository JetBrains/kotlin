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
package com.intellij.openapi.compiler.util;

import com.intellij.compiler.CompilerIOUtil;
import com.intellij.openapi.compiler.ValidityState;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.io.IOUtil;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PsiElementsValidityState implements ValidityState {
  private final Map<String, Long> myDependencies = new HashMap<>();

  public PsiElementsValidityState() {
  }

  public void addDependency(final String key, final Long value) {
    myDependencies.put(key, value);
  }

  @Override
  public boolean equalsTo(ValidityState otherState) {
    return otherState instanceof PsiElementsValidityState &&
           myDependencies.equals(((PsiElementsValidityState)otherState).myDependencies);
  }

  @Override
  public void save(DataOutput out) throws IOException {
    final Set<Map.Entry<String, Long>> entries = myDependencies.entrySet();
    out.writeInt(entries.size());
    for (Map.Entry<String, Long> entry : entries) {
      IOUtil.writeString(entry.getKey(), out);
      out.writeLong(entry.getValue().longValue());
    }
  }

  public static PsiElementsValidityState load(DataInput input) throws IOException {
    int size = input.readInt();
    final PsiElementsValidityState state = new PsiElementsValidityState();
    while (size-- > 0) {
      final String s = CompilerIOUtil.readString(input);
      final long timestamp = input.readLong();
      state.addDependency(s, timestamp);
    }
    return state;
  }

  public void addDependency(final PsiElement element) {
    final PsiFile psiFile = element.getContainingFile();
    if (psiFile != null) {
      VirtualFile file = psiFile.getVirtualFile();
      if (file != null) {
        addDependency(file.getUrl(), file.getTimeStamp());
      }
    }
  }
}