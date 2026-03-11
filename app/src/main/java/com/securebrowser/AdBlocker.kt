package com.securebrowser

class AdBlocker {

    private val blockedDomains = setOf(
        // Google Ads & Analytics
        "doubleclick.net", "googleadservices.com", "googlesyndication.com",
        "googletagmanager.com", "googletagservices.com", "google-analytics.com",
        "adservice.google.com", "pagead2.googlesyndication.com", "adtrafficquality.google",
        // Facebook/Meta trackers
        "connect.facebook.net", "an.facebook.com", "pixel.facebook.com",
        // Major ad networks
        "ads.yahoo.com", "advertising.com", "adcolony.com", "adnxs.com",
        "adroll.com", "adsafeprotected.com", "amazon-adsystem.com",
        "criteo.com", "rubiconproject.com", "pubmatic.com", "openx.net",
        "openx.com", "appnexus.com", "rlcdn.com", "taboola.com",
        "outbrain.com", "revcontent.com", "mgid.com", "media.net",
        // Analytics & tracking
        "scorecardresearch.com", "quantserve.com", "quantcast.com",
        "comscore.com", "omtrdc.net", "2mdn.net", "mixpanel.com",
        "hotjar.com", "fullstory.com", "logrocket.com", "mouseflow.com",
        "heap.io", "amplitude.com", "segment.com",
        // Push notification trackers
        "onesignal.com", "pushcrew.com", "pushwoosh.com",
        "xtremepush.com", "airship.com",
        // Click/redirect trackers
        "adf.ly", "bc.vc", "ouo.io", "clk.sh",
        // Misc trackers
        "krxd.net", "lijit.com", "pointroll.com", "dotomi.com",
        "burstmedia.com", "yieldbot.com", "adtech.de",
        "moatads.com", "demdex.net", "bluekai.com", "exelator.com",
        "smartadserver.com", "casalemedia.com", "adsrvr.org",
        "bidswitch.net", "contextweb.com", "improvedigital.com",
        "sharethrough.com", "spotxchange.com", "teads.tv"
    )

    // FIX: More precise URL patterns to avoid false positives
    // e.g. don't block /pixel/ broadly as it matches legitimate URLs
    private val blockedUrlPatterns = listOf(
        "/adserve/", "/adserver/", "/adsystem/",
        "pagead/js/", "pagead/show_ads",
        "googleadservices.com/pagead",
        "googlesyndication.com/pagead",
        "doubleclick.net/",
        "/advert.php", "/ads.php", "/ad.php",
        "affiliate/track", "click.php?aff"
    )

    fun shouldBlock(url: String): Boolean {
        val lower = url.lowercase()
        // Check full domain match (prevents partial matches like 'media.net' matching 'medinetwork.com')
        val host = try {
            android.net.Uri.parse(url).host?.lowercase() ?: ""
        } catch (_: Exception) { "" }

        for (domain in blockedDomains) {
            if (host == domain || host.endsWith(".$domain")) return true
        }
        // Check URL patterns only for very specific known ad patterns
        for (pattern in blockedUrlPatterns) {
            if (lower.contains(pattern)) return true
        }
        return false
    }

    fun blockedDomainsCount() = blockedDomains.size
}
