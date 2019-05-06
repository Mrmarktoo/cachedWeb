package com.marktoo.lib.cacheweb

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Environment
import android.os.Message
import android.text.TextUtils
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.Toast
import okhttp3.*
import java.io.File
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

/**
 * @className: CacheWeb
 * @desc: TODO
 * @author: zyy
 * @date: 2019/4/10 17:49
 * @company: joinUTech
 * @leader: ke
 */
enum class CacheType {
    NORMAL, FORCE
}

const val KEY_CACHE = "cache_web"

class HttpCacheInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val cache = request.header(KEY_CACHE)
        val originResponse = chain.proceed(request)
        if (TextUtils.isEmpty(cache) && cache.equals("${CacheType.NORMAL.ordinal}")) {
            return originResponse
        }
        return originResponse.newBuilder().removeHeader("pragma").removeHeader("Cache-Control")
            .header("Cache-Control", "max-age=3153600000").build()
    }
}

class CacheFilterConfig {
    private val cacheSet: LinkedHashSet<String> = defaultCacheSet
    private val unCacheSet: LinkedHashSet<String> = defaultUnCacheSet

    fun addCache(extension: String) {
        if (!cacheSet.contains(extension))
            cacheSet.add(extension)
    }

    fun removeCache(extension: String) {
        if (cacheSet.contains(extension))
            cacheSet.remove(extension)
    }

    fun addUnCache(extension: String) {
        if (!unCacheSet.contains(extension))
            unCacheSet.add(extension)
    }

    fun removeUnCache(extension: String) {
        if (unCacheSet.contains(extension))
            unCacheSet.remove(extension)
    }

    fun noCache(extension: String?): Boolean {
        return if (unCacheSet.isEmpty()) true
        else extension in unCacheSet
    }

    fun canCache(extension: String?): Boolean {
        return if (cacheSet.isEmpty()) true
        else extension in cacheSet
    }

    companion object {
        val defaultCacheSet: LinkedHashSet<String> = linkedSetOf(
            "html", "htm", "js", "css", "png", "jpg", "jpeg", "gif", "bmp", "ttf", "svg", "xml", "txt", "conf", "webp"
        )

        val defaultUnCacheSet: LinkedHashSet<String> = linkedSetOf(
            "mp4", "mp3", "ogg", "avi", "flv", "wmv", "amr", "3gp"
        )
    }
}

interface ResourceInterceptor {
    fun interceptor(url: String): Boolean
}

interface WebRequestInterceptor {
    fun interceptRequest(request: WebResourceRequest): WebResourceResponse?
    fun interceptRequest(url: String): WebResourceResponse?
    fun getCachePath(): File
    fun clearCache()
    fun enableForce(force: Boolean)
    fun overrideUrl(webView: WebView?, url: String, headers: Map<String, String>?): Boolean
}

class WebViewCacheInterceptor private constructor(var mContext: Context) : WebRequestInterceptor {

    private var cacheFile: File
    var cacheSize: Long = 100 * 1024 * 1024
    var connectTimeout: Long = 20
    var readTimeout: Long = 20
    var cacheType: CacheType? = null
    var trustAllHostName: Boolean = false
    var sslSocketFactory: SSLSocketFactory? = null
    var x509TrustManager: X509TrustManager? = null
    var dns: Dns? = null
    var resourceInterceptor: ResourceInterceptor? = null
    var cacheFilter: CacheFilterConfig? = null
    var assetsDir: File? = null
    var client: OkHttpClient? = null
    var debug: Boolean = false

