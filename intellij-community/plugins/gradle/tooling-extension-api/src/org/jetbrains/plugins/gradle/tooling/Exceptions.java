// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling;

import org.gradle.tooling.model.UnsupportedMethodException;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;

public class Exceptions {
  public static UnsupportedMethodException unsupportedMethod(String method) {
    return new UnsupportedMethodException(formatUnsupportedModelMethod(method));
  }

  private static String formatUnsupportedModelMethod(String method) {
    return String.format("Unsupported method: %s."
                         + "\nThe version of Gradle you connect to does not support that method."
                         + "\nTo resolve the problem you can change/upgrade the target version of Gradle you connect to."
                         + "\nAlternatively, you can ignore this exception and read other information from the model.",
                         method);
  }


  @NotNull
  public static Throwable unwrap(@NotNull Throwable e) {
    for (Throwable candidate = e; candidate != null; candidate = candidate.getCause()) {
      Class<? extends Throwable> clazz = candidate.getClass();
      if (clazz != InvocationTargetException.class && clazz != UndeclaredThrowableException.class) {
        return candidate;
      }
    }
    return e;
  }
}
