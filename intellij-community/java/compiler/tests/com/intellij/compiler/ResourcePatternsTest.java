package com.intellij.compiler;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * @author peter
 */
public class ResourcePatternsTest extends LightJavaCodeInsightFixtureTestCase {
  private String[] oldPatterns;

  @NotNull
  @Override
  protected LightProjectDescriptor getProjectDescriptor() {
    return new DefaultLightProjectDescriptor() {
      @Override
      public void configureModule(@NotNull Module module, @NotNull ModifiableRootModel model, @NotNull ContentEntry contentEntry) {
        try {
          contentEntry.clearSourceFolders();
          VirtualFile aaaBbb = contentEntry.getFile()
            .createChildDirectory(this, "aaa")
            .createChildDirectory(this, "bbb");
          contentEntry.addSourceFolder(aaaBbb, false);
          super.configureModule(module, model, contentEntry);
        }
        catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    };
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    oldPatterns = getConf().getResourceFilePatterns();
    getConf().removeResourceFilePatterns();
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      getConf().removeResourceFilePatterns();
      for (String pattern : oldPatterns) {
        getConf().addResourceFilePattern(pattern);
      }
    }
    catch (Throwable e) {
      addSuppressedException(e);
    }
    finally {
      super.tearDown();
    }
  }

  public void testFilePattern() {
    getConf().addResourceFilePattern("*.ttt");

    assertTrue(getConf().isResourceFile(createFile("foo/bar.ttt")));
    assertFalse(getConf().isResourceFile(createFile("foo/bar.xxx")));
  }

  public void testWildcards() {
    getConf().addResourceFilePattern("*.t?t");
    assertTrue(getConf().isResourceFile(createFile("foo/bar.txt")));
    assertTrue(getConf().isResourceFile(createFile("foo/bar.ttt")));
    assertTrue(getConf().isResourceFile(createFile("foo/bar.tyt")));
    assertFalse(getConf().isResourceFile(createFile("foo/bar.xml")));
  }

  public void testDirectory() {
    getConf().addResourceFilePattern("*/foo/*");
    assertTrue(getConf().isResourceFile(createFile("goo/foo/bar.ttt")));
    assertFalse(getConf().isResourceFile(createFile("goo/foo.zzz")));
    assertFalse(getConf().isResourceFile(createFile("foo/bar.ttt")));
  }

  public void testRootDirectory() {
    getConf().addResourceFilePattern("foo/*");
    assertFalse(getConf().isResourceFile(createFile("goo/foo/bar.ttt")));
    assertFalse(getConf().isResourceFile(createFile("goo/foo.zzz")));
    assertFalse(getConf().isResourceFile(createFile("goo/foo/zzz")));
    assertTrue(getConf().isResourceFile(createFile("foo/bar.ttt")));
  }

  public void testDoubleAsterisk() {
    getConf().addResourceFilePattern("**/foo/*");
    assertTrue(getConf().isResourceFile(createFile("foo/bar.ttt")));
    assertTrue(getConf().isResourceFile(createFile("goo/foo/bar.ttt")));
    assertFalse(getConf().isResourceFile(createFile("goo/foo.zzz")));
    assertTrue(getConf().isResourceFile(createFile("goo/foo/zzz")));
  }

  public void testResourceRoot() {
    getConf().addResourceFilePattern("bbb:*.ttt");

    assertTrue(getConf().isResourceFile(createFile("foo/bar.ttt")));
    assertFalse(getConf().isResourceFile(createFile("foo/bar.xxx")));
    assertFalse(getConf().isResourceFile(myFixture.addFileToProject("aaa/ccc/xxx.ttt", "").getVirtualFile()));
  }

  private VirtualFile createFile(String path) {
    return myFixture.addFileToProject("aaa/bbb/" + path, "").getVirtualFile();
  }

  private CompilerConfigurationImpl getConf() {
    return ((CompilerConfigurationImpl)(CompilerConfiguration.getInstance(getProject())));
  }

}
