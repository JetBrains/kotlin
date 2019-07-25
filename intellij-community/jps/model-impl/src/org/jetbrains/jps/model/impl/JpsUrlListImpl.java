/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package org.jetbrains.jps.model.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.JpsUrlList;
import org.jetbrains.jps.model.ex.JpsElementBase;

import java.util.ArrayList;
import java.util.List;

/**
 * @author nik
 */
public class JpsUrlListImpl extends JpsElementBase<JpsUrlListImpl> implements JpsUrlList {
  private final List<String> myUrls = new ArrayList<>();

  public JpsUrlListImpl() {
  }

  public JpsUrlListImpl(JpsUrlListImpl list) {
    myUrls.addAll(list.myUrls);
  }

  @NotNull
  @Override
  public JpsUrlListImpl createCopy() {
    return new JpsUrlListImpl(this);
  }

  @NotNull
  @Override
  public List<String> getUrls() {
    return myUrls;
  }

  @Override
  public void addUrl(@NotNull String url) {
    myUrls.add(url);
    fireElementChanged();
  }

  @Override
  public void removeUrl(@NotNull String url) {
    myUrls.remove(url);
    fireElementChanged();
  }

  @Override
  public void applyChanges(@NotNull JpsUrlListImpl modified) {
    if (!myUrls.equals(modified.myUrls)) {
      myUrls.clear();
      myUrls.addAll(modified.myUrls);
      fireElementChanged();
    }
  }
}
