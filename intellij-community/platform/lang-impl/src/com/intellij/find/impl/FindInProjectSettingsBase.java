/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.find.impl;

import com.intellij.openapi.application.PathMacroFilter;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.impl.stores.FileStorageCoreUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.XCollection;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class FindInProjectSettingsBase implements PersistentStateComponent<FindInProjectSettingsBase> {
  private static final int MAX_RECENT_SIZE = 30;

  @XCollection(style = XCollection.Style.v2, elementName = "find", valueAttributeName = "")
  public List<String> findStrings = new ArrayList<>();

  @XCollection(style = XCollection.Style.v2, elementName = "replace", valueAttributeName = "")
  public List<String> replaceStrings = new ArrayList<>();

  @XCollection(style = XCollection.Style.v2, elementName = "dir", valueAttributeName = "")
  public List<String> dirStrings = new ArrayList<>();

  @Override
  public void loadState(@NotNull FindInProjectSettingsBase state) {
    XmlSerializerUtil.copyBean(state, this);
    //Avoid duplicates
    LinkedHashSet<String> tmp = new LinkedHashSet<>(findStrings);
    findStrings.clear();
    findStrings.addAll(tmp);

    tmp.clear();
    tmp.addAll(replaceStrings);
    replaceStrings.clear();
    replaceStrings.addAll(tmp);

    tmp.clear();
    tmp.addAll(dirStrings);
    dirStrings.clear();
    dirStrings.addAll(tmp);
  }

  @Override
  public FindInProjectSettingsBase getState() {
    return this;
  }

  public void addDirectory(@NotNull String s) {
    if (s.isEmpty()){
      return;
    }
    addRecentStringToList(s, dirStrings);
  }

  @NotNull
  public List<String> getRecentDirectories() {
    return new ArrayList<>(dirStrings);
  }

  public void addStringToFind(@NotNull String s){
    if (s.indexOf('\r') >= 0 || s.indexOf('\n') >= 0){
      return;
    }
    addRecentStringToList(s, findStrings);
  }

  public void addStringToReplace(@NotNull String s) {
    if (s.indexOf('\r') >= 0 || s.indexOf('\n') >= 0){
      return;
    }
    addRecentStringToList(s, replaceStrings);
  }

  @NotNull
  public String[] getRecentFindStrings(){
    return ArrayUtil.toStringArray(findStrings);
  }

  @NotNull
  public String[] getRecentReplaceStrings(){
    return ArrayUtil.toStringArray(replaceStrings);
  }


  static void addRecentStringToList(@NotNull String str, @NotNull List<? super String> list) {
    list.remove(str);
    list.add(str);
    while (list.size() > MAX_RECENT_SIZE) {
      list.remove(0);
    }
  }

  static class FindInProjectPathMacroFilter extends PathMacroFilter {
    @Override
    public boolean skipPathMacros(@NotNull Element element) {
      String tag = element.getName();
      // dirStrings must be replaced, so, we must not skip it
      if (tag.equals("findStrings") || tag.equals("replaceStrings")) {
        String component = FileStorageCoreUtil.findComponentName(element);
        return component != null && (component.equals("FindSettings") || component.equals("FindInProjectRecents"));
      }
      return false;
    }
  }
}
