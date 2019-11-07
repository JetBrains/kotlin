// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions;

import com.intellij.ide.actions.searcheverywhere.FoundItemDescriptor;
import com.intellij.ide.util.gotoByName.*;
import com.intellij.navigation.ChooseByNameContributor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.codeStyle.FixingLayoutMatcher;
import com.intellij.psi.codeStyle.MinusculeMatcher;
import com.intellij.psi.codeStyle.NameUtil;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.FList;
import com.intellij.util.containers.JBIterable;
import com.intellij.util.indexing.FindSymbolParameters;
import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
* @author peter
*/
public class GotoFileItemProvider extends DefaultChooseByNameItemProvider {
  private static final Logger LOG = Logger.getInstance(GotoFileItemProvider.class);

  private static final int EXACT_MATCH_DEGREE = 5000;
  private static final int DIRECTORY_MATCH_DEGREE = 0;

  private final Project myProject;
  private final GotoFileModel myModel;

  public GotoFileItemProvider(@NotNull Project project, @Nullable PsiElement context, GotoFileModel model) {
    super(context);
    myProject = project;
    myModel = model;
  }

  @Override
  public boolean filterElementsWithWeights(@NotNull ChooseByNameBase base,
                                @NotNull FindSymbolParameters parameters,
                                @NotNull ProgressIndicator indicator,
                                @NotNull Processor<FoundItemDescriptor<?>> consumer) {
    return ProgressManager.getInstance().computePrioritized(() -> doFilterElements(base, parameters, indicator, consumer));
  }

