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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.DefaultExternalDependencyId;
import org.jetbrains.plugins.gradle.ExternalDependencyId;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Vladislav.Soroka
 */
public abstract class AbstractExternalDependency implements ExternalDependency {

  private static final long serialVersionUID = 1L;

  @NotNull
  private final DefaultExternalDependencyId myId;
  private String myScope;
  private final Collection<ExternalDependency> myDependencies;
  private String mySelectionReason;
  private int myClasspathOrder;
  private boolean myExported;

  public AbstractExternalDependency() {
    this(new DefaultExternalDependencyId(), null, null);
  }

  public AbstractExternalDependency(ExternalDependencyId id,
                                    String selectionReason,
                                    Collection<? extends ExternalDependency> dependencies) {
    myId = new DefaultExternalDependencyId(id);
    mySelectionReason = selectionReason;
    myDependencies = dependencies == null ? new ArrayList<ExternalDependency>() : ModelFactory.createCopy(dependencies);
  }

  public AbstractExternalDependency(ExternalDependency dependency) {
    this(
      dependency.getId(),
      dependency.getSelectionReason(),
      dependency.getDependencies()
    );
    myScope = dependency.getScope();
    myClasspathOrder = dependency.getClasspathOrder();
    myExported = dependency.getExported();
  }

  @NotNull
  @Override
  public ExternalDependencyId getId() {
    return myId;
  }

  @Override
  public String getName() {
    return myId.getName();
  }

  public void setName(String name) {
    myId.setName(name);
  }

  @Override
  public String getGroup() {
    return myId.getGroup();
  }

  public void setGroup(String group) {
    myId.setGroup(group);
  }

  @Override
  public String getVersion() {
    return myId.getVersion();
  }

  public void setVersion(String version) {
    myId.setVersion(version);
  }

  @NotNull
  @Override
  public String getPackaging() {
    return myId.getPackaging();
  }

  public void setPackaging(@NotNull String packaging) {
    myId.setPackaging(packaging);
  }

  @Nullable
  @Override
  public String getClassifier() {
    return myId.getClassifier();
  }

  public void setClassifier(@Nullable String classifier) {
    myId.setClassifier(classifier);
  }

  @Nullable
  @Override
  public String getSelectionReason() {
    return mySelectionReason;
  }

  @Override
  public int getClasspathOrder() {
    return myClasspathOrder;
  }

  public void setClasspathOrder(int order) {
    myClasspathOrder = order;
  }

  public void setSelectionReason(String selectionReason) {
    this.mySelectionReason = selectionReason;
  }

  @Override
  public String getScope() {
    return myScope;
  }

  public void setScope(String scope) {
    this.myScope = scope;
  }

  @NotNull
  @Override
  public Collection<ExternalDependency> getDependencies() {
    return myDependencies;
  }

  @Override
  public boolean getExported() {
    return myExported;
  }

  public void setExported(boolean exported) {
    myExported = exported;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof AbstractExternalDependency)) return false;
    AbstractExternalDependency that = (AbstractExternalDependency)o;
    return Objects.equal(myId, that.myId) &&
           Objects.equal(myScope, that.myScope) &&
           Objects.equal(myDependencies, that.myDependencies) &&
           Objects.equal(mySelectionReason, that.mySelectionReason);
  }

  @Override
  public int hashCode() {
    return 31 + Objects.hashCode(myId, myScope, myDependencies, mySelectionReason);
  }
}

