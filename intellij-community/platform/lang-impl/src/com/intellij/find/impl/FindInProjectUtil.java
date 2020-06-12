// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.find.impl;

import com.intellij.find.*;
import com.intellij.find.findInProject.FindInProjectManager;
import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.fileTypes.UnknownFileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.util.ProgressWrapper;
import com.intellij.openapi.progress.util.TooManyUsagesStatus;
import com.intellij.openapi.project.DumbServiceImpl;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Conditions;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.*;
import com.intellij.openapi.vfs.impl.VirtualFileManagerImpl;
import com.intellij.psi.*;
import com.intellij.psi.search.*;
import com.intellij.ui.content.Content;
import com.intellij.usageView.UsageInfo;
import com.intellij.usageView.UsageViewContentManager;
import com.intellij.usages.ConfigurableUsageTarget;
import com.intellij.usages.FindUsagesProcessPresentation;
import com.intellij.usages.UsageView;
import com.intellij.usages.UsageViewPresentation;
import com.intellij.util.PatternUtil;
import com.intellij.util.Processor;
import gnu.trove.THashSet;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.PropertyKey;

import javax.swing.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

public final class FindInProjectUtil {
  private static final int USAGES_PER_READ_ACTION = 100;

  private FindInProjectUtil() {}

  public static void setDirectoryName(@NotNull FindModel model, @NotNull DataContext dataContext) {
    PsiElement psiElement = null;
    Project project = CommonDataKeys.PROJECT.getData(dataContext);

    Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
    if (project != null && editor == null && !DumbServiceImpl.getInstance(project).isDumb()) {
      try {
        psiElement = CommonDataKeys.PSI_ELEMENT.getData(dataContext);
      }
      catch (IndexNotReadyException ignore) {}
    }

    String directoryName = null;

    if (psiElement instanceof PsiDirectory) {
      directoryName = ((PsiDirectory)psiElement).getVirtualFile().getPresentableUrl();
    }

    if (directoryName == null && psiElement instanceof PsiDirectoryContainer) {
      final PsiDirectory[] directories = ((PsiDirectoryContainer)psiElement).getDirectories();
      directoryName = directories.length == 1 ? directories[0].getVirtualFile().getPresentableUrl():null;
    }

    if (directoryName == null) {
      VirtualFile virtualFile = CommonDataKeys.VIRTUAL_FILE.getData(dataContext);
      if (virtualFile != null && virtualFile.isDirectory()) directoryName = virtualFile.getPresentableUrl();
    }

    Module module = LangDataKeys.MODULE_CONTEXT.getData(dataContext);
    if (module != null) {
      model.setModuleName(module.getName());
      model.setDirectoryName(null);
      model.setCustomScope(false);
    }

    if (model.getModuleName() == null || editor == null) {
      if (directoryName != null) {
        model.setDirectoryName(directoryName);
        model.setCustomScope(false); // to select "Directory: " radio button
      }
    }

    if (directoryName == null) {
      for (FindInProjectExtension extension : FindInProjectExtension.EP_NAME.getExtensionList()) {
        boolean success = extension.initModelFromContext(model, dataContext);
        if (success) break;
      }
    }

    // set project scope if we have no other settings
    model.setProjectScope(model.getDirectoryName() == null && model.getModuleName() == null && !model.isCustomScope());
  }

  /** @deprecated use {@link #getDirectory(FindModel)} */
  @Deprecated
  @Nullable
  @ApiStatus.ScheduledForRemoval(inVersion = "2021.1")
  public static PsiDirectory getPsiDirectory(@NotNull FindModel findModel, @NotNull Project project) {
    VirtualFile directory = getDirectory(findModel);
    return directory == null ? null : PsiManager.getInstance(project).findDirectory(directory);
  }

