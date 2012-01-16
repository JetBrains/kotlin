package jet.modules;

import java.util.List;

/**
 * @author yole
 */
public interface Module {
    String getModuleName();
    List<String> getSourceFiles();
    List<String> getClasspathRoots();
}
