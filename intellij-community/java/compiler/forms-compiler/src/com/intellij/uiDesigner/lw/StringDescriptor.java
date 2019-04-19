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
package com.intellij.uiDesigner.lw;

/**
 * @author Vladimir Kondratyev
 */
public final class StringDescriptor {
  /**
   * Name of resource bundle
   */
  private final String myBundleName;
  /**
   * Key in the resource bundle
   */
  private final String myKey;
  /**
   * Value has sense if it's calculated not via resource bundle
   */
  private final String myValue;
  /**
   * Cached resolved value. We need it here to speed up property inspector
   * painting.
   */
  private String myResolvedValue;

  /**
   * Marker for string values which do not need internationalization
   */
  private boolean myNoI18n;

  private StringDescriptor(final String value){
    if (value == null) {
      throw new IllegalArgumentException("value cannot be null");
    }
    myBundleName = null;
    myKey = null;
    myValue = value;
  }

  public StringDescriptor(final String bundleName, final String key) {
    if (bundleName == null) {
      throw new IllegalArgumentException("bundleName cannot be null");
    }
    if (key == null) {
      throw new IllegalArgumentException("key cannot be null");
    }
    myBundleName = bundleName.replace('.', '/');
    myKey = key;
    myValue = null;
  }

  /**
   * Creates "trivial" StringDescriptor.
   */
  public static StringDescriptor create(final String value){
    return value != null ? new StringDescriptor(value) : null;
  }

  /**
   * @return not {@code null} value if this is "trivial" StringDescriptor.
   * If StringDescriptor is "trivial" then {@link #getBundleName()} and {@link #getKey()}
   * return {@code null}.
   */
  public String getValue(){
    return myValue;
  }

  /**
   * @return not {@code null} value if this is non "trivial" StringDescriptor.
   */
  public String getBundleName() {
    return myBundleName;
  }

  public String getDottedBundleName() {
    return myBundleName == null ? null : myBundleName.replace('/', '.');
  }

  /**
   * @return not {@code null} value if this is non "trivial" StringDescriptor.
   */
  public String getKey() {
    return myKey;
  }

  /**
   * @return can be null
   */
  public String getResolvedValue() {
    return myResolvedValue;
  }

  /**
   * @param resolvedValue can be null
   */
  public void setResolvedValue(final String resolvedValue) {
    myResolvedValue = resolvedValue;
  }

  public boolean isNoI18n() {
    return myNoI18n;
  }

  public void setNoI18n(final boolean noI18n) {
    myNoI18n = noI18n;
  }

  public boolean equals(final Object o) {
    if (this == o) return true;
    if (!(o instanceof StringDescriptor)) return false;

    final StringDescriptor descriptor = (StringDescriptor)o;

    if (myBundleName != null ? !myBundleName.equals(descriptor.myBundleName) : descriptor.myBundleName != null) return false;
    if (myKey != null ? !myKey.equals(descriptor.myKey) : descriptor.myKey != null) return false;
    if (myValue != null ? !myValue.equals(descriptor.myValue) : descriptor.myValue != null) return false;
    if (myNoI18n != descriptor.myNoI18n) return false;

    return true;
  }

  public int hashCode() {
    int result;
    result = (myBundleName != null ? myBundleName.hashCode() : 0);
    result = 29 * result + (myKey != null ? myKey.hashCode() : 0);
    result = 29 * result + (myValue != null ? myValue.hashCode() : 0);
    return result;
  }

  public String toString() {
    if (myValue != null) {
      return "[StringDescriptor:" + myValue + "]";
    }
    return "[StringDescriptor" + myBundleName + ":" + myKey + "]";
  }
}
