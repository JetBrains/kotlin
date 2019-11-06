// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.util.gotoByName;

import com.intellij.concurrency.JobLauncher;
import com.intellij.ide.actions.searcheverywhere.FoundItemDescriptor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiCompiledElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.codeStyle.MinusculeMatcher;
import com.intellij.psi.codeStyle.NameUtil;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.proximity.PsiProximityComparator;
import com.intellij.util.*;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.FList;
import com.intellij.util.indexing.FindSymbolParameters;
import com.intellij.util.indexing.IdFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

public class DefaultChooseByNameItemProvider implements ChooseByNameInScopeItemProvider {
  private static final Logger LOG = Logger.getInstance(DefaultChooseByNameItemProvider.class);
  private static final String UNIVERSAL_SEPARATOR = "\u0000";
  private final SmartPsiElementPointer myContext;

  public DefaultChooseByNameItemProvider(@Nullable PsiElement context) {
    myContext = context == null ? null : SmartPointerManager.getInstance(context.getProject()).createSmartPsiElementPointer(context);
  }

  @Override
  public boolean filterElements(@NotNull ChooseByNameBase base,
                                @NotNull String pattern,
                                boolean everywhere,
                                @NotNull ProgressIndicator indicator,
                                @NotNull Processor<Object> consumer) {
    return filterElementsWithWeights(base, createParameters(base, pattern, everywhere), indicator,
                                     res -> consumer.process(res.getItem()));
  }

  @Override
  public boolean filterElements(@NotNull ChooseByNameBase base,
                                @NotNull FindSymbolParameters parameters,
                                @NotNull ProgressIndicator indicator,
                                @NotNull Processor<Object> consumer) {
    return filterElementsWithWeights(base, parameters, indicator, res -> consumer.process(res.getItem()));
  }

  @Override
  public boolean filterElementsWithWeights(@NotNull ChooseByNameBase base,
                                           @NotNull String pattern,
                                           boolean everywhere,
                                           @NotNull ProgressIndicator indicator,
                                           @NotNull Processor<FoundItemDescriptor<?>> consumer) {
    return filterElementsWithWeights(base, createParameters(base, pattern, everywhere), indicator, consumer);
  }

  @Override
  public boolean filterElementsWithWeights(@NotNull ChooseByNameBase base,
                                @NotNull FindSymbolParameters parameters,
                                @NotNull ProgressIndicator indicator,
                                @NotNull Processor<FoundItemDescriptor<?>> consumer) {
    return ProgressManager.getInstance().computePrioritized(
      () -> filterElements(base, indicator, myContext == null ? null : myContext.getElement(),
                           () -> base.getNames(parameters.isSearchInLibraries()), consumer, parameters));
  }

  /**
   * Filters and sorts elements in the given choose by name popup according to the given pattern.
   *
   * @param indicator Progress indicator which can be used to cancel the operation
   * @param context The PSI element currently open in the editor (used for proximity ordering of returned results)
   * @param consumer The consumer to which the results (normally NavigationItem instances) are passed
   * @return true if the operation completed normally, false if it was interrupted
   */
  public static boolean filterElements(@NotNull ChooseByNameViewModel base,
                                       @NotNull String pattern,
                                       boolean everywhere,
                                       @NotNull ProgressIndicator indicator,
                                       @Nullable PsiElement context,
                                       @NotNull Processor<Object> consumer) {
    return filterElements(base, indicator, context, null,
                          res -> consumer.process(res.getItem()),
                          createParameters(base, pattern, everywhere));
  }

  private static boolean filterElements(@NotNull ChooseByNameViewModel base,
                                        @NotNull ProgressIndicator indicator,
                                        @Nullable PsiElement context,
                                        @Nullable Supplier<String[]> allNamesProducer,
                                        @NotNull Processor<FoundItemDescriptor<?>> consumer,
                                        @NotNull FindSymbolParameters parameters) {
    boolean everywhere = parameters.isSearchInLibraries();
    String pattern = parameters.getCompletePattern();
    if (base.getProject() != null) {
      base.getProject().putUserData(ChooseByNamePopup.CURRENT_SEARCH_PATTERN, pattern);
    }

    String namePattern = getNamePattern(base, pattern);
    boolean preferStartMatches = !pattern.startsWith("*");

    List<MatchResult> namesList = getSortedNamesForAllWildcards(base, parameters, indicator, allNamesProducer, namePattern, preferStartMatches);

    indicator.checkCanceled();

    return processByNames(base, everywhere, indicator, context, consumer, preferStartMatches, namesList, parameters);
  }

