/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package org.jetbrains.jps.model.java.impl.compiler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.JpsElementChildRole;
import org.jetbrains.jps.model.ex.JpsCompositeElementBase;
import org.jetbrains.jps.model.ex.JpsElementChildRoleBase;
import org.jetbrains.jps.model.java.compiler.*;
import org.jetbrains.jps.model.module.JpsModule;

import java.io.File;
import java.util.*;

/**
 * @author nik
 */
public class JpsJavaCompilerConfigurationImpl extends JpsCompositeElementBase<JpsJavaCompilerConfigurationImpl> implements JpsJavaCompilerConfiguration {
  public static final JpsElementChildRole<JpsJavaCompilerConfiguration> ROLE = JpsElementChildRoleBase.create("compiler configuration");
  private boolean myAddNotNullAssertions = true;
  private List<String> myNotNullAnnotations = Collections.singletonList(NotNull.class.getName());
  private boolean myClearOutputDirectoryOnRebuild = true;
  private final JpsCompilerExcludes myCompilerExcludes = new JpsCompilerExcludesImpl();
  private final JpsCompilerExcludes myValidationExcludes = new JpsCompilerExcludesImpl();
  private final List<String> myResourcePatterns = new ArrayList<>();
  private final List<ProcessorConfigProfile> myAnnotationProcessingProfiles = new ArrayList<>();
  private final ProcessorConfigProfileImpl myDefaultAnnotationProcessingProfile = new ProcessorConfigProfileImpl("Default");
  private boolean myUseReleaseOption = true;
  private String myProjectByteCodeTargetLevel;
  private final Map<String, String> myModulesByteCodeTargetLevels = new HashMap<>();
  private final Map<String, JpsJavaCompilerOptions> myCompilerOptions = new HashMap<>();
  private String myJavaCompilerId = "Javac";
  private Map<JpsModule, ProcessorConfigProfile> myAnnotationProcessingProfileMap;
  private ResourcePatterns myCompiledPatterns;
  private JpsValidationConfiguration myValidationConfiguration = new JpsValidationConfigurationImpl(false, Collections.emptySet());

  public JpsJavaCompilerConfigurationImpl() {
  }

  private JpsJavaCompilerConfigurationImpl(JpsJavaCompilerConfigurationImpl original) {
    super(original);
  }

  @NotNull
  @Override
  public JpsJavaCompilerConfigurationImpl createCopy() {
    return new JpsJavaCompilerConfigurationImpl(this);
  }

  @Override
  public boolean isAddNotNullAssertions() {
    return myAddNotNullAssertions;
  }

  @Override
  public List<String> getNotNullAnnotations() {
    return myNotNullAnnotations;
  }

  @Override
  public boolean isClearOutputDirectoryOnRebuild() {
    return myClearOutputDirectoryOnRebuild;
  }

  @Override
  public void setAddNotNullAssertions(boolean addNotNullAssertions) {
    myAddNotNullAssertions = addNotNullAssertions;
  }

  @Override
  public void setNotNullAnnotations(List<String> notNullAnnotations) {
    myNotNullAnnotations = Collections.unmodifiableList(notNullAnnotations);
  }

  @Override
  public void setClearOutputDirectoryOnRebuild(boolean clearOutputDirectoryOnRebuild) {
    myClearOutputDirectoryOnRebuild = clearOutputDirectoryOnRebuild;
  }

  @NotNull
  @Override
  public JpsCompilerExcludes getCompilerExcludes() {
    return myCompilerExcludes;
  }

  @NotNull
  @Override
  public JpsCompilerExcludes getValidationExcludes() {
    return myValidationExcludes;
  }

  @NotNull
  @Override
  public JpsValidationConfiguration getValidationConfiguration() {
    return myValidationConfiguration;
  }

  @Override
  public void setValidationConfiguration(boolean validateOnBuild, @NotNull Set<String> disabledValidators) {
    myValidationConfiguration = new JpsValidationConfigurationImpl(validateOnBuild, disabledValidators);
  }

