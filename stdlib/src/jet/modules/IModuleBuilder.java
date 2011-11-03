package jet.modules;

import java.util.List;

/**
 * @author yole
 */
public interface IModuleBuilder {
    String getModuleName();
    List<String> getSourceFiles();
    List<String> getClasspathRoots();
    String getJarName();
}