  @NotNull
  private static List<MatchResult> getSortedNamesForAllWildcards(@NotNull ChooseByNameViewModel base,
                                                                 @NotNull FindSymbolParameters parameters,
                                                                 @NotNull ProgressIndicator indicator,
                                                                 @Nullable Supplier<String[]> allNamesProducer,
                                                                 String namePattern,
                                                                 boolean preferStartMatches) {
    String matchingPattern = convertToMatchingPattern(base, namePattern);
    if (matchingPattern.isEmpty() && !base.canShowListForEmptyPattern()) return Collections.emptyList();

    List<MatchResult> result = getSortedNames(base, parameters, indicator, allNamesProducer, matchingPattern, preferStartMatches);
    if (!namePattern.contains("*")) return result;

    Set<String> allNames = new HashSet<>(ContainerUtil.map(result, mr -> mr.elementName));
    for (int i = 1; i < namePattern.length() - 1; i++) {
      if (namePattern.charAt(i) == '*') {
        List<MatchResult> namesForSuffix = getSortedNames(base, parameters, indicator, allNamesProducer,
                                                          convertToMatchingPattern(base, namePattern.substring(i + 1)),
                                                          preferStartMatches);
        for (MatchResult mr : namesForSuffix) {
          if (allNames.add(mr.elementName)) {
            result.add(mr);
          }
        }
      }
    }
    return result;
  }

  @NotNull
  private static List<MatchResult> getSortedNames(@NotNull ChooseByNameViewModel base,
                                                  @NotNull FindSymbolParameters parameters,
                                                  @NotNull ProgressIndicator indicator,
                                                  @Nullable Supplier<String[]> allNamesProducer,
                                                  String namePattern, boolean preferStartMatches) {
    List<MatchResult> namesList = getAllNames(base, parameters, indicator, allNamesProducer, namePattern);

    indicator.checkCanceled();
    String pattern = parameters.getCompletePattern();

    long started = System.currentTimeMillis();
    Collections.sort(namesList, Comparator.comparing((MatchResult mr) -> !pattern.equalsIgnoreCase(mr.elementName))
                                          .thenComparing((MatchResult mr) -> !namePattern.equalsIgnoreCase(mr.elementName))
                                          .thenComparing((mr1, mr2) -> mr1.compareWith(mr2, preferStartMatches)));
    if (LOG.isDebugEnabled()) {
      LOG.debug("sorted:"+ (System.currentTimeMillis() - started) + ",results:" + namesList.size());
    }
    return namesList;
  }

  @NotNull
  private static List<MatchResult> getAllNames(@NotNull ChooseByNameViewModel base,
                                               @NotNull FindSymbolParameters parameters,
                                               @NotNull ProgressIndicator indicator,
                                               @Nullable Supplier<String[]> allNamesProducer,
                                               String namePattern) {
    List<MatchResult> namesList = new ArrayList<>();

    final CollectConsumer<MatchResult> collect = new SynchronizedCollectConsumer<>(namesList);

    ChooseByNameModel model = base.getModel();
    if (model instanceof ChooseByNameModelEx) {
      indicator.checkCanceled();
      long started = System.currentTimeMillis();
      String fullPattern = parameters.getCompletePattern();
      MinusculeMatcher matcher = buildPatternMatcher(namePattern);
      ((ChooseByNameModelEx)model).processNames(sequence -> {
        indicator.checkCanceled();
        MatchResult result = matches(base, fullPattern, matcher, sequence);
        if (result != null) {
          collect.consume(result);
          return true;
        }
        return false;
      }, parameters);
      if (LOG.isDebugEnabled()) {
        LOG.debug("loaded + matched:"+ (System.currentTimeMillis() - started)+ "," + collect.getResult().size());
      }
    }
    else {
      if (allNamesProducer == null) {
        throw new IllegalArgumentException("Need to specify allNamesProducer when using a model which isn't a ChooseByNameModelEx");
      }
      String[] names = allNamesProducer.get();
      long started = System.currentTimeMillis();
      processNamesByPattern(base, names, namePattern, indicator, collect);
      if (LOG.isDebugEnabled()) {
        LOG.debug("matched:"+ (System.currentTimeMillis() - started)+ "," + names.length);
      }
    }
    return namesList;
  }

  @NotNull
  private static FindSymbolParameters createParameters(@NotNull ChooseByNameViewModel base, @NotNull String pattern, boolean everywhere) {
    ChooseByNameModel model = base.getModel();
    IdFilter idFilter = model instanceof ContributorsBasedGotoByModel ? ((ContributorsBasedGotoByModel)model).getIdFilter(everywhere) : null;
    GlobalSearchScope searchScope = FindSymbolParameters.searchScopeFor(base.getProject(), everywhere);
    return new FindSymbolParameters(pattern, getNamePattern(base, pattern), searchScope, idFilter);
  }

