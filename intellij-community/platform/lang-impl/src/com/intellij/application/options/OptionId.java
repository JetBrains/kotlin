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

package com.intellij.application.options;

/**
 * @author yole
 */
public class OptionId {
  private OptionId() {
  }

  public static final OptionId RENAME_IN_PLACE = new OptionId();
  public static final OptionId COMPLETION_SMART_TYPE = new OptionId();
  public static final OptionId AUTOCOMPLETE_ON_BASIC_CODE_COMPLETION = new OptionId();
  public static final OptionId SHOW_PARAMETER_NAME_HINTS_ON_COMPLETION = new OptionId();
}
