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
package org.jetbrains.plugins.gradle.tooling.internal.scala;

import org.jetbrains.plugins.gradle.model.scala.ScalaCompileOptions;
import org.jetbrains.plugins.gradle.model.scala.ScalaForkOptions;

import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public class ScalaCompileOptionsImpl implements ScalaCompileOptions {
  private boolean useCompileDaemon;
  private String daemonServer;
  private boolean failOnError;
  private boolean deprecation;
  private boolean unchecked;
  private String debugLevel;
  private boolean optimize;
  private String encoding;
  private String force;
  private List<String> additionalParameters;
  private boolean listFiles;
  private String loggingLevel;
  private List<String> loggingPhases;
  private boolean fork;
  private ScalaForkOptionsImpl forkOptions;
  private boolean useAnt;

  @Override
  public boolean isUseCompileDaemon() {
    return useCompileDaemon;
  }

  public void setUseCompileDaemon(boolean useCompileDaemon) {
    this.useCompileDaemon = useCompileDaemon;
  }

  @Override
  public String getDaemonServer() {
    return daemonServer;
  }

  public void setDaemonServer(String daemonServer) {
    this.daemonServer = daemonServer;
  }

  @Override
  public boolean isFailOnError() {
    return failOnError;
  }

  public void setFailOnError(boolean failOnError) {
    this.failOnError = failOnError;
  }

  @Override
  public boolean isDeprecation() {
    return deprecation;
  }

  public void setDeprecation(boolean deprecation) {
    this.deprecation = deprecation;
  }

  @Override
  public boolean isUnchecked() {
    return unchecked;
  }

  public void setUnchecked(boolean unchecked) {
    this.unchecked = unchecked;
  }

  @Override
  public String getDebugLevel() {
    return debugLevel;
  }

  public void setDebugLevel(String debugLevel) {
    this.debugLevel = debugLevel;
  }

  @Override
  public boolean isOptimize() {
    return optimize;
  }

  public void setOptimize(boolean optimize) {
    this.optimize = optimize;
  }

  @Override
  public String getEncoding() {
    return encoding;
  }

  public void setEncoding(String encoding) {
    this.encoding = encoding;
  }

  @Override
  public String getForce() {
    return force;
  }

  public void setForce(String force) {
    this.force = force;
  }

  @Override
  public List<String> getAdditionalParameters() {
    return additionalParameters;
  }

  public void setAdditionalParameters(List<String> additionalParameters) {
    this.additionalParameters = additionalParameters;
  }

  @Override
  public boolean isListFiles() {
    return listFiles;
  }

  public void setListFiles(boolean listFiles) {
    this.listFiles = listFiles;
  }

  @Override
  public String getLoggingLevel() {
    return loggingLevel;
  }

  public void setLoggingLevel(String loggingLevel) {
    this.loggingLevel = loggingLevel;
  }

  @Override
  public List<String> getLoggingPhases() {
    return loggingPhases;
  }

  public void setLoggingPhases(List<String> loggingPhases) {
    this.loggingPhases = loggingPhases;
  }

  @Override
  public boolean isFork() {
    return fork;
  }

  public void setFork(boolean fork) {
    this.fork = fork;
  }

  @Override
  public boolean isUseAnt() {
    return useAnt;
  }

  public void setUseAnt(boolean useAnt) {
    this.useAnt = useAnt;
  }

  @Override
  public ScalaForkOptions getForkOptions() {
    return forkOptions;
  }

  public void setForkOptions(ScalaForkOptionsImpl forkOptions) {
    this.forkOptions = forkOptions;
  }
}
