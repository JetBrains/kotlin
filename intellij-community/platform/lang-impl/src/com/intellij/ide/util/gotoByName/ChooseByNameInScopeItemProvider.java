// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.util.gotoByName;

import com.intellij.ide.actions.searcheverywhere.FoundItemDescriptor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.util.Processor;
import com.intellij.util.indexing.FindSymbolParameters;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * @author gregsh
 * @author msokolov
 */
public interface ChooseByNameInScopeItemProvider extends ChooseByNameWeightedItemProvider {

  /**
   * Searches for elements that match specified filters. Uses extended filters for searching (see {@link FindSymbolParameters})
   *
   * @param parameters search parameters
   * @param indicator  {@link ProgressIndicator} which could be used to cancel search process
   * @param consumer   consumer for process all found items
   * @return {@code true} if all found items were processed. If {@code consumer} returns {@code false} for any of them
   * search will be stopped and method will return {@code false}
   * @see FindSymbolParameters
   *
   * @deprecated this method is used only for compatibility issues.
   * Please use {@link ChooseByNameInScopeItemProvider#filterElements(ChooseByNameViewModel, FindSymbolParameters, ProgressIndicator, Processor)} instead.
   * Please avoid any implementations of this method except  calling of
   * {@link ChooseByNameInScopeItemProvider#filterElements(ChooseByNameViewModel, FindSymbolParameters, ProgressIndicator, Processor)} method.
   * Method going to be removed in version 2021.1
   */
  @ApiStatus.ScheduledForRemoval(inVersion = "2021.1")
  @Deprecated
  boolean filterElements(@NotNull ChooseByNameBase base,
                         @NotNull FindSymbolParameters parameters,
                         @NotNull ProgressIndicator indicator,
                         @NotNull Processor<Object> consumer);

  /**
   * Searches for elements that match specified filters. Uses extended filters for searching (see {@link FindSymbolParameters})
   *
   * @param parameters search parameters
   * @param indicator  {@link ProgressIndicator} which could be used to cancel search process
   * @param consumer   consumer for process all found items
   * @return {@code true} if all found items were processed. If {@code consumer} returns {@code false} for any of them
   * search will be stopped and method will return {@code false}
   * @see FindSymbolParameters
   */
  boolean filterElements(@NotNull ChooseByNameViewModel base,
                         @NotNull FindSymbolParameters parameters,
                         @NotNull ProgressIndicator indicator,
                         @NotNull Processor<Object> consumer);

  /**
   * Searches for elements that match specified filters and returns also their weights. Uses extended filters for searching (see {@link FindSymbolParameters})
   * Method should be used when receiver wants to sort found items by itself (for example when they should be mixed with results from other providers)
   *
   * @param parameters search parameters
   * @param indicator  {@link ProgressIndicator} which could be used to cancel search process
   * @param consumer   consumer for process all found items. Takes instances of {@link FoundItemDescriptor} wrapper, which contains also
   *                   item's wights
   * @return {@code true} if all found items were processed. If {@code consumer} returns {@code false} for any of them search will be
   * stopped and method will return {@code false}
   * @see FindSymbolParameters
   * @see FoundItemDescriptor
   *
   * @deprecated this method is used only for compatibility issues.
   * Please use {@link ChooseByNameInScopeItemProvider#filterElementsWithWeights(ChooseByNameViewModel, FindSymbolParameters, ProgressIndicator, Processor)} instead.
   * Please avoid any implementations of this method except  calling of
   * {@link ChooseByNameInScopeItemProvider#filterElementsWithWeights(ChooseByNameViewModel, FindSymbolParameters, ProgressIndicator, Processor)} method.
   * Method going to be removed in version 2021.1
   */
  @ApiStatus.ScheduledForRemoval(inVersion = "2021.1")
  @Deprecated
  boolean filterElementsWithWeights(@NotNull ChooseByNameBase base,
                                    @NotNull FindSymbolParameters parameters,
                                    @NotNull ProgressIndicator indicator,
                                    @NotNull Processor<? super FoundItemDescriptor<?>> consumer);

  /**
   * Searches for elements that match specified filters and returns also their weights. Uses extended filters for searching (see {@link FindSymbolParameters})
   * Method should be used when receiver wants to sort found items by itself (for example when they should be mixed with results from other providers)
   *
   * @param parameters search parameters
   * @param indicator  {@link ProgressIndicator} which could be used to cancel search process
   * @param consumer   consumer for process all found items. Takes instances of {@link FoundItemDescriptor} wrapper, which contains also
   *                   item's wights
   * @return {@code true} if all found items were processed. If {@code consumer} returns {@code false} for any of them search will be
   * stopped and method will return {@code false}
   * @see FindSymbolParameters
   * @see FoundItemDescriptor
   */
  boolean filterElementsWithWeights(@NotNull ChooseByNameViewModel base,
                                    @NotNull FindSymbolParameters parameters,
                                    @NotNull ProgressIndicator indicator,
                                    @NotNull Processor<? super FoundItemDescriptor<?>> consumer);
}
