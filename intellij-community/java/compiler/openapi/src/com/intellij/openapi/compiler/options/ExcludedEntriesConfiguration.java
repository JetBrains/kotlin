// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.openapi.compiler.options;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.JDOMExternalizable;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * @author nik
 */
public class ExcludedEntriesConfiguration implements PersistentStateComponent<ExcludedEntriesConfiguration>, JDOMExternalizable, Disposable,
                                                     ExcludesConfiguration {
  @NonNls private static final String FILE = "file";
  @NonNls private static final String DIRECTORY = "directory";
  @NonNls private static final String URL = "url";
  @NonNls private static final String INCLUDE_SUBDIRECTORIES = "includeSubdirectories";
  private final Collection<ExcludeEntryDescription> myExcludeEntryDescriptions = new LinkedHashSet<>();
  @Nullable private final ExcludedEntriesListener myEventPublisher;
  private ExcludeEntryDescription[] myCachedDescriptions = null;

  @SuppressWarnings("unused")
  public ExcludedEntriesConfiguration() {
    this(null);
  }

  public ExcludedEntriesConfiguration(@Nullable ExcludedEntriesListener eventPublisher) {
    myEventPublisher = eventPublisher;
  }

  @Override
  public synchronized ExcludeEntryDescription[] getExcludeEntryDescriptions() {
    if (myCachedDescriptions == null) {
      myCachedDescriptions = myExcludeEntryDescriptions.toArray(new ExcludeEntryDescription[0]);
    }
    return myCachedDescriptions;
  }

  @Override
  public synchronized void addExcludeEntryDescription(ExcludeEntryDescription description) {
    if (myExcludeEntryDescriptions.add(description) && myEventPublisher != null) {
      myEventPublisher.onEntryAdded(description);
    }
    myCachedDescriptions = null;
  }

  @Override
  public synchronized void removeExcludeEntryDescription(ExcludeEntryDescription description) {
    if (myExcludeEntryDescriptions.remove(description) && myEventPublisher != null) {
      myEventPublisher.onEntryRemoved(description);
    }
    myCachedDescriptions = null;
  }

  @Override
  public synchronized void removeAllExcludeEntryDescriptions() {
    ExcludeEntryDescription[] oldDescriptions = getExcludeEntryDescriptions();
    myExcludeEntryDescriptions.clear();
    if (myEventPublisher != null) {
      for (ExcludeEntryDescription description : oldDescriptions) {
        myEventPublisher.onEntryRemoved(description);
      }
    }
    myCachedDescriptions = null;
  }

  @Override
  public synchronized boolean containsExcludeEntryDescription(ExcludeEntryDescription description) {
    return myExcludeEntryDescriptions.contains(description);
  }

  @Override
  public void readExternal(final Element node) {
    removeAllExcludeEntryDescriptions();
    for (final Element element : node.getChildren()) {
      String url = element.getAttributeValue(URL);
      if (url == null) continue;
      if (FILE.equals(element.getName())) {
        ExcludeEntryDescription excludeEntryDescription = new ExcludeEntryDescription(url, false, true, this);
        addExcludeEntryDescription(excludeEntryDescription);
      }
      if (DIRECTORY.equals(element.getName())) {
        boolean includeSubdirectories = Boolean.parseBoolean(element.getAttributeValue(INCLUDE_SUBDIRECTORIES));
        ExcludeEntryDescription excludeEntryDescription = new ExcludeEntryDescription(url, includeSubdirectories, false,this);
        addExcludeEntryDescription(excludeEntryDescription);
      }
    }
  }

  @Override
  public void writeExternal(final Element element) {
    for (final ExcludeEntryDescription description : getExcludeEntryDescriptions()) {
      if (description.isFile()) {
        Element entry = new Element(FILE);
        entry.setAttribute(URL, description.getUrl());
        element.addContent(entry);
      }
      else {
        Element entry = new Element(DIRECTORY);
        entry.setAttribute(URL, description.getUrl());
        entry.setAttribute(INCLUDE_SUBDIRECTORIES, Boolean.toString(description.isIncludeSubdirectories()));
        element.addContent(entry);
      }
    }
  }

  @Override
  public boolean isExcluded(VirtualFile virtualFile) {
    for (final ExcludeEntryDescription entryDescription : getExcludeEntryDescriptions()) {
      VirtualFile descriptionFile = entryDescription.getVirtualFile();
      if (descriptionFile == null) {
        continue;
      }
      if (entryDescription.isFile()) {
        if (descriptionFile.equals(virtualFile)) {
          return true;
        }
      }
      else if (entryDescription.isIncludeSubdirectories()) {
        if (VfsUtilCore.isAncestor(descriptionFile, virtualFile, false)) {
          return true;
        }
      }
      else {
        if (virtualFile.isDirectory()) {
          continue;
        }
        if (descriptionFile.equals(virtualFile.getParent())) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public void dispose() {
    for (ExcludeEntryDescription description : myExcludeEntryDescriptions) {
      Disposer.dispose(description);
    }
  }

  @Override
  public ExcludedEntriesConfiguration getState() {
    return this;
  }

  @Override
  public void loadState(@NotNull final ExcludedEntriesConfiguration state) {
    for (ExcludeEntryDescription description : state.getExcludeEntryDescriptions()) {
      addExcludeEntryDescription(description.copy(this));
    }
    Disposer.dispose(state);
  }
}
