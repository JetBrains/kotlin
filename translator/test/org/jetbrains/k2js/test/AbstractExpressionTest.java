package org.jetbrains.k2js.test;

import java.util.Arrays;
import java.util.List;

/**
 * @author Talanov Pavel
 */
public abstract class AbstractExpressionTest extends TranslationTest {

    private final String SUITE = "expression/";

    @Override
    protected List<String> generateFilenameList(String inputFile) {
        return Arrays.asList(inputFile);
    }

    @Override
    protected String suiteDirectoryName() {
        return SUITE;
    }
}