  @Nullable
  public static VirtualFile getDirectory(@NotNull FindModel findModel) {
    String directoryName = findModel.getDirectoryName();
    if (findModel.isProjectScope() || StringUtil.isEmptyOrSpaces(directoryName)) {
      return null;
    }

    String path = FileUtil.toSystemIndependentName(directoryName);
    VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByPath(path);
    if (virtualFile == null || !virtualFile.isDirectory()) {
      virtualFile = null;
      // path doesn't contain file system prefix so try to find it inside archives (IDEA-216479)
      List<VirtualFileSystem> fileSystems = ((VirtualFileManagerImpl)VirtualFileManager.getInstance()).getPhysicalFileSystems();

      for (VirtualFileSystem fs : fileSystems) {
        if (!(fs instanceof LocalFileProvider)) continue;
        VirtualFile file = fs.findFileByPath(path);
        if (file != null && file.isDirectory()) {
          if (file.getChildren().length > 0) {
            virtualFile = file;
            break;
          }
          if (virtualFile == null) {
            virtualFile = file;
          }
        }
      }
      if (virtualFile == null && !path.contains(JarFileSystem.JAR_SEPARATOR)) {
        virtualFile = JarFileSystem.getInstance().findFileByPath(path + JarFileSystem.JAR_SEPARATOR);
      }
    }
    return virtualFile;
  }

  /* filter can have form "*.js, !*_min.js", latter means except matched by *_min.js */
  @NotNull
  public static Condition<CharSequence> createFileMaskCondition(@Nullable String filter) throws PatternSyntaxException {
    if (StringUtil.isEmpty(filter)) {
      return Conditions.alwaysTrue();
    }

    String pattern = "";
    String negativePattern = "";
    final List<String> masks = StringUtil.split(filter, ",");

    for(String mask:masks) {
      mask = mask.trim();
      if (StringUtil.startsWith(mask, "!")) {
        negativePattern += (negativePattern.isEmpty() ? "" : "|") + "(" + PatternUtil.convertToRegex(mask.substring(1)) + ")";
      }
      else {
        pattern += (pattern.isEmpty() ? "" : "|") + "(" + PatternUtil.convertToRegex(mask) + ")";
      }
    }

    if (pattern.isEmpty()) pattern = PatternUtil.convertToRegex("*");
    final String finalPattern = pattern;
    final String finalNegativePattern = negativePattern;

    return new Condition<CharSequence>() {
      final Pattern regExp = Pattern.compile(finalPattern, Pattern.CASE_INSENSITIVE);
      final Pattern negativeRegExp = StringUtil.isEmpty(finalNegativePattern) ? null : Pattern.compile(finalNegativePattern, Pattern.CASE_INSENSITIVE);
      @Override
      public boolean value(CharSequence input) {
        return regExp.matcher(input).matches() && (negativeRegExp == null || !negativeRegExp.matcher(input).matches());
      }
    };
  }

  /**
   * @deprecated Use {@link #findUsages(FindModel, Project, Processor, FindUsagesProcessPresentation)} instead. To remove in IDEA 16
   */
  @Deprecated
  @ApiStatus.ScheduledForRemoval(inVersion = "2020.2")
  public static void findUsages(@NotNull FindModel findModel,
                                @Nullable final PsiDirectory psiDirectory,
                                @NotNull final Project project,
                                @NotNull final Processor<? super UsageInfo> consumer,
                                @NotNull FindUsagesProcessPresentation processPresentation) {
    findUsages(findModel, project, consumer, processPresentation);
  }

  public static void findUsages(@NotNull FindModel findModel,
                                @NotNull final Project project,
                                @NotNull final Processor<? super UsageInfo> consumer,
                                @NotNull FindUsagesProcessPresentation processPresentation) {
    findUsages(findModel, project, processPresentation, Collections.emptySet(), consumer);
  }

  public static void findUsages(@NotNull final FindModel findModel,
                                @NotNull final Project project,
                                @NotNull final FindUsagesProcessPresentation processPresentation,
                                @NotNull final Set<? extends VirtualFile> filesToStart,
                                @NotNull final Processor<? super UsageInfo> consumer) {
    Runnable runnable = () -> new FindInProjectTask(findModel, project, filesToStart).findUsages(processPresentation, consumer);
    if (ProgressManager.getGlobalProgressIndicator() == null) {
      ProgressManager.getInstance().runProcess(runnable, new EmptyProgressIndicator());
    }
    else {
      runnable.run();
    }
  }

