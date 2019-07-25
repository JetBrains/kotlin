// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler;

import com.intellij.CommonBundle;
import com.intellij.ProjectTopics;
import com.intellij.compiler.impl.javaCompiler.BackendCompiler;
import com.intellij.compiler.impl.javaCompiler.eclipse.EclipseCompiler;
import com.intellij.compiler.impl.javaCompiler.javac.JavacCompiler;
import com.intellij.compiler.server.BuildManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.compiler.options.ExcludeEntryDescription;
import com.intellij.openapi.compiler.options.ExcludedEntriesConfiguration;
import com.intellij.openapi.compiler.options.ExcludedEntriesListener;
import com.intellij.openapi.compiler.options.ExcludesConfiguration;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.ModuleListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.*;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.impl.artifacts.ArtifactBySourceFileFinder;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.execution.ParametersListUtil;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.xmlb.SkipDefaultValuesSerializationFilters;
import com.intellij.util.xmlb.XmlSerializer;
import org.apache.oro.text.regex.*;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.compiler.CompilerOptions;
import org.jetbrains.jps.model.java.compiler.JavaCompilers;
import org.jetbrains.jps.model.java.compiler.JpsJavaCompilerOptions;
import org.jetbrains.jps.model.java.compiler.ProcessorConfigProfile;
import org.jetbrains.jps.model.java.impl.compiler.ProcessorConfigProfileImpl;
import org.jetbrains.jps.model.java.impl.compiler.ResourcePatterns;
import org.jetbrains.jps.model.serialization.java.compiler.AnnotationProcessorProfileSerializer;
import org.jetbrains.jps.model.serialization.java.compiler.JpsJavaCompilerConfigurationSerializer;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static com.intellij.compiler.ExternalCompilerConfigurationStorageKt.*;
import static org.jetbrains.jps.model.java.impl.compiler.ResourcePatterns.normalizeWildcards;
import static org.jetbrains.jps.model.serialization.java.compiler.JpsJavaCompilerConfigurationSerializer.DEFAULT_WILDCARD_PATTERNS;

@State(name = "CompilerConfiguration", storages = @Storage("compiler.xml"))
public class CompilerConfigurationImpl extends CompilerConfiguration implements PersistentStateComponent<Element>, ProjectComponent {
  private static final Logger LOG = Logger.getInstance("#com.intellij.compiler.CompilerConfiguration");

  public static final String TESTS_EXTERNAL_COMPILER_HOME_PROPERTY_NAME = "tests.external.compiler.home";
  public static final int DEFAULT_BUILD_PROCESS_HEAP_SIZE = 700;

  private BackendCompiler myDefaultJavaCompiler;
  private State myState = new State();

  // extensions of the files considered as resource files
  private final List<Pattern> myRegexpResourcePatterns = new ArrayList<>();
  // extensions of the files considered as resource files. If present, overrides patterns in old regexp format stored in myRegexpResourcePatterns
  private final List<String> myWildcardPatterns = new ArrayList<>();
  private final List<CompiledPattern> myCompiledPatterns = new ArrayList<>();
  private final List<CompiledPattern> myNegatedCompiledPatterns = new ArrayList<>();
  private boolean myWildcardPatternsInitialized = false;
  private final Project myProject;
  private final ExcludedEntriesConfiguration myExcludesConfiguration;

  private final Collection<BackendCompiler> myRegisteredCompilers = new ArrayList<>();
  private JavacCompiler JAVAC_EXTERNAL_BACKEND;
  private final Perl5Matcher myPatternMatcher = new Perl5Matcher();

  {
    loadDefaultWildcardPatterns();
  }
  private boolean myAddNotNullAssertions = true;

  private final ProcessorConfigProfile myDefaultProcessorsProfile = new ProcessorConfigProfileImpl("Default");
  private final List<ProcessorConfigProfile> myModuleProcessorProfiles = new ArrayList<>();

  // the map is calculated by module processor profiles list for faster access to module settings
  private Map<Module, ProcessorConfigProfile> myProcessorsProfilesMap = null;

  @Nullable
  private String myBytecodeTargetLevel = null;  // null means same as effective language level
  private final Map<String, String> myModuleBytecodeTarget = new HashMap<>();

  public CompilerConfigurationImpl(Project project) {
    myProject = project;
    myExcludesConfiguration = createExcludedEntriesConfiguration(project);
    MessageBusConnection connection = project.getMessageBus().connect(project);
    connection.subscribe(ProjectTopics.MODULES, new ModuleListener() {
      @Override
      public void beforeModuleRemoved(@NotNull Project project, @NotNull Module module) {
        getAnnotationProcessingConfiguration(module).removeModuleName(module.getName());
      }

      @Override
      public void moduleAdded(@NotNull Project project, @NotNull Module module) {
        myProcessorsProfilesMap = null; // clear cache
      }

      @Override
      public void modulesRenamed(@NotNull Project project,
                                 @NotNull List<Module> modules,
                                 @NotNull Function<Module, String> oldNameProvider) {
        updateModuleNames(modules.stream().collect(Collectors.toMap(oldNameProvider::fun, Module::getName)));
      }
    });
  }

