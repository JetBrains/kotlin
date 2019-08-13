/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.codeInsight.actions;

public class LayoutCodeInfoCollector {

  private String optimizeImportsNotification = null;
  private String reformatCodeNotification = null;
  private String rearrangeCodeNotification = null;

  public String getOptimizeImportsNotification() {
    return optimizeImportsNotification;
  }

  public void setOptimizeImportsNotification(String optimizeImportsNotification) {
    this.optimizeImportsNotification = optimizeImportsNotification;
  }

  public String getReformatCodeNotification() {
    return reformatCodeNotification;
  }

  public void setReformatCodeNotification(String reformatCodeNotification) {
    this.reformatCodeNotification = reformatCodeNotification;
  }

  public String getRearrangeCodeNotification() {
    return rearrangeCodeNotification;
  }

  public void setRearrangeCodeNotification(String rearrangeCodeNotification) {
    this.rearrangeCodeNotification = rearrangeCodeNotification;
  }

  public boolean hasReformatOrRearrangeNotification() {
    return rearrangeCodeNotification != null
           || reformatCodeNotification != null;
  }

  public boolean isEmpty() {
    return optimizeImportsNotification == null
           && rearrangeCodeNotification == null
           && reformatCodeNotification == null;
  }
}
