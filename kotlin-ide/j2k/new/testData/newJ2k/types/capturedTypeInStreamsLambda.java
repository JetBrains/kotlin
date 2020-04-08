// RUNTIME_WITH_FULL_JDK

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class Test {
    private void addPackages(Collection<? extends Number> packages) {
        List<Number> sorted = packages.stream()
                .filter(repoPackage -> repoPackage != null)
                .collect(Collectors.toList());
    }
}