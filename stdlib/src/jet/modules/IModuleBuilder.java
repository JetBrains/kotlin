package jet.modules;

import java.util.List;

/**
 * @author yole
 */
public interface IModuleBuilder {
    List<String> getSourceFiles();
    List<String> getClasspathRoots();
}
