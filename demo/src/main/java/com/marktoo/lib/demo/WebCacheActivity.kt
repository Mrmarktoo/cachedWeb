package com.marktoo.lib.demo

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.AdapterView
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import com.marktoo.lib.cacheweb.CommonWebConfig
import com.marktoo.lib.cacheweb.LogUtil
import com.marktoo.lib.cacheweb.WebListener
import com.marktoo.lib.demo.R.*


class WebCacheActivity : AppCompatActivity() {
    var mWebView: WebView? = null
    lateinit var urls: Array<String>
    lateinit var tvJsTitle: TextView
    lateinit var tvListenerTitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout.activity_web_cache)

        mWebView = findViewById(id.webView)
        tvJsTitle = findViewById(id.tv_web_title)
        tvListenerTitle = findViewById(id.tv_listener_title)
        initInterceptor()
//        initWebView()
        urls = resources.getStringArray(array.urls)

        findViewById<Spinner>(id.spinner).onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                showLog("onNothingSelected")
                loadUrl(urls[0])
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                showLog("onItemSelected")
                loadUrl(urls[position])
            }
        }

        findViewById<Switch>(id.toggleButton).setOnCheckedChangeListener { _, isChecked ->
            commWeb.useCached(isChecked)
        }
    }

    private lateinit var commWeb: CommonWebConfig

    private fun initInterceptor() {
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
        commWeb.useCached(false)
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
        showLog("overrideUrl= $url")
        commWeb.loadUrl(url, null)
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
        if (mWebView!!.parent is ViewGroup) {
            (mWebView!!.parent as ViewGroup).removeView(mWebView)
        }
        mWebView!!.destroy()
        mWebView = null
    }

    override fun onBackPressed() {
        if (mWebView!!.canGoBack()) {
            mWebView!!.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
