// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.bootRuntime;

public interface BinTrayConfig {
  String subject = "jetbrains";
  String repoName = "intellij-jdk";
  String jbrRepoName = "intellij-jbr";
  String urlPattern = "https://dl.bintray.com/%s/%s/%s";
}
