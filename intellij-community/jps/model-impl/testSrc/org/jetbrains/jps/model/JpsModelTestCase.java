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
package org.jetbrains.jps.model;

import com.intellij.testFramework.UsefulTestCase;
import org.jetbrains.jps.model.impl.JpsModelImpl;

/**
 * @author nik
 */
public abstract class JpsModelTestCase extends UsefulTestCase {
  protected JpsModel myModel;
  protected TestJpsEventDispatcher myDispatcher;
  protected JpsProject myProject;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    myDispatcher = new TestJpsEventDispatcher();
    myModel = new JpsModelImpl(myDispatcher);
    myProject = myModel.getProject();
  }
}
