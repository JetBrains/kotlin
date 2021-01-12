// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.script;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class IDE {
  public final Application application = ApplicationManager.getApplication();
  public final Project project;

  private final Map<Object, Object> bindings = new ConcurrentHashMap<>();
  private final IdeScriptEngine myEngine;

  public IDE(@Nullable Project project, @NotNull IdeScriptEngine engine) {
    this.project = project;
    myEngine = engine;
  }

  public void print(Object o) {
    print(myEngine.getStdOut(), o);
  }

  public void error(Object o) {
    print(myEngine.getStdErr(), o);
  }

  public Object put(Object key, Object value) {
    return value == null ? bindings.remove(key) : bindings.put(key, value);
  }

  public Object get(Object key) {
    return bindings.get(key);
  }

  private static void print(Writer writer, Object o) {
    try {
      writer.append(String.valueOf(o)).append("\n");
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
