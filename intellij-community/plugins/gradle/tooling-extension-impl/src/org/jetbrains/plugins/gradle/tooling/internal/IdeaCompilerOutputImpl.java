/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.plugins.gradle.tooling.internal;

import org.jetbrains.plugins.gradle.model.ExtIdeaCompilerOutput;

import java.io.File;

public class IdeaCompilerOutputImpl implements ExtIdeaCompilerOutput {
  private File myMainClassesDir;
  private File myMainResourcesDir;
  private File myTestClassesDir;
  private File myTestResourcesDir;

  @Override
  public File getMainClassesDir() {
    return myMainClassesDir;
  }

  @Override
  public File getMainResourcesDir() {
    return myMainResourcesDir;
  }

  @Override
  public File getTestClassesDir() {
    return myTestClassesDir;
  }

  @Override
  public File getTestResourcesDir() {
    return myTestResourcesDir;
  }

  public void setMainClassesDir(File mainClassesDir) {
    myMainClassesDir = mainClassesDir;
  }

  public void setMainResourcesDir(File mainResourcesDir) {
    myMainResourcesDir = mainResourcesDir;
  }

  public void setTestClassesDir(File testClassesDir) {
    myTestClassesDir = testClassesDir;
  }

  public void setTestResourcesDir(File testResourcesDir) {
    myTestResourcesDir = testResourcesDir;
  }
}
