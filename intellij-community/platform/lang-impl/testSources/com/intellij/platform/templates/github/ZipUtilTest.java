package com.intellij.platform.templates.github;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.util.io.TestFileSystemBuilder;
import junit.framework.Assert;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.ZipInputStream;

import static com.intellij.util.io.TestFileSystemBuilder.fs;

/**
 * @author Sergey Simonchik
 */
public class ZipUtilTest {

  @Test
  public void testSimpleUnzip() throws Exception {
    File tempDir = FileUtil.createTempDirectory("unzip-test-", null);
    File simpleZipFile = new File(getZipParentDir(), "simple.zip");
    try (ZipInputStream stream = new ZipInputStream(new FileInputStream(simpleZipFile))) {
      ZipUtil.unzip(null, tempDir, stream, null, null, true);
      checkFileStructure(tempDir,
                         fs()
                           .file("a.txt")
                           .dir("dir").file("b.txt"));
    }
  }

  @Test
  public void testSingleRootDirUnzip() throws Exception {
    File tempDir = FileUtil.createTempDirectory("unzip-test-", null);
    File simpleZipFile = new File(getZipParentDir(), "single-root-dir-archive.zip");
    try (ZipInputStream stream = new ZipInputStream(new FileInputStream(simpleZipFile))) {
      ZipUtil.unzip(null, tempDir, stream, null, null, true);
      checkFileStructure(tempDir,
                         fs()
                           .file("a.txt")
                           .dir("dir").file("b.txt"));
    }
  }

  @Test
  public void testSimpleUnzipUsingFile() throws Exception {
    File tempDir = FileUtil.createTempDirectory("unzip-test-", null);
    File simpleZipFile = new File(getZipParentDir(), "simple.zip");
    ZipUtil.unzip(null, tempDir, simpleZipFile, null, null, true);
    checkFileStructure(tempDir,
                       fs()
                         .file("a.txt")
                         .dir("dir").file("b.txt"));
  }

  @Test
  public void testSingleRootDirUnzipUsingFile() throws Exception {
    File tempDir = FileUtil.createTempDirectory("unzip-test-", null);
    File simpleZipFile = new File(getZipParentDir(), "single-root-dir-archive.zip");
    ZipUtil.unzip(null, tempDir, simpleZipFile, null, null, true);
    checkFileStructure(tempDir,
                       fs()
                         .file("a.txt")
                         .dir("dir").file("b.txt"));
  }

  @Test
  public void testExpectedFailureOnBrokenZipArchive() throws Exception {
    File tempDir = FileUtil.createTempDirectory("unzip-test-", null);
    File file = new File(getZipParentDir(), "invalid-archive.zip");
    try {
      ZipUtil.unzip(null, tempDir, file, null, null, true);
      Assert.fail("Zip archive is broken, but it was unzipped without exceptions.");
    }
    catch (IOException e) {
      // expected exception
    }
  }

  private static void checkFileStructure(@NotNull File parentDir, @NotNull TestFileSystemBuilder expected) {
    expected.build().assertDirectoryEqual(parentDir);
  }

  @NotNull
  private static File getZipParentDir() {
    File communityDir = new File(PlatformTestUtil.getCommunityPath().replace(File.separatorChar, '/'));
    return new File(communityDir, "platform/lang-impl/testData/platform/templates/github");
  }

}
