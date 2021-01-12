// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.execution.build.output;

import com.intellij.build.events.BuildEvent;
import com.intellij.build.events.MessageEvent;
import com.intellij.build.events.impl.MessageEventImpl;
import com.intellij.build.output.BuildOutputInstantReader;
import com.intellij.build.output.BuildOutputParser;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * @author Vladislav.Soroka
 */
public class GradleSyncOutputParser implements BuildOutputParser {

  private static final String ERROR_PREFIX = "[sync error]";
  private static final String WARNING_PREFIX = "[sync warning]";

  @Override
  public boolean parse(@NotNull String line, @NotNull BuildOutputInstantReader reader, @NotNull Consumer<? super BuildEvent> messageConsumer) {
    MessageEvent.Kind kind = MessageEvent.Kind.ERROR;
    String prefix = ERROR_PREFIX;
    int prefixIndex = line.indexOf(ERROR_PREFIX);
    if (prefixIndex < 0) {
      kind = MessageEvent.Kind.WARNING;
      prefix = WARNING_PREFIX;
      prefixIndex = line.indexOf(WARNING_PREFIX);
      if (prefixIndex < 0) {
        return false;
      }
    }

    String text = line.substring(prefixIndex + prefix.length()).trim();
    messageConsumer.accept(new MessageEventImpl(reader.getParentEventId(), kind, null, text, text));
    return true;
  }
}