  static boolean processUsagesInFile(@NotNull final PsiFile psiFile,
                                     @NotNull final VirtualFile virtualFile,
                                     @NotNull final FindModel findModel,
                                     @NotNull final Processor<? super UsageInfo> consumer) {
    if (findModel.getStringToFind().isEmpty()) {
      return ReadAction.compute(() -> consumer.process(new UsageInfo(psiFile)));
    }
    if (virtualFile.getFileType().isBinary()) return true; // do not decompile .class files
    final Document document = ReadAction.compute(() -> virtualFile.isValid() ? FileDocumentManager.getInstance().getDocument(virtualFile) : null);
    if (document == null) return true;
    final int[] offsetRef = {0};
    ProgressIndicator current = ProgressManager.getInstance().getProgressIndicator();
    if (current == null) throw new IllegalStateException("must find usages under progress");
    ProgressIndicator indicator = ProgressWrapper.unwrapAll(current);
    TooManyUsagesStatus tooManyUsagesStatus = TooManyUsagesStatus.getFrom(indicator);
    int before;
    do {
      tooManyUsagesStatus.pauseProcessingIfTooManyUsages(); // wait for user out of read action
      before = offsetRef[0];
      boolean success = ReadAction.compute(() ->
                                             !psiFile.isValid() ||
                                             processSomeOccurrencesInFile(document, findModel, psiFile, offsetRef, consumer));
      if (!success) {
        return false;
      }
    }
    while (offsetRef[0] != before);
    return true;
  }

  private static boolean processSomeOccurrencesInFile(@NotNull Document document,
                                                      @NotNull FindModel findModel,
                                                      @NotNull final PsiFile psiFile,
                                                      int @NotNull [] offsetRef,
                                                      @NotNull Processor<? super UsageInfo> consumer) {
    CharSequence text = document.getCharsSequence();
    int textLength = document.getTextLength();
    int offset = offsetRef[0];

    Project project = psiFile.getProject();

    FindManager findManager = FindManager.getInstance(project);
    int count = 0;
    while (offset < textLength) {
      FindResult result = findManager.findString(text, offset, findModel, psiFile.getVirtualFile());
      if (!result.isStringFound()) break;

      final int prevOffset = offset;
      offset = result.getEndOffset();
      if (prevOffset == offset || offset == result.getStartOffset()) {
        // for regular expr the size of the match could be zero -> could be infinite loop in finding usages!
        ++offset;
      }

      final SearchScope customScope = findModel.getCustomScope();
      if (customScope instanceof LocalSearchScope) {
        final TextRange range = new TextRange(result.getStartOffset(), result.getEndOffset());
        if (!((LocalSearchScope)customScope).containsRange(psiFile, range)) continue;
      }
      UsageInfo info = new FindResultUsageInfo(findManager, psiFile, prevOffset, findModel, result);
      if (!consumer.process(info)) {
        return false;
      }
      count++;

      if (count >= USAGES_PER_READ_ACTION) {
        break;
      }
    }
    offsetRef[0] = offset;
    return true;
  }

  @NotNull
  private static String getTitleForScope(@NotNull final FindModel findModel) {
    String scopeName;
    if (findModel.isProjectScope()) {
      scopeName = FindBundle.message("find.scope.project.title");
    }
    else if (findModel.getModuleName() != null) {
      scopeName = FindBundle.message("find.scope.module.title", findModel.getModuleName());
    }
    else if(findModel.getCustomScopeName() != null) {
      scopeName = findModel.getCustomScopeName();
    }
    else {
      scopeName = FindBundle.message("find.scope.directory.title", findModel.getDirectoryName());
    }

    String result = scopeName;
    if (findModel.getFileFilter() != null) {
      result += " "+FindBundle.message("find.scope.files.with.mask", findModel.getFileFilter());
    }

    return result;
  }

  @NotNull
  public static UsageViewPresentation setupViewPresentation(@NotNull FindModel findModel) {
    return setupViewPresentation(FindSettings.getInstance().isShowResultsInSeparateView(), findModel);
  }

  @NotNull
  public static UsageViewPresentation setupViewPresentation(final boolean toOpenInNewTab, @NotNull FindModel findModel) {
    final UsageViewPresentation presentation = new UsageViewPresentation();
    setupViewPresentation(presentation, toOpenInNewTab, findModel);
    return presentation;
  }

  public static void setupViewPresentation(UsageViewPresentation presentation, @NotNull FindModel findModel) {
    setupViewPresentation(presentation, FindSettings.getInstance().isShowResultsInSeparateView(), findModel);
  }

