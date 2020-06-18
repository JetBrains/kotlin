// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.navigation.actions;

import com.intellij.internal.statistic.collectors.fus.actions.persistence.ActionsEventLogGroup;
import com.intellij.internal.statistic.eventLog.*;
import com.intellij.internal.statistic.eventLog.fus.FeatureUsageLogger;
import com.intellij.internal.statistic.service.fus.collectors.CounterUsagesCollector;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

final class GTDUCollector extends CounterUsagesCollector {

  enum GTDUChoice {
    GTD,
    SU,
    ;
  }

  private static final EnumEventField<GTDUChoice> CHOICE = EventFields.Enum("choice", GTDUChoice.class);
  private static final EventLogGroup GROUP = new EventLogGroup("gtdu", FeatureUsageLogger.getConfigVersion());
  private static final VarargEventId INVOKED = GROUP.registerVarargEvent(
    "invoked",
    EventFields.InputEvent,
    EventFields.ActionPlace,
    ActionsEventLogGroup.CONTEXT_MENU,
    CHOICE
  );

  static void record(@NotNull List<@NotNull EventPair<?>> eventData, @NotNull GTDUChoice choice) {
    INVOKED.log(ContainerUtil.append(eventData, CHOICE.with(choice)).toArray(new EventPair[0]));
  }

  @Override
  public EventLogGroup getGroup() {
    return GROUP;
  }
}
