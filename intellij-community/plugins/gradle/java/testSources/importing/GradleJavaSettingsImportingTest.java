// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing;

import com.intellij.compiler.CompilerConfiguration;
import com.intellij.compiler.CompilerConfigurationImpl;
import com.intellij.compiler.CompilerWorkspaceConfiguration;
import com.intellij.compiler.impl.javaCompiler.javac.JavacConfiguration;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.packaging.elements.PackagingElement;
import com.intellij.packaging.impl.elements.ArchivePackagingElement;
import com.intellij.packaging.impl.elements.ArtifactPackagingElement;
import com.intellij.packaging.impl.elements.ModuleOutputPackagingElement;
import org.jetbrains.jps.model.java.compiler.JpsJavaCompilerOptions;
import org.junit.Test;

import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public class GradleJavaSettingsImportingTest extends GradleSettingsImportingTestCase {
  @Test
  public void testCompilerConfigurationSettingsImport() throws Exception {

    importProject(
      withGradleIdeaExtPlugin(
        "idea {\n" +
        "  project.settings {\n" +
        "    compiler {\n" +
        "      resourcePatterns '!*.java;!*.class'\n" +
        "      clearOutputDirectory false\n" +
        "      addNotNullAssertions false\n" +
        "      autoShowFirstErrorInEditor false\n" +
        "      displayNotificationPopup false\n" +
        "      enableAutomake false\n" +
        "      parallelCompilation true\n" +
        "      rebuildModuleOnDependencyChange false\n" +
        "      javac {\n" +
        "        preferTargetJDKCompiler false\n" +
        "        javacAdditionalOptions '-Dkey=val'\n" +
        "        generateNoWarnings true\n" +
        "      }\n" +
        "    }\n" +
        "  }\n" +
        "}")
    );

    final CompilerConfigurationImpl compilerConfiguration = (CompilerConfigurationImpl)CompilerConfiguration.getInstance(myProject);
    final CompilerWorkspaceConfiguration workspaceConfiguration = CompilerWorkspaceConfiguration.getInstance(myProject);

    assertSameElements(compilerConfiguration.getResourceFilePatterns(), "!*.class", "!*.java");
    assertFalse(workspaceConfiguration.CLEAR_OUTPUT_DIRECTORY);
    assertFalse(compilerConfiguration.isAddNotNullAssertions());
    assertFalse(workspaceConfiguration.AUTO_SHOW_ERRORS_IN_EDITOR);
    assertFalse(workspaceConfiguration.DISPLAY_NOTIFICATION_POPUP);
    assertFalse(workspaceConfiguration.MAKE_PROJECT_ON_SAVE);
    assertTrue(workspaceConfiguration.PARALLEL_COMPILATION);
    assertFalse(workspaceConfiguration.REBUILD_ON_DEPENDENCY_CHANGE);

    final JpsJavaCompilerOptions javacOpts = JavacConfiguration.getOptions(myProject, JavacConfiguration.class);
    assertFalse(javacOpts.PREFER_TARGET_JDK_COMPILER);
    assertEquals("-Dkey=val", javacOpts.ADDITIONAL_OPTIONS_STRING);
    assertTrue(javacOpts.GENERATE_NO_WARNINGS);
  }

  @Test
  public void testArtifactsSettingsImport() throws Exception {
    importProject(
      withGradleIdeaExtPlugin(
        "import org.jetbrains.gradle.ext.*\n" +
        "idea {\n" +
        "  project.settings {\n" +
        "    ideArtifacts {\n" +
        "      myArt {\n" +
        "         file(\"build.gradle\")\n" +
        "      }\n" +
        "    }\n" +
        "  }\n" +
        "}"
      )
    );
    importProject();
    final ArtifactManager artifactManager = ArtifactManager.getInstance(myProject);

    final Artifact artifact = artifactManager.getArtifacts()[0];
    assertEquals("myArt", artifact.getName());
  }


  @Test
  public void testArtifactsReferenceImport() throws Exception {
    importProject(
      new GradleBuildScriptBuilderEx()
        .withGradleIdeaExtPlugin(IDEA_EXT_PLUGIN_VERSION)
        .addPostfix(
          "idea.project.settings {",
          "  ideArtifacts {",
          "    SomeArt {",
          "      archive(\"main.jar\") {",
          "        moduleOutput(idea.module.name)",
          "      }",
          "    }",
          "    ArtifactRef {",
          "      artifact('SomeArt')",
          "    }",
          "  }",
          "}"
        )
        .generate()
    );


    ArtifactManager artifactsManager = ArtifactManager.getInstance(myProject);
    Artifact artifact = artifactsManager.findArtifact("ArtifactRef");
    assertNotNull(artifact);
    List<PackagingElement<?>> children = artifact.getRootElement().getChildren();
    assertSize( 1, children);
    PackagingElement<?> element = children.get(0);
    assertInstanceOf(element, ArtifactPackagingElement.class);
    assertEquals("SomeArt", ((ArtifactPackagingElement)element).getArtifactName());
  }

  @Test
  public void testModuleReferenceImport() throws Exception {
    importProject(
      new GradleBuildScriptBuilderEx()
        .withGradleIdeaExtPlugin(IDEA_EXT_PLUGIN_VERSION)
        .addPostfix(
          "idea.project.settings {",
          "  ideArtifacts {",
          "    SomeArt {",
          "      archive(\"main.jar\") {",
          "        moduleOutput(idea.module.name)",
          "        moduleOutput(\"unknown_module\")",
          "      }",
          "    }",
          "  }",
          "}"
        )
        .generate()
    );


    ArtifactManager artifactsManager = ArtifactManager.getInstance(myProject);

    Artifact artifact = artifactsManager.findArtifact("SomeArt");
    assertNotNull(artifact);
    List<PackagingElement<?>> artifactChildren = artifact.getRootElement().getChildren();
    assertSize( 1, artifactChildren);
    PackagingElement<?> archive = artifactChildren.get(0);
    assertNotNull(archive);
    assertInstanceOf(archive, ArchivePackagingElement.class);
    List<PackagingElement<?>> archiveChildren = ((ArchivePackagingElement)archive).getChildren();
    assertSize( 2, archiveChildren);

    PackagingElement<?> moduleOutput1 = archiveChildren.get(0);
    assertInstanceOf(moduleOutput1, ModuleOutputPackagingElement.class);
    assertEquals("project", ((ModuleOutputPackagingElement)moduleOutput1).getModuleName());

    PackagingElement<?> moduleOutput2 = archiveChildren.get(1);
    assertInstanceOf(moduleOutput2, ModuleOutputPackagingElement.class);
    assertEquals("unknown_module", ((ModuleOutputPackagingElement)moduleOutput2).getModuleName());
  }
}
