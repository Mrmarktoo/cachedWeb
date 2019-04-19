* WebView cached support library
* update CommonWebConfig ，only use config to use this lib
* use fun addClient() or addDefaultClient() to set client
* use fun addClient() to set you custom WebClient and WebChromeClient
* use fun addDefaultClient() to use the default client
* use fun addOkHttpClient() to set cache requestClient
* use fun useCached() turn on cache interceptor
* use fun applyWebSettings() apply settings:> debug //开启log
                                                 jsBridge //JavaScript 支持
                                                 zoomable//可缩放
                                                 autoWide//宽度自适应
                                                 multiWindow//多窗口
                                                 userAgent//userAgent 字符串
                                                 defaultEncoding//页面编码
                                                 cacheable//开启缓存
* use fun addInterceptor() set you custom WebRequestInterceptor
* use fun addMutualInterface() set javascriptInterface object
* use fun invokeJS() to execute javascript code
* use fun loadUrl() load web page
* use webListener to add web page listener