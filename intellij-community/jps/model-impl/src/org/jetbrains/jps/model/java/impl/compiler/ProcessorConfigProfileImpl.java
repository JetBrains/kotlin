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

import gnu.trove.THashMap;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.compiler.ProcessorConfigProfile;

import java.util.*;

/**
 * @author Eugene Zhuravlev
 */
public final class ProcessorConfigProfileImpl implements ProcessorConfigProfile {

  private String myName = "";
  private boolean myEnabled = false;
  private boolean myObtainProcessorsFromClasspath = true;
  private String myProcessorPath = "";
  private final Set<String> myProcessors = new LinkedHashSet<>(1); // empty list means all discovered
  private final Map<String, String> myProcessorOptions = new THashMap<>(1); // key=value map of options
  private String myGeneratedProductionDirectoryName = DEFAULT_PRODUCTION_DIR_NAME;
  private String myGeneratedTestsDirectoryName = DEFAULT_TESTS_DIR_NAME;
  private boolean myOutputRelativeToContentRoot = false;

  private final Set<String> myModuleNames = new THashSet<>(1);

  public ProcessorConfigProfileImpl(String name) {
    myName = name;
  }

  public ProcessorConfigProfileImpl(ProcessorConfigProfile profile) {
    initFrom(profile);
  }

  @Override
  public final void initFrom(ProcessorConfigProfile other) {
    myName = other.getName();
    myEnabled = other.isEnabled();
    myObtainProcessorsFromClasspath = other.isObtainProcessorsFromClasspath();
    myProcessorPath = other.getProcessorPath();
    myProcessors.clear();
    myProcessors.addAll(other.getProcessors());
    myProcessorOptions.clear();
    myProcessorOptions.putAll(other.getProcessorOptions());
    myGeneratedProductionDirectoryName = other.getGeneratedSourcesDirectoryName(false);
    myGeneratedTestsDirectoryName = other.getGeneratedSourcesDirectoryName(true);
    myOutputRelativeToContentRoot = other.isOutputRelativeToContentRoot();
    myModuleNames.clear();
    myModuleNames.addAll(other.getModuleNames());
  }

  @Override
  public String getName() {
    return myName;
  }

  @Override
  public void setName(String name) {
    myName = name;
  }

  @Override
  public boolean isEnabled() {
    return myEnabled;
  }

  @Override
  public void setEnabled(boolean enabled) {
    myEnabled = enabled;
  }

  @Override
  @NotNull
  public String getProcessorPath() {
    return myProcessorPath;
  }

  @Override
  public void setProcessorPath(@Nullable String processorPath) {
    myProcessorPath = processorPath != null? processorPath : "";
  }

  @Override
  public boolean isObtainProcessorsFromClasspath() {
    return myObtainProcessorsFromClasspath;
  }

  @Override
  public void setObtainProcessorsFromClasspath(boolean value) {
    myObtainProcessorsFromClasspath = value;
  }

  @Override
  @NotNull
  public String getGeneratedSourcesDirectoryName(boolean forTests) {
    return forTests? myGeneratedTestsDirectoryName : myGeneratedProductionDirectoryName;
  }

  @Override
  public void setGeneratedSourcesDirectoryName(@Nullable String name, boolean forTests) {
    if (forTests) {
      myGeneratedTestsDirectoryName = name != null? name.trim() : DEFAULT_TESTS_DIR_NAME;
    }
    else {
      myGeneratedProductionDirectoryName = name != null? name.trim() : DEFAULT_PRODUCTION_DIR_NAME;
    }
  }

  @Override
  public boolean isOutputRelativeToContentRoot() {
    return myOutputRelativeToContentRoot;
  }

  @Override
  public void setOutputRelativeToContentRoot(boolean relativeToContent) {
    myOutputRelativeToContentRoot = relativeToContent;
  }

  @Override
  @NotNull
  public Set<String> getModuleNames() {
    return myModuleNames;
  }

  @Override
  public boolean addModuleName(String name) {
    return myModuleNames.add(name);
  }

  @Override
  public boolean addModuleNames(Collection<String> names) {
    return myModuleNames.addAll(names);
  }

  @Override
  public boolean removeModuleName(String name) {
    return myModuleNames.remove(name);
  }

  @Override
  public boolean removeModuleNames(Collection<String> names) {
    return myModuleNames.removeAll(names);
  }

  @Override
  public void clearModuleNames() {
    myModuleNames.clear();
  }

  @Override
  public void clearProcessors() {
    myProcessors.clear();
  }

  @Override
  public boolean addProcessor(String processor) {
    return myProcessors.add(processor);
  }

  @Override
  public boolean removeProcessor(String processor) {
    return myProcessors.remove(processor);
  }

  @Override
  @NotNull
  public Set<String> getProcessors() {
    return Collections.unmodifiableSet(myProcessors);
  }

  @Override
  @NotNull
  public Map<String, String> getProcessorOptions() {
    return Collections.unmodifiableMap(myProcessorOptions);
  }

  @Override
  public String setOption(String key, String value) {
    return myProcessorOptions.put(key, value);
  }

  @Override
  @Nullable
  public String getOption(String key) {
    return myProcessorOptions.get(key);
  }

  @Override
  public void clearProcessorOptions() {
    myProcessorOptions.clear();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ProcessorConfigProfileImpl profile = (ProcessorConfigProfileImpl)o;

    if (myEnabled != profile.myEnabled) return false;
    if (myObtainProcessorsFromClasspath != profile.myObtainProcessorsFromClasspath) return false;
    if (myGeneratedProductionDirectoryName != null
        ? !myGeneratedProductionDirectoryName.equals(profile.myGeneratedProductionDirectoryName)
        : profile.myGeneratedProductionDirectoryName != null) {
      return false;
    }
    if (myGeneratedTestsDirectoryName != null
        ? !myGeneratedTestsDirectoryName.equals(profile.myGeneratedTestsDirectoryName)
        : profile.myGeneratedTestsDirectoryName != null) {
      return false;
    }
    if (myOutputRelativeToContentRoot != profile.myOutputRelativeToContentRoot)return false;
    if (!myModuleNames.equals(profile.myModuleNames)) return false;
    if (!myProcessorOptions.equals(profile.myProcessorOptions)) return false;
    if (myProcessorPath != null ? !myProcessorPath.equals(profile.myProcessorPath) : profile.myProcessorPath != null) return false;
    if (!myProcessors.equals(profile.myProcessors)) return false;
    if (!myName.equals(profile.myName)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myName.hashCode();
    result = 31 * result + (myEnabled ? 1 : 0);
    result = 31 * result + (myObtainProcessorsFromClasspath ? 1 : 0);
    result = 31 * result + (myProcessorPath != null ? myProcessorPath.hashCode() : 0);
    result = 31 * result + myProcessors.hashCode();
    result = 31 * result + myProcessorOptions.hashCode();
    result = 31 * result + (myGeneratedProductionDirectoryName != null ? myGeneratedProductionDirectoryName.hashCode() : 0);
    result = 31 * result + (myGeneratedTestsDirectoryName != null ? myGeneratedTestsDirectoryName.hashCode() : 0);
    result = 31 * result + (myOutputRelativeToContentRoot ? 1 : 0);
    result = 31 * result + myModuleNames.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return myName;
  }
}