  @NotNull
  @Override
  public ProcessorConfigProfile getDefaultAnnotationProcessingProfile() {
    return myDefaultAnnotationProcessingProfile;
  }

  @NotNull
  @Override
  public Collection<ProcessorConfigProfile> getAnnotationProcessingProfiles() {
    return myAnnotationProcessingProfiles;
  }

  @Override
  public void addResourcePattern(String pattern) {
    myResourcePatterns.add(pattern);
  }

  @Override
  public List<String> getResourcePatterns() {
    return myResourcePatterns;
  }

  @Override
  public boolean isResourceFile(@NotNull File file, @NotNull File srcRoot) {
    ResourcePatterns patterns = myCompiledPatterns;
    if (patterns == null) {
      myCompiledPatterns = patterns = new ResourcePatterns(this);
    }
    return patterns.isResourceFile(file, srcRoot);
  }

  @Override
  @Nullable
  public String getByteCodeTargetLevel(String moduleName) {
    String level = myModulesByteCodeTargetLevels.get(moduleName);
    if (level != null) {
      return level.isEmpty() ? null : level;
    }
    return myProjectByteCodeTargetLevel;
  }

  @Override
  public void setModuleByteCodeTargetLevel(String moduleName, String level) {
    myModulesByteCodeTargetLevels.put(moduleName, level);
  }

  @NotNull
  @Override
  public String getJavaCompilerId() {
    return myJavaCompilerId;
  }

  @Override
  public void setJavaCompilerId(@NotNull String compiler) {
    myJavaCompilerId = compiler;
  }

  @NotNull
  @Override
  public JpsJavaCompilerOptions getCompilerOptions(@NotNull String compilerId) {
    JpsJavaCompilerOptions options = myCompilerOptions.get(compilerId);
    if (options == null) {
      options = new JpsJavaCompilerOptions();
      myCompilerOptions.put(compilerId, options);
    }
    return options;
  }

  @Override
  public void setCompilerOptions(@NotNull String compilerId, @NotNull JpsJavaCompilerOptions options) {
    myCompilerOptions.put(compilerId, options);
  }

  @NotNull
  @Override
  public JpsJavaCompilerOptions getCurrentCompilerOptions() {
    return getCompilerOptions(getJavaCompilerId());
  }

  @Override
  public void setProjectByteCodeTargetLevel(String level) {
    myProjectByteCodeTargetLevel = level;
  }

  @Override
  public boolean useReleaseOption() {
    return myUseReleaseOption;
  }

  @Override
  public void setUseReleaseOption(boolean useReleaseOption) {
    myUseReleaseOption = useReleaseOption;
  }

  @Override
  public ProcessorConfigProfile addAnnotationProcessingProfile() {
    ProcessorConfigProfileImpl profile = new ProcessorConfigProfileImpl("");
    myAnnotationProcessingProfiles.add(profile);
    return profile;
  }

  @Override
  @NotNull
  public ProcessorConfigProfile getAnnotationProcessingProfile(JpsModule module) {
    Map<JpsModule, ProcessorConfigProfile> map = myAnnotationProcessingProfileMap;
    if (map == null) {
      map = new HashMap<>();
      final Map<String, JpsModule> namesMap = new HashMap<>();
      for (JpsModule m : module.getProject().getModules()) {
        namesMap.put(m.getName(), m);
      }
      if (!namesMap.isEmpty()) {
        for (ProcessorConfigProfile profile : getAnnotationProcessingProfiles()) {
          for (String name : profile.getModuleNames()) {
            final JpsModule mod = namesMap.get(name);
            if (mod != null) {
              map.put(mod, profile);
            }
          }
        }
      }
      myAnnotationProcessingProfileMap = map;
    }
    final ProcessorConfigProfile profile = map.get(module);
    return profile != null? profile : getDefaultAnnotationProcessingProfile();
  }
}
