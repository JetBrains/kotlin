/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.openapi.compiler;

import java.io.DataOutput;
import java.io.IOException;

/**
 * Empty validity state for force recompilation
 *
 * @author Konstantin Bulenkov
 */
public final class EmptyValidityState implements ValidityState {
  /**
   * In most cases this method returns false to force recompile
   *
   * @param otherState the state to compare with.
   * @return true if and only if otherState == this
   */
  @Override
  public boolean equalsTo(ValidityState otherState) {
      return otherState == this;
  }

  /**
   * Do nothing here
   */
  @Override
  public void save(DataOutput dataOutput) throws IOException {
  }
}
