// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.highlighting

import com.intellij.codeInspection.deadCode.UnusedDeclarationInspectionBase
import com.intellij.psi.PsiMethod
import org.jetbrains.plugins.groovy.codeInspection.GroovyUnusedDeclarationInspection
import org.junit.Test
import org.junit.runners.Parameterized

class GradleHighlightingTest extends GradleHighlightingBaseTest {

  /**
   * It's sufficient to run the test against one gradle version
   */
  @Parameterized.Parameters(name = "with Gradle-{0}")
  static Collection<Object[]> data() { [BASE_GRADLE_VERSION] }

  @Test
  void testConfiguration() throws Exception {
    importProject("")
    testHighlighting """
configurations.create("myConfiguration")
configurations.myConfiguration {
    transitive = false
}
"""
  }

  @Test
  void testConfigurationResolve() throws Exception {
    def result = testResolve """
configurations.create("myConfiguration")
configurations.myConfiguration {
    transitive = false
}
""", "transitive"
    assert result instanceof PsiMethod
    assert result.getName() == "setTransitive"
  }

  @Test
  void testGradleImplicitUsages() {
    // create dummy source file to have gradle buildSrc project imported
    createProjectSubFile("buildSrc/src/main/groovy/Dummy.groovy", "")
    importProject("")

    fixture.enableInspections(new GroovyUnusedDeclarationInspection(), new UnusedDeclarationInspectionBase(true))
    testHighlighting "buildSrc/src/main/groovy/org/buildsrc/GrTask.groovy", """
package org.buildsrc

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Console
import org.gradle.api.tasks.Destroys
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.LocalState
import org.gradle.api.tasks.OutputDirectories
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.file.FileCollection

class <warning>GrTask</warning> extends DefaultTask {
    @Input String inputString
    @InputFile File inputFile
    @InputFiles FileCollection inputFiles
    @InputDirectory File inputDirectory
    @OutputDirectory File outputDirectory
    @OutputDirectories FileCollection outputDirectories
    @OutputFile File outputFile
    @LocalState File localStateFile
    @Destroys File destroyedFile
    @Classpath FileCollection classpath
    @Console String consoleString

    @TaskAction
    private void action() {
    }
}
"""

    testHighlighting "buildSrc/src/main/java/org/buildsrc/JavaTask.java", """
package org.buildsrc;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.*;
import org.gradle.api.file.FileCollection;

import java.io.File;

public class JavaTask extends DefaultTask {
    private String inputString;
    private String unusedField;

    @TaskAction
    private void action() {
    }

    public String <warning>getUnusedField</warning>() {
        return unusedField;
    }

    public void <warning>setUnusedField</warning>(String unusedField) {
        this.unusedField = unusedField;
    }

    @Input
    public String getInputString() {
        return inputString;
    }

    public void setInputString(String inputString) {
        this.inputString = inputString;
    }

    @InputFile
    public File getInputFile() {
        return null;
    }

    @InputFiles
    public FileCollection getInputFiles() {
        return null;
    }

    @InputDirectory
    public File getInputDirectory() {
        return null;
    }

    @OutputDirectory
    public File getOutputDirectory() {
        return null;
    }

    @OutputDirectories
    public FileCollection getOutputDirectories() {
        return null;
    }

    @OutputFile
    public File getOutputFile() {
        return null;
    }

    @LocalState
    public File getLocalStateFile() {
        return null;
    }

    @Destroys
    public File getDestroyedFile() {
        return null;
    }

    @Classpath
    public FileCollection getClasspath() {
        return null;
    }

    @Console
    public String getConsoleString() {
        return null;
    }
}
"""
  }
}