    init {
        cacheFilter = CacheFilterConfig()
        val cacheDir =
            if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) mContext.externalCacheDir else mContext.cacheDir
        cacheFile = File(cacheDir.absolutePath, "mkWEB")
    }

    companion object {
        fun init(context: Context): WebViewCacheInterceptor {
            return WebViewCacheInterceptor(context)
        }
    }

    private fun initHttpClient() {
        val cache = Cache(cacheFile, cacheSize)
        val builder = OkHttpClient.Builder()
            .cache(cache)
            .connectTimeout(connectTimeout, TimeUnit.SECONDS)
            .readTimeout(readTimeout, TimeUnit.SECONDS)
            .addNetworkInterceptor(HttpCacheInterceptor())
        if (trustAllHostName) {
            builder.hostnameVerifier { hostname, session -> true }
        }
        if (sslSocketFactory != null && x509TrustManager != null) {
            builder.sslSocketFactory(sslSocketFactory, x509TrustManager)
        }
        if (dns != null) {
            builder.dns(dns)
        }
        client = builder.build()
    }

    @TargetApi(21)
    override fun interceptRequest(request: WebResourceRequest): WebResourceResponse? {
        return interceptRequest(request.url.toString(), request.requestHeaders)
    }

    override fun interceptRequest(url: String): WebResourceResponse? {
        return interceptRequest(url, buildHeader())
    }

    private fun buildHeader(): MutableMap<String, String> {
        return mutableMapOf("Origin" to mOrigin, "Referer" to mReferer, "User-Agent" to mUserAgent)
    }

    private fun interceptRequest(url: String, headers: MutableMap<String, String>): WebResourceResponse? {
        showLog("判断是否拦截请求")
        if (cacheType == CacheType.NORMAL) {
            showLog("未开启缓存模式，不拦截请求")
            return null
        }

        //检查url
        if (!canCacheable(url)) {
            showLog("资源不再缓存范围内，不拦截请求")
            return null
        }

        if (client == null) {
            initHttpClient()
        }
        try {
            val requestBuilder = Request.Builder().url(url)
            //添加header
            if (FileUtil.isHtml(url)) {
                headers!![KEY_CACHE] = "${cacheType!!.ordinal}"
            }

            addHeader(requestBuilder, headers)

            //添加无网络cache control设置
            if (!NetUtil.isConnected(mContext)) {
                requestBuilder.cacheControl(CacheControl.FORCE_CACHE)
            }
            val request = requestBuilder.build()
            val response = client!!.newCall(request).execute()
            val cacheResponse = response.cacheResponse()
            if (cacheResponse != null) {
                showLog("from cache: $url")
            } else {
                showLog("from server: $url")
            }
            val mimeType = getMimeType(url)
            val webResponse = WebResourceResponse(mimeType, "", response.body()!!.byteStream())
            if (response.code() == 504) return null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                var msg = response.message()
                if (msg.isNullOrBlank()) {
                    msg = "ok"
                }
                try {
                    webResponse.setStatusCodeAndReasonPhrase(response.code(), msg)
                } catch (e: Exception) {
                    e.printStackTrace()
                    showLog("拦截加载资源加载正常，但是设置状态码发生异常，导致请求失败...")
                    return null
                }
                webResponse.responseHeaders = mapConvert(response.headers().toMultimap())
            }
            return webResponse
        } catch (e: Exception) {
            showLog("拦截加载资源异常，未请求到指定资源...")
            e.printStackTrace()
        }
        return null
    }

    private fun canCacheable(url: String?): Boolean {
        showLog("缓存资源判断")
        if (url.isNullOrEmpty()) return false
        if (!url.startsWith("http")) return false
        if (resourceInterceptor != null && !resourceInterceptor!!.interceptor(url)) return false
        val extension = MimeTypeMap.getFileExtensionFromUrl(url)
        if (extension.isNullOrEmpty()) return false
        if (cacheFilter!!.noCache(extension)) return false
        if (!cacheFilter!!.canCache(extension)) return false
        return true
    }

    private fun addHeader(requestBuilder: Request.Builder?, headers: MutableMap<String, String>?) {
        if (headers == null) return
        if (requestBuilder == null) return
        headers.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }
    }

    private fun mapConvert(source: Map<String, List<String>>): Map<String, String> {
        val sb = StringBuilder()
        val map = hashMapOf<String, String>()
        if (!map.isNullOrEmpty()) {
            source.forEach { (k, v) ->
                sb.clear()
                if (!v.isNullOrEmpty()) {
                    v.forEach { sb.append(it).append(";") }
                    sb.deleteCharAt(sb.length - 1)
                }
                map.put(k, sb.toString())
            }
        }
        return map
    }

    private fun getMimeType(url: String): String {
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(getExtension(url)) ?: ""
    }

    private fun getExtension(url: String): String {
        return MimeTypeMap.getFileExtensionFromUrl(url)
    }

    override fun getCachePath(): File = cacheFile

    override fun clearCache() {
        FileUtil.deleteFiles(cacheFile, false)
    }

    override fun enableForce(force: Boolean) {
        if (force) {
            this.cacheType = CacheType.FORCE
        } else {
            this.cacheType = CacheType.NORMAL
        }
    }

    var mReferer: String = ""
    var mOrigin: String = ""
    var mUserAgent: String = ""

    override fun overrideUrl(webView: WebView?, url: String, headers: Map<String, String>?): Boolean {
        showLog("overrideUrl: $url")
        if (!NetUtil.isValidUrl(url)) return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        webView?.apply {
            mReferer = url
            mOrigin = NetUtil.getOriginUrl(url)
            mUserAgent = settings.userAgentString
            return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                if (headers == null || headers.isEmpty()) {
                    loadUrl(url)
                } else {
                    loadUrl(url, headers)
                }
                true
            } else {
                false
            }
        }
    }

    private fun showLog(msg: String) {
        if (debug) {
            LogUtil.showLog(msg)
        }
    }
}

