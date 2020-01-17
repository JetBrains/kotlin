// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.util;

public interface BiFunction<Result, Param1, Param2> {
  Result fun(Param1 var1, Param2 var2);
}
