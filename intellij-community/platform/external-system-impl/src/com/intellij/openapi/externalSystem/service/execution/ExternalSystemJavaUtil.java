// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.execution;

import com.intellij.openapi.projectRoots.*;
import com.intellij.openapi.projectRoots.impl.JavaAwareProjectJdkTableImpl;
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * todo [Vlad, IDEA-187832]: move to `external-system-java` module
 */
@ApiStatus.Experimental
public class ExternalSystemJavaUtil {
  @Nullable
  static SdkType getJavaSdk() {
    try {
      return JavaSdk.getInstance();
    }
    catch (Throwable ignore) {
    }
    return null;
  }

  @Nullable
  static Sdk getInternalJdk() {
    ProjectJdkTable projectJdkTable = ProjectJdkTable.getInstance();
    try {
      if (projectJdkTable instanceof JavaAwareProjectJdkTableImpl) {
        return ((JavaAwareProjectJdkTableImpl)projectJdkTable).getInternalJdk();
      }
    }
    catch (Throwable ignore) {
      // todo [Vlad, IDEA-187832]: extract to `external-system-java` module
    }
    return null;
  }

  @Nullable
  static Sdk tryAddJdk(String homePath) {
    SdkType javaSdk = getJavaSdk();
    if (javaSdk == null) return null;
    Sdk jdk = ((JavaSdk)javaSdk).createJdk(javaSdk.suggestSdkName(null, homePath), homePath, !JdkUtil.checkForJdk(homePath));
    SdkConfigurationUtil.addSdk(jdk);
    return jdk;
  }
}
