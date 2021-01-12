// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.script;

import com.intellij.openapi.project.Project;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class IdeConsoleScriptBindings {

  public static final Binding<IDE> IDE = Binding.create("IDE", IDE.class);

  public static void ensureIdeIsBound(@Nullable Project project, @NotNull IdeScriptEngine engine) {
    IDE oldIdeBinding = IDE.get(engine);
    if (oldIdeBinding == null) {
      IDE.set(engine, new IDE(project, engine));
    }
  }

  private IdeConsoleScriptBindings() {
  }

  public static class Binding<T> {
    private final String myName;
    private final Class<T> myClass;

    private Binding(@NotNull String name, @NotNull Class<T> clazz) {
      myName = name;
      myClass = clazz;
    }

    public void set(@NotNull IdeScriptEngine engine, T value) {
      engine.setBinding(myName, value);
    }

    public T get(@NotNull IdeScriptEngine engine) {
      return ObjectUtils.tryCast(engine.getBinding(myName), myClass);
    }

    static <T> Binding<T> create(@NotNull String name, @NotNull Class<T> clazz) {
      return new Binding<>(name, clazz);
    }
  }
}