  public static void setupViewPresentation(UsageViewPresentation presentation, boolean toOpenInNewTab, @NotNull FindModel findModel) {
    String scope = getTitleForScope(findModel);
    final String stringToFind = findModel.getStringToFind();
    final String stringToReplace = findModel.getStringToReplace();
    presentation.setScopeText(scope);
    if (stringToFind.isEmpty()) {
      if (!scope.isEmpty()) {
        scope = Character.toLowerCase(scope.charAt(0)) + scope.substring(1);
      }
      presentation.setTabText("Files");
      presentation.setToolwindowTitle("Files in " + scope);
      presentation.setUsagesString("files");
    }
    else {
      FindModel.SearchContext searchContext = findModel.getSearchContext();
      String contextText = "";
      if (searchContext != FindModel.SearchContext.ANY) {
        contextText = FindBundle.message("find.context.presentation.scope.label", FindInProjectUtil.getPresentableName(searchContext));
      }
      if (!findModel.isReplaceState()) {
        presentation.setTabText(FindBundle.message("find.usage.view.tab.text", stringToFind, contextText));
        presentation.setToolwindowTitle(FindBundle.message("find.usage.view.toolwindow.title", stringToFind, scope, contextText));
      } else {
        presentation.setTabText(FindBundle.message("replace.usage.view.tab.text", stringToFind, stringToReplace, contextText));
        presentation.setToolwindowTitle(FindBundle.message("replace.usage.view.toolwindow.title", stringToFind, stringToReplace, scope, contextText));
      }
      presentation.setSearchString(FindBundle.message("find.occurrences.search.string", stringToFind, searchContext.ordinal()));
      presentation.setUsagesWord(FindBundle.message("occurrence"));
      presentation.setCodeUsagesString(FindBundle.message("found.occurrences"));
    }
    presentation.setOpenInNewTab(toOpenInNewTab);
    presentation.setCodeUsages(false);
    presentation.setUsageTypeFilteringAvailable(true);
    if (findModel.isReplaceState() && findModel.isRegularExpressions()) {
      presentation.setSearchPattern(findModel.compileRegExp());
      try {
        presentation.setReplacePattern(Pattern.compile(findModel.getStringToReplace()));
      }
      catch (Exception e) {
        presentation.setReplacePattern(null);
      }
    } else {
      presentation.setSearchPattern(null);
      presentation.setReplacePattern(null);
    }
    presentation.setReplaceMode(findModel.isReplaceState());
  }

  @NotNull
  public static FindUsagesProcessPresentation setupProcessPresentation(@NotNull final Project project,

                                                                       @NotNull final UsageViewPresentation presentation) {
    return setupProcessPresentation(project, !FindSettings.getInstance().isSkipResultsWithOneUsage(), presentation);
  }

  @NotNull
  public static FindUsagesProcessPresentation setupProcessPresentation(@NotNull final Project project,
                                                                       final boolean showPanelIfOnlyOneUsage,
                                                                       @NotNull final UsageViewPresentation presentation) {
    FindUsagesProcessPresentation processPresentation = new FindUsagesProcessPresentation(presentation);
    processPresentation.setShowNotFoundMessage(true);
    processPresentation.setShowPanelIfOnlyOneUsage(showPanelIfOnlyOneUsage);
    return processPresentation;
  }

  private static List<PsiElement> getTopLevelRegExpChars(String regExpText, Project project) {
    String regexFileName = "A.regexp";
    FileType regexFileType = FileTypeRegistry.getInstance().getFileTypeByFileName(regexFileName);
    if (regexFileType == UnknownFileType.INSTANCE) return Collections.emptyList();

    PsiFile file = PsiFileFactory.getInstance(project).createFileFromText(regexFileName, regexFileType, regExpText);
    List<PsiElement> result = null;
    final PsiElement[] children = file.getChildren();

    for (PsiElement child:children) {
      PsiElement[] grandChildren = child.getChildren();
      if (grandChildren.length != 1) return Collections.emptyList(); // a | b, more than one branch, can not predict in current way

      for(PsiElement grandGrandChild:grandChildren[0].getChildren()) {
        if (result == null) result = new ArrayList<>();
        result.add(grandGrandChild);
      }
    }
    return result != null ? result : Collections.emptyList();
  }

  @NotNull
  public static String extractStringToFind(@NotNull String regexp, @NotNull Project project) {
    return ReadAction.compute(() -> {
      final List<PsiElement> topLevelRegExpChars = getTopLevelRegExpChars("a", project);
      if (topLevelRegExpChars.size() != 1) return " ";

      // leave only top level regExpChars

      final Class regExpCharPsiClass = topLevelRegExpChars.get(0).getClass();
      return getTopLevelRegExpChars(regexp, project)
        .stream()
        .map(psi -> {
          if (regExpCharPsiClass.isInstance(psi)) {
            String text = psi.getText();
            if (!text.startsWith("\\")) return text;
          }
          return " ";
        })
        .collect(Collectors.joining());
    });
  }

