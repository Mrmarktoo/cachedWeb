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

#version update record:
###1.0.0.8:
 ```
 更新shouldOverrideUrlLoading，适配8.0,8.0后此方法返回true表示不重载，与之前版本正好相反
 更新log相关，cachedweb和logutil均可以配置是否显示，debug字段
 增加cachedweb中方法，可以释放webview->releaseWebView(),加载链接不再走interceptor中的override方法，直接用webview加载url
 ```
### 1.0.0.11
```
增加js方法传参生成调用方法，提供方法名，参数数组，自动拼接调用功能
优化日历打印，提供打印log 标签配置功能，可以动态配置打印log范围
```

