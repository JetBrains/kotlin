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
package com.intellij.packaging.impl.artifacts;

import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.elements.*;
import com.intellij.packaging.impl.elements.ArtifactPackagingElement;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author nik
 */
public class PackagingElementPath {
  public static final PackagingElementPath EMPTY = new PackagingElementPath(null, null);
  private final PackagingElementPath myParentPath;
  private final PackagingElement<?> myLastElement;

  private PackagingElementPath(PackagingElementPath parentPath, PackagingElement<?> lastElement) {
    myParentPath = parentPath;
    myLastElement = lastElement;
  }

  public PackagingElementPath appendComplex(ComplexPackagingElement<?> element) {
    return new PackagingElementPath(this, element);
  }

  public PackagingElementPath appendComposite(CompositePackagingElement<?> element) {
    return new PackagingElementPath(this, element);
  }

  @NotNull
  public String getPathString() {
    return getPathString("/");
  }

  @NotNull
  public String getPathString(String separator) {
    return getPathStringFrom(separator, null);
  }

  @NotNull
  public String getPathStringFrom(String separator, @Nullable CompositePackagingElement<?> ancestor) {
    final List<CompositePackagingElement<?>> parents = getParentsFrom(ancestor);
    // StringUtil.join ignores empty strings whereas this monstrosity doesn't
    return ContainerUtil.reverse(parents).stream().map(RenameablePackagingElement::getName).collect(Collectors.joining("/"));
  }
  
  public List<CompositePackagingElement<?>> getParents() {
    return getParentsFrom(null);
  }

  public List<CompositePackagingElement<?>> getParentsFrom(@Nullable CompositePackagingElement<?> ancestor) {
    List<CompositePackagingElement<?>> result = new SmartList<>();
    PackagingElementPath path = this;
    while (path != EMPTY && path.myLastElement != ancestor) {
      if (path.myLastElement instanceof CompositePackagingElement<?>) {
        result.add((CompositePackagingElement)path.myLastElement);
      }
      path = path.myParentPath;
    }
    return result;
  }

  public List<PackagingElement<?>> getAllElements() {
    List<PackagingElement<?>> result = new SmartList<>();
    PackagingElementPath path = this;
    while (path != EMPTY) {
      result.add(path.myLastElement);
      path = path.myParentPath;
    }
    return result;
  }

  @Nullable
  public CompositePackagingElement<?> getLastParent() {
    PackagingElementPath path = this;
    while (path != EMPTY) {
      if (path.myLastElement instanceof CompositePackagingElement<?>) {
        return (CompositePackagingElement)path.myLastElement;
      }
      path = path.myParentPath;
    }
    return null;
  }

  @Nullable
  public Artifact findLastArtifact(PackagingElementResolvingContext context) {
    PackagingElementPath path = this;
    while (path != EMPTY) {
      final PackagingElement<?> element = path.myLastElement;
      if (element instanceof ArtifactPackagingElement) {
        return ((ArtifactPackagingElement)element).findArtifact(context);
      }
      path = path.myParentPath;
    }
    return null;
  }

  public static PackagingElementPath createPath(@NotNull List<? extends PackagingElement<?>> elements) {
    PackagingElementPath path = EMPTY;
    for (PackagingElement<?> element : elements) {
      path = new PackagingElementPath(path, element);
    }
    return path;
  }
}
