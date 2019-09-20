/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.compiler.impl;

import org.jetbrains.annotations.NonNls;

/**
* @author Eugene Zhuravlev
*/
public class ExitStatus {
  private final String myName;

  ExitStatus(@NonNls String name) {
    myName = name;
  }

  public String toString() {
    return myName;
  }

  public static final ExitStatus CANCELLED = new ExitStatus("CANCELLED");
  public static final ExitStatus ERRORS = new ExitStatus("ERRORS");
  public static final ExitStatus SUCCESS = new ExitStatus("SUCCESS");
  public static final ExitStatus UP_TO_DATE = new ExitStatus("UP_TO_DATE");
}
