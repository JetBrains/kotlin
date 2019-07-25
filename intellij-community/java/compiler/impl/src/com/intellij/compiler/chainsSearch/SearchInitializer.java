/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.compiler.chainsSearch;

import com.intellij.compiler.chainsSearch.context.ChainCompletionContext;

import java.util.*;

public class SearchInitializer {
  private final ChainCompletionContext myContext;
  private final LinkedList<OperationChain> myQueue;
  private final LinkedHashMap<RefChainOperation, OperationChain> myChains;

  public SearchInitializer(SortedSet<? extends ChainOpAndOccurrences<? extends RefChainOperation>> indexValues,
                           ChainCompletionContext context) {
    myContext = context;
    int size = indexValues.size();
    List<OperationChain> chains = new ArrayList<>(size);
    populateFrequentlyUsedMethod(indexValues, chains);
    myQueue = new LinkedList<>();
    myChains = new LinkedHashMap<>(chains.size());
    for (OperationChain chain : chains) {
      RefChainOperation signature = chain.getHead();
      myQueue.add(chain);
      myChains.put(signature, chain);
    }
  }

  public LinkedList<OperationChain> getChainQueue() {
    return myQueue;
  }

  public LinkedHashMap<RefChainOperation, OperationChain> getChains() {
    return myChains;
  }

  private void populateFrequentlyUsedMethod(SortedSet<? extends ChainOpAndOccurrences> operations,
                                            List<? super OperationChain> chains) {
    int bestOccurrences = -1;
    for (ChainOpAndOccurrences indexValue : operations) {
      OperationChain operationChain = OperationChain.create(indexValue.getOperation(), indexValue.getOccurrenceCount(), myContext);
      if (operationChain != null) {
        chains.add(operationChain);
        int occurrences = indexValue.getOccurrenceCount();
        if (bestOccurrences == -1) {
          bestOccurrences = occurrences;
        }
        else if (bestOccurrences > occurrences * ChainSearchMagicConstants.FILTER_RATIO) {
          return;
        }
      }
    }
  }
}