class CommonWebConfig(private val context: Context, private var mWebView: WebView?) {

    private val tag: String = "MkWebView"

    var debug = false
    var jsBridge = false
    var zoomable = false
    var autoWide = false
    var multiWindow = false
    var userAgent: String = ""
    var defaultEncoding: String = "utf-8"
    var cacheable: Boolean = false

    var currentUrl: String? = null

    private var interceptor: WebViewCacheInterceptor? = null
    var webListener: WebListener? = null

    fun addInterceptor(interceptor: WebViewCacheInterceptor? = null) {
        if (interceptor != null) {
            this.interceptor = interceptor
        } else {
            this.interceptor = WebViewCacheInterceptor.init(context)
        }
        this.interceptor!!.debug = debug
    }

    fun applyWebSettings() {
        mWebView?.apply {
            //白屏问题解决方案，白屏产生由于硬件加速导致
//            setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
            setBackgroundResource(android.R.color.black)
            isDrawingCacheEnabled = true

            this.settings?.apply {
                savePassword = false
                //网页相关设置
                userAgentString = userAgent
                defaultTextEncodingName = defaultEncoding
                //js开关
                javaScriptEnabled = jsBridge
                //js弹窗需要
                javaScriptCanOpenWindowsAutomatically = jsBridge

                //缩放开关
                setSupportZoom(zoomable)
                builtInZoomControls = zoomable
                displayZoomControls = false

                //自适应屏幕开关
                useWideViewPort = autoWide
                loadWithOverviewMode = autoWide
                layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL

                //支持混合开发 cookieManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                    val cookieManager = CookieManager.getInstance()
                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                }

                //多窗口开关
                setSupportMultipleWindows(multiWindow)

                domStorageEnabled = cacheable
                //允许加载本地文件时设置以下为true
                allowFileAccess = cacheable
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    allowFileAccessFromFileURLs = cacheable
                    allowUniversalAccessFromFileURLs = cacheable
                }

            }
        }
    }

    fun isCacheables(): Boolean = cacheable && interceptor != null

    fun useCached(useCache: Boolean) {
        interceptor?.enableForce(useCache)
    }

    private val webClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            showLog("页面开始加载++++++++++++++++")
            startTime = System.currentTimeMillis()
            if (webListener != null) {
                webListener?.onPageStarted(view, url, favicon)
            }
            super.onPageStarted(view, url, favicon)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            showLog("页面完成加载++++++++++++++++")
            if (webListener != null) {
                webListener!!.onPageFinished(view, url)
            }
            pageFinished = true
            loadOver()
        }

        @TargetApi(Build.VERSION_CODES.N)
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            showLog("dOverrideUrlLoading(24) ${request?.url}")
//                    return super.shouldOverrideUrlLoading(view, request)
            return if (isCacheables() && mWebView != null && interceptor != null) {
                interceptor!!.overrideUrl(mWebView, request?.url.toString(), request?.requestHeaders)
            } else {
                super.shouldOverrideUrlLoading(view, request)
            }
        }

        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            showLog("dOverrideUrlLoading(blew 24) $url")
