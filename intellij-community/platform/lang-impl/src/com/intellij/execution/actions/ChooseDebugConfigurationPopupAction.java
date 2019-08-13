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

package com.intellij.execution.actions;

import com.intellij.execution.Executor;

public class ChooseDebugConfigurationPopupAction extends ChooseRunConfigurationPopupAction {
  @Override
  protected Executor getDefaultExecutor() {
    return super.getAlternativeExecutor();
  }

  @Override
  protected Executor getAlternativeExecutor() {
    return super.getDefaultExecutor();
  }

  @Override
  protected String getAdKey() {
    return "debug.configuration.alternate.action.ad";
  }
}
