/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Instances of this class are associated with the files processed by compilers.
 * A file is considered modified if currently associated ValidityState differs from the previously stored ValiditySate for this file.
 */
public interface ValidityState {
  /**
   * Compares this validity state to other ValidityState.
   *
   * @param otherState the state to compare with.
   * @return true if states can be considered equal, false otherwise.
   */
  boolean equalsTo(ValidityState otherState);
  /**
   * Invoked by make subsystem in order to store the state.
   *
   * @param out the output to which the state should be stored.
   * @throws IOException if the save operation failed because of an I/O error.
   * @see TimestampValidityState#load(DataInput)
   */
  void save(DataOutput out) throws IOException;
}
