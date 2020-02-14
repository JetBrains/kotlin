import Preference.OnPreferenceClickListener

class Foo {
    fun foo(l: Preference, pm: Preference) {
        l.onPreferenceClickListener = OnPreferenceClickListener { p: Preference? -> true }
        pm.onPreferenceClickListener = OnPreferenceClickListener { true }
    }

    private fun bar(l: Preference) {
        l.onPreferenceClickListener = OnPreferenceClickListener { p: Preference? -> true }
    }
}
