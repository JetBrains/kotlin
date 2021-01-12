// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.completion.impl;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.Classifier;
import com.intellij.codeInsight.lookup.ClassifierFactory;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.ide.plugins.DynamicPluginListener;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.util.Disposer;
import com.intellij.patterns.ElementPattern;
import com.intellij.psi.Weigher;
import com.intellij.util.Consumer;
import com.intellij.util.ExceptionUtil;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.codeWithMe.ClientIdKt.isForeignClientOnServer;

/**
 * @author peter
 */
public final class CompletionServiceImpl extends BaseCompletionService {
  private static final Logger LOG = Logger.getInstance(CompletionServiceImpl.class);
  private static volatile CompletionPhase ourPhase = CompletionPhase.NoCompletion;
  private static boolean ourTracePhases;
  private static Throwable ourPhaseTrace;

  public CompletionServiceImpl() {
    super();
    MessageBusConnection connection = ApplicationManager.getApplication().getMessageBus().connect();
    connection.subscribe(ProjectManager.TOPIC, new ProjectManagerListener() {
      @Override
      public void projectClosing(@NotNull Project project) {
        CompletionProgressIndicator indicator = getCurrentCompletionProgressIndicator();
        if (indicator != null && indicator.getProject() == project) {
          indicator.closeAndFinish(true);
          setCompletionPhase(CompletionPhase.NoCompletion);
        }
        else if (indicator == null) {
          setCompletionPhase(CompletionPhase.NoCompletion);
        }
      }
    });
    connection.subscribe(DynamicPluginListener.TOPIC, new DynamicPluginListener() {
      @Override
      public void beforePluginUnload(@NotNull IdeaPluginDescriptor pluginDescriptor, boolean isUpdate) {
        setCompletionPhase(CompletionPhase.NoCompletion);
      }
    });
  }

  @SuppressWarnings("MethodOverridesStaticMethodOfSuperclass")
  public static CompletionServiceImpl getCompletionService() {
    return (CompletionServiceImpl)CompletionService.getCompletionService();
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
  protected CompletionResultSet createResultSet(@NotNull CompletionParameters parameters,
                                                @NotNull Consumer<? super CompletionResult> consumer,
                                                @NotNull CompletionContributor contributor,
                                                @NotNull PrefixMatcher matcher) {
    return new CompletionResultSetImpl(consumer, matcher, contributor, parameters, null, null);
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

  private static class CompletionResultSetImpl extends BaseCompletionResultSet {
    CompletionResultSetImpl(Consumer<? super CompletionResult> consumer, PrefixMatcher prefixMatcher,
                            CompletionContributor contributor, CompletionParameters parameters,
                            @Nullable CompletionSorter sorter, @Nullable CompletionResultSetImpl original) {
      super(consumer, prefixMatcher, contributor, parameters, sorter, original);
    }

    @Override
    public void addAllElements(@NotNull Iterable<? extends LookupElement> elements) {
      CompletionThreadingBase.withBatchUpdate(() -> super.addAllElements(elements), myParameters.getProcess());
    }

    @Override
    @NotNull
    public CompletionResultSet withPrefixMatcher(@NotNull final PrefixMatcher matcher) {
      if (matcher.equals(getPrefixMatcher())) {
        return this;
      }

      return new CompletionResultSetImpl(getConsumer(), matcher, myContributor, myParameters, mySorter, this);
    }

    @NotNull
    @Override
    public CompletionResultSet withRelevanceSorter(@NotNull CompletionSorter sorter) {
      return new CompletionResultSetImpl(getConsumer(), getPrefixMatcher(), myContributor, myParameters, sorter, this);
    }

    @Override
    public void addLookupAdvertisement(@NotNull String text) {
      getCompletionService().setAdvertisementText(text);
    }

    @Override
    public void restartCompletionOnPrefixChange(ElementPattern<String> prefixCondition) {
      CompletionProcess process = myParameters.getProcess();
      if (process instanceof CompletionProcessBase) {
        ((CompletionProcessBase)process)
          .addWatchedPrefix(myParameters.getOffset() - getPrefixMatcher().getPrefix().length(), prefixCondition);
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
  public static void assertPhase(Class<? extends CompletionPhase> @NotNull ... possibilities) {
    if (!isPhase(possibilities)) {
      reportPhase();
    }
  }

  private static void reportPhase() {
    LOG.error(ourPhase + (ourPhaseTrace != null ? "; set at " + ExceptionUtil.getThrowableText(ourPhaseTrace) : ""));
    ourTracePhases = true; // let's have more diagnostics in case the exception happens again in this session
  }

  @SafeVarargs
  public static boolean isPhase(Class<? extends CompletionPhase> @NotNull ... possibilities) {
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
    if (oldIndicator != null &&
        !(phase instanceof CompletionPhase.BgCalculation) &&
        oldIndicator.isRunning() &&
        !oldIndicator.isCanceled()) {
      LOG.error("don't change phase during running completion: oldPhase=" + oldPhase);
    }
    boolean wasCompletionRunning = isRunningPhase(oldPhase);
    boolean isCompletionRunning = isRunningPhase(phase);
    if (isCompletionRunning != wasCompletionRunning) {
      ApplicationManager.getApplication().getMessageBus().syncPublisher(CompletionPhaseListener.TOPIC)
        .completionPhaseChanged(isCompletionRunning);
    }

    Disposer.dispose(oldPhase);
    ourPhase = phase;
    if (ourTracePhases) {
      ourPhaseTrace = new Throwable();
    }
  }

  private static boolean isRunningPhase(@NotNull CompletionPhase phase) {
    return phase != CompletionPhase.NoCompletion && !(phase instanceof CompletionPhase.ZombiePhase) &&
           !(phase instanceof CompletionPhase.ItemsCalculated);
  }


  public static CompletionPhase getCompletionPhase() {
    return ourPhase;
  }

  @NotNull
  @Override
  protected CompletionSorterImpl addWeighersBefore(@NotNull CompletionSorterImpl sorter) {
    CompletionSorterImpl processed = super.addWeighersBefore(sorter);
    return processed.withClassifier(CompletionSorterImpl.weighingFactory(new LiveTemplateWeigher()));
  }

  @NotNull
  @Override
  protected CompletionSorterImpl processStatsWeigher(@NotNull CompletionSorterImpl sorter,
                                                     @NotNull Weigher weigher,
                                                     @NotNull CompletionLocation location) {
    CompletionSorterImpl processedSorter = super.processStatsWeigher(sorter, weigher, location);
    return processedSorter.withClassifier(new ClassifierFactory<LookupElement>("stats") {
      @Override
      public Classifier<LookupElement> createClassifier(Classifier<LookupElement> next) {
        return new StatisticsWeigher.LookupStatisticsWeigher(location, next);
      }
    });
  }
}