  // Overridden in Upsource
  @NotNull
  protected ExcludedEntriesConfiguration createExcludedEntriesConfiguration(@NotNull Project project) {
    final ExcludedEntriesConfiguration cfg = new ExcludedEntriesConfiguration(project.getMessageBus().syncPublisher(ExcludedEntriesListener.TOPIC));
    Disposer.register(project, cfg);
    project.getMessageBus().connect().subscribe(ExcludedEntriesListener.TOPIC, new ExcludedEntriesListener() {
      @Override
      public void onEntryAdded(@NotNull ExcludeEntryDescription description) {
        clearState();
      }

      @Override
      public void onEntryRemoved(@NotNull ExcludeEntryDescription description) {
        clearState();
      }

      private void clearState() {
        if (project.isOpen()) {
          BuildManager.getInstance().clearState(project);
        }
      }
    });
    return cfg;
  }

  private static class State {
    public String DEFAULT_COMPILER = JavaCompilers.JAVAC_ID;
    public int BUILD_PROCESS_HEAP_SIZE = DEFAULT_BUILD_PROCESS_HEAP_SIZE;
    public String BUILD_PROCESS_ADDITIONAL_VM_OPTIONS = "";
    public boolean USE_RELEASE_OPTION = true;
  }

  @Override
  public Element getState() {
    Element state = new Element("state");
    XmlSerializer.serializeInto(myState, state, new SkipDefaultValuesSerializationFilters());

    if (!myAddNotNullAssertions) {
      addChild(state, JpsJavaCompilerConfigurationSerializer.ADD_NOTNULL_ASSERTIONS).setAttribute(
        JpsJavaCompilerConfigurationSerializer.ENABLED, String.valueOf(myAddNotNullAssertions));
    }

    if (myExcludesConfiguration.getExcludeEntryDescriptions().length > 0) {
      myExcludesConfiguration.writeExternal(addChild(state, JpsJavaCompilerConfigurationSerializer.EXCLUDE_FROM_COMPILE));
    }

    Element resourceExtensions = new Element(JpsJavaCompilerConfigurationSerializer.RESOURCE_EXTENSIONS);
    for (String pattern : getRegexpPatterns()) {
      addChild(resourceExtensions, JpsJavaCompilerConfigurationSerializer.ENTRY).setAttribute(JpsJavaCompilerConfigurationSerializer.NAME, pattern);
    }
    if (!JDOMUtil.isEmpty(resourceExtensions)) {
      state.addContent(resourceExtensions);
    }

    if ((myWildcardPatternsInitialized || !myWildcardPatterns.isEmpty()) &&
        !DEFAULT_WILDCARD_PATTERNS.equals(myWildcardPatterns)) {
      final Element wildcardPatterns = addChild(state, JpsJavaCompilerConfigurationSerializer.WILDCARD_RESOURCE_PATTERNS);
      for (final String wildcardPattern : myWildcardPatterns) {
        addChild(wildcardPatterns, JpsJavaCompilerConfigurationSerializer.ENTRY)
          .setAttribute(JpsJavaCompilerConfigurationSerializer.NAME, wildcardPattern);
      }
    }

    Element annotationProcessingSettings = new Element(JpsJavaCompilerConfigurationSerializer.ANNOTATION_PROCESSING);

    Element profileElement = new Element("profile");
    profileElement.setAttribute("default", "true");
    AnnotationProcessorProfileSerializer.writeExternal(myDefaultProcessorsProfile, profileElement);
    if (!JDOMUtil.isEmpty(profileElement, 2)) {
      annotationProcessingSettings.addContent(profileElement);
    }

    for (ProcessorConfigProfile profile : myModuleProcessorProfiles) {
      Element element = new Element("profile");
      AnnotationProcessorProfileSerializer.writeExternal(profile, element);
      annotationProcessingSettings.addContent(element);
    }

    if (!JDOMUtil.isEmpty(annotationProcessingSettings)) {
      state.addContent(annotationProcessingSettings);
    }

    List<String> moduleNames = getFilteredModuleNameList(myProject, myModuleBytecodeTarget, false);
    if (!StringUtil.isEmpty(myBytecodeTargetLevel) || !moduleNames.isEmpty()) {
      final Element bytecodeTarget = new Element(JpsJavaCompilerConfigurationSerializer.BYTECODE_TARGET_LEVEL);
      state.addContent(bytecodeTarget);
      if (!StringUtil.isEmpty(myBytecodeTargetLevel)) {
        bytecodeTarget.setAttribute(JpsJavaCompilerConfigurationSerializer.TARGET_ATTRIBUTE, myBytecodeTargetLevel);
      }
      writeBytecodeTarget(moduleNames, myModuleBytecodeTarget, bytecodeTarget);
    }
    return state;
  }

  @Override
  public int getBuildProcessHeapSize(final int javacPreferredHeapSize) {
    final int heapSize = myState.BUILD_PROCESS_HEAP_SIZE;
    if (heapSize != DEFAULT_BUILD_PROCESS_HEAP_SIZE) {
      return heapSize;
    }
    // compatibility with older builds: if javac is set to use larger heap, and if so, use it.
    return Math.max(heapSize, javacPreferredHeapSize);
  }

