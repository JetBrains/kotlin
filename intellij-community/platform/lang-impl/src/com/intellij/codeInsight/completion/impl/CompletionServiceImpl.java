// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.completion.impl;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.ElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.Weigher;
import com.intellij.psi.WeighingService;
import com.intellij.psi.impl.DebugUtil;
import com.intellij.util.Consumer;
import com.intellij.util.ExceptionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

/**
 * @author peter
 */
public final class CompletionServiceImpl extends CompletionService {
  private static final Logger LOG = Logger.getInstance(CompletionServiceImpl.class);
  private static volatile CompletionPhase ourPhase = CompletionPhase.NoCompletion;
  private static Throwable ourPhaseTrace;

  @Nullable private CompletionProcess myApiCompletionProcess;

  public CompletionServiceImpl() {
    ApplicationManager.getApplication().getMessageBus().connect().subscribe(ProjectManager.TOPIC, new ProjectManagerListener() {
      @Override
      public void projectClosing(@NotNull Project project) {
        CompletionProgressIndicator indicator = getCurrentCompletionProgressIndicator();
        if (indicator != null && indicator.getProject() == project) {
          indicator.closeAndFinish(true);
          setCompletionPhase(CompletionPhase.NoCompletion);
        } else if (indicator == null) {
          setCompletionPhase(CompletionPhase.NoCompletion);
        }
      }
    });
  }

  @Override
  public void performCompletion(final CompletionParameters parameters, final Consumer<? super CompletionResult> consumer) {
    myApiCompletionProcess = parameters.getProcess();
    try {
      super.performCompletion(parameters, consumer);
    }
    finally {
      myApiCompletionProcess = null;
    }
  }

  @SuppressWarnings({"MethodOverridesStaticMethodOfSuperclass"})
  public static CompletionServiceImpl getCompletionService() {
    return (CompletionServiceImpl) CompletionService.getCompletionService();
  }

  @Override
  public void setAdvertisementText(@Nullable final String text) {
    if (text == null) return;
    final CompletionProgressIndicator completion = getCurrentCompletionProgressIndicator();
    if (completion != null) {
      completion.addAdvertisement(text, null);
    }
  }

  @Override
  protected String suggestPrefix(CompletionParameters parameters) {
    final PsiElement position = parameters.getPosition();
    final int offset = parameters.getOffset();
    TextRange range = position.getTextRange();
    assert range.containsOffset(offset) : position + "; " + offset + " not in " + range;
    //noinspection deprecation
    return CompletionData.findPrefixStatic(position, offset);
  }

  @Override
  @NotNull
  protected PrefixMatcher createMatcher(String prefix, boolean typoTolerant) {
    return createMatcher(prefix, true, typoTolerant);
  }

  @NotNull
  private static CamelHumpMatcher createMatcher(String prefix, boolean caseSensitive, boolean typoTolerant) {
    return new CamelHumpMatcher(prefix, caseSensitive, typoTolerant);
  }

  @Override
  protected CompletionResultSet createResultSet(CompletionParameters parameters, Consumer<? super CompletionResult> consumer,
                                                @NotNull CompletionContributor contributor, PrefixMatcher matcher) {
    return new CompletionResultSetImpl(consumer, matcher, contributor, parameters, defaultSorter(parameters, matcher), null);
  }

  @Override
  public CompletionProcess getCurrentCompletion() {
    CompletionProgressIndicator indicator = getCurrentCompletionProgressIndicator();
    return indicator != null ? indicator : myApiCompletionProcess;
  }

  public static CompletionProgressIndicator getCurrentCompletionProgressIndicator() {
    if (isPhase(CompletionPhase.BgCalculation.class, CompletionPhase.ItemsCalculated.class, CompletionPhase.CommittingDocuments.class,
                CompletionPhase.Synchronous.class)) {
      return ourPhase.indicator;
    }
    return null;
  }

  private static class CompletionResultSetImpl extends CompletionResultSet {
    private final CompletionParameters myParameters;
    private final CompletionSorterImpl mySorter;
    @Nullable
    private final CompletionResultSetImpl myOriginal;

