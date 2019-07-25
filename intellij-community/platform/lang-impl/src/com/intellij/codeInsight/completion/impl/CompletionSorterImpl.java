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
package com.intellij.codeInsight.completion.impl;

import com.intellij.codeInsight.completion.CompletionSorter;
import com.intellij.codeInsight.lookup.*;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author peter
 */
public class CompletionSorterImpl extends CompletionSorter {
  private final List<? extends ClassifierFactory<LookupElement>> myMembers;
  private final int myHashCode;

  CompletionSorterImpl(List<? extends ClassifierFactory<LookupElement>> members) {
    myMembers = members;
    myHashCode = myMembers.hashCode();
  }

  public static ClassifierFactory<LookupElement> weighingFactory(final LookupElementWeigher weigher) {
    final String id = weigher.toString();
    return new ClassifierFactory<LookupElement>(id) {
      @Override
      public Classifier<LookupElement> createClassifier(Classifier<LookupElement> next) {
        return new CachingComparingClassifier(next, weigher);
      }
    };
  }

  @Override public CompletionSorterImpl weighBefore(@NotNull final String beforeId, LookupElementWeigher... weighers) {
    if (weighers.length == 0) return this;

    CompletionSorterImpl result = this;
    for (LookupElementWeigher weigher : weighers) {
      result = result.withClassifier(beforeId, true, weighingFactory(weigher));
    }
    return result;
  }

  @Override public CompletionSorterImpl weighAfter(@NotNull final String afterId, LookupElementWeigher... weighers) {
    if (weighers.length == 0) return this;

    CompletionSorterImpl result = this;
    for (int i = weighers.length - 1; i >= 0; i--) {
      LookupElementWeigher weigher = weighers[i];
      result = result.withClassifier(afterId, false, weighingFactory(weigher));
    }
    return result;
  }

  @Override public CompletionSorterImpl weigh(final LookupElementWeigher weigher) {
    return withClassifier(weighingFactory(weigher));
  }

  public CompletionSorterImpl withClassifier(ClassifierFactory<LookupElement> classifierFactory) {
    return enhanced(classifierFactory, myMembers.size());
  }

  public CompletionSorterImpl withClassifier(@NotNull String anchorId,
                                             boolean beforeAnchor, ClassifierFactory<LookupElement> classifierFactory) {
    final int i = idIndex(anchorId);
    return enhanced(classifierFactory, beforeAnchor ? Math.max(0, i) : i + 1);
  }

  private CompletionSorterImpl enhanced(ClassifierFactory<LookupElement> classifierFactory, int index) {
    final List<ClassifierFactory<LookupElement>> copy = new ArrayList<>(myMembers);
    copy.add(index, classifierFactory);
    return new CompletionSorterImpl(copy);
  }


  private int idIndex(final String id) {
    return ContainerUtil.indexOf(myMembers, factory -> id.equals(factory.getId()));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof CompletionSorterImpl)) return false;

    CompletionSorterImpl that = (CompletionSorterImpl)o;

    if (!myMembers.equals(that.myMembers)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return myHashCode;
  }

  private static Classifier<LookupElement> createClassifier(final int index,
                                                            final List<? extends ClassifierFactory<LookupElement>> components,
                                                            Classifier<LookupElement> tail) {
    if (index == components.size()) {
      return tail;
    }

    return components.get(index).createClassifier(createClassifier(index + 1, components, tail));
  }

  public Classifier<LookupElement> buildClassifier(Classifier<LookupElement> tail) {
    return createClassifier(0, myMembers, tail);
  }
}
