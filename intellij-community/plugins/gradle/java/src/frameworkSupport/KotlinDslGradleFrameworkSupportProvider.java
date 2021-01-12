// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.frameworkSupport;

import com.intellij.openapi.extensions.ExtensionPointName;

abstract public class KotlinDslGradleFrameworkSupportProvider extends GradleFrameworkSupportProvider {
  public static final ExtensionPointName<GradleFrameworkSupportProvider> EP_NAME =
    ExtensionPointName.create("org.jetbrains.plugins.gradle.kotlinDslFrameworkSupport");
}
