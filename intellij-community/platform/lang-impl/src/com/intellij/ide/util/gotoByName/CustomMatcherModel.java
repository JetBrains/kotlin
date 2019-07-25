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

package com.intellij.ide.util.gotoByName;

import org.jetbrains.annotations.NotNull;

/**
 * @author Roman.Chernyatchik
 * @date Mar 11, 2009
 */
public interface CustomMatcherModel {
  /**
   * Allows to implement custom matcher for matching items from ChooseByName popup
   * with user pattern
   * @param popupItem Item from list
   * @param userPattern Pattern defined by user in Choose by name popup
   * @return True if matches
   */
  boolean matches(@NotNull final String popupItem, @NotNull final String userPattern);
}
