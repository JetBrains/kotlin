package org.jetbrains.k2js.test;

/**
 * @author Talanov Pavel
 */
public abstract class AbstractExpressionTest extends IncludeLibraryTest {

    private final String SUITE = "expression/";

    @Override
    protected String suiteDirectoryName() {
        return SUITE;
    }
}
