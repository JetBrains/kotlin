/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package org.jetbrains.plugins.gradle.model.data;

import java.io.Serializable;
import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public class ScalaCompileOptionsData implements Serializable {
  private static final long serialVersionUID = 1L;

  private boolean useCompileDaemon;
  private String daemonServer;
  private boolean failOnError;
  private boolean deprecation;
  private boolean unchecked;
  private String debugLevel;
  private boolean optimize;
  private String encoding;
  private String force;
  private String targetCompatibility;
  private List<String> additionalParameters;
  private boolean listFiles;
  private String loggingLevel;
  private List<String> loggingPhases;
  private boolean fork;
  private ScalaForkOptions forkOptions;
  private boolean useAnt;

  /**
   * @deprecated see https://docs.gradle.org/3.0/release-notes#ant-based-scala-compiler-has-been-removed
   */
  @Deprecated
  public boolean isUseCompileDaemon() {
    return useCompileDaemon;
  }

  public void setUseCompileDaemon(boolean useCompileDaemon) {
    this.useCompileDaemon = useCompileDaemon;
  }

  /**
   * @deprecated see https://docs.gradle.org/3.0/release-notes#ant-based-scala-compiler-has-been-removed
   */
  @Deprecated
  public String getDaemonServer() {
    return daemonServer;
  }

  public void setDaemonServer(String daemonServer) {
    this.daemonServer = daemonServer;
  }

  public boolean isFailOnError() {
    return failOnError;
  }

  public void setFailOnError(boolean failOnError) {
    this.failOnError = failOnError;
  }

  public boolean isDeprecation() {
    return deprecation;
  }

  public void setDeprecation(boolean deprecation) {
    this.deprecation = deprecation;
  }

  public boolean isUnchecked() {
    return unchecked;
  }

  public void setUnchecked(boolean unchecked) {
    this.unchecked = unchecked;
  }

  public String getDebugLevel() {
    return debugLevel;
  }

  public void setDebugLevel(String debugLevel) {
    this.debugLevel = debugLevel;
  }

  public boolean isOptimize() {
    return optimize;
  }

  public void setOptimize(boolean optimize) {
    this.optimize = optimize;
  }

  public String getEncoding() {
    return encoding;
  }

  public void setEncoding(String encoding) {
    this.encoding = encoding;
  }

  public String getForce() {
    return force;
  }

  public void setForce(String force) {
    this.force = force;
  }

  public String getTargetCompatibility() {
    return targetCompatibility;
  }

  public void setTargetCompatibility(String targetCompatibility) {
    this.targetCompatibility = targetCompatibility;
  }

  public List<String> getAdditionalParameters() {
    return additionalParameters;
  }

  public void setAdditionalParameters(List<String> additionalParameters) {
    this.additionalParameters = additionalParameters;
  }

  public boolean isListFiles() {
    return listFiles;
  }

  public void setListFiles(boolean listFiles) {
    this.listFiles = listFiles;
  }

  public String getLoggingLevel() {
    return loggingLevel;
  }

  public void setLoggingLevel(String loggingLevel) {
    this.loggingLevel = loggingLevel;
  }

  public List<String> getLoggingPhases() {
    return loggingPhases;
  }

  public void setLoggingPhases(List<String> loggingPhases) {
    this.loggingPhases = loggingPhases;
  }

  /**
   * @deprecated see https://docs.gradle.org/3.0/release-notes#ant-based-scala-compiler-has-been-removed
   */
  @Deprecated
  public boolean isFork() {
    return fork;
  }

  public void setFork(boolean fork) {
    this.fork = fork;
  }

  /**
   * @deprecated see https://docs.gradle.org/3.0/release-notes#ant-based-scala-compiler-has-been-removed
   */
  @Deprecated
  public boolean isUseAnt() {
    return useAnt;
  }

  public void setUseAnt(boolean useAnt) {
    this.useAnt = useAnt;
  }

  public ScalaForkOptions getForkOptions() {
    return forkOptions;
  }

  public void setForkOptions(ScalaForkOptions forkOptions) {
    this.forkOptions = forkOptions;
  }

  public static class ScalaForkOptions implements Serializable {
    private static final long serialVersionUID = 1L;
    private String memoryInitialSize;
    private String memoryMaximumSize;
    private List<String> jvmArgs;

    public String getMemoryInitialSize() {
      return memoryInitialSize;
    }

    public void setMemoryInitialSize(String memoryInitialSize) {
      this.memoryInitialSize = memoryInitialSize;
    }

    public String getMemoryMaximumSize() {
      return memoryMaximumSize;
    }

    public void setMemoryMaximumSize(String memoryMaximumSize) {
      this.memoryMaximumSize = memoryMaximumSize;
    }

    public List<String> getJvmArgs() {
      return jvmArgs;
    }

    public void setJvmArgs(List<String> jvmArgs) {
      this.jvmArgs = jvmArgs;
    }
  }
}
