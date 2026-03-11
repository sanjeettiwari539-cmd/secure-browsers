package com.securebrowser

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var etUrl: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var incognitoBanner: LinearLayout
    private lateinit var ivLock: ImageView
    private lateinit var btnRefresh: ImageButton
    private lateinit var btnBottomBack: ImageButton
    private lateinit var btnBottomForward: ImageButton
    private lateinit var btnIncognito: ImageButton

    private var isIncognito = false
    private var isDarkMode = false
    private val adBlocker = AdBlocker()

    companion object {
        private const val HOME_URL = "https://duckduckgo.com"
        private const val PERM_WRITE = 101
        private const val KEY_URL = "saved_url"
        private const val KEY_DARK = "is_dark_mode"
        private const val KEY_INCOGNITO = "is_incognito"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Restore state
        isDarkMode = savedInstanceState?.getBoolean(KEY_DARK, false) ?: false
        isIncognito = savedInstanceState?.getBoolean(KEY_INCOGNITO, false) ?: false

        initViews()
        setupWebView()
        setupUrlBar()
        setupTopButtons()
        setupBottomButtons()
        setupBackPress()

        // Restore incognito UI state after recreation
        if (isIncognito) applyIncognitoUI(true)

        // Determine URL to load
        val restoredUrl = savedInstanceState?.getString(KEY_URL)
        val intentUrl = intent?.data?.toString()
        val urlToLoad = restoredUrl ?: intentUrl ?: HOME_URL
        loadUrl(urlToLoad)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_URL, webView.url ?: HOME_URL)
        outState.putBoolean(KEY_DARK, isDarkMode)
        outState.putBoolean(KEY_INCOGNITO, isIncognito)
    }

    private fun initViews() {
        webView = findViewById(R.id.webView)
        etUrl = findViewById(R.id.etUrl)
        progressBar = findViewById(R.id.progressBar)
        incognitoBanner = findViewById(R.id.incognitoBanner)
        ivLock = findViewById(R.id.ivLock)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnBottomBack = findViewById(R.id.btnBottomBack)
        btnBottomForward = findViewById(R.id.btnBottomForward)
        btnIncognito = findViewById(R.id.btnIncognito)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = !isIncognito
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.setSupportZoom(true)
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.loadsImagesAutomatically = true
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.safeBrowsingEnabled = true
        }
        settings.cacheMode = if (isIncognito) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT

        CookieManager.getInstance().apply {
            setAcceptCookie(!isIncognito)
            setAcceptThirdPartyCookies(webView, false)
        }

        webView.webViewClient = BrowserWebViewClient(
            adBlocker = adBlocker,
            onPageStarted = { url -> runOnUiThread { onPageStarted(url) } },
            onPageFinished = { url -> runOnUiThread { onPageFinished(url) } }
        )

        webView.webChromeClient = BrowserChromeClient(
            progressBar = progressBar,
            onTitleReceived = { }
        )

        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            handleDownload(url, userAgent, contentDisposition, mimeType)
        }
    }

    private fun setupUrlBar() {
        etUrl.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                val input = etUrl.text.toString().trim()
                if (input.isNotEmpty()) {
                    loadUrl(processInput(input))
                }
                hideKeyboard()
                true
            } else false
        }
        etUrl.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) etUrl.selectAll()
        }
    }

    private fun setupTopButtons() {
        btnRefresh.setOnClickListener {
            if (webView.progress < 100) {
                webView.stopLoading()
                progressBar.visibility = View.GONE
            } else {
                webView.reload()
            }
        }
        findViewById<ImageButton>(R.id.btnMenu).setOnClickListener { showBrowserMenu(it) }
    }

    private fun setupBottomButtons() {
        btnBottomBack.setOnClickListener {
            if (webView.canGoBack()) webView.goBack()
        }
        btnBottomForward.setOnClickListener {
            if (webView.canGoForward()) webView.goForward()
        }
        findViewById<ImageButton>(R.id.btnHome).setOnClickListener {
            loadUrl(HOME_URL)
        }
        btnIncognito.setOnClickListener { toggleIncognito() }
        findViewById<ImageButton>(R.id.btnBottomMenu).setOnClickListener { showBrowserMenu(it) }
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun showBrowserMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menuInflater.inflate(R.menu.browser_menu, popup.menu)
        popup.menu.findItem(R.id.menu_dark_mode)?.title =
            if (isDarkMode) "Light Mode" else "Dark Mode"
        popup.menu.findItem(R.id.menu_incognito)?.title =
            if (isIncognito) "Exit Incognito" else "Incognito Mode"
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_dark_mode    -> { toggleDarkMode();    true }
                R.id.menu_incognito    -> { toggleIncognito();   true }
                R.id.menu_desktop_site -> { toggleDesktopSite(); true }
                R.id.menu_share        -> { shareCurrentUrl();   true }
                R.id.menu_copy_url     -> { copyCurrentUrl();    true }
                R.id.menu_clear_data   -> { showClearDataDialog(); true }
                else -> false
            }
        }
        popup.show()
    }

    private fun loadUrl(url: String) {
        webView.loadUrl(url)
        etUrl.setText(url)
    }

    private fun processInput(input: String): String {
        return when {
            input.startsWith("http://") || input.startsWith("https://") -> input
            input.matches(Regex("^[\\w.-]+\\.[a-zA-Z]{2,}(/.*)?$")) -> "https://$input"
            else -> "https://duckduckgo.com/?q=${Uri.encode(input)}"
        }
    }

    private fun onPageStarted(url: String) {
        etUrl.setText(url)
        updateLockIcon(url)
        updateNavButtonStates()
        // Show stop icon while loading
        btnRefresh.setImageResource(R.drawable.ic_stop)
    }

    private fun onPageFinished(url: String) {
        etUrl.setText(url)
        updateLockIcon(url)
        updateNavButtonStates()
        btnRefresh.setImageResource(R.drawable.ic_refresh)
        if (isIncognito) {
            CookieManager.getInstance().removeSessionCookies(null)
        }
    }

    private fun updateLockIcon(url: String) {
        when {
            url.startsWith("https://") -> {
                ivLock.setImageResource(R.drawable.ic_lock)
                ivLock.visibility = View.VISIBLE
            }
            url.startsWith("http://") -> {
                ivLock.setImageResource(R.drawable.ic_lock_open)
                ivLock.visibility = View.VISIBLE
            }
            else -> ivLock.visibility = View.GONE
        }
    }

    private fun updateNavButtonStates() {
        val canBack = webView.canGoBack()
        val canFwd = webView.canGoForward()
        btnBottomBack.alpha = if (canBack) 1.0f else 0.35f
        btnBottomForward.alpha = if (canFwd) 1.0f else 0.35f
        btnBottomBack.isEnabled = canBack
        btnBottomForward.isEnabled = canFwd
    }

    private fun applyIncognitoUI(active: Boolean) {
        incognitoBanner.visibility = if (active) View.VISIBLE else View.GONE
        btnIncognito.alpha = if (active) 1.0f else 0.6f
    }

    private fun toggleIncognito() {
        isIncognito = !isIncognito
        applyIncognitoUI(isIncognito)

        val cookieMgr = CookieManager.getInstance()
        if (isIncognito) {
            cookieMgr.setAcceptCookie(false)
            webView.settings.domStorageEnabled = false
            webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE
            webView.clearCache(true)
            Toast.makeText(this, "🔒 Incognito ON – No history saved", Toast.LENGTH_SHORT).show()
        } else {
            cookieMgr.setAcceptCookie(true)
            webView.settings.domStorageEnabled = true
            webView.settings.cacheMode = WebSettings.LOAD_DEFAULT
            webView.clearCache(true)
            webView.clearHistory()
            cookieMgr.removeAllCookies(null)
            Toast.makeText(this, "Incognito OFF", Toast.LENGTH_SHORT).show()
        }
    }

    // FIX: Save URL before dark mode toggle to restore after activity recreation
    private fun toggleDarkMode() {
        isDarkMode = !isDarkMode
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
        // Activity will recreate — URL is saved via onSaveInstanceState
    }

    private fun toggleDesktopSite() {
        val settings = webView.settings
        val isMobile = settings.userAgentString.contains("Mobile")
        if (isMobile) {
            settings.userAgentString =
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/121.0.0.0 Safari/537.36"
            Toast.makeText(this, "Desktop Site", Toast.LENGTH_SHORT).show()
        } else {
            settings.userAgentString = WebView(this).settings.userAgentString
            Toast.makeText(this, "Mobile Site", Toast.LENGTH_SHORT).show()
        }
        webView.reload()
    }

    private fun shareCurrentUrl() {
        val url = webView.url ?: return
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, url)
                    putExtra(Intent.EXTRA_SUBJECT, webView.title ?: url)
                }, "Share URL"
            )
        )
    }

    private fun copyCurrentUrl() {
        val url = webView.url ?: return
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("URL", url))
        Toast.makeText(this, "URL copied!", Toast.LENGTH_SHORT).show()
    }

    private fun showClearDataDialog() {
        AlertDialog.Builder(this)
            .setTitle("Clear Browsing Data")
            .setMessage("Clear cache, cookies, and history?")
            .setPositiveButton("Clear") { _, _ ->
                webView.clearCache(true)
                webView.clearHistory()
                CookieManager.getInstance().removeAllCookies(null)
                WebStorage.getInstance().deleteAllData()
                Toast.makeText(this, "✅ Data cleared", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun handleDownload(
        url: String, userAgent: String,
        contentDisposition: String, mimeType: String
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERM_WRITE
            )
            return
        }
        try {
            val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setMimeType(mimeType)
                addRequestHeader("User-Agent", userAgent)
                setTitle(fileName)
                setDescription("Downloading...")
                setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            }
            (getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
            Toast.makeText(this, "⬇️ Downloading: $fileName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(etUrl.windowToken, 0)
        etUrl.clearFocus()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
    }

    override fun onDestroy() {
        if (isIncognito) {
            webView.clearCache(true)
            webView.clearHistory()
            CookieManager.getInstance().removeAllCookies(null)
            WebStorage.getInstance().deleteAllData()
        }
        webView.stopLoading()
        webView.destroy()
        super.onDestroy()
    }
}
