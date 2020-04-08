// "Remove 1st parameter from method 'foo'" "true"

public class J {
    void foo() {
        new K().foo(<caret>);
    }
}