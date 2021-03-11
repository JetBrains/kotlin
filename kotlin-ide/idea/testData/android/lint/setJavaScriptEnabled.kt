// INSPECTION_CLASS: com.android.tools.idea.lint.AndroidLintSetJavaScriptEnabledInspection

import android.annotation.SuppressLint
import android.app.Activity
import android.webkit.WebView

@Suppress("UsePropertyAccessSyntax", "UNUSED_VARIABLE", "unused", "UNUSED_PARAMETER", "DEPRECATION")
public class HelloWebApp : Activity() {

    fun test(webView: WebView) {
        <warning descr="Using `setJavaScriptEnabled` can introduce XSS vulnerabilities into your application, review carefully.">webView.settings.javaScriptEnabled</warning> = true // bad
        <warning descr="Using `setJavaScriptEnabled` can introduce XSS vulnerabilities into your application, review carefully.">webView.getSettings().setJavaScriptEnabled(true)</warning> // bad
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