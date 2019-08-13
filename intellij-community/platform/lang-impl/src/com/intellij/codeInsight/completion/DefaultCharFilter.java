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

import com.intellij.codeInsight.lookup.CharFilter;
import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.impl.LookupImpl;
import com.intellij.util.containers.ContainerUtil;

public class DefaultCharFilter extends CharFilter {

  @Override
  public Result acceptChar(char c, final int prefixLength, final Lookup lookup) {
    if (Character.isJavaIdentifierPart(c)) return Result.ADD_TO_PREFIX;
    switch(c){
      case '.':
      case ',':
      case ';':
      case '=':
      case ' ':
      case ':':
      case '(':
        return Result.SELECT_ITEM_AND_FINISH_LOOKUP;

      case '-':
        return ContainerUtil.exists(lookup.getItems(), item -> matchesAfterAppendingChar(lookup, item, c))
               ? Result.ADD_TO_PREFIX
               : Result.HIDE_LOOKUP;

      default:
        return Result.HIDE_LOOKUP;
    }
  }

  private static boolean matchesAfterAppendingChar(Lookup lookup, LookupElement item, char c) {
    PrefixMatcher matcher = lookup.itemMatcher(item);
    return matcher.cloneWithPrefix((matcher.getPrefix() + ((LookupImpl)lookup).getAdditionalPrefix()) + c).prefixMatches(item);
  }
}