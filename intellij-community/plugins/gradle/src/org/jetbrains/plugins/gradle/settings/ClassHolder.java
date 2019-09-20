/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package org.jetbrains.plugins.gradle.settings;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

/**
 * Holds information about target class.
 * 
 * @author Denis Zhdanov
 */
public class ClassHolder<T> implements Serializable {

  private static final long serialVersionUID = 1L;

  @NotNull private final String myClassName;
  
  @Nullable private transient Class<T> myTargetClass;

  /**
   * @param targetClass  class to use
   */
  public ClassHolder(@NotNull Class<T> targetClass) {
    myTargetClass = targetClass;
    myClassName = myTargetClass.getName();
  }

  @NotNull
  public static <T> ClassHolder<T> from(@NotNull Class<T> clazz) {
    return new ClassHolder<>(clazz);
  }

  @SuppressWarnings("unchecked")
  @NotNull
  public Class<T> getTargetClass() throws ClassNotFoundException {
    if (myTargetClass == null) {
      // We're not afraid of race condition here as the class will be loaded by the same class loader.
      myTargetClass = (Class<T>)Class.forName(myClassName);
    }
    return myTargetClass;
  }

  @NotNull
  public String getTargetClassName() {
    return myClassName;
  }

  @Override
  public int hashCode() {
    return myClassName.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ClassHolder holder = (ClassHolder)o;

    if (!myClassName.equals(holder.myClassName)) return false;
    if (myTargetClass != null ? !myTargetClass.equals(holder.myTargetClass) : holder.myTargetClass != null) return false;

    return true;
  }

  @Override
  public String toString() {
    return myClassName;
  }
}
