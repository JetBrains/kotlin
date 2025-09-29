package org.jetbrains.kotlin.maven;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;

@RunWith(Parameterized.class)
public class IncrementalCompilationIT extends MavenITBase {
    private final MavenProject.ExecutionStrategy executionStrategy;

    @Parameterized.Parameters
    public static Object[][] data() {
        return new Object[][] {
                {MavenProject.ExecutionStrategy.IN_PROCESS},
                {MavenProject.ExecutionStrategy.DAEMON},
        };
    }

    public IncrementalCompilationIT(MavenProject.ExecutionStrategy executionStrategy) {
        this.executionStrategy = executionStrategy;
    }

    @Test
    public void testSimpleCompile() throws Exception {
        MavenProject project = new MavenProject("kotlinSimple");
        project.exec(executionStrategy, "package", "-X")
               .succeeded()
               .filesExist(kotlinSimpleOutputPaths())
               .compiledKotlin("src/main/kotlin/A.kt", "src/main/kotlin/useA.kt", "src/main/kotlin/Dummy.kt");
    }

    @NotNull
    private String[] kotlinSimpleOutputPaths() {
        return new String[]{
            "target/classes/test.properties",
            "target/classes/A.class",
            "target/classes/UseAKt.class",
            "target/classes/Dummy.class",
            "target/classes/JavaUtil.class",
            "target/classes/JavaAUser.class"
        };
    }

    @Test
    public void testNoChanges() throws Exception {
        MavenProject project = new MavenProject("kotlinSimple");
        project.exec(executionStrategy, "package");

        project.exec(executionStrategy, "package", "-X")
               .succeeded()
               .filesExist(kotlinSimpleOutputPaths())
               .compiledKotlin();
    }

    @Test
    public void testCompileError() throws Exception {
        MavenProject project = new MavenProject("kotlinSimple");
        project.exec(executionStrategy, "package");

        File aKt = project.file("src/main/kotlin/A.kt");
        String original = "class A";
        String replacement = "private class A";
        MavenTestUtils.replaceFirstInFile(aKt, original, replacement);

        project.exec(executionStrategy, "package")
               .failed()
               .contains("Cannot access 'class A : Any': it is private in file");

        MavenTestUtils.replaceFirstInFile(aKt, replacement, original);
        project.exec(executionStrategy, "package", "-X")
               .succeeded()
               .filesExist(kotlinSimpleOutputPaths())
               .compiledKotlin("src/main/kotlin/A.kt", "src/main/kotlin/useA.kt");

    }

    @Test
    public void testFunctionVisibilityChanged() throws Exception {
        MavenProject project = new MavenProject("kotlinSimple");
        project.exec(executionStrategy, "package");

        File aKt = project.file("src/main/kotlin/A.kt");
        MavenTestUtils.replaceFirstInFile(aKt, "fun foo", "internal fun foo");

        project.exec(executionStrategy, "package", "-X")
               .succeeded()
               .filesExist(kotlinSimpleOutputPaths())
               .compiledKotlin("src/main/kotlin/A.kt", "src/main/kotlin/useA.kt");

        // todo rebuild and compare output
    }

    @Test
    public void testJavaChanged() throws Exception {
        MavenProject project = new MavenProject("kotlinSimple");
        project.exec(executionStrategy, "package");

        File aKt = project.file("src/main/java/JavaUtil.java");
        MavenTestUtils.replaceFirstInFile(aKt, "CONST = 0", "CONST = 1");

        project.exec(executionStrategy, "package", "-X")
                .succeeded()
                .filesExist(kotlinSimpleOutputPaths())
                .compiledKotlin("src/main/kotlin/A.kt");
    }
}
