// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInspection.ui;

import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Exports aggregate results that can't be attached to any specific problem descriptor.
 *
 * @author Pavel.Dolgov
 */
public interface AggregateResultsExporter {
  void exportAggregateResults(@NotNull Consumer<? super Element> resultConsumer);
}
