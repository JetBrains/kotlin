/*
 * Copyright 2000-2008 JetBrains s.r.o.
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

package com.intellij.execution;

/**
 * Created by IntelliJ IDEA.
 *
 * @author: oleg
 * @date: 24.08.2006
 */
public class Output {
  private final String stdout;
  private final String stderr;
  private final int exitCode;

  public Output(String stdout, String stderr) {
    this(stdout, stderr, -1);
  }

  public Output(String stdout, String stderr, int exitCode) {
    this.stdout = stdout;
    this.stderr = stderr;
    this.exitCode = exitCode;
  }

  public String getStdout() {
    return stdout;
  }

  public String getStderr() {
    return stderr;
  }

  public int getExitCode() {
    return exitCode;
  }
}
