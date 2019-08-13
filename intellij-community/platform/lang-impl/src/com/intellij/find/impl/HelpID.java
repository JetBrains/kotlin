
/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

package com.intellij.find.impl;

import org.jetbrains.annotations.NonNls;

public interface HelpID {
  @NonNls String FIND_PACKAGE_USAGES = "reference.dialogs.findUsages.package";
  @NonNls String FIND_CLASS_USAGES = "reference.dialogs.findUsages.class";
  @NonNls String FIND_METHOD_USAGES = "reference.dialogs.findUsages.method";
  @NonNls String FIND_THROW_USAGES = "reference.dialogs.findUsages.throwUsages";

  @NonNls String FIND_IN_PATH = "reference.dialogs.findinpath";
  @NonNls String REPLACE_IN_PATH = "reference.dialogs.findinpath";
  @NonNls String FIND_IN_EDITOR = "ixFindText";
  @NonNls String REPLACE_IN_EDITOR = "Replace_the_found_target";
}
