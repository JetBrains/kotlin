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
package org.jetbrains.plugins.gradle.model;

import org.gradle.internal.impldep.com.google.common.base.Objects;

/**
 * @author Vladislav.Soroka
 */
public class DefaultUnresolvedExternalDependency extends AbstractExternalDependency implements UnresolvedExternalDependency {

  private static final long serialVersionUID = 1L;

  private String myFailureMessage;

  public DefaultUnresolvedExternalDependency() {
  }

  public DefaultUnresolvedExternalDependency(UnresolvedExternalDependency dependency) {
    super(dependency);
    myFailureMessage = dependency.getFailureMessage();
  }

  @Override
  public String getFailureMessage() {
    return myFailureMessage;
  }

  public void setFailureMessage(String failureMessage) {
    myFailureMessage = failureMessage;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof DefaultUnresolvedExternalDependency)) return false;
    if (!super.equals(o)) return false;
    DefaultUnresolvedExternalDependency that = (DefaultUnresolvedExternalDependency)o;
    return Objects.equal(myFailureMessage, that.myFailureMessage);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(super.hashCode(), myFailureMessage);
  }

  @Override
  public String toString() {
    return "Unresolved dependency '" + getId() + "\':" + myFailureMessage;
  }
}
