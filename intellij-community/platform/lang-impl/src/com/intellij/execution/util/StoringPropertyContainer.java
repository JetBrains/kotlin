/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.execution.util;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.util.config.AbstractProperty;
import com.intellij.util.config.Storage;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class StoringPropertyContainer extends AbstractProperty.AbstractPropertyContainer<AbstractProperty<Boolean>> {
  private final Map<AbstractProperty<Boolean>, Boolean> myValues = new HashMap<>();
  private final Storage myStorage;

  public StoringPropertyContainer(String groupName, PropertiesComponent propertiesComponent) {
    this(new Storage.PropertiesComponentStorage(groupName, propertiesComponent));
  }

  public StoringPropertyContainer(@NotNull Storage storage) {
    myStorage = storage;
  }

  public void setIfUndefined(@NotNull AbstractProperty<Boolean> property, boolean value) {
    if (myStorage.get(property.getName()) == null) {
      setValueOf(property, value);
    }
  }

  @Override
  protected void setValueOf(@NotNull AbstractProperty<Boolean> property, Object value) {
    myValues.put(property, (Boolean)value);
    onPropertyChanged(property, (Boolean)value);
    myStorage.put(property.getName(), value.toString());
  }

  @Override
  public boolean hasProperty(@NotNull AbstractProperty property) {
    return myValues.containsKey(property);
  }

  @Override
  protected Object getValueOf(@NotNull AbstractProperty<Boolean> property) {
    Boolean value = myValues.get(property);
    if (value == null) {
      String stringValue = myStorage.get(property.getName());
      value = stringValue != null ? Boolean.valueOf(stringValue) : property.getDefault(this);
      myValues.put(property, value);
    }
    return value;
  }

  protected <T> void onPropertyChanged(@NotNull AbstractProperty<T> property, T value) { }
}
