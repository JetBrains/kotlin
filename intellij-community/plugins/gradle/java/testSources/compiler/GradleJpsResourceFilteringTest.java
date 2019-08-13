// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.compiler;

import org.junit.Test;

/**
 * @author Vladislav.Soroka
 */
public class GradleJpsResourceFilteringTest extends GradleJpsCompilingTestCase {

  @Test
  public void testHeadFilter() throws Exception {
    createProjectSubFile(
      "src/main/resources/dir/file.txt", "1 Header\n" +
                                         "2\n" +
                                         "3 another text\n" +
                                         "4\n" +
                                         "5 another text \n" +
                                         "6 another text @token@ another text\n" +
                                         "7\n" +
                                         "8 Footer");
    importProject(
      "apply plugin: 'java'\n" +
      "\n" +
      "import org.apache.tools.ant.filters.*\n" +
      "processResources {\n" +
      "  filter(HeadFilter, lines:3, skip:2)\n" +
      "}"
    );
    assertModules("project", "project.main", "project.test");
    compileModules("project.main");

    assertCopied("out/production/resources/dir/file.txt", "3 another text\n" +
                                                      "4\n" +
                                                      "5 another text \n");
  }

  @Test
  public void testHeadFilter_MergedProject() throws Exception {
    createProjectSubFile(
      "src/main/resources/dir/file.txt", "1 Header\n" +
                                         "2\n" +
                                         "3 another text\n" +
                                         "4\n" +
                                         "5 another text \n" +
                                         "6 another text @token@ another text\n" +
                                         "7\n" +
                                         "8 Footer");
    importProjectUsingSingeModulePerGradleProject(
      "apply plugin: 'java'\n" +
      "\n" +
      "import org.apache.tools.ant.filters.*\n" +
      "processResources {\n" +
      "  filter(HeadFilter, lines:3, skip:2)\n" +
      "}"
    );

    assertModules("project");
    compileModules("project");

    assertCopied("out/production/resources/dir/file.txt", "3 another text\n" +
                                                      "4\n" +
                                                      "5 another text \n");
  }

  @Test
  public void testReplaceTokensFilter() throws Exception {
    createProjectSubFile(
      "src/main/resources/dir/file.txt", "1 Header\n" +
                                         "2\n" +
                                         "3 #token1#another text\n" +
                                         "4\n" +
                                         "5 another text \n" +
                                         "6 another text #token2# another text\n" +
                                         "7\n" +
                                         "8 Footer");
    importProject(
      "apply plugin: 'java'\n" +
      "\n" +
      "import org.apache.tools.ant.filters.*\n" +
      "processResources {\n" +
      "  filter(ReplaceTokens, tokens:[token1:'<11111>', token2:'<2222>'], beginToken: '#', endToken: '#')\n" +
      "}"
    );
    assertModules("project", "project.main", "project.test");
    compileModules("project.main");

    assertCopied("out/production/resources/dir/file.txt", "1 Header\n" +
                                                      "2\n" +
                                                      "3 <11111>another text\n" +
                                                      "4\n" +
                                                      "5 another text \n" +
                                                      "6 another text <2222> another text\n" +
                                                      "7\n" +
                                                      "8 Footer");
  }

  @Test
  public void testReplaceTokensFilter_MergedProject() throws Exception {
    createProjectSubFile(
      "src/main/resources/dir/file.txt", "1 Header\n" +
                                         "2\n" +
                                         "3 #token1#another text\n" +
                                         "4\n" +
                                         "5 another text \n" +
                                         "6 another text #token2# another text\n" +
                                         "7\n" +
                                         "8 Footer");
    importProjectUsingSingeModulePerGradleProject(
      "apply plugin: 'java'\n" +
      "\n" +
      "import org.apache.tools.ant.filters.*\n" +
      "processResources {\n" +
      "  filter(ReplaceTokens, tokens:[token1:'<11111>', token2:'<2222>'], beginToken: '#', endToken: '#')\n" +
      "}"
    );
    assertModules("project");
    compileModules("project");

    assertCopied("out/production/resources/dir/file.txt", "1 Header\n" +
                                                      "2\n" +
                                                      "3 <11111>another text\n" +
                                                      "4\n" +
                                                      "5 another text \n" +
                                                      "6 another text <2222> another text\n" +
                                                      "7\n" +
                                                      "8 Footer");
  }

  @Test
  public void testRenameFilter() throws Exception {
    createProjectSubFile("src/main/resources/dir/file.txt");
    importProject(
      "apply plugin: 'java'\n" +
      "\n" +
      "import org.apache.tools.ant.filters.*\n" +
      "processResources {\n" +
      "  rename 'file.txt', 'file001.txt'\n" +
      "}"
    );
    assertModules("project", "project.main", "project.test");
    compileModules("project.main");

    assertCopied("out/production/resources/dir/file001.txt");
  }

