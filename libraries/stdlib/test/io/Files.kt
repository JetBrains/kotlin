package test.io

import org.junit.Test as test
import kotlin.test.assertEquals
import java.io.File
import java.io.IOException
import java.io.FileNotFoundException
import java.util.NoSuchElementException
import java.util.HashSet
import java.util.ArrayList

class FilesTest {
    test fun testCreateTempDir() {
        val dirSuf = System.currentTimeMillis().toString()
        val dir1 = createTempDir("temp", dirSuf)
        assert(dir1.exists() && dir1.isDirectory() && dir1.name.startsWith("temp") && dir1.name.endsWith(dirSuf))
        try {
            createTempDir("a")
            assert(false)
        } catch(e: IllegalArgumentException) {}

        val dir2 = createTempDir("temp")
        assert(dir2.exists() && dir2.isDirectory() && dir2.name.endsWith(".tmp"))

        val dir3 = createTempDir()
        assert(dir3.exists() && dir3.isDirectory())

        dir1.delete()
        dir2.delete()
        dir3.delete()
    }

    test fun testCreateTempFile() {
        val fileSuf = System.currentTimeMillis().toString()
        val file1 = createTempFile("temp", fileSuf)
        assert(file1.exists() && file1.name.startsWith("temp") && file1.name.endsWith(fileSuf))
        try {
            createTempFile("a")
            assert(false)
        } catch(e: IllegalArgumentException) {}
        val file2 = createTempFile("temp")
        assert(file2.exists() && file2.name.endsWith(".tmp"))

        val file3 = createTempFile()
        assert(file3.exists())

        file1.delete()
        file2.delete()
        file3.delete()
    }

    class Walks {
        fun createTestFiles(): File {
            val basedir = createTempDir()
            File(basedir, "1").mkdir()
            File(basedir, "1/2".separatorsToSystem()).mkdir()
            File(basedir, "1/3".separatorsToSystem()).mkdir()
            File(basedir, "1/3/4.txt".separatorsToSystem()).createNewFile()
            File(basedir, "1/3/5.txt".separatorsToSystem()).createNewFile()
            File(basedir, "6").mkdir()
            File(basedir, "7.txt").createNewFile()
            File(basedir, "8").mkdir()
            File(basedir, "8/9.txt".separatorsToSystem()).createNewFile()
            return basedir
        }

        test fun withFileVisitor() {
            val basedir = createTestFiles()
            try {
                val files = HashSet<String>()
                val dirs = HashSet<String>()
                val failed = HashSet<String>()
                val stack = ArrayList<File>()
                val fileVisitor = object : AbstractFileVisitor() {
                    override fun beforeVisitDirectory(dir: File): FileVisitResult {
                        stack.add(dir)
                        dirs.add(dir.relativeTo(basedir))
                        return FileVisitResult.CONTINUE
                    }

                    override fun afterVisitDirectory(dir: File): FileVisitResult {
                        assertEquals(stack.last(), dir)
                        stack.remove(stack.lastIndex)
                        return FileVisitResult.CONTINUE
                    }

                    override fun visitFile(file: File): FileVisitResult {
                        assert(stack.last().listFiles().contains(file), file)
                        files.add(file.relativeTo(basedir))
                        return FileVisitResult.CONTINUE
                    }

                    override fun visitDirectoryFailed(dir: File, e: IOException): FileVisitResult {
                        assertEquals(stack.last(), dir)
                        stack.remove(stack.lastIndex)
                        failed.add(dir.name)
                        return FileVisitResult.CONTINUE
                    }
                }
                basedir.walkFileTree(fileVisitor)
                assert(stack.isEmpty())
                val sep = File.separator
                for (fileName in array("", "1", "1${sep}2", "1${sep}3", "6", "8")) {
                    assert(dirs.contains(fileName), fileName)
                }
                for (fileName in array("1${sep}3${sep}4.txt", "1${sep}3${sep}4.txt",  "7.txt", "8${sep}9.txt")) {
                    assert(files.contains(fileName), fileName)
                }

                //limit maxDepth
                files.clear()
                dirs.clear()
                basedir.walkFileTree(fileVisitor, maxDepth = 1)
                assert(stack.isEmpty())
                assert(dirs.size() == 1 && dirs.contains(""), dirs.size())
                for (file in array("1", "6", "7.txt", "8")) {
                    assert(files.contains(file), file)
                }

                //restrict access
                if (File(basedir, "1").setReadable(false)) {
                    try {
                        files.clear()
                        dirs.clear()
                        basedir.walkFileTree(fileVisitor)
                        assert(stack.isEmpty())
                        assert(failed.size() == 1 && failed.contains("1"), failed.size())
                        assert(dirs.size() == 4, dirs.size())
                        for (dir in array("", "1", "6", "8")) {
                            assert(dirs.contains(dir), dir)
                        }
                        assert(files.size() == 2, files.size())
                        for (file in array("7.txt", "8${sep}9.txt")) {
                            assert(files.contains(file), file)
                        }
                    } finally {
                        File(basedir, "1").setReadable(true)
                    }
                } else {
                    System.err.println("cannot restrict access")
                }
            } finally {
                basedir.deleteRecursively()
            }
        }