//                    super.shouldOverrideUrlLoading(view, url)
            return if (isCacheables() && mWebView != null && interceptor != null) {
                interceptor!!.overrideUrl(mWebView, url!!, null)
            } else {
                super.shouldOverrideUrlLoading(view, url)
            }
        }

        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
            showLog("更新访问历史： url = $url , isReload = $isReload")
            super.doUpdateVisitedHistory(view, url, isReload)
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
            showLog("InterceptRequest(21) ${request?.url}")
//                    return super.shouldInterceptRequest(view, request)
            return if (isCacheables() && request != null && request.url != null) {
                shouldInterceptRequest(view, request.url.toString())
                    ?: super.shouldInterceptRequest(view, request)
            } else {
                super.shouldInterceptRequest(view, request)
            }
        }

        override fun shouldInterceptRequest(view: WebView?, url: String?): WebResourceResponse? {
            showLog("InterceptRequest(blew 21) $url")
//                    return super.shouldInterceptRequest(view, url)
            return if (isCacheables()) {
                interceptor?.interceptRequest(url!!)
                    ?: super.shouldInterceptRequest(view, url)
            } else {
                super.shouldInterceptRequest(view, url)
            }
        }

        override fun onLoadResource(view: WebView?, url: String?) {
            showLog("加载资源 $url")
            super.onLoadResource(view, url)
        }

        @TargetApi(Build.VERSION_CODES.M)
        override fun onPageCommitVisible(view: WebView?, url: String?) {
            showLog("$url 页面body开始加载并在dom上绘制可见时回调")
            super.onPageCommitVisible(view, url)
        }

        @TargetApi(Build.VERSION_CODES.M)
        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
            showLog("接收到加载资源错误 ${request?.url} has error is ${error?.errorCode} : ${error?.description}")
            if (webListener != null) {
                webListener?.onReceivedError(view, error!!.errorCode, request!!.url.toString())
            } else {
                super.onReceivedError(view, request, error)
            }
        }

        override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
            showLog("接收到加载资源错误 $failingUrl has error is $errorCode : $description")
            if (webListener != null) {
                webListener?.onReceivedError(view, errorCode, failingUrl!!)
            } else {
                super.onReceivedError(view, errorCode, description, failingUrl)
            }
        }

        /**
         * 在加载资源(iframe,image,js,css,ajax...)时收到了 HTTP 错误(状态码>=400)
         */
        @TargetApi(Build.VERSION_CODES.M)
        override fun onReceivedHttpError(
            view: WebView?,
            request: WebResourceRequest?,
            errorResponse: WebResourceResponse?
        ) {
            showLog("接收到http错误 ${request?.url} has error is ${request?.method}:${errorResponse?.statusCode}")
            if (webListener != null) {
                webListener?.onHttpError(view, request, errorResponse)
            }
            super.onReceivedHttpError(view, request, errorResponse)
        }

        override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
            showLog("接收到ssl错误 ${error?.url} : ${error?.primaryError}")
            if (webListener != null) {
                webListener?.onSslError(view, handler, error)
            }
            super.onReceivedSslError(view, handler, error)
        }

        override fun onFormResubmission(view: WebView?, dontResend: Message?, resend: Message?) {
            showLog("表单是否重新提交，默认不重发")
            super.onFormResubmission(view, dontResend, resend)
        }

        override fun onUnhandledKeyEvent(view: WebView?, event: KeyEvent?) {
            super.onUnhandledKeyEvent(view, event)
            showLog("未拦截事件")
        }

        @TargetApi(21)
        override fun onReceivedClientCertRequest(view: WebView?, request: ClientCertRequest?) {
            showLog("接收到client 证书请求 ${request?.apply { "$host:$port" }}")
            super.onReceivedClientCertRequest(view, request)
        }

        override fun onReceivedHttpAuthRequest(
            view: WebView?,
            handler: HttpAuthHandler?,
            host: String?,
            realm: String?
        ) {
            showLog("接收到http 认证请求")
            super.onReceivedHttpAuthRequest(view, handler, host, realm)
        }

        override fun onReceivedLoginRequest(view: WebView?, realm: String?, account: String?, args: String?) {
            showLog("页面自动登录请求")
            super.onReceivedLoginRequest(view, realm, account, args)
        }

        override fun onScaleChanged(view: WebView?, oldScale: Float, newScale: Float) {
            showLog("页面缩放变化更新 $oldScale to $newScale")
            if (webListener != null) {
                webListener?.onSaleChanged(view, oldScale, newScale)
            }
            super.onScaleChanged(view, oldScale, newScale)
        }
    }

    private val webChromeClient = object : WebChromeClient() {

        // 支持到api 7
        @TargetApi(7)
        override fun onConsoleMessage(message: String?, lineNumber: Int, sourceID: String?) {
            showLog("api 7以上版本log回调：$message -- From line $lineNumber of $sourceID")
            super.onConsoleMessage(message, lineNumber, sourceID)
        }

        // 支持到api 8
        @TargetApi(8)
        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
            showLog("api 8 以上版本log回调:")
            consoleMessage?.apply {
                showLog("${message()} -- From line ${lineNumber()} of ${sourceId()}")
            }
            return true
        }

        override fun getVideoLoadingProgressView(): View? {
            showLog("video 加载视频时进度条控件")
            if (webListener != null) {
                return webListener?.getVideoLoadingProgressView() ?: super.getVideoLoadingProgressView()
            }
            return super.getVideoLoadingProgressView()
        }

        override fun getDefaultVideoPoster(): Bitmap? {
            showLog("页面未指定video控件的poster属性时，在这里可以拦截提供该属性，提供默认海报图")
            if (webListener != null) {
                return webListener?.getVideoPoster() ?: super.getDefaultVideoPoster()
            }
            return super.getDefaultVideoPoster()
        }

        override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
            showLog("接收到页面角标")
            if (webListener != null) {
                webListener?.onReceivedIcon(view, icon)
            } else {
                super.onReceivedIcon(view, icon)
            }
        }

        override fun onReceivedTitle(view: WebView?, title: String?) {
            showLog("接收到页面标题：$title")
            if (webListener != null) {
                webListener?.onReceivedTitle(view, title)
            } else {
                super.onReceivedTitle(view, title)
            }
        }

        /**
         * 通知主机程序当前界面已进入全屏模式.主机应用程序必须显示自定义视图包含web内容-视频或者其他HTML内容再全屏模式.
         */
        @TargetApi(17)
        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
            showLog("显示全屏模式 自定义视图")
            if (webListener != null) {
                webListener?.onFullView(view, -1, callback)
            } else {
                super.onShowCustomView(view, callback)
            }
        }

        /**
         * 通知主机程序当前界面已进入全屏模式.主机应用程序必须显示自定义视图包含web内容-视频或者其他HTML内容再全屏模式.
         */
        override fun onShowCustomView(view: View?, requestedOrientation: Int, callback: CustomViewCallback?) {
            showLog("显示全屏模式 自定义视图")
            if (webListener != null) {
                webListener?.onFullView(view, requestedOrientation, callback)
            } else {
                super.onShowCustomView(view, requestedOrientation, callback)
            }
        }

        /**
         * 当前页面通知主机应用程序已经退出全屏模式.主机应用程序必须将自定义视图隐藏.即,传递给onShowCustomView的View参数,具体可以参阅WebView全屏文档.
         */
        override fun onHideCustomView() {
            showLog("退出全屏模式")
            if (webListener != null) {
                webListener?.onQuitFullView()
            } else {
                super.onHideCustomView()
            }
        }

        override fun onRequestFocus(view: WebView?) {
            showLog("页面获取焦点")
            super.onRequestFocus(view)
        }

        /**
         * 告诉客户端显示JavaScript警告的dialog,
         * 如果客户端返回true,WebView将会处理对话框,如果客户端返回false,将会跳过继续执行.
         */
        override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
            showLog("js 警告窗")
            if (webListener != null) {
                return webListener?.onJsAlert(view, url, message, result) ?: super.onJsAlert(
                    view,
                    url,
                    message,
                    result
                )
            }
            return super.onJsAlert(view, url, message, result)
        }

        /**
         * 告诉客户端要向用户显示一个需要用户输入的对话框,
         * 如果客户端返回true,那么WebView将会处理提示对话框和调用适当的JsPromptJResult方法,
         * 如果客户端返回false,会将false的值返回给JavaScript.
         * 默认情况下将会返回false. 即客户端不处理，web自行处理
         */
        override fun onJsPrompt(
            view: WebView?,
            url: String?,
            message: String?,
            defaultValue: String?,
            result: JsPromptResult?
        ): Boolean {
            showLog("js 对话框")
            if (webListener != null) {
                return webListener?.onJsPrompt(view, url, message, defaultValue, result) ?: super.onJsPrompt(
                    view,
                    url,
                    message,
                    defaultValue,
                    result
                )
            }
            return super.onJsPrompt(view, url, message, defaultValue, result)
        }

        /**
         * 通知客户端要展示一个确认的对话框,
         * 如果客户端返回true,WebView将会处理这个对话框,
         * 如果返回false,那么将会给JavaScripe返回false值,
         * 默认的行为是返回false.
         */
        override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
            showLog("js 确认框")
            if (webListener != null) {
                return webListener?.onJsConfirm(view, url, message, result) ?: super.onJsConfirm(
                    view,
                    url,
                    message,
                    result
                )
            }
            return super.onJsConfirm(view, url, message, result)
        }

        override fun onJsBeforeUnload(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
            showLog("js dialog client是否处理，返回true后，webView 处理，返回false，webView 跳过处理")
            if (webListener != null) {
                return webListener?.onJsBeforeUnload(view, url, message, result) ?: super.onJsBeforeUnload(
                    view,
                    url,
                    message,
                    result
                )
            }
            return super.onJsBeforeUnload(view, url, message, result)
        }

        /**
         * 请求应用程序创建一个新的窗口,
         * 如果应用程序选择接受这个请求,它应该从这个方法返回true,
         * 然后创建一个新的WebView的窗口,将其插入到视图系统,然后提供resultMsg消息与新的WebView作为参数发送出去.
         * @param view
         * @param isDialog
         * @param isUserGesture
         * @param resultMsg
         * @return true application 接受这个请求，创建新的窗口，并插入到视图系统中，并提供resultMsg作为新窗口的参数
         *          false application不接受创建窗口，交给系统处理，默认也是返回false
         */
        override fun onCreateWindow(
            view: WebView?,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: Message?
        ): Boolean {
            showLog("js 打开新窗口")
            if (webListener != null) {
                return webListener?.onCreateWindow(view, isDialog, isUserGesture, resultMsg) ?: super.onCreateWindow(
                    view,
                    isDialog,
                    isUserGesture,
                    resultMsg
                )
            }
            return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg)
        }

        /**
         * 通知应用程序如果有必要,需要关闭和删除在视图结构中删除给定的WebView,
         * 在这一点上,将会停止正在加载的WebView和消除了任何cross-scripting的javascript的能力
         * @param window 要关闭的视图webview
         */
        override fun onCloseWindow(window: WebView?) {
            showLog("js 关闭窗口")
            if (webListener != null) {
                webListener?.onCloseWindow(window)
            } else {
                super.onCloseWindow(window)
            }
        }

        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            showLog("页面加载进度更新 $newProgress")
            if (webListener != null) {
                webListener?.onProgressChanged(view, newProgress)
            } else {
                super.onProgressChanged(view, newProgress)
            }
            if (newProgress == 100) {
                progressOver = true
                loadOver()
            }
        }

        /**
         * 通知应用程序,web内容从发出请求使用地理定位的API,但是目前没有权限状态,指定的应用程序应该获得回调所需的权限状态.
         */
        @TargetApi(24)
        override fun onGeolocationPermissionsShowPrompt(origin: String?, callback: GeolocationPermissions.Callback?) {
            showLog("js 获取定位权限弹窗")
            super.onGeolocationPermissionsShowPrompt(origin, callback)
        }

        /**
         * 通知应用程序请求地理定位权限,如果前一个地理定位权限取消了,任何相关的UI应该隐藏起来.在此方法中进行隐藏设置.
         */
        @TargetApi(24)
        override fun onGeolocationPermissionsHidePrompt() {
            showLog("js 获取定位弹窗隐藏")
            super.onGeolocationPermissionsHidePrompt()
        }

        /**
         * 通知主机应用程序Web内容需要访问本地特殊的资源对象,询问应用程序是拒绝还是允许,主机应用程序必须调用grant(String[])或者deny().
         * 如果不重写此方法，默认拒绝所有权限
         * */
        @TargetApi(21)
        override fun onPermissionRequest(request: PermissionRequest?) {
            super.onPermissionRequest(request)
//                    request.grant()
//                    request.deny()
            showLog("页面请求权限 ${request.toString()}")
        }

        /**
         * 权限请求取消
         * */
        @TargetApi(21)
        override fun onPermissionRequestCanceled(request: PermissionRequest?) {
            super.onPermissionRequestCanceled(request)
            showLog("页面请求权限 ${request.toString()} 被拒绝")
        }

        //为'<input type="file" />'显示文件选择器，返回false使用默认处理
        /**
         * 通知应用程序显示文件的选择器,这是来自HTML需要输入文件的请求,需要用户通过”选择文件”按钮来操作,如果取消请求,那么将会filePathCallback.onReceiverValue(null)并且返回true.
         */
        @TargetApi(21)
        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?
        ): Boolean {
            showLog("文件选择")
            if (webListener != null) {
                return webListener?.onShowFileChooser(webView, filePathCallback, fileChooserParams)
                    ?: super.onShowFileChooser(webView, filePathCallback, fileChooserParams)
            }
            return super.onShowFileChooser(webView, filePathCallback, fileChooserParams)
        }

        override fun onReceivedTouchIconUrl(view: WebView?, url: String?, precomposed: Boolean) {
            showLog("点击图片时响应 $url")
            if (webListener != null) {
                webListener?.onReceivedTouchIconUrl(view, url, precomposed)
            } else {
                super.onReceivedTouchIconUrl(view, url, precomposed)
            }
        }

        override fun onExceededDatabaseQuota(
            url: String?,
            databaseIdentifier: String?,
            quota: Long,
            estimatedDatabaseSize: Long,
            totalQuota: Long,
            quotaUpdater: WebStorage.QuotaUpdater?
        ) {
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
            super.onReachedMaxAppCacheSize(requiredStorage, quota, quotaUpdater)
        }


        /**
         * Obtains a list of all visited history items, used for link coloring
         * 主要用于提供链接的点击记录，给点击过的链接着色用
         */
        override fun getVisitedHistory(callback: ValueCallback<Array<String>>?) {
            super.getVisitedHistory(callback)
        }

        override fun onJsTimeout(): Boolean {
            return super.onJsTimeout()
        }
    }

    fun addOkHttpClient(client: OkHttpClient) {
        interceptor?.client = client
    }

    fun addClient(client: WebViewClient, chromeClient: WebChromeClient) {
        mWebView?.apply {
            //页面自动加载图片
//            settings.loadsImagesAutomatically = false
            webViewClient = client
            webChromeClient = chromeClient
        }
    }

    fun addDefaultClient() {
        addClient(webClient, webChromeClient)
    }

    /**用于消除web加载时白屏或黑屏问题*/
    fun noHardWareSupport() {
        mWebView?.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
    }

    /**
     * @param obj 接口对象内方法在web中调用时，需要用@JavascriptInterface修饰
     * */
    @SuppressLint("AddJavascriptInterface", "JavascriptInterface")
    fun addMutualInterface(obj: Any, name: String) {
        mWebView?.addJavascriptInterface(obj, name)
    }

    /**
     * js代码在onPageFinished后才起作用
     * @param jsContent 需要执行的javascript脚本 eg: javascript:func_name(params) or: file:///android_asset/xxxx.html
     * @param inJavaBridge 是否在web调用方法中执行，如果是，必须设置为true，否则会抛异常,5.0以下没有问题，5.0开始出现这个问题
     * {RuntimeException: A WebView method was called on thread 'JavaBridge'.All WebView methods must be called on the same thread}
     */
    fun invokeJS(jsContent: String, inJavaBridge: Boolean = false) {
        if (inJavaBridge) {
            mWebView?.post {
                mWebView?.loadUrl(jsContent)
            }
        } else {
            mWebView?.loadUrl(jsContent)
        }
    }

    /**
     * @param jsContent 需要执行的javascript脚本
     * @param resultCallback 执行脚本结果回调，从web获取数据时可以使用，android 4.4才开始支持此类调用
     * @param inJavaBridge 是否在web调用方法中执行，如果是，必须设置为true，否则会抛异常,5.0以下没有问题，5.0开始出现这个问题
     * {RuntimeException: A WebView method was called on thread 'JavaBridge'.All WebView methods must be called on the same thread}
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    fun invokeJS(jsContent: String, resultCallback: ValueCallback<String>, inJavaBridge: Boolean = false) {
        if (inJavaBridge) {
            mWebView?.post {
                mWebView?.evaluateJavascript(jsContent, resultCallback)
            }
        } else {
            mWebView?.evaluateJavascript(jsContent, resultCallback)
        }
    }

    fun loadUrl(url: String, headers: Map<String, String>?) {
//        interceptor?.overrideUrl(mWebView, url, headers)
        mWebView?.loadUrl(url, headers)
        currentUrl = url
    }

    /**打印webview回退站历史记录*/
    fun showHistories() {
        if (debug && mWebView != null) {
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

    fun isInHistories(url: String): Boolean {
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

    fun releaseWebView() {
        val parent = mWebView?.parent
        if (parent != null && parent is ViewGroup) {
            parent.removeView(mWebView)
        }
        mWebView?.removeAllViews()
        mWebView?.destroy()
        mWebView = null
    }

    private var pageFinished = false
    private var progressOver = false
    private var startTime: Long = System.currentTimeMillis()

    private fun loadOver() {
        if (progressOver && pageFinished) {
            showLog("加载页面完成")
            if (debug) {
                Toast.makeText(context, "页面加载完成", Toast.LENGTH_SHORT).show()
            }
            showLog("总共耗时 ${System.currentTimeMillis() - startTime} 毫秒")
//            showLog(JsonUtil.toJson(counter))
//            mWebView?.settings?.loadsImagesAutomatically = true
        }
    }

    private fun showLog(msg: String) {
        if (debug) {
            LogUtil.tag = tag
            LogUtil.showLog(msg)
        }
    }
}

open class WebListener {

    /**js弹窗*/
    open fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
        return false
    }

    /**js交互操作*/
    open fun onJsPrompt(
        view: WebView?,
        url: String?,
        message: String?,
        defaultValue: String?,
        result: JsPromptResult?
    ): Boolean {
        return false
    }

    /**js确认弹窗*/
    open fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
        return false
    }

    /**js 弹窗后webview是否处理*/
    open fun onJsBeforeUnload(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
        return false
    }

    /**js 创建弹窗*/
    open fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
        return false
    }

    /**js 关闭弹窗*/
    open fun onCloseWindow(window: WebView?) {

    }

    open fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: WebChromeClient.FileChooserParams?
    ): Boolean {
        return false
    }

    open fun onReceivedTouchIconUrl(view: WebView?, url: String?, precomposed: Boolean) {

    }

    open fun onProgressChanged(view: WebView?, newProgress: Int) {

    }

    open fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {

    }

    open fun onPageFinished(view: WebView?, url: String?) {

    }

    open fun onReceivedError(view: WebView?, errorCode: Int?, url: String) {

    }

    open fun onHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {

    }


    open fun onSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {

    }

    open fun onReceivedIcon(view: WebView?, icon: Bitmap?) {

    }

    open fun onReceivedTitle(view: WebView?, title: String?) {

    }

    open fun onSaleChanged(view: WebView?, oldScale: Float, newScale: Float) {

    }

    open fun getVideoLoadingProgressView(): View? {
        return null
    }

    open fun getVideoPoster(): Bitmap? {
        return null
    }

    open fun onFullView(view: View?, requestedOrientation: Int, callback: WebChromeClient.CustomViewCallback?) {

    }

    open fun onQuitFullView() {

    }

}
