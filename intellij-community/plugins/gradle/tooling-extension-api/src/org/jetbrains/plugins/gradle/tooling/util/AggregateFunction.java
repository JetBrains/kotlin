// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.util;

import com.intellij.openapi.util.Getter;
import com.intellij.util.Consumer;

public interface AggregateFunction<Result, PARAM> extends Getter<Result>, Consumer<PARAM> {
}
