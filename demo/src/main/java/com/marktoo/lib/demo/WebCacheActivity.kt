package com.marktoo.lib.demo

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.text.TextUtils
import android.view.KeyEvent
import android.view.View
import android.webkit.*
import android.widget.AdapterView
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.marktoo.lib.cacheweb.CommonWebConfig
import com.marktoo.lib.cachedweb.LogUtil
import com.marktoo.lib.cacheweb.WebListener
import java.net.URLEncoder


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

    val holder = MyHistoryHolder()

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
                layoutAlgorithm = WebSettings.LayoutAlgorithm.NARROW_COLUMNS

                //支持混合开发 5.0 以后的WebView加载的链接为Https开头，但是链接里面的内容，比如图片为Http链接，这时候，图片就会加载不出来
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                    val cookieManager = CookieManager.getInstance()
//                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                }

                //多窗口开关
                setSupportMultipleWindows(false)

                setAppCacheEnabled(true)
                domStorageEnabled = true
                allowContentAccess = true
                //允许加载本地文件时设置以下为true
                allowFileAccess = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    allowFileAccessFromFileURLs = true
                    allowUniversalAccessFromFileURLs = true
                }

            }

            webViewClient = object : WebViewClient() {

                /**
                 * 通知Application页面已经开始加载资源，页面加载过程中，onPageStarted至多会被执行一次。
                 */
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    showLog("onPageStarted( $url )")
                    super.onPageStarted(view, url, favicon)
                }

                /**
                 * 通知Application页面已经加载完毕。
                 */
                override fun onPageFinished(view: WebView?, url: String?) {
                    showLog("onPageFinished()")
                    super.onPageFinished(view, url)
                }

                /**
                 * 在API 24以后过时，当一个url即将被webview加载时，给Application一个机会来接管处理这个url，
                 * 方法返回true代表Application自己处理url；
                 * 返回false代表Webview处理url。
                 */
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    showLog("shouldOverrideUrlLoading( $url )")
//                    val uri = Uri.parse(url)
//                    val scheme = uri.scheme
//                    if (TextUtils.isEmpty(scheme)) return true
//                    if (scheme == "nativeapi") {
//                        //如定义nativeapi://showImg是用来查看大图，这里添加查看大图逻辑
//                        return true
//                    } else if (scheme == "http" || scheme == "https") {
//                        //处理http协议
//                        if (Uri.parse(url).host == "www.example.com") {
//                            // 内部网址，不拦截，用自己的webview加载
//                            return false
//                        } else {
//                            //跳转外部浏览器
//                            val intent = Intent(Intent.ACTION_VIEW, uri)
//                            context.startActivity(intent)
//                            return true
//                        }
//                    }
                    return super.shouldOverrideUrlLoading(view, url)
                }

                @TargetApi(Build.VERSION_CODES.N)
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    showLog("shouldOverrideUrlLoading(24 ${request?.url})")
                    return super.shouldOverrideUrlLoading(view, request)
                }

                /**
                 * 在API 21以后过时，通知Application加载资源的请求并返回请求的资源，如果返回值是Null，Webview仍然会按正常加载资源；否则返回的数据将会被使用。
                 * 注：回调发生在子线程中,不能直接进行UI操作
                 */
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


                override fun onLoadResource(view: WebView?, url: String?) {
                    showLog("onLoadResource( $url )")
                    super.onLoadResource(view, url)
                }

                /**
                 * 更新浏览历史记录
                 */
                override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                    showLog("doUpdateVisitedHistory( $url , isReload= $isReload)")
                    holder.checkHistory(url)
                    super.doUpdateVisitedHistory(view, url, isReload)
                }

                /**
                 * 通知Application有错误发生，这些错误是不可恢复的(即主要的资源不可用)
                 */
                override fun onReceivedError(
                    view: WebView?,
                    errorCode: Int,
                    description: String?,
                    failingUrl: String?
                ) {
                    showLog("onReceivedError( $errorCode : $description : $failingUrl )")
                    super.onReceivedError(view, errorCode, description, failingUrl)
                }

                @TargetApi(Build.VERSION_CODES.M)
                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    showLog("onReceivedError( ${error?.errorCode} : ${error?.description} : ${request?.url} )")
                    super.onReceivedError(view, request, error)
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

                override fun onScaleChanged(view: WebView?, oldScale: Float, newScale: Float) {
                    showLog("onScaleChanged( from $oldScale to $newScale )")
                    super.onScaleChanged(view, oldScale, newScale)
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

            }

            webChromeClient = object : WebChromeClient() {

                /**
                 * 通知Application的加载进度，newProgress取值范围[0,100]，可以通过这个方法来编写一个带加载进度条的Webview
                 */
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    showLog("onProgressChanged( $newProgress )")
                    super.onProgressChanged(view, newProgress)
                }

                /**
                 * 获取浏览记录
                 */
                override fun getVisitedHistory(callback: ValueCallback<Array<String>>?) {
                    showLog("getVisitedHistory()")
                    super.getVisitedHistory(callback)
                }

                /**
                 * 当加载页面标题有改变时会通知Application，title即为新标题
                 */
                override fun onReceivedTitle(view: WebView?, title: String?) {
                    showLog("onReceivedTitle( $title )")
                    super.onReceivedTitle(view, "考勤统计")
                }

                override fun onConsoleMessage(message: String?, lineNumber: Int, sourceID: String?) {
                    showLog("onConsoleMessage(blew 8-> $lineNumber : $sourceID : $message )")
                    super.onConsoleMessage(message, lineNumber, sourceID)
                }

                @TargetApi(Build.VERSION_CODES.FROYO)
                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                    showLog("onConsoleMessage(8-> ${consoleMessage?.lineNumber()} : ${consoleMessage?.sourceId()} : ${consoleMessage?.message()})")
                    return super.onConsoleMessage(consoleMessage)
                }

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

                override fun onReachedMaxAppCacheSize(
                    requiredStorage: Long,
                    quota: Long,
                    quotaUpdater: WebStorage.QuotaUpdater?
                ) {
                    showLog("onReachedMaxAppCacheSize()")
                    super.onReachedMaxAppCacheSize(requiredStorage, quota, quotaUpdater)
                }

                override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                    showLog("onJsConfirm()")
                    return super.onJsConfirm(view, url, message, result)
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

    class MyHistoryHolder {
        private val visitHistories: ArrayList<String> = arrayListOf()
        private val visitItemHistories: ArrayList<MyHistoryItem> = arrayListOf()
        var currentUrl: String? = null

        fun checkHistory(url: String?) {
            if (TextUtils.isEmpty(url)) return
            val realUrl = URLEncoder.encode(url, "utf-8")
            if (realUrl in visitHistories) {
                for (item in visitItemHistories) {
                    if (item.url == realUrl) {
                        item.count += 1
                    }
                }
            } else {
                visitHistories.add(realUrl!!)
                val item = MyHistoryItem(realUrl, 1)
                visitItemHistories.add(item)
            }
        }

        fun canGoBack(): Boolean = visitHistories.size > 1

        fun goBack(webView: WebView) {
            visitHistories.removeAt(visitHistories.lastIndex)
            currentUrl = visitHistories.last()
            val item = visitItemHistories.last()
            while (item.count > 0) {
                webView.goBack()
                item.count--
            }
        }

        fun showHistories() {
            LogUtil.showLog(visitItemHistories)
        }
    }

    class MyHistoryItem(val url: String, var count: Int) {
        override fun toString(): String {
            return "MyHistoryItem(url='$url', count=$count)"
        }
    }

    override fun onBackPressed() {
        holder.showHistories()
        showHistories()
        if (holder.canGoBack()) {
            holder.goBack(mWebView!!)
//            mWebView!!.loadUrl(holder.currentUrl)
//            mWebView!!.goBack()
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

    fun inHistories(url: String): Boolean {
        val bfList: WebBackForwardList = mWebView!!.copyBackForwardList()
        val size = bfList.size
        if (size > 0) {
            for (i in 0 until size) {
                val item: WebHistoryItem = bfList.getItemAtIndex(i)
                if (item.url == url) {
                    return true
                }
            }
        }
        return false
    }
}