        test fun parentsFirst() {
            val basedir = createTestFiles()
            try {
                val visited = HashSet<File>()
                val block: (File) -> Unit = {
                    assert(!visited.contains(it), it)
                    assert(it == basedir && visited.isEmpty() || visited.contains(it.getParentFile()), it)
                    visited.add(it)
                }
                basedir.walkSelectively {
                    block(it)
                    FileVisitResult.CONTINUE
                }
                assert(visited.size() == 10, visited.size())

                visited.clear()
                basedir.walkFileTree(block = block)
                assert(visited.size() == 10, visited.size())
            } finally {
                basedir.deleteRecursively()
            }
        }

        test fun restrictedAccess() {
            val basedir = createTestFiles()
            val restricted = File(basedir, "1")
            try {
                if (restricted.setReadable(false)) {
                    val visited = HashSet<File>()
                    val block: (File) -> Unit = {
                        assert(!visited.contains(it), it)
                        assert(it == basedir && visited.isEmpty() || visited.contains(it.getParentFile()), it)
                        visited.add(it)
                    }
                    basedir.walkSelectively {
                        block(it)
                        FileVisitResult.CONTINUE
                    }
                    assert(visited.size() == 6, visited.size())

                    visited.clear()
                    basedir.walkFileTree(block = block)
                    assert(visited.size() == 6, visited.size())
                }
            } finally {
                restricted.setReadable(true)
                basedir.deleteRecursively()
            }
        }

        test fun backup() {
            var count = 0
            fun makeBackup(file: File) {
                count++
                val bakFile = File(file.toString() + ".bak")
                file.copyTo(bakFile)
            }

            val basedir1 = createTestFiles()
            try {
                basedir1.walkSelectively {
                    if (it.isFile()) {
                        makeBackup(it)
                    }
                    FileVisitResult.CONTINUE
                }
                assert(count == 4)
            } finally {
                basedir1.deleteRecursively()
            }

            count = 0
            val basedir2 = createTestFiles()
            try {
                basedir2.walkFileTree {
                    if (it.isFile()) {
                        makeBackup(it)
                    }
                }
                assert(count == 4)
            } finally {
                basedir2.deleteRecursively()
            }
        }

        test fun find() {
            val basedir = createTestFiles()
            try {
                File(basedir, "8/4.txt".separatorsToSystem()).createNewFile()
                var count = 0
                basedir.walkSelectively {
                    if (it.name == "4.txt") {
                        count++
                        FileVisitResult.TERMINATE
                    } else {
                        FileVisitResult.CONTINUE
                    }
                }
                assert(count == 1)
            } finally {
                basedir.deleteRecursively()
            }
        }

        test fun skipSiblings() {
            val basedir = createTestFiles()
            try {
                File(basedir, "1/3/.git").mkdir()
                File(basedir, "1/2/.git").mkdir()
                File(basedir, "6/.git").mkdir()
                val found = HashSet<File>()
                basedir.walkSelectively {
                    assert(!found.contains(it.getParentFile()))
                    if (it.name == ".git") {
                        found.add(it.getParentFile())
                        FileVisitResult.SKIP_SIBLINGS
                    } else {
                        FileVisitResult.CONTINUE
                    }
                }
                assert(found.size() == 3)
            } finally {
                basedir.deleteRecursively()
            }
        }

        test fun streamFileTree() {
            val dir = createTempDir()
            try {
                val subDir1 = createTempDir(prefix = "d1_", directory = dir)
                val subDir2 = createTempDir(prefix = "d2_", directory = dir)
                createTempDir(prefix = "d1_", directory = subDir1)
                createTempFile(prefix = "f1_", directory = subDir1)
                createTempDir(prefix = "d1_", directory = subDir2)
                assertEquals(6, dir.streamFileTree().count())
            } finally {
                dir.deleteRecursively()
            }
            dir.mkdir()
            try {
                val it = dir.streamFileTree().iterator()
                it.next()
                it.next()
                assert(false)
            } catch(e: NoSuchElementException) {
            } finally {
                dir.delete()
            }
            try {
                dir.streamFileTree()
                assert(false)
            } catch(e: FileNotFoundException) {}
        }
    }

