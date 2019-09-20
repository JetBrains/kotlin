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
package org.jetbrains.plugins.gradle.util;

import com.intellij.AbstractBundle;
import com.intellij.CommonBundle;
import com.intellij.reference.SoftReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.lang.ref.Reference;
import java.util.ResourceBundle;

/**
 * @author Vladislav.Soroka
 */
public class GradleDocumentationBundle extends AbstractBundle {

  public static String message(@NotNull @PropertyKey(resourceBundle = PATH_TO_BUNDLE) String key, @NotNull Object... params) {
    return BUNDLE.getMessage(key, params);
  }

  public static final String PATH_TO_BUNDLE = "i18n.GradleDocumentationBundle";
  private static final GradleDocumentationBundle BUNDLE = new GradleDocumentationBundle();
  private static Reference<ResourceBundle> ourBundle;

  public GradleDocumentationBundle() {
    super(PATH_TO_BUNDLE);
  }

  public static String messageOrDefault(@NotNull @PropertyKey(resourceBundle = PATH_TO_BUNDLE) String key,
                                        String defaultValue,
                                        @NotNull Object... params) {
    return CommonBundle.messageOrDefault(getBundle(), key, defaultValue, params);
  }

  private static ResourceBundle getBundle() {
    ResourceBundle bundle = SoftReference.dereference(ourBundle);
    if (bundle == null) {
      bundle = ResourceBundle.getBundle(PATH_TO_BUNDLE);
      ourBundle = new SoftReference<>(bundle);
    }
    return bundle;
  }
}
