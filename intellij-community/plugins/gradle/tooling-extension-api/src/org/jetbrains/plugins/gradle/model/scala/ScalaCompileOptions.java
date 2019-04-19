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
package org.jetbrains.plugins.gradle.model.scala;

import java.io.Serializable;
import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public interface ScalaCompileOptions extends Serializable {
  /**
   * @deprecated see https://docs.gradle.org/3.0/release-notes#ant-based-scala-compiler-has-been-removed
   */
  @Deprecated
  boolean isUseCompileDaemon();

  /**
   * @deprecated see https://docs.gradle.org/3.0/release-notes#ant-based-scala-compiler-has-been-removed
   */
  @Deprecated
  String getDaemonServer();

  boolean isFailOnError();

  boolean isDeprecation();

  boolean isUnchecked();

  String getDebugLevel();

  boolean isOptimize();

  String getEncoding();

  String getForce();

  List<String> getAdditionalParameters();

  boolean isListFiles();

  String getLoggingLevel();

  List<String> getLoggingPhases();

  /**
   * @deprecated see https://docs.gradle.org/3.0/release-notes#ant-based-scala-compiler-has-been-removed
   */
  @Deprecated
  boolean isFork();

  /**
   * @deprecated see https://docs.gradle.org/3.0/release-notes#ant-based-scala-compiler-has-been-removed
   */
  @Deprecated
  boolean isUseAnt();

  ScalaForkOptions getForkOptions();
}
