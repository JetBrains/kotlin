/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.openapi.externalSystem.service.task.ui;

import org.jetbrains.annotations.NotNull;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * @author Denis Zhdanov
 */
@SuppressWarnings("unchecked")
public class ExternalSystemNode<T> extends DefaultMutableTreeNode {

  public ExternalSystemNode(@NotNull ExternalSystemNodeDescriptor<T> descriptor) {
    super(descriptor);
  }

  @NotNull
  public ExternalSystemNodeDescriptor<T> getDescriptor() {
    return (ExternalSystemNodeDescriptor<T>)getUserObject();
  }

  @Override
  public ExternalSystemNode<?> getChildAt(int index) {
    return (ExternalSystemNode<?>)super.getChildAt(index);
  }
}
