//file
package test;
import java.io.File;

/**
 * User: ignatov
 */
public class Test {
    public static boolean isDir(File parent) {
        if (parent == null || !parent.exists()) {
            return false;
        }
        boolean result = true;
        if (parent.isDirectory()) {
            return true;
        } else
            return false;
    }
}