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

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.util.NullableComputable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.proximity.PsiProximityComparator;
import org.jetbrains.annotations.NotNull;

/**
 * @author peter
*/
public class LookupElementProximityWeigher extends CompletionWeigher {

  @Override
  public Comparable weigh(@NotNull final LookupElement item, @NotNull final CompletionLocation location) {
    // don't extract variable from getPsiElement to avoid excessive memory usage
    if (item.getPsiElement() != null) {
      return PsiProximityComparator.getProximity((NullableComputable<PsiElement>)() -> item.getPsiElement(), location.getCompletionParameters().getPosition(), location.getProcessingContext());
    }
    return null;
  }
}
