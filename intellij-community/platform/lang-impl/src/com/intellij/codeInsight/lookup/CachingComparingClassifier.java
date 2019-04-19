// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.lookup;

import com.intellij.codeInsight.completion.CompletionLookupArranger;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.ForceableComparable;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
* @author peter
*/
public class CachingComparingClassifier extends ComparingClassifier<LookupElement> {
  private final Map<LookupElement, Comparable> myWeights = new IdentityHashMap<>();
  private final LookupElementWeigher myWeigher;
  private Ref<Comparable> myFirstWeight;
  private boolean myPrimitive = true;
  private int myPrefixChanges = -1;

  public CachingComparingClassifier(Classifier<LookupElement> next, LookupElementWeigher weigher) {
    super(next, weigher.toString(), weigher.isNegated());
    myWeigher = weigher;
  }

  @Nullable
  @Override
  public final Comparable getWeight(LookupElement element, ProcessingContext context) {
    Comparable w = myWeights.get(element);
    if (w == null && myWeigher.isPrefixDependent()) {
      myWeights.put(element, w = myWeigher.weigh(element, context.get(CompletionLookupArranger.WEIGHING_CONTEXT)));
    }
    return w;
  }

  @Override
  public void removeElement(@NotNull LookupElement element, @NotNull ProcessingContext context) {
    myWeights.remove(element);
    super.removeElement(element, context);
  }

  @NotNull
  @Override
  public Iterable<LookupElement> classify(@NotNull Iterable<LookupElement> source, @NotNull ProcessingContext context) {
    if (!myWeigher.isPrefixDependent() && myPrimitive) {
      return myNext.classify(source, context);
    }
    checkPrefixChanged(context);

    return super.classify(source, context);
  }

  private void checkPrefixChanged(ProcessingContext context) {
    int actualPrefixChanges = context.get(CompletionLookupArranger.PREFIX_CHANGES).intValue();
    if (myWeigher.isPrefixDependent() && myPrefixChanges != actualPrefixChanges) {
      myPrefixChanges = actualPrefixChanges;
      myWeights.clear();
    }
  }

  @NotNull
  @Override
  public List<Pair<LookupElement, Object>> getSortingWeights(@NotNull Iterable<LookupElement> items, @NotNull ProcessingContext context) {
    checkPrefixChanged(context);
    return super.getSortingWeights(items, context);
  }

  @Override
  public void addElement(@NotNull LookupElement t, @NotNull ProcessingContext context) {
    Comparable weight = myWeigher.weigh(t, context.get(CompletionLookupArranger.WEIGHING_CONTEXT));
    if (weight instanceof ForceableComparable) {
      ((ForceableComparable)weight).force();
    }
    if (!myWeigher.isPrefixDependent() && myPrimitive) {
      if (myFirstWeight == null) {
        myFirstWeight = Ref.create(weight);
      } else if (!Comparing.equal(myFirstWeight.get(), weight)) {
        myPrimitive = false;
      }
    }
    myWeights.put(t, weight);
    super.addElement(t, context);
  }

}
