package com.marktoo.lib.demo

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.support.v7.app.AppCompatActivity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.AdapterView
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import com.marktoo.lib.cacheweb.CommonWebConfig
import com.marktoo.lib.cacheweb.LogUtil
import com.marktoo.lib.cacheweb.WebListener
import com.marktoo.lib.demo.R


class WebCacheActivity : AppCompatActivity() {
    var mWebView: WebView? = null
    lateinit var urls: Array<String>
    lateinit var tvJsTitle: TextView
    lateinit var tvListenerTitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_cache)

        mWebView = findViewById(R.id.webView)
        tvJsTitle = findViewById(R.id.tv_web_title)
        tvListenerTitle = findViewById(R.id.tv_listener_title)
//        initInterceptor()
        initWebView()
        urls = resources.getStringArray(R.array.urls)

        findViewById<Spinner>(R.id.spinner).onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                showLog("onNothingSelected")
                loadUrl(urls[0])
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                showLog("onItemSelected")
                loadUrl(urls[position])
            }
        }

        findViewById<Switch>(R.id.toggleButton).setOnCheckedChangeListener { _, isChecked ->
            //            commWeb.useCached(isChecked)
        }
    }

    private fun initWebView() {
        LogUtil.debug = true
        mWebView?.apply {
            //白屏问题解决方案，白屏产生由于硬件加速导致
//            setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
            setBackgroundResource(android.R.color.black)
            isDrawingCacheEnabled = true

            this.settings?.apply {
                savePassword = false
                //网页相关设置
                userAgentString = "myweb"
                defaultTextEncodingName = "utf-8"
                //js开关
                javaScriptEnabled = true
                //js弹窗需要
                javaScriptCanOpenWindowsAutomatically = true

                //缩放开关
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false

                //自适应屏幕开关
                useWideViewPort = true
                loadWithOverviewMode = true
                layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL

                //支持混合开发 cookieManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                    val cookieManager = CookieManager.getInstance()
                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                }

                //多窗口开关
                setSupportMultipleWindows(false)

                domStorageEnabled = true
                //允许加载本地文件时设置以下为true
                allowFileAccess = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    allowFileAccessFromFileURLs = true
                    allowUniversalAccessFromFileURLs = true
                }

            }

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    showLog("onPageFinished()")
                    super.onPageFinished(view, url)
                }

                override fun shouldInterceptRequest(view: WebView?, url: String?): WebResourceResponse? {
                    showLog("shouldInterceptRequest( $url )")
                    return super.shouldInterceptRequest(view, url)
                }

                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    showLog("shouldInterceptRequest(24 ${request?.url?.toString()})")
                    return super.shouldInterceptRequest(view, request)
                }

                override fun shouldOverrideKeyEvent(view: WebView?, event: KeyEvent?): Boolean {
                    showLog("shouldOverrideKeyEvent()")
                    return super.shouldOverrideKeyEvent(view, event)
                }

                override fun onSafeBrowsingHit(
                    view: WebView?,
                    request: WebResourceRequest?,
                    threatType: Int,
                    callback: SafeBrowsingResponse?
                ) {
                    showLog("onSafeBrowsingHit()")
                    super.onSafeBrowsingHit(view, request, threatType, callback)
                }

                override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                    showLog("doUpdateVisitedHistory( $url , isReload= $isReload)")
                    super.doUpdateVisitedHistory(view, url, isReload)
                }

                override fun onReceivedError(
                    view: WebView?,
                    errorCode: Int,
                    description: String?,
                    failingUrl: String?
                ) {
                    showLog("onReceivedError()")
                    super.onReceivedError(view, errorCode, description, failingUrl)
                }

                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    showLog("onReceivedError()")
                    super.onReceivedError(view, request, error)
                }

                override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                    showLog("onRenderProcessGone()")
                    return super.onRenderProcessGone(view, detail)
                }

                override fun onReceivedLoginRequest(view: WebView?, realm: String?, account: String?, args: String?) {
                    showLog("onReceivedLoginRequest()")
                    super.onReceivedLoginRequest(view, realm, account, args)
                }

                override fun onReceivedHttpError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    errorResponse: WebResourceResponse?
                ) {
                    showLog("onReceivedHttpError()")
                    super.onReceivedHttpError(view, request, errorResponse)
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    showLog("onPageStarted( $url )")
                    super.onPageStarted(view, url, favicon)
                }

                override fun onScaleChanged(view: WebView?, oldScale: Float, newScale: Float) {
                    showLog("onScaleChanged( from $oldScale to $newScale )")
                    super.onScaleChanged(view, oldScale, newScale)
                }

                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    showLog("shouldOverrideUrlLoading()")
                    return super.shouldOverrideUrlLoading(view, url)
                }

                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    showLog("shouldOverrideUrlLoading()")
                    return super.shouldOverrideUrlLoading(view, request)
                }

                override fun onPageCommitVisible(view: WebView?, url: String?) {
                    showLog("onUnhandledKeyEvent()")
                    super.onPageCommitVisible(view, url)
                }

                override fun onUnhandledKeyEvent(view: WebView?, event: KeyEvent?) {
                    showLog("onUnhandledKeyEvent()")
                    super.onUnhandledKeyEvent(view, event)
                }

                override fun onReceivedClientCertRequest(view: WebView?, request: ClientCertRequest?) {
                    showLog("onReceivedClientCertRequest()")
                    super.onReceivedClientCertRequest(view, request)
                }

                override fun onReceivedHttpAuthRequest(
                    view: WebView?,
                    handler: HttpAuthHandler?,
                    host: String?,
                    realm: String?
                ) {
                    showLog("onReceivedHttpAuthRequest()")
                    super.onReceivedHttpAuthRequest(view, handler, host, realm)
                }

                override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                    showLog("onReceivedSslError()")
                    super.onReceivedSslError(view, handler, error)
                }

                override fun onTooManyRedirects(view: WebView?, cancelMsg: Message?, continueMsg: Message?) {
                    showLog("onTooManyRedirects()")
                    super.onTooManyRedirects(view, cancelMsg, continueMsg)
                }

                override fun onFormResubmission(view: WebView?, dontResend: Message?, resend: Message?) {
                    showLog("onFormResubmission()")
                    super.onFormResubmission(view, dontResend, resend)
                }

                override fun onLoadResource(view: WebView?, url: String?) {
                    showLog("onLoadResource( $url )")
                    super.onLoadResource(view, url)
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onRequestFocus(view: WebView?) {
                    showLog("onRequestFocus()")
                    super.onRequestFocus(view)
                }

                override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                    showLog("onJsAlert()")
                    return super.onJsAlert(view, url, message, result)
                }

                override fun onJsPrompt(
                    view: WebView?,
                    url: String?,
                    message: String?,
                    defaultValue: String?,
                    result: JsPromptResult?
                ): Boolean {
                    showLog("onJsPrompt()")
                    return super.onJsPrompt(view, url, message, defaultValue, result)
                }

                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    showLog("onShowCustomView()")
                    super.onShowCustomView(view, callback)
                }

                override fun onShowCustomView(view: View?, requestedOrientation: Int, callback: CustomViewCallback?) {
                    showLog("onShowCustomView()")
                    super.onShowCustomView(view, requestedOrientation, callback)
                }

                override fun onGeolocationPermissionsShowPrompt(
                    origin: String?,
                    callback: GeolocationPermissions.Callback?
                ) {
                    showLog("onGeolocationPermissionsShowPrompt()")
                    super.onGeolocationPermissionsShowPrompt(origin, callback)
                }

                override fun onPermissionRequest(request: PermissionRequest?) {
                    showLog("onPermissionRequest()")
                    super.onPermissionRequest(request)
                }

                override fun onConsoleMessage(message: String?, lineNumber: Int, sourceID: String?) {
                    showLog("onConsoleMessage( $lineNumber : $sourceID : $message )")
                    super.onConsoleMessage(message, lineNumber, sourceID)
                }

                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                    showLog("onConsoleMessage( ${consoleMessage?.lineNumber()} : ${consoleMessage?.sourceId()} : ${consoleMessage?.message()})")
                    return super.onConsoleMessage(consoleMessage)
                }

                override fun onPermissionRequestCanceled(request: PermissionRequest?) {
                    showLog("onPermissionRequestCanceled()")
                    super.onPermissionRequestCanceled(request)
                }

                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    showLog("onShowFileChooser()")
                    return super.onShowFileChooser(webView, filePathCallback, fileChooserParams)
                }

                override fun onReceivedTouchIconUrl(view: WebView?, url: String?, precomposed: Boolean) {
                    showLog("onReceivedTouchIconUrl()")
                    super.onReceivedTouchIconUrl(view, url, precomposed)
                }

                override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                    showLog("onReceivedIcon()")
                    super.onReceivedIcon(view, icon)
                }

                override fun onExceededDatabaseQuota(
                    url: String?,
                    databaseIdentifier: String?,
                    quota: Long,
                    estimatedDatabaseSize: Long,
                    totalQuota: Long,
                    quotaUpdater: WebStorage.QuotaUpdater?
                ) {
                    showLog("onExceededDatabaseQuota()")
                    super.onExceededDatabaseQuota(
                        url,
                        databaseIdentifier,
                        quota,
                        estimatedDatabaseSize,
                        totalQuota,
                        quotaUpdater
                    )
                }

                override fun onReceivedTitle(view: WebView?, title: String?) {
                    showLog("onReceivedTitle( $title )")
                    super.onReceivedTitle(view, title)
                }

                override fun onReachedMaxAppCacheSize(
                    requiredStorage: Long,
                    quota: Long,
                    quotaUpdater: WebStorage.QuotaUpdater?
                ) {
                    showLog("onReachedMaxAppCacheSize()")
                    super.onReachedMaxAppCacheSize(requiredStorage, quota, quotaUpdater)
                }

                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    showLog("onProgressChanged( $newProgress )")
                    super.onProgressChanged(view, newProgress)
                }

                override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                    showLog("onJsConfirm()")
                    return super.onJsConfirm(view, url, message, result)
                }

                override fun getVisitedHistory(callback: ValueCallback<Array<String>>?) {
                    showLog("getVisitedHistory()")
                    super.getVisitedHistory(callback)
                }

                override fun getVideoLoadingProgressView(): View? {
                    showLog("getVideoLoadingProgressView()")
                    return super.getVideoLoadingProgressView()
                }

                override fun onGeolocationPermissionsHidePrompt() {
                    showLog("onGeolocationPermissionsHidePrompt()")
                    super.onGeolocationPermissionsHidePrompt()
                }

                override fun getDefaultVideoPoster(): Bitmap? {
                    showLog("getDefaultVideoPoster()")
                    return super.getDefaultVideoPoster()
                }

                override fun onJsBeforeUnload(
                    view: WebView?,
                    url: String?,
                    message: String?,
                    result: JsResult?
                ): Boolean {
                    showLog("onJsBeforeUnload()")
                    return super.onJsBeforeUnload(view, url, message, result)
                }

                override fun onHideCustomView() {
                    showLog("onHideCustomView()")
                    super.onHideCustomView()
                }

                override fun onCreateWindow(
                    view: WebView?,
                    isDialog: Boolean,
                    isUserGesture: Boolean,
                    resultMsg: Message?
                ): Boolean {
                    showLog("onCreateWindow()")
                    return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg)
                }

                override fun onCloseWindow(window: WebView?) {
                    showLog("onCreateWindow()")
                    super.onCloseWindow(window)
                }

                override fun onJsTimeout(): Boolean {
                    showLog("onJsTimeout()")
                    return super.onJsTimeout()
                }
            }
        }
    }

    private lateinit var commWeb: CommonWebConfig

    private fun initInterceptor() {
        LogUtil.debug = true
        commWeb = CommonWebConfig(this, mWebView!!)
        commWeb.debug = true
        commWeb.cacheable = true
        commWeb.autoWide = true
        commWeb.zoomable = true
        commWeb.multiWindow = false
        commWeb.defaultEncoding = "utf-8"
        commWeb.userAgent = "mkweb"
        commWeb.jsBridge = true
        commWeb.applyWebSettings()
        commWeb.addInterceptor()
        commWeb.addDefaultClient()
        commWeb.webListener = object : WebListener() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
            }

            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress == 100) {

                } else {

                }
                super.onProgressChanged(view, newProgress)
            }

            override fun onReceivedError(view: WebView?, errorCode: Int?, url: String) {
//                when (errorCode) {
//                    404 -> {
//                        view!!.loadUrl("error page offline html")
//                    }
//                }
                super.onReceivedError(view, errorCode, url)
            }

            @SuppressLint("SetTextI18n")
            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                tvListenerTitle.text = "listener title: $title"
            }
        }
        commWeb.useCached(true)
        val mutualInterface = WebMutualInterface()
        mutualInterface.setTitleView(tvJsTitle)
        commWeb.addMutualInterface(mutualInterface, "DDBESOFFICE")
    }

    @SuppressLint("JavascriptInterface")
    class WebMutualInterface {

        @SuppressLint("SetTextI18n")
        @JavascriptInterface
        fun dd_getNavigationTitle(title: String) {
            tvWebTitle.text = "from js: $title"
            LogUtil.showLog("receive from js: $title")
        }

        private lateinit var tvWebTitle: TextView
        fun setTitleView(tvWebTitle: TextView) {
            this.tvWebTitle = tvWebTitle
        }

    }

    fun loadUrl(url: String) {
        showLog("activity loadUrl= $url")
//        commWeb.loadUrl(url, null)
        mWebView?.loadUrl(url)
    }

    fun showLog(msg: String) {
        LogUtil.showLog(msg)
    }

    override fun onResume() {
        super.onResume()
        mWebView!!.onResume()
    }

    override fun onPause() {
        super.onPause()
        mWebView!!.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
//        if (mWebView!!.parent is ViewGroup) {
//            (mWebView!!.parent as ViewGroup).removeView(mWebView)
//        }
//        mWebView!!.destroy()
//        mWebView = null
//        commWeb.releaseWebView()
    }

    override fun onBackPressed() {
        showHistories()
        if (mWebView!!.canGoBack()) {
            mWebView!!.goBack()
        } else {
            super.onBackPressed()
        }
    }

    fun showHistories() {
        if (mWebView != null) {
            val bfList: WebBackForwardList = mWebView!!.copyBackForwardList()
            val size = bfList.size
            if (size > 0) {
                for (i in 0 until size) {
                    val item: WebHistoryItem = bfList.getItemAtIndex(i)
                    showLog("webview history index = $i , info: title=${item.title} ,url=${item.url}")
                }
            } else {
                showLog("webview history in none")
            }
        }
    }
}
