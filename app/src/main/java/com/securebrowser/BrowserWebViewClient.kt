package com.securebrowser

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.ByteArrayInputStream

class BrowserWebViewClient(
    private val adBlocker: AdBlocker,
    private val onPageStarted: (String) -> Unit,
    private val onPageFinished: (String) -> Unit
) : WebViewClient() {

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        val url = request.url.toString()
        if (adBlocker.shouldBlock(url)) {
            // Return empty response for blocked resources
            return WebResourceResponse(
                "text/plain", "utf-8",
                ByteArrayInputStream(ByteArray(0))
            )
        }
        return super.shouldInterceptRequest(view, request)
    }

    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest
    ): Boolean {
        val url = request.url.toString()
        return when (request.url.scheme ?: "") {
            // http/https: let WebView handle natively (returning true + loadUrl = infinite loop bug)
            "http", "https" -> false
            // Non-web schemes: pass to system
            "mailto", "tel", "sms" -> {
                try {
                    view.context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                } catch (_: Exception) { }
                true
            }
            // Block unknown schemes silently
            else -> true
        }
    }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        onPageStarted(url)
    }

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
        onPageFinished(url)
    }

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceError
    ) {
        super.onReceivedError(view, request, error)
        // Only show error page for main frame failures, not sub-resource errors
        if (request.isForMainFrame) {
            val html = """
                <!DOCTYPE html><html><head>
                <meta name="viewport" content="width=device-width,initial-scale=1">
                <style>
                  body{font-family:sans-serif;text-align:center;padding:48px 24px;
                       background:#f8f9fa;color:#202124;}
                  h2{color:#d93025;margin-bottom:8px;}
                  p{color:#5f6368;margin:8px 0;}
                  small{color:#9aa0a6;}
                </style></head><body>
                <h2>⚠️ Page Not Available</h2>
                <p>This page could not be loaded.</p>
                <small>${request.url}</small><br><br>
                <small>Check your internet connection and try again.</small>
                </body></html>
            """.trimIndent()
            view.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        }
    }
}
