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

/**
 * The class contains all magic constants used in relevant chain search
 */
public interface ChainSearchMagicConstants {
  /**
   * Used to know do 2 methods frequently occurred in one file simultaneously
   */
  int METHOD_PROBABILITY_THRESHOLD = 5;

  /**
   * Used to know do 2 variables frequently occurred in one file simultaneously
   */
  int VAR_PROBABILITY_THRESHOLD = 1;

  /**
   * Maximum count of completion contributor response chains
   */
  int MAX_SEARCH_RESULT_SIZE = 5;

  /**
   * Maximum method calls count in a chain
   */
  int MAX_CHAIN_SIZE = 4;

  /**
   * Relative coefficient to filter out rarely used methods
   */
  int FILTER_RATIO = 10;

  /**
   * Qualifier class hierarchy max cardinality
   */
  int MAX_HIERARCHY_SIZE = 20;
}