  @Override
  public void setBuildProcessHeapSize(int size) {
    myState.BUILD_PROCESS_HEAP_SIZE = size > 0? size : DEFAULT_BUILD_PROCESS_HEAP_SIZE;
  }

  @Override
  public String getBuildProcessVMOptions() {
    return myState.BUILD_PROCESS_ADDITIONAL_VM_OPTIONS;
  }

  @Override
  public void setBuildProcessVMOptions(String options) {
    myState.BUILD_PROCESS_ADDITIONAL_VM_OPTIONS = options == null? "" : options.trim();
  }

  private void updateModuleNames(Map<String, String> moduleNameMap) {
    JpsJavaCompilerOptions settings = getJavaCompilerSettings();
    boolean updated = false;
    for (Map.Entry<String, String> entry : moduleNameMap.entrySet()) {
      String targetLevel = myModuleBytecodeTarget.remove(entry.getKey());
      if (targetLevel != null) {
        myModuleBytecodeTarget.put(entry.getValue(), targetLevel);
        updated = true;
      }
      if (settings != null) {
        String options = settings.ADDITIONAL_OPTIONS_OVERRIDE.remove(entry.getKey());
        if (options != null) {
          settings.ADDITIONAL_OPTIONS_OVERRIDE.put(entry.getValue(), options);
          updated = true;
        }
      }
    }

    for (ProcessorConfigProfile profile : myModuleProcessorProfiles) {
      Set<String> names = profile.getModuleNames();
      Collection<String> updatedNames = ContainerUtil.intersection(names, moduleNameMap.keySet());
      if (!updatedNames.isEmpty()) {
        profile.removeModuleNames(updatedNames);
        profile.addModuleNames(ContainerUtil.map(updatedNames, moduleNameMap::get));
        updated = true;
      }
    }

    if (updated) {
      BuildManager.getInstance().clearState(myProject);
    }
  }

  @Override
  public void setProjectBytecodeTarget(@Nullable String level) {
    final String previous = myBytecodeTargetLevel;
    myBytecodeTargetLevel = level;
    if (!myProject.isDefault() && !Comparing.equal(previous, level)) {
      BuildManager.getInstance().clearState(myProject);
    }
  }

  @Override
  @Nullable
  public String getProjectBytecodeTarget() {
    return myBytecodeTargetLevel;
  }

  @Override
  public boolean useReleaseOption() {
    return myState.USE_RELEASE_OPTION;
  }

  @Override
  public void setUseReleaseOption(boolean useReleaseOption) {
    myState.USE_RELEASE_OPTION = useReleaseOption;
  }

  public void setModulesBytecodeTargetMap(@NotNull Map<String, String> mapping) {
    final boolean shouldNotify = !myProject.isDefault() && !myModuleBytecodeTarget.equals(mapping);
    myModuleBytecodeTarget.clear();
    myModuleBytecodeTarget.putAll(mapping);
    if (shouldNotify) {
      BuildManager.getInstance().clearState(myProject);
    }
  }

  public Map<String, String> getModulesBytecodeTargetMap() {
    return myModuleBytecodeTarget;
  }

  @Override
  public void setBytecodeTargetLevel(Module module, String level) {
    final String previous;
    if (StringUtil.isEmpty(level)) {
      previous = myModuleBytecodeTarget.remove(module.getName());
    }
    else {
      previous = myModuleBytecodeTarget.put(module.getName(), level);
    }
    if (!Comparing.equal(previous, level)) {
      final Project project = module.getProject();
      if (!project.isDefault()) {
        BuildManager.getInstance().clearState(project);
      }
    }
  }

  @Override
  @Nullable
  public String getBytecodeTargetLevel(Module module) {
    final String level = myModuleBytecodeTarget.get(module.getName());
    if (level != null) {
      return level.isEmpty() ? null : level;
    }
    return myBytecodeTargetLevel;
  }

  private void loadDefaultWildcardPatterns() {
    if (!myWildcardPatterns.isEmpty()) {
      removeWildcardPatterns();
    }
    try {
      for (String pattern : DEFAULT_WILDCARD_PATTERNS) {
        addWildcardResourcePattern(pattern);
      }
    }
    catch (MalformedPatternException e) {
      LOG.error(e);
    }
  }

  @NotNull
  @Override
  public List<String> getAdditionalOptions(@NotNull Module module) {
    JpsJavaCompilerOptions settings = getJavaCompilerSettings();
    if (settings != null) {
      String options = settings.ADDITIONAL_OPTIONS_OVERRIDE.getOrDefault(module.getName(), settings.ADDITIONAL_OPTIONS_STRING);
      if (!StringUtil.isEmptyOrSpaces(options)) {
        return ParametersListUtil.parse(options);
      }
    }
    return Collections.emptyList();
  }

  @Override
  public void setAdditionalOptions(@NotNull Module module, @NotNull List<String> options) {
    JpsJavaCompilerOptions settings = getJavaCompilerSettings();
    if (settings != null) {
      setAdditionalOptions(settings, module, options);
    }
  }

