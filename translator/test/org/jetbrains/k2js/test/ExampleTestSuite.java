package org.jetbrains.k2js.test;

import com.intellij.testFramework.UsefulTestCase;
import junit.framework.Test;
import org.jetbrains.annotations.NotNull;

public final class ExampleTestSuite extends UsefulTestCase {

    public static Test suite() {
        return Suite.suiteForDirectory("examples/", new Suite.SingleFileTester() {
            @Override
            public void performTest(@NotNull Suite test, @NotNull String filename) throws Exception {
                test.testFunctionOutput(filename, "Anonymous", "box", "OK");
            }
        });
    }
}
