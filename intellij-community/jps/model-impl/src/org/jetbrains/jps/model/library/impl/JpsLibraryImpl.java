/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package org.jetbrains.jps.model.library.impl;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.*;
import org.jetbrains.jps.model.ex.JpsElementCollectionRole;
import org.jetbrains.jps.model.ex.JpsNamedCompositeElementBase;
import org.jetbrains.jps.model.impl.JpsElementCollectionImpl;
import org.jetbrains.jps.model.library.*;
import org.jetbrains.jps.util.JpsPathUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * @author nik
 */
public class JpsLibraryImpl<P extends JpsElement> extends JpsNamedCompositeElementBase<JpsLibraryImpl<P>> implements JpsTypedLibrary<P> {
  private static final ConcurrentMap<JpsOrderRootType, JpsElementCollectionRole<JpsLibraryRoot>> ourRootRoles = ContainerUtil.newConcurrentMap();
  private final JpsLibraryType<P> myLibraryType;

  public JpsLibraryImpl(@NotNull String name, @NotNull JpsLibraryType<P> type, @NotNull P properties) {
    super(name);
    myLibraryType = type;
    myContainer.setChild(myLibraryType.getPropertiesRole(), properties);
  }

  private JpsLibraryImpl(@NotNull JpsLibraryImpl<P> original) {
    super(original);
    myLibraryType = original.myLibraryType;
  }

  @Override
  @NotNull
  public JpsLibraryType<P> getType() {
    return myLibraryType;
  }

  @Nullable
  @Override
  public <P extends JpsElement> JpsTypedLibrary<P> asTyped(@NotNull JpsLibraryType<P> type) {
    //noinspection unchecked
    return myLibraryType.equals(type) ? (JpsTypedLibrary<P>)this : null;
  }

  @NotNull
  @Override
  public P getProperties() {
    return myContainer.getChild(myLibraryType.getPropertiesRole());
  }

  @NotNull
  @Override
  public List<JpsLibraryRoot> getRoots(@NotNull JpsOrderRootType rootType) {
    final JpsElementCollection<JpsLibraryRoot> rootsCollection = myContainer.getChild(getRole(rootType));
    return rootsCollection != null ? rootsCollection.getElements() : Collections.emptyList();
  }

  @Override
  public void addRoot(@NotNull String url, @NotNull JpsOrderRootType rootType) {
    addRoot(url, rootType, JpsLibraryRoot.InclusionOptions.ROOT_ITSELF);
  }

  @Override
  public void addRoot(@NotNull File file, @NotNull JpsOrderRootType rootType) {
    addRoot(JpsPathUtil.getLibraryRootUrl(file), rootType);
  }

  @Override
  public void addRoot(@NotNull final String url, @NotNull final JpsOrderRootType rootType,
                      @NotNull JpsLibraryRoot.InclusionOptions options) {
    myContainer.getOrSetChild(getRole(rootType)).addChild(new JpsLibraryRootImpl(url, rootType, options));
  }

  @Override
  public void removeUrl(@NotNull final String url, @NotNull final JpsOrderRootType rootType) {
    final JpsElementCollection<JpsLibraryRoot> rootsCollection = myContainer.getChild(getRole(rootType));
    if (rootsCollection != null) {
      for (JpsLibraryRoot root : rootsCollection.getElements()) {
        if (root.getUrl().equals(url) && root.getRootType().equals(rootType)) {
          rootsCollection.removeChild(root);
          break;
        }
      }
    }
  }

  private static JpsElementCollectionRole<JpsLibraryRoot> getRole(JpsOrderRootType type) {
    JpsElementCollectionRole<JpsLibraryRoot> role = ourRootRoles.get(type);
    if (role != null) return role;
    ourRootRoles.putIfAbsent(type, JpsElementCollectionRole.create(new JpsLibraryRootRole(type)));
    return ourRootRoles.get(type);
  }

  @Override
  public void delete() {
    getParent().removeChild(this);
  }

  @Override
  public JpsElementCollectionImpl<JpsLibrary> getParent() {
    //noinspection unchecked
    return (JpsElementCollectionImpl<JpsLibrary>)myParent;
  }

  @NotNull
  @Override
  public JpsLibraryImpl<P> createCopy() {
    return new JpsLibraryImpl<>(this);
  }

  @NotNull
  @Override
  public JpsLibraryReference createReference() {
    return new JpsLibraryReferenceImpl(getName(), createParentReference());
  }

  private JpsElementReference<JpsCompositeElement> createParentReference() {
    //noinspection unchecked
    return ((JpsReferenceableElement<JpsCompositeElement>)getParent().getParent()).createReference();
  }

  @Override
  public List<File> getFiles(final JpsOrderRootType rootType) {
    List<String> urls = getRootUrls(rootType);
    List<File> files = new ArrayList<>(urls.size());
    for (String url : urls) {
      if (!JpsPathUtil.isJrtUrl(url)) {
        files.add(JpsPathUtil.urlToFile(url));
      }
    }
    return files;
  }

  @Override
  public List<String> getRootUrls(JpsOrderRootType rootType) {
    List<String> urls = new ArrayList<>();
    for (JpsLibraryRoot root : getRoots(rootType)) {
      switch (root.getInclusionOptions()) {
        case ROOT_ITSELF:
          urls.add(root.getUrl());
          break;
        case ARCHIVES_UNDER_ROOT:
          collectArchives(JpsPathUtil.urlToFile(root.getUrl()), false, urls);
          break;
        case ARCHIVES_UNDER_ROOT_RECURSIVELY:
          collectArchives(JpsPathUtil.urlToFile(root.getUrl()), true, urls);
          break;
      }
    }
    return urls;
  }

  private static final Set<String> AR_EXTENSIONS  = ContainerUtil.newTroveSet(FileUtil.PATH_HASHING_STRATEGY, "jar", "zip", "swc", "ane");

  private static void collectArchives(File file, boolean recursively, List<String> result) {
    final File[] children = file.listFiles();
    if (children != null) {
      for (File child : children) {
        final String extension = FileUtilRt.getExtension(child.getName());
        if (child.isDirectory()) {
          if (recursively) {
            collectArchives(child, recursively, result);
          }
        }
        // todo [nik] get list of extensions mapped to Archive file type from IDE settings
        else if (AR_EXTENSIONS.contains(extension)) {
          result.add(JpsPathUtil.getLibraryRootUrl(child));
        }
      }
    }
  }
}