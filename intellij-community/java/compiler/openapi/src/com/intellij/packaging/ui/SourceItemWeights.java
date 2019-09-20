/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.packaging.ui;

/**
 * @author nik
 */
public class SourceItemWeights {
  public static final int ARTIFACTS_GROUP_WEIGHT = 200;
  public static final int ARTIFACT_WEIGHT = 150;
  public static final int MODULE_GROUP_WEIGHT = 100;
  public static final int MODULE_WEIGHT = 50;
  public static final int MODULE_OUTPUT_WEIGHT = 30;
  public static final int MODULE_SOURCE_WEIGHT = 25;
  public static final int LIBRARY_WEIGHT = 10;

  private SourceItemWeights() {
  }
}