  private boolean doFilterElements(@NotNull ChooseByNameBase base,
                                   @NotNull FindSymbolParameters parameters,
                                   @NotNull ProgressIndicator indicator,
                                   @NotNull Processor<FoundItemDescriptor<?>> consumer) {
    long start = System.currentTimeMillis();
    try {
      String pattern = parameters.getCompletePattern();
      PsiFileSystemItem absolute = getFileByAbsolutePath(pattern);
      if (absolute != null && !consumer.process(new FoundItemDescriptor<>(absolute, EXACT_MATCH_DEGREE))) {
        return true;
      }

      if (pattern.startsWith("./") || pattern.startsWith(".\\")) {
        parameters = parameters.withCompletePattern(pattern.substring(1));
      }

      if (!processItemsForPattern(base, parameters, consumer, indicator)) {
        return false;
      }
      String fixedPattern = FixingLayoutMatcher.fixLayout(pattern);
      return fixedPattern == null || processItemsForPattern(base, parameters.withCompletePattern(fixedPattern), consumer, indicator);
    }
    finally {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Goto File \"" + parameters.getCompletePattern() + "\" took " + (System.currentTimeMillis() - start) + " ms");
      }
    }
  }

  private boolean processItemsForPattern(@NotNull ChooseByNameBase base,
                                         @NotNull FindSymbolParameters parameters,
                                         @NotNull Processor<FoundItemDescriptor<?>> consumer,
                                         @NotNull ProgressIndicator indicator) {
    String sanitized = getSanitizedPattern(parameters.getCompletePattern(), myModel);
    int qualifierEnd = sanitized.lastIndexOf('/') + 1;
    NameGrouper grouper = new NameGrouper(sanitized.substring(qualifierEnd), indicator);
    processNames(FindSymbolParameters.simple(myProject, true), grouper::processName);

    Ref<Boolean> hasSuggestions = Ref.create(false);
    DirectoryPathMatcher dirMatcher = DirectoryPathMatcher.root(myModel, sanitized.substring(0, qualifierEnd));
    while (dirMatcher != null) {
      int index = grouper.index;
      SuffixMatches group = grouper.nextGroup(base);
      if (group == null) break;
      if (!group.processFiles(parameters.withLocalPattern(dirMatcher.dirPattern), consumer, hasSuggestions, dirMatcher)) {
        return false;
      }
      dirMatcher = dirMatcher.appendChar(grouper.namePattern.charAt(index));
      if (!myModel.isSlashlessMatchingEnabled()) {
        return true;
      }
    }
    return true;
  }

  /**
   * Invoke contributors directly, as multi-threading isn't of much value in Goto File,
   * and filling {@link ContributorsBasedGotoByModel#myContributorToItsSymbolsMap} is expensive for the default contributor.
   */
  private void processNames(@NotNull FindSymbolParameters parameters,
                            @NotNull Processor<? super String> nameProcessor) {
    List<ChooseByNameContributor> contributors = DumbService.getDumbAwareExtensions(myProject, ChooseByNameContributor.FILE_EP_NAME);
    for (ChooseByNameContributor contributor : contributors) {
      if (contributor instanceof DefaultFileNavigationContributor) {
        FilenameIndex.processAllFileNames(nameProcessor,
                                          parameters.getSearchScope(), // todo why it was true?
                                          parameters.getIdFilter());
      }
      else {
        myModel.processContributorNames(contributor, parameters, nameProcessor);
      }
    }
  }

  @NotNull
  public static String getSanitizedPattern(@NotNull String pattern, GotoFileModel model) {
    return removeSlashes(StringUtil.replace(ChooseByNamePopup.getTransformedPattern(pattern, model), "\\", "/"));
  }

  @NotNull
  public static MinusculeMatcher getQualifiedNameMatcher(@NotNull String pattern) {
    return NameUtil.buildMatcher("*" + StringUtil.replace(StringUtil.replace(pattern, "\\", "*\\*"), "/", "*/*"), NameUtil.MatchingCaseSensitivity.NONE);
  }

  @NotNull
  private static String removeSlashes(String s) {
    return StringUtil.trimLeading(StringUtil.trimTrailing(s, '/'), '/');
  }

  @Nullable
  private PsiFileSystemItem getFileByAbsolutePath(@NotNull String pattern) {
    if (pattern.contains("/") || pattern.contains("\\")) {
      String path = FileUtil.toSystemIndependentName(ChooseByNamePopup.getTransformedPattern(pattern, myModel));
      VirtualFile vFile = LocalFileSystem.getInstance().findFileByPathIfCached(path);
      if (vFile != null) {
        ProjectFileIndex index = ProjectFileIndex.SERVICE.getInstance(myProject);
        if (index.isInContent(vFile) || index.isInLibrary(vFile)) {
          return PsiUtilCore.findFileSystemItem(myProject, vFile);
        }
      }
    }
    return null;
  }

  private Iterable<FoundItemDescriptor<PsiFileSystemItem>> matchQualifiers(MinusculeMatcher qualifierMatcher, Iterable<? extends PsiFileSystemItem> iterable) {
    List<FoundItemDescriptor<PsiFileSystemItem>> matching = new ArrayList<>();
    for (PsiFileSystemItem item : iterable) {
      ProgressManager.checkCanceled();
      String qualifier = Objects.requireNonNull(getParentPath(item));
      FList<TextRange> fragments = qualifierMatcher.matchingFragments(qualifier);
      if (fragments != null) {
        int gapPenalty = fragments.isEmpty() ? 0 : qualifier.length() - fragments.get(fragments.size() - 1).getEndOffset();
        int degree = qualifierMatcher.matchingDegree(qualifier, false, fragments) - gapPenalty;
        matching.add(new FoundItemDescriptor<>(item, degree));
      }
    }
    if (matching.size() > 1) {
      Comparator<FoundItemDescriptor<PsiFileSystemItem>> comparator =
        Comparator.comparing((FoundItemDescriptor<PsiFileSystemItem> res) -> res.getWeight()).reversed();
      Collections.sort(matching, comparator);
    }
    return matching;
  }

  @Nullable
  private String getParentPath(@NotNull PsiFileSystemItem item) {
    String fullName = myModel.getFullName(item);
    return fullName == null ? null : StringUtil.getPackageName(FileUtilRt.toSystemIndependentName(fullName), '/') + '/';
  }

  private static JBIterable<FoundItemDescriptor<PsiFileSystemItem>> moveDirectoriesToEnd(Iterable<FoundItemDescriptor<PsiFileSystemItem>> iterable) {
    List<FoundItemDescriptor<PsiFileSystemItem>> dirs = new ArrayList<>();
    return JBIterable.from(iterable).filter(res -> {
      if (res.getItem() instanceof PsiDirectory) {
        dirs.add(new FoundItemDescriptor<>(res.getItem(), DIRECTORY_MATCH_DEGREE));
        return false;
      }
      return true;
    }).append(dirs);
  }

  // returns a lazy iterable, where the next element is calculated only when it's needed
  @NotNull
  private JBIterable<FoundItemDescriptor<PsiFileSystemItem>> getFilesMatchingPath(@NotNull FindSymbolParameters parameters,
                                                             @NotNull List<MatchResult> fileNames,
                                                             @NotNull DirectoryPathMatcher dirMatcher,
                                                             @NotNull ProgressIndicator indicator) {
    GlobalSearchScope scope = dirMatcher.narrowDown(parameters.getSearchScope());
    FindSymbolParameters adjusted = parameters.withScope(scope);

    List<List<MatchResult>> sortedNames = sortAndGroup(fileNames, Comparator.comparing(mr -> StringUtil.toLowerCase(FileUtilRt.getNameWithoutExtension(mr.elementName))));
    return JBIterable.from(sortedNames).flatMap(nameGroup -> getItemsForNames(indicator, adjusted, nameGroup));
  }

  private Iterable<FoundItemDescriptor<PsiFileSystemItem>> getItemsForNames(@NotNull ProgressIndicator indicator,
                                                       FindSymbolParameters parameters, List<MatchResult> matchResults) {
    List<PsiFileSystemItem> group = new ArrayList<>();
    Map<PsiFileSystemItem, Integer> nesting = new HashMap<>();
    Map<PsiFileSystemItem, Integer> matchDegrees = new HashMap<>();
    for (MatchResult matchResult : matchResults) {
      ProgressManager.checkCanceled();
      for (Object o : myModel.getElementsByName(matchResult.elementName, parameters, indicator)) {
        ProgressManager.checkCanceled();
        if (o instanceof PsiFileSystemItem) {
          PsiFileSystemItem psiItem = (PsiFileSystemItem)o;
          String qualifier = getParentPath(psiItem);
          if (qualifier != null) {
            group.add(psiItem);
            nesting.put(psiItem, StringUtil.countChars(qualifier, '/'));
            matchDegrees.put(psiItem, matchResult.matchingDegree);
          }
        }
      }
    }

    if (group.size() > 1) {
      Collections.sort(group,
                       Comparator.<PsiFileSystemItem, Integer>comparing(nesting::get).
                         thenComparing(getPathProximityComparator()).
                         thenComparing(myModel::getFullName));
    }
    return ContainerUtil.map(group, item -> new FoundItemDescriptor<>(item, matchDegrees.get(item)));
  }

  /**
   * @return Minimal {@code pos} such that {@code candidateName} can potentially match {@code namePattern.substring(pos)}
   * (i.e. contains the same letters as a sub-sequence).
   * Matching attempts with longer pattern substrings certainly will fail.
   */
  private static int findMatchStartingPosition(String candidateName, String namePattern) {
    int namePos = candidateName.length();
    for (int i = namePattern.length(); i > 0; i--) {
      char c = namePattern.charAt(i - 1);
      if (Character.isLetterOrDigit(c)) {
        namePos = StringUtil.lastIndexOfIgnoreCase(candidateName, c, namePos - 1);
        if (namePos < 0) {
          return i;
        }
      }
    }
    return 0;
  }

  private class NameGrouper {
    private final String namePattern;
    @NotNull private final ProgressIndicator indicator;
    
    /** Names placed into buckets where the index of bucket == {@link #findMatchStartingPosition} */
    private final List<List<String>> candidateNames;

    private int index;

    NameGrouper(@NotNull String namePattern, @NotNull ProgressIndicator indicator) {
      this.namePattern = namePattern;
      candidateNames = IntStreamEx.range(0, namePattern.length()).mapToObj(__ -> (List<String>)new ArrayList<String>()).toList();
      this.indicator = indicator;
    }

    boolean processName(String name) {
      indicator.checkCanceled();
      int position = findMatchStartingPosition(name, namePattern);
      if (position < namePattern.length()) {
        candidateNames.get(position).add(name);
      }
      return true;
    }

    @Nullable
    SuffixMatches nextGroup(ChooseByNameBase base) {
      if (index >= namePattern.length()) return null;
      
      SuffixMatches matches = new SuffixMatches(namePattern, index, indicator);
      for (String name : candidateNames.get(index)) {
        if (!matches.matchName(base, name) && index + 1 < namePattern.length()) {
          candidateNames.get(index + 1).add(name); // try later with a shorter matcher
        }
      }
      candidateNames.set(index, null);
      index++;
      return matches;
    }
  }

  private class SuffixMatches {
    final String patternSuffix;
    final MinusculeMatcher matcher;
    final List<MatchResult> matchingNames = new ArrayList<>();
    final ProgressIndicator indicator;

    SuffixMatches(String pattern, int from, @NotNull ProgressIndicator indicator) {
      patternSuffix = pattern.substring(from);
      matcher = NameUtil.buildMatcher((from > 0 ? " " : "*") + patternSuffix, NameUtil.MatchingCaseSensitivity.NONE);
      this.indicator = indicator;
    }

    @Override
    public String toString() {
      return "SuffixMatches{" +
             "patternSuffix='" + patternSuffix + '\'' +
             ", matchingNames=" + matchingNames +
             '}';
    }

    boolean matchName(@NotNull ChooseByNameBase base, String name) {
      MatchResult result = matches(base, patternSuffix, matcher, name);
      if (result != null) {
        matchingNames.add(result);
        return true;
      }
      return false;
    }

    boolean processFiles(@NotNull FindSymbolParameters parameters,
                         @NotNull Processor<FoundItemDescriptor<?>> processor,
                         @NotNull Ref<Boolean> hasSuggestions,
                         @NotNull DirectoryPathMatcher dirMatcher) {
      MinusculeMatcher qualifierMatcher = getQualifiedNameMatcher(parameters.getLocalPatternName());

      List<MatchResult> matchingNames = this.matchingNames;
      if (patternSuffix.length() <= 3 && !dirMatcher.dirPattern.isEmpty()) {
        // just enumerate over files
        // otherwise there are too many names matching the remaining few letters, and querying index for all of them with a very constrained scope is expensive
        Set<String> existingNames = dirMatcher.findFileNamesMatchingIfCheap(patternSuffix.charAt(0), matcher);
        if (existingNames != null) {
          matchingNames = ContainerUtil.filter(matchingNames, mr -> existingNames.contains(mr.elementName));
        }
      }

      List<List<MatchResult>> groups = groupByMatchingDegree(!parameters.getCompletePattern().startsWith("*"), matchingNames);
      for (List<MatchResult> group : groups) {
        JBIterable<FoundItemDescriptor<PsiFileSystemItem>> filesMatchingPath = getFilesMatchingPath(parameters, group, dirMatcher, indicator);
        Iterable<FoundItemDescriptor<PsiFileSystemItem>> matchedFiles =
          parameters.getLocalPatternName().isEmpty()
          ? filesMatchingPath
          : matchQualifiers(qualifierMatcher, filesMatchingPath.map(res -> res.getItem()));

        matchedFiles = moveDirectoriesToEnd(matchedFiles);
        Processor<FoundItemDescriptor<PsiFileSystemItem>> trackingProcessor = res -> {
          hasSuggestions.set(true);
          return processor.process(res);
        };
        if (!ContainerUtil.process(matchedFiles, trackingProcessor)) {
          return false;
        }
      }

      // let the framework switch to searching outside project to display these well-matching suggestions
      // instead of worse-matching ones in project (that are very expensive to calculate)
      return hasSuggestions.get() ||
             parameters.isSearchInLibraries() ||
             !hasSuggestionsOutsideProject(parameters.getCompletePattern(), groups, dirMatcher);
    }

    private boolean hasSuggestionsOutsideProject(@NotNull String pattern,
                                                 @NotNull List<? extends List<MatchResult>> groups,
                                                 @NotNull DirectoryPathMatcher dirMatcher) {
      return ContainerUtil.exists(groups, group ->
        !getFilesMatchingPath(FindSymbolParameters.wrap(pattern, myProject, true),
                              group, dirMatcher, indicator).isEmpty());
    }

    private List<List<MatchResult>> groupByMatchingDegree(boolean preferStartMatches, List<MatchResult> matchingNames) {
      Comparator<MatchResult> comparator = (mr1, mr2) -> {
        boolean exactPrefix1 = StringUtil.startsWith(mr1.elementName, patternSuffix);
        boolean exactPrefix2 = StringUtil.startsWith(mr2.elementName, patternSuffix);
        if (exactPrefix1 && exactPrefix2) return 0;
        if (exactPrefix1 != exactPrefix2) return exactPrefix1 ? -1 : 1;
        return mr1.compareDegrees(mr2, preferStartMatches);
      };

      return sortAndGroup(matchingNames, comparator);
    }

  }

  private static <T> List<List<T>> sortAndGroup(@NotNull List<T> items, @NotNull Comparator<? super T> comparator) {
    return StreamEx.of(items).sorted(comparator).groupRuns((n1, n2) -> comparator.compare(n1, n2) == 0).toList();
  }
}
