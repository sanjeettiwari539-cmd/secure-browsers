package com.securebrowser

import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.ProgressBar

class BrowserChromeClient(
    private val progressBar: ProgressBar,
    private val onTitleReceived: (String) -> Unit
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        progressBar.progress = newProgress
        progressBar.visibility = if (newProgress < 100) View.VISIBLE else View.GONE
    }

    override fun onReceivedTitle(view: WebView, title: String) {
        super.onReceivedTitle(view, title)
        onTitleReceived(title)
    }
}
