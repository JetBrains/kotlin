/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.codeInsight.completion;

import org.jetbrains.annotations.NonNls;

/**
 * @author peter
 */
public interface
  CodeCompletionFeatures {
  @NonNls String EXCLAMATION_FINISH = "editing.completion.finishByExclamation";
  @NonNls String SECOND_BASIC_COMPLETION = "editing.completion.second.basic";
  @NonNls String EDITING_COMPLETION_SMARTTYPE_GENERAL = "editing.completion.smarttype.general";
  @NonNls String EDITING_COMPLETION_BASIC = "editing.completion.basic";
  @NonNls String EDITING_COMPLETION_CLASSNAME = "editing.completion.classname";
  @NonNls String EDITING_COMPLETION_CAMEL_HUMPS = "editing.completion.camelHumps";
  @NonNls String EDITING_COMPLETION_REPLACE = "editing.completion.replace";
  @NonNls String EDITING_COMPLETION_FINISH_BY_DOT_ETC = "editing.completion.finishByDotEtc";
  @NonNls String EDITING_COMPLETION_FINISH_BY_CONTROL_DOT = "editing.completion.finishByCtrlDot";
  @NonNls String EDITING_COMPLETION_FINISH_BY_SMART_ENTER = "editing.completion.finishBySmartEnter";

  @NonNls String EDITING_COMPLETION_CONTROL_ENTER = "editing.completion.finishByControlEnter";
  @NonNls String EDITING_COMPLETION_CONTROL_ARROWS = "editing.completion.cancelByControlArrows";
  @NonNls String EDITING_COMPLETION_CHANGE_SORTING = "editing.completion.changeSorting";
}
