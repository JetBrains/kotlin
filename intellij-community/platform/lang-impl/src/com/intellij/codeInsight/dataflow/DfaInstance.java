/*
 * Copyright 2000-2007 JetBrains s.r.o.
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
package com.intellij.codeInsight.dataflow;

import com.intellij.codeInsight.controlflow.Instruction;
import org.jetbrains.annotations.NotNull;

public interface DfaInstance<E> {
  // Please ensure that E has correctly implemented equals method

  // Invariant: fun must create new instance of DFAMap if modifies it
  E fun(E e, Instruction instruction);

  @NotNull
  E initial();
}
