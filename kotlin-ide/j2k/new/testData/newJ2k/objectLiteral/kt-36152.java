public class Foo {
    public void foo(Preference l, Preference pm) {
        l.setOnPreferenceClickListener((p) -> true);

        pm.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                return true;
            }
        });
    }

    private void bar(Preference l) {
        l.setOnPreferenceClickListener((p) -> true);
    }
}