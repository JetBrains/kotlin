/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
import org.jetbrains.jps.model.JpsElementChildRole;
import org.jetbrains.jps.model.JpsFileTypesConfiguration;
import org.jetbrains.jps.model.ex.JpsElementBase;
import org.jetbrains.jps.model.ex.JpsElementChildRoleBase;

/**
 * @author nik
 */
public class JpsFileTypesConfigurationImpl extends JpsElementBase<JpsFileTypesConfigurationImpl> implements JpsFileTypesConfiguration {
  public static final JpsElementChildRole<JpsFileTypesConfiguration> ROLE = JpsElementChildRoleBase.create("file types");
  private String myIgnoredPatternString;

  public JpsFileTypesConfigurationImpl() {
    this("CVS;.DS_Store;.svn;.pyc;.pyo;*.pyc;*.pyo;.git;*.hprof;_svn;.hg;*.lib;*~;__pycache__;.bundle;vssver.scc;vssver2.scc;*.rbc;");
  }

  private JpsFileTypesConfigurationImpl(String ignoredPatternString) {
    myIgnoredPatternString = ignoredPatternString;
  }

  @NotNull
  @Override
  public JpsFileTypesConfigurationImpl createCopy() {
    return new JpsFileTypesConfigurationImpl(myIgnoredPatternString);
  }

  @Override
  public String getIgnoredPatternString() {
    return myIgnoredPatternString;
  }

  @Override
  public void setIgnoredPatternString(@NotNull String ignoredPatternString) {
    if (!myIgnoredPatternString.equals(ignoredPatternString)) {
      myIgnoredPatternString = ignoredPatternString;
      fireElementChanged();
    }
  }

  @Override
  public void applyChanges(@NotNull JpsFileTypesConfigurationImpl modified) {
    setIgnoredPatternString(modified.myIgnoredPatternString);
  }
}
