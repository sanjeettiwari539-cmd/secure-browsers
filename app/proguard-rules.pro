# Keep WebView JS interface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.securebrowser.** { *; }