    test fun listFilesWithFilter() {
        val dir = createTempDir("temp")

        createTempFile("temp1", ".kt", dir)
        createTempFile("temp2", ".java", dir)
        createTempFile("temp3", ".kt", dir)

        val result = dir.listFiles { it.getName().endsWith(".kt") }
        assertEquals(2, result!!.size)
    }

    test fun relativeTo() {
        assertEquals("kotlin", File("src/kotlin".separatorsToSystem()).relativeTo(File("src")))
        assertEquals("", File("dir").relativeTo(File("dir")))
        assertEquals("..", File("dir").relativeTo(File("dir/subdir".separatorsToSystem())))
        assertEquals("../../test".separatorsToSystem(), File("test").relativeTo(File("dir/dir".separatorsToSystem())))

        val file1 = File("C:/dir1".separatorsToSystem())
        val file2 = File("D:/dir2".separatorsToSystem())
        try {
            val winRelPath = file1.relativeTo(file2)
            assert(file1.canonicalPath.charAt(0) == '/')
            assertEquals("../../C:/dir1", winRelPath)
        } catch (e: IllegalArgumentException) {
            assert(Character.isLetter(file1.canonicalPath.charAt(0)))
        }
    }

    test fun relativePath() {
        val file1 = File("src")
        val file2 = File(file1, "kotlin")
        val file3 = File("test")

        assertEquals("kotlin", file1.relativePath(file2))
        assertEquals("", file1.relativePath(file1))
        assertEquals(file3.canonicalPath, file1.relativePath(file3))
    }

    test fun extension() {
        assertEquals("bbb", File("aaa.bbb").extension)
        assertEquals("", File("aaa").extension)
        assertEquals("", File("aaa.").extension)
        // maybe we should think that such files have name .bbb and no extension
        assertEquals("bbb", File(".bbb").extension)
    }

    test fun nameWithoutExtension() {
        assertEquals("aaa", File("aaa.bbb").nameWithoutExtension)
        assertEquals("aaa", File("aaa").nameWithoutExtension)
        assertEquals("aaa", File("aaa.").nameWithoutExtension)
        assertEquals("", File(".bbb").nameWithoutExtension)
    }

    test fun separatorsToSystem() {
        var path = "/aaa/bbb/ccc"
        assertEquals(path.replace("/", File.separator), File(path).separatorsToSystem())

        path = "C:\\Program Files\\My Awesome Program"
        assertEquals(path.replace("\\", File.separator), File(path).separatorsToSystem())

        path = "/Libraries\\Java:/Libraries/Python:/Libraries/Ruby"
        assertEquals(path.replace(":", File.pathSeparator), path.pathSeparatorsToSystem())

        path = "/Libraries\\Java;/Libraries/Python;/Libraries/Ruby"
        assertEquals(path.replace(";", File.pathSeparator), path.pathSeparatorsToSystem())

        path = "/Libraries\\Java;/Libraries/Python:\\Libraries/Ruby"
        assertEquals(path.replace("/", File.separator).replace("\\", File.separator)
                .replace(":", File.pathSeparator).replace(";", File.pathSeparator), path.allSeparatorsToSystem())

        assertEquals("test", "test".allSeparatorsToSystem())
    }

    test fun testCopyTo() {
        val srcFile = createTempFile()
        val dstFile = createTempFile()
        srcFile.writeText("Hello, World!")
        try {
            srcFile.copyTo(dstFile)
            assert(false)
        } catch (e: FileAlreadyExistsException) {}

        var len = srcFile.copyTo(dstFile, rewrite = true)
        assertEquals(13L, len)
        assertEquals(srcFile.readText(), dstFile.readText())

        assert(dstFile.delete())
        len = srcFile.copyTo(dstFile)
        assertEquals(13L, len)
        assertEquals(srcFile.readText(), dstFile.readText())

        assert(dstFile.delete())
        dstFile.mkdir()
        val child = File(dstFile, "child")
        child.createNewFile()
        try {
            srcFile.copyTo(dstFile, rewrite = true)
            assert(false)
        } catch (e: DirectoryNotEmptyException) {}

        assert(srcFile.delete())
        assert(child.delete() && dstFile.delete())

        try {
            srcFile.copyTo(dstFile)
            assert(false)
        } catch (e: NoSuchFileException) {}

        srcFile.mkdir()
        try {
            srcFile.copyTo(dstFile)
            assert(false)
        } catch (e: FileIsDirectoryException) {}
        srcFile.delete()
    }

    test fun deleteRecursively() {
        val dir = createTempDir()
        dir.delete()
        dir.mkdir()
        val subDir = File(dir, "subdir");
        subDir.mkdir()
        File(dir, "test1.txt").createNewFile()
        File(subDir, "test2.txt").createNewFile()

        assert(dir.deleteRecursively())
        assert(!dir.exists())
        assert(!dir.deleteRecursively())
    }
}
