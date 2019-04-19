/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package org.jetbrains.plugins.gradle.settings;

/**
 * @author Vladislav.Soroka
 */
public enum DistributionType {
  /**
   * Gradle version packaged with IDE used
   */
  BUNDLED,
  /**
   * The default configuration of the wrapper task assumed based on manually wrapper generated files:
   * <p/>
   * Build layout with default wrapper task:
   * <pre>
   * project_dir/
   *    gradlew
   *    gradlew.bat
   *    gradle/wrapper/
   *        gradle-wrapper.jar
   *        <b>gradle-wrapper.properties</b>
   * </pre>
   */
  DEFAULT_WRAPPED,
  /**
   * Wrapper task configuration based on build.gradle script to be used.
   */
  WRAPPED,
  /**
   * Locally installed gradle to be used
   */
  LOCAL;

  /**
   * Check for wrapped mode
   * @return true in case of DEFAULT_WRAPPED or WRAPPED mode
   */
  public boolean isWrapped() {
    return this == DEFAULT_WRAPPED || this == WRAPPED;
  }
}
