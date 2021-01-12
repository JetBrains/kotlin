/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.util.Processor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Consider implementing {@link ChooseByNameInScopeItemProvider}
 */
public interface ChooseByNameItemProvider {

  /**
   * @deprecated this method is used only for compatibility issues.
   * Please use {@link ChooseByNameItemProvider#filterNames(ChooseByNameViewModel, String[], String)} instead.
   * Please avoid any implementations of this method except  calling of
   * {@link ChooseByNameItemProvider#filterNames(ChooseByNameViewModel, String[], String)}  method.
   * Method going to be removed in version 2021.1
   */
  @ApiStatus.ScheduledForRemoval(inVersion = "2021.1")
  @Deprecated
  @NotNull
  List<String> filterNames(@NotNull ChooseByNameBase base, String @NotNull [] names, @NotNull String pattern);

  @NotNull
  default List<String> filterNames(@NotNull ChooseByNameViewModel base, String @NotNull [] names, @NotNull String pattern) {
    assert base instanceof ChooseByNameBase : "This method supposed to be overridden if you want to use ChooseByNameViewModel as param";
    return filterNames((ChooseByNameBase)base, names, pattern);
  }

  /**
   * @deprecated this method is used only for compatibility issues.
   * Please use {@link ChooseByNameItemProvider#filterElements(ChooseByNameViewModel, String, boolean, ProgressIndicator, Processor)} instead.
   * Please avoid any implementations of this method except  calling of
   * {@link ChooseByNameItemProvider#filterElements(ChooseByNameViewModel, String, boolean, ProgressIndicator, Processor)} method.
   * Method going to be removed in version 2021.1
   */
  @ApiStatus.ScheduledForRemoval(inVersion = "2021.1")
  @Deprecated
  boolean filterElements(@NotNull ChooseByNameBase base,
                         @NotNull String pattern,
                         boolean everywhere,
                         @NotNull ProgressIndicator cancelled,
                         @NotNull Processor<Object> consumer);

  default boolean filterElements(@NotNull ChooseByNameViewModel base,
                         @NotNull String pattern,
                         boolean everywhere,
                         @NotNull ProgressIndicator cancelled,
                         @NotNull Processor<Object> consumer) {
    assert base instanceof ChooseByNameBase : "This method supposed to be overridden if you want to use ChooseByNameViewModel as param";
    return filterElements((ChooseByNameBase)base, pattern, everywhere, cancelled, consumer);
  }
}
