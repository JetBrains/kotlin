package org.jetbrains.k2js.test;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import java.util.Arrays;
import java.util.List;

/**
 * @author Talanov Pavel
 */
public class KotlinLibTest extends TranslationTest {

    final private static String MAIN = "kotlin_lib/";

    @Override
    protected List<String> generateFilenameList(String inputFile) {
        return Arrays.asList(kotlinLibraryPath());
    }

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    @Test
    public void kotlinJsLibRunsWithRhino() throws Exception {
        Context context = Context.enter();
        Scriptable scope = context.initStandardObjects();
        runFileWithRhino(kotlinLibraryPath(), context, scope);
        Context.exit();
    }

}