  @Test
  public void testRenameFilter_MergedProject() throws Exception {
    createProjectSubFile("src/main/resources/dir/file.txt");
    importProjectUsingSingeModulePerGradleProject(
      "apply plugin: 'java'\n" +
      "\n" +
      "import org.apache.tools.ant.filters.*\n" +
      "processResources {\n" +
      "  rename 'file.txt', 'file001.txt'\n" +
      "}"
    );
    assertModules("project");
    compileModules("project");

    assertCopied("out/production/resources/dir/file001.txt");
  }

  @Test
  public void testExpandPropertiesFilter() throws Exception {
    createProjectSubFile(
      "src/main/resources/dir/file.txt", "some text ${myProp} another text");
    importProject(
      "apply plugin: 'java'\n" +
      "\n" +
      "import org.apache.tools.ant.filters.*\n" +
      "ant.project.setProperty('myProp', 'myPropValue')\n" +
      "processResources {\n" +
      "  filter (ExpandProperties, project: ant.project)\n" +
      "}"
    );
    assertModules("project", "project.main", "project.test");
    compileModules("project.main");

    assertCopied("out/production/resources/dir/file.txt", "some text myPropValue another text");
  }

  @Test
  public void testExpandPropertiesFilter_MergedProject() throws Exception {
    createProjectSubFile(
      "src/main/resources/dir/file.txt", "some text ${myProp} another text");
    importProjectUsingSingeModulePerGradleProject(
      "apply plugin: 'java'\n" +
      "\n" +
      "import org.apache.tools.ant.filters.*\n" +
      "ant.project.setProperty('myProp', 'myPropValue')\n" +
      "processResources {\n" +
      "  filter (ExpandProperties, project: ant.project)\n" +
      "}"
    );
    assertModules("project");
    compileModules("project");

    assertCopied("out/production/resources/dir/file.txt", "some text myPropValue another text");
  }

  @Test
  public void testEscapeUnicodeFilter() throws Exception {
    createProjectSubFile(
      "src/main/resources/dir/file.txt", "some text テキスト");
    importProject(
      "apply plugin: 'java'\n" +
      "\n" +
      "import org.apache.tools.ant.filters.*\n" +
      "processResources {\n" +
      "  filter (EscapeUnicode)\n" +
      "}"
    );
    assertModules("project", "project.main", "project.test");
    compileModules("project.main");

    assertCopied("out/production/resources/dir/file.txt", "some text \\u30c6\\u30ad\\u30b9\\u30c8");
  }

  @Test
  public void testFiltersChain() throws Exception {
    createProjectSubFile(
      "src/main/resources/dir/file.txt", "1 Header\n" +
                                         "2\n" +
                                         "3 another text@token1@\n" +
                                         "4\n" +
                                         "5 another text \n" +
                                         "6 another text @token2@ another text\n" +
                                         "7\n" +
                                         "8 Footer");
    importProject(
      "apply plugin: 'java'\n" +
      "\n" +
      "import org.apache.tools.ant.filters.*\n" +
      "processResources {\n" +
      "  filter(HeadFilter, lines:4, skip:2)\n" +
      "  filter(ReplaceTokens, tokens:[token1:'<11111>', token2:'<2222>'])\n" +
      "  rename 'file.txt', 'file001.txt'\n" +
      "}"
    );
    assertModules("project", "project.main", "project.test");
    compileModules("project.main");

    assertCopied("out/production/resources/dir/file001.txt", "3 another text<11111>\n" +
                                                         "4\n" +
                                                         "5 another text \n" +
                                                         "6 another text <2222> another text");
  }

  @Test
  public void testFiltersChain_MergedProject() throws Exception {
    createProjectSubFile(
      "src/main/resources/dir/file.txt", "1 Header\n" +
                                         "2\n" +
                                         "3 another text@token1@\n" +
                                         "4\n" +
                                         "5 another text \n" +
                                         "6 another text @token2@ another text\n" +
                                         "7\n" +
                                         "8 Footer");
    importProjectUsingSingeModulePerGradleProject(
      "apply plugin: 'java'\n" +
      "\n" +
      "import org.apache.tools.ant.filters.*\n" +
      "processResources {\n" +
      "  filter(HeadFilter, lines:4, skip:2)\n" +
      "  filter(ReplaceTokens, tokens:[token1:'<11111>', token2:'<2222>'])\n" +
      "  rename 'file.txt', 'file001.txt'\n" +
      "}"
    );
    assertModules("project");
    compileModules("project");

    assertCopied("out/production/resources/dir/file001.txt", "3 another text<11111>\n" +
                                                         "4\n" +
                                                         "5 another text \n" +
                                                         "6 another text <2222> another text");
  }
}