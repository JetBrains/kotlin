package com.intellij.webcore.packaging;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

public class RepoPackage implements Comparable {
  private final String myName;
  @Nullable final String myRepoUrl;
  @Nullable final String myLatestVersion;
  private final Collection<String> myKeywords;

  public RepoPackage(String name, String repoUrl) {
   this(name, repoUrl, null);
  }

  public RepoPackage(String name, @Nullable final String repoUrl, @Nullable String latestVersion) {
    this(name, repoUrl, latestVersion, Collections.emptyList());
  }

  public RepoPackage(String name, @Nullable final String repoUrl, @Nullable String latestVersion, Collection<String> keywords) {
    myName = name;
    myRepoUrl = repoUrl;
    myLatestVersion = latestVersion;
    myKeywords = keywords;
  }

  public String getName() {
    return myName;
  }

  @Nullable
  public String getRepoUrl() {
    return myRepoUrl;
  }

  @Nullable
  public String getLatestVersion() {
    return myLatestVersion;
  }

  public Collection<String> getKeywords() { return myKeywords; }

  @Override
  public int compareTo(Object o) {
    if (o instanceof RepoPackage)
      return myName.compareTo(((RepoPackage)o).getName());
    return 0;
  }
}