/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.internal.psiView.stubtree;

import com.intellij.psi.stubs.StubElement;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author Konstantin.Ulitin
 */
public class StubTreeNode extends SimpleNode {
  
  @NotNull
  private final StubElement<?> myStub;

  public StubTreeNode(@NotNull StubElement<?> stub, StubTreeNode parent) {
    super(parent);
    myStub = stub;
  }

  @NotNull
  public StubElement<?> getStub() {
    return myStub;
  }

  @NotNull
  @Override
  public StubTreeNode[] getChildren() {
    return ContainerUtil.map2Array(myStub.getChildrenStubs(), StubTreeNode.class, stub -> new StubTreeNode(stub, this));
  }

  @NotNull
  @Override
  public Object[] getEqualityObjects() {
    return new Object[]{myStub};
  }

  @Override
  public String getName() {
    return myStub.toString();
  }
}
