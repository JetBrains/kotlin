public class Deprecation {
    @Deprecated() public void deprecate00() {}
    @Deprecated(forRemoval = false) public void deprecate10() {}
    @Deprecated(forRemoval = true) public void deprecate20() {}
    @Deprecated(since = "2.0") public void deprecate01() {}
    @Deprecated(forRemoval = true, since = "2.0") public void deprecate21() {}
}
