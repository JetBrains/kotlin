// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.jps.model.module.impl;

import com.intellij.openapi.util.Condition;
import com.intellij.util.CollectConsumer;
import com.intellij.util.Consumer;
import com.intellij.util.EmptyConsumer;
import com.intellij.util.Processor;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.library.JpsLibrary;
import org.jetbrains.jps.model.module.*;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public abstract class JpsDependenciesEnumeratorBase<Self extends JpsDependenciesEnumerator> implements JpsDependenciesEnumerator {
  private boolean myWithoutSdk;
  private boolean myWithoutLibraries;
  private boolean myWithoutDepModules;
  private boolean myWithoutModuleSourceEntries;
  protected boolean myRecursively;
  private final Collection<JpsModule> myRootModules;
  private Condition<? super JpsDependencyElement> myCondition;

  protected JpsDependenciesEnumeratorBase(Collection<JpsModule> rootModules) {
    myRootModules = rootModules;
  }

  @NotNull
  @Override
  public Self withoutLibraries() {
    myWithoutLibraries = true;
    return self();
  }

  @NotNull
  @Override
  public Self withoutDepModules() {
    myWithoutDepModules = true;
    return self();
  }

  @NotNull
  @Override
  public Self withoutSdk() {
    myWithoutSdk = true;
    return self();
  }

  @NotNull
  @Override
  public Self withoutModuleSourceEntries() {
    myWithoutModuleSourceEntries = true;
    return self();
  }

  @NotNull
  @Override
  public Self satisfying(@NotNull Condition<? super JpsDependencyElement> condition) {
    myCondition = condition;
    return self();
  }

  @NotNull
  @Override
  public Self recursively() {
    myRecursively = true;
    return self();
  }

  protected abstract Self self();

  @NotNull
  @Override
  public Set<JpsModule> getModules() {
    Set<JpsModule> result = new LinkedHashSet<>();
    processModules(new CollectConsumer<>(result));
    return result;
  }

  @Override
  public void processModules(@NotNull final Consumer<? super JpsModule> consumer) {
    processModuleAndLibraries(consumer, EmptyConsumer.getInstance());
  }

  protected boolean shouldProcessDependenciesRecursively() {
    return true;
  }

  public boolean processDependencies(Processor<? super JpsDependencyElement> processor) {
    Set<JpsModule> processed = new ObjectOpenHashSet<>();
    for (JpsModule module : myRootModules) {
      if (!doProcessDependencies(module, processor, processed)) {
        return false;
      }
    }
    return true;
  }

  private boolean doProcessDependencies(JpsModule module, Processor<? super JpsDependencyElement> processor, Set<? super JpsModule> processed) {
    if (!processed.add(module)) return true;

    for (JpsDependencyElement element : module.getDependenciesList().getDependencies()) {
      if (myCondition != null && !myCondition.value(element)) continue;

      if (myWithoutSdk && element instanceof JpsSdkDependency
       || myWithoutLibraries && element instanceof JpsLibraryDependency
       || myWithoutModuleSourceEntries && element instanceof JpsModuleSourceDependency) continue;

      if (myWithoutDepModules) {
        if (!myRecursively && element instanceof JpsModuleDependency) continue;
        if (element instanceof JpsModuleSourceDependency && !isEnumerationRootModule(module)) continue;
      }

      if (!shouldProcess(module, element)) {
        continue;
      }

      if (element instanceof JpsModuleDependency) {
        if (myRecursively && shouldProcessDependenciesRecursively()) {
          JpsModule depModule = ((JpsModuleDependency)element).getModule();
          if (depModule != null) {
            doProcessDependencies(depModule, processor, processed);
            continue;
          }
        }
        if (myWithoutDepModules) continue;
      }

      if (!processor.process(element)) {
        return false;
      }
    }

    return true;
  }

  protected boolean shouldProcess(JpsModule module, JpsDependencyElement element) {
    return true;
  }

  public boolean isEnumerationRootModule(JpsModule module) {
    return myRootModules.contains(module);
  }

  @NotNull
  @Override
  public Set<JpsLibrary> getLibraries() {
    Set<JpsLibrary> libraries = new LinkedHashSet<>();
    processLibraries(new CollectConsumer<>(libraries));
    return libraries;
  }

  @Override
  public void processLibraries(@NotNull final Consumer<? super JpsLibrary> consumer) {
    processModuleAndLibraries(EmptyConsumer.getInstance(), consumer);
  }

  @Override
  public void processModuleAndLibraries(@Nullable final Consumer<? super JpsModule> moduleConsumer, @Nullable final Consumer<? super JpsLibrary> libraryConsumer) {
    processDependencies(dependencyElement -> {
      if (moduleConsumer != null) {
        if (myRecursively && dependencyElement instanceof JpsModuleSourceDependency) {
          moduleConsumer.consume(dependencyElement.getContainingModule());
        }
        else if ((!myRecursively || !shouldProcessDependenciesRecursively()) && dependencyElement instanceof JpsModuleDependency) {
          JpsModule module = ((JpsModuleDependency)dependencyElement).getModule();
          if (module != null) {
            moduleConsumer.consume(module);
          }
        }
      }
      if (libraryConsumer != null && dependencyElement instanceof JpsLibraryDependency) {
        JpsLibrary library = ((JpsLibraryDependency)dependencyElement).getLibrary();
        if (library != null) {
          libraryConsumer.consume(library);
        }
      }
      return true;
    });
  }
}
