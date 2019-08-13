package com.intellij.webcore.packaging;

import org.jetbrains.annotations.Nullable;

/**
 * @author yole
 */
public class InstalledPackage {
  private final String myName;
  private final String myVersion;

  public InstalledPackage(String name, String version) {
    myName = name;
    myVersion = version;
  }

  public String getName() {
    return myName;
  }

  public String getVersion() {
    return myVersion;
  }

  @Nullable
  public String getTooltipText() {
    return null;
  }

  @Override
  public String toString() {
    return getName();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    InstalledPackage aPackage = (InstalledPackage)o;
    if (myName != null ? !myName.equals(aPackage.myName) : aPackage.myName != null) return false;
    if (myVersion != null ? !myVersion.equals(aPackage.myVersion) : aPackage.myVersion != null) return false;
    return true;
  }

  @Override
  public int hashCode() {
    int result = myName != null ? myName.hashCode() : 0;
    result = 31 * result + (myVersion != null ? myVersion.hashCode() : 0);
    return result;
  }
}
