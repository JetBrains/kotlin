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
import org.jetbrains.plugins.gradle.model.scala.ScalaModel;

import java.io.File;
import java.util.Set;

/**
 * @author Vladislav.Soroka
 */
public class ScalaModelImpl implements ScalaModel {
  private Set<File> scalaClasspath;
  private Set<File> zincClasspath;
  private String sourceCompatibility;
  private String targetCompatibility;
  private ScalaCompileOptionsImpl scalaCompileOptions;

  @Override
  public Set<File> getScalaClasspath() {
    return scalaClasspath;
  }

  public void setScalaClasspath(Set<File> scalaClasspath) {
    this.scalaClasspath = scalaClasspath;
  }

  @Override
  public Set<File> getZincClasspath() {
    return zincClasspath;
  }

  public void setZincClasspath(Set<File> zincClasspath) {
    this.zincClasspath = zincClasspath;
  }

  @Override
  public ScalaCompileOptions getScalaCompileOptions() {
    return scalaCompileOptions;
  }

  public void setScalaCompileOptions(ScalaCompileOptionsImpl scalaCompileOptions) {
    this.scalaCompileOptions = scalaCompileOptions;
  }

  @Override
  public String getSourceCompatibility() {
    return sourceCompatibility;
  }

  public void setSourceCompatibility(String sourceCompatibility) {
    this.sourceCompatibility = sourceCompatibility;
  }

  @Override
  public String getTargetCompatibility() {
    return targetCompatibility;
  }

  public void setTargetCompatibility(String targetCompatibility) {
    this.targetCompatibility = targetCompatibility;
  }
}