  @NotNull
  public static String buildStringToFindForIndicesFromRegExp(@NotNull String stringToFind, @NotNull Project project) {
    if (!Registry.is("idea.regexp.search.uses.indices")) return "";

    return StringUtil.trim(StringUtil.join(extractStringToFind(stringToFind, project), " "));
  }

  public static void initStringToFindFromDataContext(FindModel findModel, @NotNull DataContext dataContext) {
    Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
    FindUtil.initStringToFindWithSelection(findModel, editor);
    if (editor == null || !editor.getSelectionModel().hasSelection()) {
      FindUtil.useFindStringFromFindInFileModel(findModel, CommonDataKeys.EDITOR_EVEN_IF_INACTIVE.getData(dataContext));
    }
  }

  public static class StringUsageTarget implements ConfigurableUsageTarget, ItemPresentation, TypeSafeDataProvider {
    @NotNull protected final Project myProject;
    @NotNull protected final FindModel myFindModel;

    public StringUsageTarget(@NotNull Project project, @NotNull FindModel findModel) {
      myProject = project;
      myFindModel = findModel.clone();
    }

    @Override
    @NotNull
    public String getPresentableText() {
      UsageViewPresentation presentation = setupViewPresentation(false, myFindModel);
      return presentation.getToolwindowTitle();
    }

    @NotNull
    @Override
    public String getLongDescriptiveName() {
      return getPresentableText();
    }

    @Override
    public String getLocationString() {
      return myFindModel + "!!";
    }

    @Override
    public Icon getIcon(boolean open) {
      return AllIcons.Actions.Find;
    }

    @Override
    public void findUsages() {
      FindInProjectManager.getInstance(myProject).startFindInProject(myFindModel);
    }

    @Override
    public boolean isValid() {
      return true;
    }

    @Override
    public String getName() {
      return myFindModel.getStringToFind().isEmpty() ? myFindModel.getFileFilter() : myFindModel.getStringToFind();
    }

    @Override
    public ItemPresentation getPresentation() {
      return this;
    }

    @Override
    public void navigate(boolean requestFocus) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean canNavigate() {
      return false;
    }

    @Override
    public boolean canNavigateToSource() {
      return false;
    }

    @Override
    public void showSettings() {
      Content selectedContent = UsageViewContentManager.getInstance(myProject).getSelectedContent(true);
      JComponent component = selectedContent == null ? null : selectedContent.getComponent();
      FindInProjectManager findInProjectManager = FindInProjectManager.getInstance(myProject);
      findInProjectManager.findInProject(DataManager.getInstance().getDataContext(component), myFindModel);
    }

    @Override
    public KeyboardShortcut getShortcut() {
      return ActionManager.getInstance().getKeyboardShortcut("FindInPath");
    }

    @Override
    public void calcData(@NotNull DataKey key, @NotNull DataSink sink) {
      if (UsageView.USAGE_SCOPE.equals(key)) {
        SearchScope scope = getScopeFromModel(myProject, myFindModel);
        sink.put(UsageView.USAGE_SCOPE, scope);
      }
    }
  }

  private static void addSourceDirectoriesFromLibraries(@NotNull Project project,
                                                        @NotNull VirtualFile directory,
                                                        @NotNull Collection<? super VirtualFile> outSourceRoots) {
    ProjectFileIndex index = ProjectFileIndex.SERVICE.getInstance(project);
    // if we already are in the sources, search just in this directory only
    if (!index.isInLibraryClasses(directory)) return;
    VirtualFile classRoot = index.getClassRootForFile(directory);
    if (classRoot == null) return;
    String relativePath = VfsUtilCore.getRelativePath(directory, classRoot);
    if (relativePath == null) return;

    Collection<VirtualFile> otherSourceRoots = new THashSet<>();

    // if we are in the library sources, return (to search in this directory only)
    // otherwise, if we outside sources or in a jar directory, add directories from other source roots
    searchForOtherSourceDirs:
    for (OrderEntry entry : index.getOrderEntriesForFile(directory)) {
      if (entry instanceof LibraryOrderEntry) {
        Library library = ((LibraryOrderEntry)entry).getLibrary();
        if (library == null) continue;
        // note: getUrls() returns jar directories too
        String[] sourceUrls = library.getUrls(OrderRootType.SOURCES);
        for (String sourceUrl : sourceUrls) {
          if (VfsUtilCore.isEqualOrAncestor(sourceUrl, directory.getUrl())) {
            // already in this library sources, no need to look for another source root
            otherSourceRoots.clear();
            break searchForOtherSourceDirs;
          }
          // otherwise we may be inside the jar file in a library which is configured as a jar directory
          // in which case we have no way to know whether this is a source jar or classes jar - so try to locate the source jar
        }
      }
      for (VirtualFile sourceRoot : entry.getFiles(OrderRootType.SOURCES)) {
        VirtualFile sourceFile = sourceRoot.findFileByRelativePath(relativePath);
        if (sourceFile != null) {
          otherSourceRoots.add(sourceFile);
        }
      }
    }
    outSourceRoots.addAll(otherSourceRoots);
  }

