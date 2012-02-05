package org.jetbrains.k2js.test;

/**
 * @author Pavel Talanov
 */
@SuppressWarnings("FieldCanBeLocal")
public abstract class JavaClassesTest extends TranslationTest {

    private final String SUITE = "java/";

    @Override
    protected String suiteDirectoryName() {
        return SUITE;
    }
}