    CompletionResultSetImpl(Consumer<? super CompletionResult> consumer, PrefixMatcher prefixMatcher,
                            CompletionContributor contributor, CompletionParameters parameters,
                            @NotNull CompletionSorterImpl sorter, @Nullable CompletionResultSetImpl original) {
      super(prefixMatcher, consumer, contributor);
      myParameters = parameters;
      mySorter = sorter;
      myOriginal = original;
    }

    @Override
    public void addAllElements(@NotNull Iterable<? extends LookupElement> elements) {
      CompletionThreadingBase.withBatchUpdate(() -> super.addAllElements(elements), myParameters.getProcess());
    }

    @Override
    public void addElement(@NotNull final LookupElement element) {
      ProgressManager.checkCanceled();
      if (!element.isValid()) {
        LOG.error("Invalid lookup element: " + element + " of " + element.getClass() +
          " in " + myParameters.getOriginalFile() + " of " + myParameters.getOriginalFile().getClass());
        return;
      }

      CompletionResult matched = CompletionResult.wrap(element, getPrefixMatcher(), mySorter);
      if (matched != null) {
        passResult(matched);
      }
    }

    @Override
    @NotNull
    public CompletionResultSet withPrefixMatcher(@NotNull final PrefixMatcher matcher) {
      if (matcher.equals(getPrefixMatcher())) {
        return this;
      }
      
      return new CompletionResultSetImpl(getConsumer(), matcher, myContributor, myParameters, mySorter, this);
    }

    @Override
    public void stopHere() {
      if (LOG.isTraceEnabled()) {
        LOG.trace("Completion stopped\n" + DebugUtil.currentStackTrace());
      }
      super.stopHere();
      if (myOriginal != null) {
        myOriginal.stopHere();
      }
    }

    @Override
    @NotNull
    public CompletionResultSet withPrefixMatcher(@NotNull final String prefix) {
      return withPrefixMatcher(getPrefixMatcher().cloneWithPrefix(prefix));
    }

    @NotNull
    @Override
    public CompletionResultSet withRelevanceSorter(@NotNull CompletionSorter sorter) {
      return new CompletionResultSetImpl(getConsumer(), getPrefixMatcher(), myContributor, myParameters, (CompletionSorterImpl) sorter,
                                         this);
    }

    @Override
    public void addLookupAdvertisement(@NotNull String text) {
      getCompletionService().setAdvertisementText(text);
    }

    @NotNull
    @Override
    public CompletionResultSet caseInsensitive() {
      PrefixMatcher matcher = getPrefixMatcher();
      boolean typoTolerant = matcher instanceof CamelHumpMatcher && ((CamelHumpMatcher)matcher).isTypoTolerant();
      return withPrefixMatcher(createMatcher(matcher.getPrefix(), false, typoTolerant));
    }

    @Override
    public void restartCompletionOnPrefixChange(ElementPattern<String> prefixCondition) {
      CompletionProcess process = myParameters.getProcess();
      if (process instanceof CompletionProgressIndicator) {
        ((CompletionProgressIndicator)process).addWatchedPrefix(myParameters.getOffset() - getPrefixMatcher().getPrefix().length(), prefixCondition);
      }
    }

    @Override
    public void restartCompletionWhenNothingMatches() {
      CompletionProcess process = myParameters.getProcess();
      if (process instanceof CompletionProgressIndicator) {
        ((CompletionProgressIndicator)process).getLookup().setStartCompletionWhenNothingMatches(true);
      }
    }
  }

  @SafeVarargs
  public static void assertPhase(@NotNull Class<? extends CompletionPhase>... possibilities) {
    if (!isPhase(possibilities)) {
      LOG.error(ourPhase + "; set at " + ExceptionUtil.getThrowableText(ourPhaseTrace));
    }
  }

