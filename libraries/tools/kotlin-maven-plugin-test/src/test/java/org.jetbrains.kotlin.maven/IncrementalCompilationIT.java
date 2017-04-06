package org.jetbrains.kotlin.maven;

import org.junit.Test;
import java.io.File;

public class IncrementalCompilationIT extends MavenITBase {
    @Test
    public void testSimpleCompile() throws Exception {
        MavenProject project = new MavenProject("kotlinSimple");
        project.exec("package")
               .succeeded()
               .compiledKotlin("src/A.kt", "src/useA.kt", "src/Dummy.kt");
    }

    @Test
    public void testNoChanges() throws Exception {
        MavenProject project = new MavenProject("kotlinSimple");
        project.exec("package");

        project.exec("package")
               .succeeded()
               .compiledKotlin();
    }

    @Test
    public void testCompileError() throws Exception {
        MavenProject project = new MavenProject("kotlinSimple");
        project.exec("package");

        File aKt = project.file("src/A.kt");
        String original = "class A";
        String replacement = "private class A";
        MavenTestUtils.replaceFirstInFile(aKt, original, replacement);

        project.exec("package")
               .failed()
               .contains("Cannot access 'A': it is private in file");

        MavenTestUtils.replaceFirstInFile(aKt, replacement, original);
        project.exec("package")
               .succeeded()
               .compiledKotlin("src/A.kt", "src/useA.kt");

    }

    @Test
    public void testFunctionVisibilityChanged() throws Exception {
        MavenProject project = new MavenProject("kotlinSimple");
        project.exec("package");

        File aKt = project.file("src/A.kt");
        MavenTestUtils.replaceFirstInFile(aKt, "fun foo", "internal fun foo");

        project.exec("package")
               .succeeded()
               .compiledKotlin("src/A.kt", "src/useA.kt");

        // todo rebuild and compare output
    }
}