  private static boolean processByNames(@NotNull ChooseByNameViewModel base,
                                        boolean everywhere,
                                        @NotNull ProgressIndicator indicator,
                                        @Nullable PsiElement context,
                                        @NotNull Processor<FoundItemDescriptor<?>> consumer,
                                        boolean preferStartMatches,
                                        List<? extends MatchResult> namesList,
                                        FindSymbolParameters parameters) {
    List<Pair<Object, MatchResult>> sameNameElements = new SmartList<>();

    ChooseByNameModel model = base.getModel();
    Comparator<Pair<Object, MatchResult>> weightComparator = new Comparator<Pair<Object, MatchResult>>() {
      @SuppressWarnings("unchecked") final
      Comparator<Object> modelComparator = model instanceof Comparator ? (Comparator<Object>)model :
                                           new PathProximityComparator(context);

      @Override
      public int compare(Pair<Object, MatchResult> o1, Pair<Object, MatchResult> o2) {
        int result = modelComparator.compare(o1.first, o2.first);
        return result != 0 ? result : o1.second.compareWith(o2.second, preferStartMatches);
      }
    };

    MinusculeMatcher fullMatcher = getFullMatcher(parameters, base);

    for (MatchResult result : namesList) {
      indicator.checkCanceled();
      String name = result.elementName;

      // use interruptible call if possible
      Object[] elements = model instanceof ContributorsBasedGotoByModel
                          ? ((ContributorsBasedGotoByModel)model).getElementsByName(name, parameters, indicator)
                          : model.getElementsByName(name, everywhere, getNamePattern(base, parameters.getCompletePattern()));
      if (elements.length > 1) {
        sameNameElements.clear();
        for (final Object element : elements) {
          indicator.checkCanceled();
          MatchResult qualifiedResult = matchQualifiedName(model, fullMatcher, element);
          if (qualifiedResult != null) {
            sameNameElements.add(Pair.create(element, qualifiedResult));
          }
        }
        Collections.sort(sameNameElements, weightComparator);
        List<FoundItemDescriptor<?>> processedItems =
          ContainerUtil.map(sameNameElements, p -> new FoundItemDescriptor<>(p.first, result.matchingDegree));
        if (!ContainerUtil.process(processedItems, consumer)) return false;
      }
      else if (elements.length == 1) {
        if (matchQualifiedName(model, fullMatcher, elements[0]) != null) {
          if (!consumer.process(new FoundItemDescriptor<>(elements[0], result.matchingDegree))) return false;
        }
      }
    }
    return true;
  }

  @NotNull
  protected PathProximityComparator getPathProximityComparator() {
    return new PathProximityComparator(myContext == null ? null : myContext.getElement());
  }

  @NotNull
  private static MinusculeMatcher getFullMatcher(FindSymbolParameters parameters, ChooseByNameViewModel base) {
    String fullRawPattern = buildFullPattern(base, parameters.getCompletePattern());
    String fullNamePattern = buildFullPattern(base, base.transformPattern(parameters.getCompletePattern()));

    return NameUtil.buildMatcherWithFallback(fullRawPattern, fullNamePattern, NameUtil.MatchingCaseSensitivity.NONE);
  }

  @NotNull
  private static String buildFullPattern(ChooseByNameViewModel base, String pattern) {
    String fullPattern = "*" + removeModelSpecificMarkup(base.getModel(), pattern);
    for (String separator : base.getModel().getSeparators()) {
      fullPattern = StringUtil.replace(fullPattern, separator, "*" + UNIVERSAL_SEPARATOR + "*");
    }
    return fullPattern;
  }

  @NotNull
  private static String getNamePattern(@NotNull ChooseByNameViewModel base, @NotNull String pattern) {
    String transformedPattern = base.transformPattern(pattern);
    return getNamePattern(base.getModel(), transformedPattern);
  }

  private static String getNamePattern(ChooseByNameModel model, String pattern) {
    final String[] separators = model.getSeparators();
    int lastSeparatorOccurrence = 0;
    for (String separator : separators) {
      int idx = pattern.lastIndexOf(separator);
      if (idx == pattern.length() - 1) {  // avoid empty name
        idx = pattern.lastIndexOf(separator, idx - 1);
      }
      lastSeparatorOccurrence = Math.max(lastSeparatorOccurrence, idx == -1 ? idx : idx + separator.length());
    }

    return pattern.substring(lastSeparatorOccurrence);
  }

