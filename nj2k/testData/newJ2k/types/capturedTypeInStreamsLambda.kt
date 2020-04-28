import java.util.stream.Collectors

class Test {
    private fun addPackages(packages: Collection<Number>) {
        val sorted = packages.stream()
            .filter { repoPackage: Number? -> repoPackage != null }
            .collect(Collectors.toList())
    }
}
