/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.plugins.gradle.model.data;

import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.util.xmlb.annotations.XCollection;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Vladislav.Soroka
 */
@Tag("build")
public class BuildParticipant implements Serializable {
  private String myRootProjectName;
  private String myRootPath;
  @NotNull private Set<String> myProjects = new HashSet<>();

  @Attribute("name")
  public String getRootProjectName() {
    return myRootProjectName;
  }

  public void setRootProjectName(String rootProjectName) {
    myRootProjectName = rootProjectName;
  }

  @Attribute("path")
  public String getRootPath() {
    return myRootPath;
  }

  public void setRootPath(String rootPath) {
    myRootPath = rootPath;
  }

  @XCollection(propertyElementName = "projects", elementName = "project", valueAttributeName = "path")
  @NotNull
  public Set<String> getProjects() {
    return myProjects;
  }

  public void setProjects(@NotNull Set<String> projects) {
    myProjects = projects;
  }

  public BuildParticipant copy() {
    BuildParticipant result = new BuildParticipant();
    result.myRootProjectName = myRootProjectName;
    result.myRootPath = myRootPath;
    result.myProjects = new HashSet<>(myProjects);
    return result;
  }
}
