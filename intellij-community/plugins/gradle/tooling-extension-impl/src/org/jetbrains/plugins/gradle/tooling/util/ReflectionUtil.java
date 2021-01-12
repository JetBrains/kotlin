// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.util;

import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.util.GradleVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectionUtil {
  @Nullable
  public static Object callByReflection(@NotNull Object receiver,@NotNull String methodName) {
    Logger logger = Logging.getLogger(ReflectionUtil.class);
    Object result = null;
    try {
      Method getMethod = receiver.getClass().getMethod(methodName);
      result = getMethod.invoke(receiver);
    }
    catch (NoSuchMethodException e) {
      logger.warn("Can not find `" + methodName + "` for receiver [" + receiver + "], gradle version " + GradleVersion.current() , e);
    }
    catch (IllegalAccessException e) {
      logger.warn("Can not call `" + methodName + "` for receiver [" + receiver + "], gradle version " + GradleVersion.current(), e);
    }
    catch (InvocationTargetException e) {
      logger.warn("Can not call `" + methodName + "` for receiver [" + receiver + "], gradle version " + GradleVersion.current(), e);
    }
    return result;
  }
}
