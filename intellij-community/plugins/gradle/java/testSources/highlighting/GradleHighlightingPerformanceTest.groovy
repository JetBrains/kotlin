// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.highlighting


import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.testFramework.EdtTestUtil
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.util.ThrowableRunnable
import org.junit.Test
import org.junit.runners.Parameterized

class GradleHighlightingPerformanceTest extends GradleHighlightingBaseTest {

  @SuppressWarnings("MethodOverridesStaticMethodOfSuperclass")
  @Parameterized.Parameters(name = "with Gradle-{0}")
  static Collection<Object[]> data() {
    return [[BASE_GRADLE_VERSION].toArray()]
  }

  @Test
  void testPerformance() throws Exception {
    def text = """
buildscript {
    repositories {
        mavenCentral()
    }

    dependencies {
        classpath 'io.github.http-builder-ng:http-builder-ng-apache:1.0.3'
    }
}
import groovyx.net.http.HttpBuilder

task bitbucketJenkinsTest() {
    doLast {
        def bitbucket = HttpBuilder.configure {
            request.uri = "https://127.0.0.1"
            request.auth.basic "", ""
        }
        
        bitbucket.post {
            request.uri.path = "/rest/api/"
            request.contentType = "a.json"
        }
    }
}"""
    VirtualFile file = createProjectSubFile "build.gradle", text

    importProject()

    def pos = text.indexOf("a.json")
    EdtTestUtil.runInEdtAndWait {
      fixture.openFileInEditor(file)
      fixture.editor.caretModel.moveToOffset(pos + 1)
      fixture.checkHighlighting()

      PlatformTestUtil.startPerformanceTest(getTestName(false), 6000, {
        fixture.psiManager.dropPsiCaches()
        'aaaa'.toCharArray().each {
          fixture.type it
          PsiDocumentManager.getInstance(fixture.project).commitAllDocuments()
          fixture.doHighlighting()
          fixture.completeBasic()
        }
      } as ThrowableRunnable).assertTiming()
    }
  }
}