  @SafeVarargs
  public static boolean isPhase(@NotNull Class<? extends CompletionPhase>... possibilities) {
    CompletionPhase phase = getCompletionPhase();
    for (Class<? extends CompletionPhase> possibility : possibilities) {
      if (possibility.isInstance(phase)) {
        return true;
      }
    }
    return false;
  }

  public static void setCompletionPhase(@NotNull CompletionPhase phase) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    CompletionPhase oldPhase = getCompletionPhase();
    CompletionProgressIndicator oldIndicator = oldPhase.indicator;
    if (oldIndicator != null && !(phase instanceof CompletionPhase.BgCalculation)) {
      LOG.assertTrue(!oldIndicator.isRunning() || oldIndicator.isCanceled(), "don't change phase during running completion: oldPhase=" + oldPhase);
    }
    boolean wasCompletionRunning = isRunningPhase(oldPhase);
    boolean isCompletionRunning = isRunningPhase(phase);
    if (isCompletionRunning != wasCompletionRunning) {
      ApplicationManager.getApplication().getMessageBus().syncPublisher(CompletionPhaseListener.TOPIC).completionPhaseChanged(isCompletionRunning);
    }

    Disposer.dispose(oldPhase);
    ourPhase = phase;
    ourPhaseTrace = new Throwable();
  }

  private static boolean isRunningPhase(@NotNull CompletionPhase phase) {
    return phase != CompletionPhase.NoCompletion && !(phase instanceof CompletionPhase.ZombiePhase) &&
           !(phase instanceof CompletionPhase.ItemsCalculated);
  }


  public static CompletionPhase getCompletionPhase() {
    return ourPhase;
  }

  @Override
  public CompletionSorterImpl defaultSorter(CompletionParameters parameters, final PrefixMatcher matcher) {
    final CompletionLocation location = new CompletionLocation(parameters);

    CompletionSorterImpl sorter = emptySorter();
    sorter = sorter.withClassifier(CompletionSorterImpl.weighingFactory(new LiveTemplateWeigher()));
    sorter = sorter.withClassifier(CompletionSorterImpl.weighingFactory(new PreferStartMatching()));

    for (final Weigher weigher : WeighingService.getWeighers(CompletionService.RELEVANCE_KEY)) {
      final String id = weigher.toString();
      if ("prefix".equals(id)) {
        sorter = sorter.withClassifier(CompletionSorterImpl.weighingFactory(new RealPrefixMatchingWeigher()));
      } else if ("stats".equals(id)) {
        sorter = sorter.withClassifier(new ClassifierFactory<LookupElement>("stats") {
          @Override
          public Classifier<LookupElement> createClassifier(Classifier<LookupElement> next) {
            return new StatisticsWeigher.LookupStatisticsWeigher(location, next);
          }
        });
      } else {
        sorter = sorter.weigh(new LookupElementWeigher(id, true, false) {
          @Override
          public Comparable weigh(@NotNull LookupElement element) {
            //noinspection unchecked
            return weigher.weigh(element, location);
          }
        });
      }
    }

    return sorter.withClassifier("priority", true, new ClassifierFactory<LookupElement>("liftShorter") {
      @Override
      public Classifier<LookupElement> createClassifier(final Classifier<LookupElement> next) {
        return new LiftShorterItemsClassifier("liftShorter", next, new LiftShorterItemsClassifier.LiftingCondition(), false);
      }
    });
  }

  @Override
  public CompletionSorterImpl emptySorter() {
    return new CompletionSorterImpl(new ArrayList<>());
  }

  public static boolean isStartMatch(LookupElement element, WeighingContext context) {
    return getItemMatcher(element, context).isStartMatch(element);
  }

  static PrefixMatcher getItemMatcher(LookupElement element, WeighingContext context) {
    PrefixMatcher itemMatcher = context.itemMatcher(element);
    String pattern = context.itemPattern(element);
    if (!pattern.equals(itemMatcher.getPrefix())) {
      return itemMatcher.cloneWithPrefix(pattern);
    }
    return itemMatcher;
  }
}
