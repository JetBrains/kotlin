package org.jetbrains.k2js.test;

import com.intellij.testFramework.UsefulTestCase;
import junit.framework.Test;
import org.jetbrains.annotations.NotNull;

/**
 * @author Pavel Talanov
 */
public final class SimpleTestSuite extends UsefulTestCase {

    public static Test suite() {
        return Suite.suiteForDirectory("simple/", new Suite.SingleFileTester() {
            @Override
            public void performTest(@NotNull Suite test, @NotNull String filename) throws Exception {
                test.testFooBoxIsTrue(filename);
            }
        });
    }
}