  @Nullable
  private static MatchResult matchQualifiedName(ChooseByNameModel model, MinusculeMatcher fullMatcher, @NotNull Object element) {
    String fullName = model.getFullName(element);
    if (fullName == null) return null;

    for (String separator : model.getSeparators()) {
      fullName = StringUtil.replace(fullName, separator, UNIVERSAL_SEPARATOR);
    }
    return matchName(fullMatcher, fullName);
  }

  @NotNull
  @Override
  public List<String> filterNames(@NotNull ChooseByNameBase base, @NotNull String[] names, @NotNull String pattern) {
    pattern = convertToMatchingPattern(base, pattern);
    if (pattern.isEmpty() && !base.canShowListForEmptyPattern()) return Collections.emptyList();

    final List<String> filtered = new ArrayList<>();
    processNamesByPattern(base, names, pattern, ProgressIndicatorProvider.getGlobalProgressIndicator(), result -> {
      synchronized (filtered) {
        filtered.add(result.elementName);
      }
    });
    synchronized (filtered) {
      return filtered;
    }
  }

  private static void processNamesByPattern(@NotNull final ChooseByNameViewModel base,
                                            @NotNull final String[] names,
                                            @NotNull final String pattern,
                                            final ProgressIndicator indicator,
                                            @NotNull final Consumer<? super MatchResult> consumer) {
    MinusculeMatcher matcher = buildPatternMatcher(pattern);
    Processor<String> processor = name -> {
      ProgressManager.checkCanceled();
      MatchResult result = matches(base, pattern, matcher, name);
      if (result != null) {
        consumer.consume(result);
      }
      return true;
    };
    if (!JobLauncher.getInstance().invokeConcurrentlyUnderProgress(Arrays.asList(names), indicator, processor)) {
      throw new ProcessCanceledException();
    }
  }

  @NotNull
  private static String convertToMatchingPattern(@NotNull ChooseByNameViewModel base, @NotNull String pattern) {
    return addSearchAnywherePatternDecorationIfNeeded(base, removeModelSpecificMarkup(base.getModel(), pattern));
  }

  @NotNull
  private static String addSearchAnywherePatternDecorationIfNeeded(@NotNull ChooseByNameViewModel base, @NotNull String pattern) {
    String trimmedPattern;
    if (base.isSearchInAnyPlace() && !(trimmedPattern = pattern.trim()).isEmpty() && trimmedPattern.length() > 1) {
      pattern = "*" + pattern;
    }
    return pattern;
  }

  @NotNull
  private static String removeModelSpecificMarkup(@NotNull ChooseByNameModel model, @NotNull String pattern) {
    if (model instanceof ContributorsBasedGotoByModel) {
      pattern = ((ContributorsBasedGotoByModel)model).removeModelSpecificMarkup(pattern);
    }
    return pattern;
  }

  @Nullable
  protected static MatchResult matches(@NotNull ChooseByNameViewModel base,
                                     @NotNull String pattern,
                                     @NotNull MinusculeMatcher matcher,
                                     @Nullable String name) {
    if (name == null) {
      return null;
    }
    if (base.getModel() instanceof CustomMatcherModel) {
      try {
        return ((CustomMatcherModel)base.getModel()).matches(name, pattern) ? new MatchResult(name, 0, true) : null;
      }
      catch (Exception e) {
        LOG.info(e);
        return null; // no matches appears valid result for "bad" pattern
      }
    }
    return matchName(matcher, name);
  }

  @Nullable
  private static MatchResult matchName(@NotNull MinusculeMatcher matcher, @NotNull String name) {
    FList<TextRange> fragments = matcher.matchingFragments(name);
    return fragments != null ? new MatchResult(name, matcher.matchingDegree(name, false, fragments), MinusculeMatcher.isStartMatch(fragments)) : null;
  }

  @NotNull
  private static MinusculeMatcher buildPatternMatcher(@NotNull String pattern) {
    return NameUtil.buildMatcher(pattern, NameUtil.MatchingCaseSensitivity.NONE);
  }

  protected static class PathProximityComparator implements Comparator<Object> {
    @NotNull private final PsiProximityComparator myProximityComparator;

    private PathProximityComparator(@Nullable final PsiElement context) {
      myProximityComparator = new PsiProximityComparator(context);
    }

    private static boolean isCompiledWithoutSource(Object o) {
      return o instanceof PsiCompiledElement && ((PsiCompiledElement)o).getNavigationElement() == o;
    }

    @Override
    public int compare(final Object o1, final Object o2) {
      int rc = myProximityComparator.compare(o1, o2);
      if (rc != 0) return rc;

      int o1Weight = isCompiledWithoutSource(o1) ? 1 : 0;
      int o2Weight = isCompiledWithoutSource(o2) ? 1 : 0;
      return o1Weight - o2Weight;
    }
  }
}
