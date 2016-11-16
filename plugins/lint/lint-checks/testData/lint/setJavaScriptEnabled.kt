// INSPECTION_CLASS: org.jetbrains.android.inspections.klint.AndroidLintInspectionToolProvider$AndroidKLintSetJavaScriptEnabledInspection

import android.annotation.SuppressLint
import android.app.Activity
import android.webkit.WebView

@Suppress("UsePropertyAccessSyntax", "UNUSED_VARIABLE", "unused", "UNUSED_PARAMETER", "DEPRECATION")
public class HelloWebApp : Activity() {

    fun test(webView: WebView) {
        webView.settings.<warning descr="Using `setJavaScriptEnabled` can introduce XSS vulnerabilities into you application, review carefully.">javaScriptEnabled</warning> = true // bad
        webView.getSettings().<warning descr="Using `setJavaScriptEnabled` can introduce XSS vulnerabilities into you application, review carefully.">setJavaScriptEnabled(true)</warning> // bad
        webView.getSettings().setJavaScriptEnabled(false) // good
        webView.loadUrl("file:///android_asset/www/index.html")
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun suppressed(webView: WebView) {
        webView.getSettings().javaScriptEnabled = true; // bad
        webView.getSettings().setJavaScriptEnabled(true) // bad
        webView.getSettings().setJavaScriptEnabled(false); // good
        webView.loadUrl("file:///android_asset/www/index.html");
    }
}