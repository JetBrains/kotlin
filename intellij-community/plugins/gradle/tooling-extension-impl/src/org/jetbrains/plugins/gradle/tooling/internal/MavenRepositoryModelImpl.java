// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.internal;

import org.jetbrains.plugins.gradle.model.MavenRepositoryModel;

public class MavenRepositoryModelImpl implements MavenRepositoryModel {

  private final String myName;
  private final String myUrl;

  public MavenRepositoryModelImpl(String name, String url) {
    myName = name;
    myUrl = url;
  }

  @Override
  public String getName() {
    return myName;
  }

  @Override
  public String getUrl() {
    return myUrl;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MavenRepositoryModelImpl)) return false;

    MavenRepositoryModelImpl model = (MavenRepositoryModelImpl)o;

    if (myName != null ? !myName.equals(model.myName) : model.myName != null) return false;
    if (myUrl != null ? !myUrl.equals(model.myUrl) : model.myUrl != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myName != null ? myName.hashCode() : 0;
    result = 31 * result + (myUrl != null ? myUrl.hashCode() : 0);
    return result;
  }
}