  public void setAdditionalOptions(@NotNull JpsJavaCompilerOptions settings, @NotNull Module module, @NotNull List<String> options) {
    String previous = settings.ADDITIONAL_OPTIONS_OVERRIDE.getOrDefault(module.getName(), settings.ADDITIONAL_OPTIONS_STRING);
    String newValue = ParametersListUtil.join(options);
    if (!newValue.equals(previous)) {
      settings.ADDITIONAL_OPTIONS_OVERRIDE.put(module.getName(), newValue);
      BuildManager.getInstance().clearState(myProject);
    }
  }

  private JpsJavaCompilerOptions getJavaCompilerSettings() {
    CompilerOptions compilerOptions = getDefaultCompiler().getOptions();
    return compilerOptions instanceof JpsJavaCompilerOptions ? (JpsJavaCompilerOptions)compilerOptions : null;
  }

  public static String getTestsExternalCompilerHome() {
    String compilerHome = System.getProperty(TESTS_EXTERNAL_COMPILER_HOME_PROPERTY_NAME, null);
    if (compilerHome == null) {
      File javaHome = new File(System.getProperty("java.home"));
      if (SystemInfo.isMac || !new File(javaHome.getParentFile(), "bin").exists()) {
        compilerHome = javaHome.getAbsolutePath();
      }
      else {
        compilerHome = javaHome.getParentFile().getAbsolutePath();
      }
    }
    return compilerHome;
  }

  private static Pattern compilePattern(@NonNls String s) throws MalformedPatternException {
    try {
      final PatternCompiler compiler = new Perl5Compiler();
      return SystemInfo.isFileSystemCaseSensitive? compiler.compile(s) : compiler.compile(s, Perl5Compiler.CASE_INSENSITIVE_MASK);
    }
    catch (org.apache.oro.text.regex.MalformedPatternException ex) {
      throw new MalformedPatternException(ex);
    }
  }

  public JavacCompiler getJavacCompiler() {
    createCompilers();
    return JAVAC_EXTERNAL_BACKEND;
  }

  @Override
  public void projectOpened() {
    createCompilers();
  }

  private void createCompilers() {
    if (JAVAC_EXTERNAL_BACKEND != null) {
      return;
    }

    JAVAC_EXTERNAL_BACKEND = new JavacCompiler(myProject);
    myRegisteredCompilers.add(JAVAC_EXTERNAL_BACKEND);

    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      if (EclipseCompiler.isInitialized()) {
        final EclipseCompiler eclipse = new EclipseCompiler(myProject);
        myRegisteredCompilers.add(eclipse);
      }
    }

    final Set<FileType> types = new HashSet<>();
    for (BackendCompiler compiler : BackendCompiler.EP_NAME.getExtensions(myProject)) {
      myRegisteredCompilers.add(compiler);
      types.addAll(compiler.getCompilableFileTypes());
    }

    final CompilerManager compilerManager = CompilerManager.getInstance(myProject);
    for (FileType type : types) {
      compilerManager.addCompilableFileType(type);
    }

