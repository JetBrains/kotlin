// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.model;

import com.intellij.util.Consumer;
import gnu.trove.THashSet;
import gnu.trove.TObjectHashingStrategy;
import org.gradle.internal.impldep.com.google.common.base.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.DefaultExternalDependencyId;
import org.jetbrains.plugins.gradle.ExternalDependencyId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Set;

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
    myDependencies = dependencies == null ? new ArrayList<ExternalDependency>(0) : ModelFactory.createCopy(dependencies);
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
           myClasspathOrder == that.myClasspathOrder &&
           calcDependenciesHash(myDependencies) == calcDependenciesHash(that.myDependencies);
  }

  @Override
  public int hashCode() {
    return 31 + Objects.hashCode(myId, myScope, myClasspathOrder);
  }

  private static int calcDependenciesHash(Collection<ExternalDependency> dependencies) {
    final int[] hashCode = {0};
    new Visitor(new Consumer<AbstractExternalDependency>() {
      @Override
      public void consume(AbstractExternalDependency dependency) {
        hashCode[0] += Objects.hashCode(dependency.myId, dependency.myScope);
      }
    }).visit(dependencies);
    return hashCode[0];
  }

  private static class Visitor {
    private final Consumer<AbstractExternalDependency> myConsumer;

    private Visitor(Consumer<AbstractExternalDependency> consumer) {
      myConsumer = consumer;
    }

    public void visit(@NotNull Collection<ExternalDependency> dependencies) {
      //noinspection unchecked
      Set<AbstractExternalDependency> seenDependencies = new THashSet<AbstractExternalDependency>(TObjectHashingStrategy.IDENTITY);
      LinkedList<ExternalDependency> toProcess = new LinkedList<ExternalDependency>(dependencies);
      ExternalDependency dependency;
      while ((dependency = toProcess.pollFirst()) != null) {
        if (seenDependencies.add((AbstractExternalDependency)dependency)) {
          myConsumer.consume((AbstractExternalDependency)dependency);
          toProcess.addAll(((AbstractExternalDependency)dependency).myDependencies);
        }
      }
    }
  }
}

