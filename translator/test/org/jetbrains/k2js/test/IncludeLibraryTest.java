package org.jetbrains.k2js.test;

import java.util.Arrays;
import java.util.List;

/**
 * @author Talanov Pavel
 */
public abstract class IncludeLibraryTest extends TranslationTest {

    @Override
    protected List<String> generateFilenameList(String inputFile) {
        return Arrays.asList(kotlinLibraryPath(), inputFile);
    }

}
