package org.jetbrains.k2js.test;

/**
 * @author Talanov Pavel
 */
public abstract class JavaClassesTest extends TranslationTest {
    private final String SUITE = "java/";

    @Override
    protected String suiteDirectoryName() {
        return SUITE;
    }
}