    myDefaultJavaCompiler = JAVAC_EXTERNAL_BACKEND;
    for (BackendCompiler compiler : myRegisteredCompilers) {
      if (compiler.getId().equals(myState.DEFAULT_COMPILER)) {
        myDefaultJavaCompiler = compiler;
        break;
      }
    }
    myState.DEFAULT_COMPILER = myDefaultJavaCompiler.getId();
  }

  public Collection<BackendCompiler> getRegisteredJavaCompilers() {
    createCompilers();
    return myRegisteredCompilers;
  }

  public String[] getResourceFilePatterns() {
    return getWildcardPatterns();
  }

  private String[] getRegexpPatterns() {
    String[] patterns = ArrayUtil.newStringArray(myRegexpResourcePatterns.size());
    int index = 0;
    for (final Pattern myRegexpResourcePattern : myRegexpResourcePatterns) {
      patterns[index++] = myRegexpResourcePattern.getPattern();
    }
    return patterns;
  }

  private String[] getWildcardPatterns() {
    return ArrayUtilRt.toStringArray(myWildcardPatterns);
  }

  @Override
  public void addResourceFilePattern(String namePattern) throws MalformedPatternException {
    addWildcardResourcePattern(namePattern);
  }

  // need this method only for handling patterns in old regexp format
  private void addRegexpPattern(String namePattern) throws MalformedPatternException {
    Pattern pattern = compilePattern(namePattern);
    if (pattern != null) {
      myRegexpResourcePatterns.add(pattern);
    }
  }

  @Override
  public ExcludesConfiguration getExcludedEntriesConfiguration() {
    return myExcludesConfiguration;
  }

  @Override
  public boolean isExcludedFromCompilation(final VirtualFile virtualFile) {
    return myExcludesConfiguration.isExcluded(virtualFile);
  }

  @Override
  public boolean isResourceFile(VirtualFile virtualFile) {
    return isResourceFile(virtualFile.getName(), virtualFile.getParent());
  }

  @Override
  public boolean isAddNotNullAssertions() {
    return myAddNotNullAssertions;
  }

  @Override
  public void setAddNotNullAssertions(boolean enabled) {
    myAddNotNullAssertions = enabled;
  }

  @NotNull
  public ProcessorConfigProfile getDefaultProcessorProfile() {
    return myDefaultProcessorsProfile;
  }

  public void setDefaultProcessorProfile(ProcessorConfigProfile profile) {
    myDefaultProcessorsProfile.initFrom(profile);
  }

  @NotNull
  public List<ProcessorConfigProfile> getModuleProcessorProfiles() {
    return myModuleProcessorProfiles;
  }

  public void setModuleProcessorProfiles(Collection<? extends ProcessorConfigProfile> moduleProfiles) {
    myModuleProcessorProfiles.clear();
    myModuleProcessorProfiles.addAll(moduleProfiles);
    myProcessorsProfilesMap = null;
  }

  @Nullable
  public ProcessorConfigProfile findModuleProcessorProfile(@NotNull String name) {
    for (ProcessorConfigProfile profile : myModuleProcessorProfiles) {
      if (name.equals(profile.getName())) {
        return profile;
      }
    }

    return null;
  }

  public void removeModuleProcessorProfile(ProcessorConfigProfile profile) {
    myModuleProcessorProfiles.remove(profile);
    myProcessorsProfilesMap = null; // clear cache
  }

  public void addModuleProcessorProfile(@NotNull ProcessorConfigProfile profile) {
    myModuleProcessorProfiles.add(profile);
    myProcessorsProfilesMap = null; // clear cache
  }

  @Override
  @NotNull
  public ProcessorConfigProfile getAnnotationProcessingConfiguration(Module module) {
    Map<Module, ProcessorConfigProfile> map = myProcessorsProfilesMap;
    if (map == null) {
      map = new HashMap<>();
      final Map<String, Module> namesMap = new HashMap<>();
      for (Module m : ModuleManager.getInstance(module.getProject()).getModules()) {
        namesMap.put(m.getName(), m);
      }
      if (!namesMap.isEmpty()) {
        for (ProcessorConfigProfile profile : myModuleProcessorProfiles) {
          for (String name : profile.getModuleNames()) {
            final Module mod = namesMap.get(name);
            if (mod != null) {
              map.put(mod, profile);
            }
          }
        }
      }
      myProcessorsProfilesMap = map;
    }
    final ProcessorConfigProfile profile = map.get(module);
    return profile != null? profile : myDefaultProcessorsProfile;
  }

  @Override
  public boolean isAnnotationProcessorsEnabled() {
    if (myDefaultProcessorsProfile.isEnabled()) {
      return true;
    }
    for (ProcessorConfigProfile profile : myModuleProcessorProfiles) {
      if (profile.isEnabled()) {
        return true;
      }
    }
    return false;
  }

  private void addWildcardResourcePattern(@NonNls final String wildcardPattern) throws MalformedPatternException {
    final CompiledPattern pattern = convertToRegexp(wildcardPattern);
    myWildcardPatterns.add(wildcardPattern);
    if (isPatternNegated(wildcardPattern)) {
      myNegatedCompiledPatterns.add(pattern);
    }
    else {
      myCompiledPatterns.add(pattern);
    }
  }

  public void removeResourceFilePatterns() {
    removeWildcardPatterns();
  }

  private void removeRegexpPatterns() {
    myRegexpResourcePatterns.clear();
  }

  private void removeWildcardPatterns() {
    myWildcardPatterns.clear();
    myCompiledPatterns.clear();
    myNegatedCompiledPatterns.clear();
  }

  private static CompiledPattern convertToRegexp(String wildcardPattern) {
    if (isPatternNegated(wildcardPattern)) {
      wildcardPattern = wildcardPattern.substring(1);
    }

    wildcardPattern = FileUtil.toSystemIndependentName(wildcardPattern);

    String srcRoot = null;
    int colon = wildcardPattern.indexOf(":");
    if (colon > 0) {
      srcRoot = wildcardPattern.substring(0, colon);
      wildcardPattern = wildcardPattern.substring(colon + 1);
    }

    String dirPattern = null;
    int slash = wildcardPattern.lastIndexOf('/');
    if (slash >= 0) {
      dirPattern = wildcardPattern.substring(0, slash + 1);
      wildcardPattern = wildcardPattern.substring(slash + 1);
      dirPattern = ResourcePatterns.optimizeDirPattern(dirPattern);
    }

    wildcardPattern = normalizeWildcards(wildcardPattern);
    wildcardPattern = optimize(wildcardPattern);

    final Pattern dirCompiled = dirPattern == null ? null : compilePattern(dirPattern);
    final Pattern srcCompiled = srcRoot == null ? null : compilePattern(optimize(normalizeWildcards(srcRoot)));
    return new CompiledPattern(compilePattern(wildcardPattern), dirCompiled, srcCompiled);
  }

  private static String optimize(String wildcardPattern) {
    return wildcardPattern.replaceAll("(?:\\.\\*)+", ".*");
  }

  public static boolean isPatternNegated(String wildcardPattern) {
    return wildcardPattern.length() > 1 && wildcardPattern.charAt(0) == '!';
  }

  @Override
  public boolean isResourceFile(String name) {
    return isResourceFile(name, null);
  }

  private boolean matches(String s, Pattern p) {
    synchronized (myPatternMatcher) {
      try {
        return myPatternMatcher.matches(s, p);
      }
      catch (Exception e) {
        LOG.error("Exception matching file name \"" + s + "\" against the pattern \"" + p + "\"", e);
        return false;
      }
    }
  }

  private boolean isResourceFile(String name, @Nullable VirtualFile parent) {
    final Ref<String> parentRef = Ref.create(null);
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < myCompiledPatterns.size(); i++) {
      if (matches(name, parent, parentRef, myCompiledPatterns.get(i))) {
        return true;
      }
    }

    if (myNegatedCompiledPatterns.isEmpty()) {
      return false;
    }

    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < myNegatedCompiledPatterns.size(); i++) {
      if (matches(name, parent, parentRef, myNegatedCompiledPatterns.get(i))) {
        return false;
      }
    }
    return true;
  }

  private boolean matches(String name, VirtualFile parent, Ref<String> parentRef, CompiledPattern pair) {
    if (!matches(name, pair.fileName)) {
      return false;
    }

    if (parent != null && (pair.dir != null || pair.srcRoot != null)) {
      VirtualFile srcRoot = ProjectRootManager.getInstance(myProject).getFileIndex().getSourceRootForFile(parent);
      if (pair.dir != null) {
        String parentPath = parentRef.get();
        if (parentPath == null) {
          parentRef.set(parentPath = srcRoot == null ? parent.getPath() : VfsUtilCore.getRelativePath(parent, srcRoot, '/'));
        }
        if (parentPath == null || !matches("/" + parentPath, pair.dir)) {
          return false;
        }
      }

      if (pair.srcRoot != null) {
        String srcRootName = srcRoot == null ? null : srcRoot.getName();
        if (srcRootName == null || !matches(srcRootName, pair.srcRoot)) {
          return false;
        }
      }
    }

    return true;
  }

  @Override
  public void loadState(@NotNull Element parentNode) {
    myState = XmlSerializer.deserialize(parentNode, State.class);
    if (!myProject.isDefault()) {
      for (Element option : parentNode.getChildren("option")) {
        if ("DEFAULT_COMPILER".equals(option.getAttributeValue("name"))) {
          break;
        }
      }
      if (myState.BUILD_PROCESS_HEAP_SIZE == DEFAULT_BUILD_PROCESS_HEAP_SIZE) {
        final CompilerWorkspaceConfiguration workspace = CompilerWorkspaceConfiguration.getInstance(myProject);
        // older version compatibility: as a fallback load this setting from workspace
        myState.BUILD_PROCESS_HEAP_SIZE = workspace.COMPILER_PROCESS_HEAP_SIZE;
      }
    }

    final Element notNullAssertions = parentNode.getChild(JpsJavaCompilerConfigurationSerializer.ADD_NOTNULL_ASSERTIONS);
    if (notNullAssertions != null) {
      myAddNotNullAssertions = Boolean.valueOf(notNullAssertions.getAttributeValue(JpsJavaCompilerConfigurationSerializer.ENABLED, "true"));
    }

    Element node = parentNode.getChild(JpsJavaCompilerConfigurationSerializer.EXCLUDE_FROM_COMPILE);
    if (node != null) {
      myExcludesConfiguration.readExternal(node);
    }

    try {
      removeRegexpPatterns();
      node = parentNode.getChild(JpsJavaCompilerConfigurationSerializer.RESOURCE_EXTENSIONS);
      if (node != null) {
        for (final Object o : node.getChildren(JpsJavaCompilerConfigurationSerializer.ENTRY)) {
          Element element = (Element)o;
          String pattern = element.getAttributeValue(JpsJavaCompilerConfigurationSerializer.NAME);
          if (!StringUtil.isEmpty(pattern)) {
            addRegexpPattern(pattern);
          }
        }
      }

      node = parentNode.getChild(JpsJavaCompilerConfigurationSerializer.WILDCARD_RESOURCE_PATTERNS);
      if (node != null) {
        myWildcardPatternsInitialized = true;
        removeWildcardPatterns();
        for (Element element : node.getChildren(JpsJavaCompilerConfigurationSerializer.ENTRY)) {
          String pattern = element.getAttributeValue(JpsJavaCompilerConfigurationSerializer.NAME);
          if (!StringUtil.isEmpty(pattern)) {
            addWildcardResourcePattern(pattern);
          }
        }
      }
    }
    catch (MalformedPatternException e) {
      LOG.error(e);
    }

    myModuleProcessorProfiles.clear();
    myProcessorsProfilesMap = null;

    final Element annotationProcessingSettings = parentNode.getChild(JpsJavaCompilerConfigurationSerializer.ANNOTATION_PROCESSING);
    if (annotationProcessingSettings != null) {
      final List profiles = annotationProcessingSettings.getChildren("profile");
      if (!profiles.isEmpty()) {
        for (Object elem : profiles) {
          final Element profileElement = (Element)elem;
          final boolean isDefault = "true".equals(profileElement.getAttributeValue("default"));
          if (isDefault) {
            AnnotationProcessorProfileSerializer.readExternal(myDefaultProcessorsProfile, profileElement);
          }
          else {
            final ProcessorConfigProfile profile = new ProcessorConfigProfileImpl("");
            AnnotationProcessorProfileSerializer.readExternal(profile, profileElement);
            myModuleProcessorProfiles.add(profile);
          }
        }
      }
      else {
        // assuming older format
        loadProfilesFromOldFormat(annotationProcessingSettings);
      }
    }

    myBytecodeTargetLevel = null;
    myModuleBytecodeTarget.clear();

    Element bytecodeTargetElement = parentNode.getChild(JpsJavaCompilerConfigurationSerializer.BYTECODE_TARGET_LEVEL);
    if (bytecodeTargetElement != null) {
      myBytecodeTargetLevel = bytecodeTargetElement.getAttributeValue(JpsJavaCompilerConfigurationSerializer.TARGET_ATTRIBUTE);
      readByteTargetLevel(parentNode, myModuleBytecodeTarget);
    }

    Map<String, String> externalState = myProject.getComponent(ExternalCompilerConfigurationStorage.class).getLoadedState();
    if (externalState != null) {
      myModuleBytecodeTarget.putAll(externalState);
    }
  }

  private void loadProfilesFromOldFormat(Element processing) {
    // collect data
    final boolean isEnabled = Boolean.parseBoolean(processing.getAttributeValue(JpsJavaCompilerConfigurationSerializer.ENABLED, "false"));
    final boolean isUseClasspath = Boolean.parseBoolean(processing.getAttributeValue("useClasspath", "true"));
    final StringBuilder processorPath = new StringBuilder();
    final Set<String> optionPairs = new HashSet<>();
    final Set<String> processors = new HashSet<>();
    final List<Couple<String>> modulesToProcess = new ArrayList<>();

    for (Object child : processing.getChildren("processorPath")) {
      final Element pathElement = (Element)child;
      final String path = pathElement.getAttributeValue("value", (String)null);
      if (path != null) {
        if (processorPath.length() > 0) {
          processorPath.append(File.pathSeparator);
        }
        processorPath.append(path);
      }
    }

    for (Object child : processing.getChildren("processor")) {
      final Element processorElement = (Element)child;
      final String proc = processorElement.getAttributeValue(JpsJavaCompilerConfigurationSerializer.NAME, (String)null);
      if (proc != null) {
        processors.add(proc);
      }
      final StringTokenizer tokenizer = new StringTokenizer(processorElement.getAttributeValue("options", ""), " ", false);
      while (tokenizer.hasMoreTokens()) {
        final String pair = tokenizer.nextToken();
        optionPairs.add(pair);
      }
    }

    for (Object child : processing.getChildren("processModule")) {
      final Element moduleElement = (Element)child;
      final String name = moduleElement.getAttributeValue(JpsJavaCompilerConfigurationSerializer.NAME, (String)null);
      if (name == null) {
        continue;
      }
      final String dir = moduleElement.getAttributeValue("generatedDirName", (String)null);
      modulesToProcess.add(Couple.of(name, dir));
    }

    myDefaultProcessorsProfile.setEnabled(false);
    myDefaultProcessorsProfile.setObtainProcessorsFromClasspath(isUseClasspath);
    if (processorPath.length() > 0) {
      myDefaultProcessorsProfile.setProcessorPath(processorPath.toString());
    }
    if (!optionPairs.isEmpty()) {
      for (String pair : optionPairs) {
        final int index = pair.indexOf("=");
        if (index > 0) {
          myDefaultProcessorsProfile.setOption(pair.substring(0, index), pair.substring(index + 1));
        }
      }
    }
    for (String processor : processors) {
      myDefaultProcessorsProfile.addProcessor(processor);
    }

    final Map<String, Set<String>> dirNameToModulesMap = new HashMap<>();
    for (Couple<String> moduleDirPair : modulesToProcess) {
      final String dir = moduleDirPair.getSecond();
      Set<String> set = dirNameToModulesMap.get(dir);
      if (set == null) {
        set = new HashSet<>();
        dirNameToModulesMap.put(dir, set);
      }
      set.add(moduleDirPair.getFirst());
    }

    int profileIndex = 0;
    for (Map.Entry<String, Set<String>> entry : dirNameToModulesMap.entrySet()) {
      final String dirName = entry.getKey();
      final ProcessorConfigProfile profile = new ProcessorConfigProfileImpl(myDefaultProcessorsProfile);
      profile.setName("Profile" + (++profileIndex));
      profile.setEnabled(isEnabled);
      profile.setGeneratedSourcesDirectoryName(dirName, false);
      for (String moduleName : entry.getValue()) {
        profile.addModuleName(moduleName);
      }
      myModuleProcessorProfiles.add(profile);
    }
  }

  public BackendCompiler getDefaultCompiler() {
    createCompilers();
    return myDefaultJavaCompiler;
  }

  /**
   * @param defaultCompiler The compiler that is passed as a parameter to setDefaultCompiler()
   * must be one of the registered compilers in compiler configuration.
   * Otherwise because of lazy compiler initialization, the value of default compiler will point to some other compiler instance
   */
  public void setDefaultCompiler(BackendCompiler defaultCompiler) {
    myDefaultJavaCompiler = defaultCompiler;
    myState.DEFAULT_COMPILER = defaultCompiler.getId();
  }

  public void convertPatterns() {
    if (!needPatternConversion()) {
      return;
    }
    try {
      boolean ok;
      try {
        ok = doConvertPatterns();
      }
      catch (MalformedPatternException ignored) {
        ok = false;
      }
      if (!ok) {
        final String initialPatternString = patternsToString(getRegexpPatterns());
        final String message = CompilerBundle.message(
          "message.resource.patterns.format.changed",
          ApplicationNamesInfo.getInstance().getProductName(),
          initialPatternString,
          CommonBundle.getOkButtonText(),
          CommonBundle.getCancelButtonText()
        );
        final String wildcardPatterns = Messages.showInputDialog(
          myProject, message, CompilerBundle.message("pattern.conversion.dialog.title"), Messages.getWarningIcon(), initialPatternString, new InputValidator() {
          @Override
          public boolean checkInput(String inputString) {
            return true;
          }
          @Override
          public boolean canClose(String inputString) {
            final StringTokenizer tokenizer = new StringTokenizer(inputString, ";", false);
            StringBuilder malformedPatterns = new StringBuilder();

            while (tokenizer.hasMoreTokens()) {
              String pattern = tokenizer.nextToken();
              try {
                addWildcardResourcePattern(pattern);
              }
              catch (MalformedPatternException e) {
                malformedPatterns.append("\n\n");
                malformedPatterns.append(pattern);
                malformedPatterns.append(": ");
                malformedPatterns.append(e.getMessage());
              }
            }

            if (malformedPatterns.length() > 0) {
              Messages.showErrorDialog(CompilerBundle.message("error.bad.resource.patterns", malformedPatterns.toString()),
                                       CompilerBundle.message("bad.resource.patterns.dialog.title"));
              removeWildcardPatterns();
              return false;
            }
            return true;
          }
        });
        if (wildcardPatterns == null) { // cancel pressed
          loadDefaultWildcardPatterns();
        }
      }
    }
    finally {
      myWildcardPatternsInitialized = true;
    }
  }

  private boolean needPatternConversion() {
    return !myWildcardPatternsInitialized && !myRegexpResourcePatterns.isEmpty();
  }

  private boolean doConvertPatterns() throws MalformedPatternException {
    final String[] regexpPatterns = getRegexpPatterns();
    final List<String> converted = new ArrayList<>();
    final Pattern multipleExtensionsPatternPattern = compilePattern("\\.\\+\\\\\\.\\((\\w+(?:\\|\\w+)*)\\)");
    final Pattern singleExtensionPatternPattern = compilePattern("\\.\\+\\\\\\.(\\w+)");
    final Perl5Matcher matcher = new Perl5Matcher();
    for (final String regexpPattern : regexpPatterns) {
      //final Matcher multipleExtensionsMatcher = multipleExtensionsPatternPattern.matcher(regexpPattern);
      if (matcher.matches(regexpPattern, multipleExtensionsPatternPattern)) {
        final MatchResult match = matcher.getMatch();
        final StringTokenizer tokenizer = new StringTokenizer(match.group(1), "|", false);
        while (tokenizer.hasMoreTokens()) {
          converted.add("?*." + tokenizer.nextToken());
        }
      }
      else {
        //final Matcher singleExtensionMatcher = singleExtensionPatternPattern.matcher(regexpPattern);
        if (matcher.matches(regexpPattern, singleExtensionPatternPattern)) {
          final MatchResult match = matcher.getMatch();
          converted.add("?*." + match.group(1));
        }
        else {
          return false;
        }
      }
    }
    for (final String aConverted : converted) {
      addWildcardResourcePattern(aConverted);
    }
    return true;
  }

  private static String patternsToString(final String[] patterns) {
    final StringBuilder extensionsString = new StringBuilder();
    for (int idx = 0; idx < patterns.length; idx++) {
      if (idx > 0) {
        extensionsString.append(";");
      }
      extensionsString.append(patterns[idx]);
    }
    return extensionsString.toString();
  }

  @Override
  public boolean isCompilableResourceFile(final Project project, final VirtualFile file) {
    if (!isResourceFile(file)) {
      return false;
    }
    final Collection<? extends Artifact> artifacts = ArtifactBySourceFileFinder.getInstance(project).findArtifacts(file);
    return artifacts.isEmpty();
  }

  private static class CompiledPattern {
    @NotNull final Pattern fileName;
    @Nullable final Pattern dir;
    @Nullable final Pattern srcRoot;

    private CompiledPattern(@NotNull Pattern fileName, @Nullable Pattern dir, @Nullable Pattern srcRoot) {
      this.fileName = fileName;
      this.dir = dir;
      this.srcRoot = srcRoot;
    }
  }

  private static Element addChild(Element parent, final String childName) {
    final Element child = new Element(childName);
    parent.addContent(child);
    return child;
  }
}