  @NotNull
  static SearchScope getScopeFromModel(@NotNull Project project, @NotNull FindModel findModel) {
    SearchScope customScope = findModel.isCustomScope() ? findModel.getCustomScope() : null;
    VirtualFile directory = getDirectory(findModel);
    Module module = findModel.getModuleName() == null ? null : ModuleManager.getInstance(project).findModuleByName(findModel.getModuleName());
    // do not alter custom scope in any way, learn from history
    return customScope != null ? customScope :
           // we don't have to check for myProjectFileIndex.isExcluded(file) here like FindInProjectTask.collectFilesInScope() does
           // because all found usages are guaranteed to be not in excluded dir
           directory != null ? forDirectory(project, findModel.isWithSubdirectories(), directory) :
           module != null ? module.getModuleContentScope() :
           findModel.isProjectScope() ? ProjectScope.getContentScope(project) :
           GlobalSearchScope.allScope(project);
  }

  @NotNull
  private static GlobalSearchScope forDirectory(@NotNull Project project,
                                                boolean withSubdirectories,
                                                @NotNull VirtualFile directory) {
    Set<VirtualFile> result = new LinkedHashSet<>();
    result.add(directory);
    addSourceDirectoriesFromLibraries(project, directory, result);
    VirtualFile[] array = result.toArray(VirtualFile.EMPTY_ARRAY);
    return GlobalSearchScopesCore.directoriesScope(project, withSubdirectories, array);
  }

  public static void initFileFilter(@NotNull final JComboBox<? super String> fileFilter, @NotNull final JCheckBox useFileFilter) {
    fileFilter.setEditable(true);
    String[] fileMasks = FindSettings.getInstance().getRecentFileMasks();
    for(int i=fileMasks.length-1; i >= 0; i--) {
      fileFilter.addItem(fileMasks[i]);
    }
    fileFilter.setEnabled(false);

    useFileFilter.addActionListener(
      __ -> {
        if (useFileFilter.isSelected()) {
          fileFilter.setEnabled(true);
          fileFilter.getEditor().selectAll();
          fileFilter.getEditor().getEditorComponent().requestFocusInWindow();
        }
        else {
          fileFilter.setEnabled(false);
        }
      }
    );
  }

  public static String getPresentableName(@NotNull FindModel.SearchContext searchContext) {
    @PropertyKey(resourceBundle = "messages.FindBundle") String messageKey = null;
    if (searchContext == FindModel.SearchContext.ANY) {
      messageKey = "find.context.anywhere.scope.label";
    } else if (searchContext == FindModel.SearchContext.EXCEPT_COMMENTS) {
      messageKey = "find.context.except.comments.scope.label";
    } else if (searchContext == FindModel.SearchContext.EXCEPT_STRING_LITERALS) {
      messageKey = "find.context.except.literals.scope.label";
    } else if (searchContext == FindModel.SearchContext.EXCEPT_COMMENTS_AND_STRING_LITERALS) {
      messageKey = "find.context.except.comments.and.literals.scope.label";
    } else if (searchContext == FindModel.SearchContext.IN_COMMENTS) {
      messageKey = "find.context.in.comments.scope.label";
    } else if (searchContext == FindModel.SearchContext.IN_STRING_LITERALS) {
      messageKey = "find.context.in.literals.scope.label";
    }
    return messageKey != null ? FindBundle.message(messageKey) : searchContext.toString();
  }
}
