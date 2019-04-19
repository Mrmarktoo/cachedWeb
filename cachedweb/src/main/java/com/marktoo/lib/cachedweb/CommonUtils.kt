package com.marktoo.lib.cacheweb

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import android.webkit.URLUtil
import com.google.gson.Gson
import com.marktoo.lib.cacheweb.LogUtil.showLog
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.lang.StringBuilder
import java.net.URL

/**
 * @className: CommonUtils
 * @desc: TODO
 * @author: zyy
 * @date: 2019/4/11 16:42
 * @company: joinUTech
 * @leader: ke
 */
object LogUtil {
    var tag: String = "LogUtil"
    fun showLog(msg: String) {
        Log.e(tag, msg)
    }

    fun showLog(obj: Any) {
        showLog(JsonUtil.toJson(obj))
    }
}

object JsonUtil {
    val json: Gson = Gson()

    fun toJson(obj: Any): String = json.toJson(obj)

    inline fun <reified T> fromJson(jsonStr: String): T = json.fromJson(jsonStr, T::class.java)
}

object NetUtil {
    fun isValidUrl(url: String): Boolean = URLUtil.isValidUrl(url)

    fun getOriginUrl(url: String?): String {
        if (url.isNullOrEmpty()) return ""
        val uRL = URL(url)
        val port = uRL.port
        val sb = StringBuilder()
        sb.append("${uRL.protocol} :// ${uRL.host}")
        if (port != -1) {
            sb.append(": $port")
        }
        return sb.toString()
    }

    fun isConnected(context: Context?): Boolean {
        if (context == null) {
            return false
        }
        val cm = context.getSystemService(Activity.CONNECTIVITY_SERVICE) as ConnectivityManager
        val ani = cm.activeNetworkInfo
        return ani != null && ani.isConnected
    }

    fun isNetworkConnected(context: Context?): Boolean {
        if (context != null) {
            val mConnectivityManager = context
                    .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val mNetworkInfo = mConnectivityManager.activeNetworkInfo
            if (mNetworkInfo != null) {
                return mNetworkInfo.isConnected
            }
        }
        return false
    }

    fun isWifiConnected(context: Context?): Boolean {
        if (context != null) {
            val mConnectivityManager = context
                    .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val mWiFiNetworkInfo = mConnectivityManager
                    .getNetworkInfo(ConnectivityManager.TYPE_WIFI)
            if (mWiFiNetworkInfo != null) {
                return mWiFiNetworkInfo.isAvailable
            }
        }
        return false
    }

    fun isMobileConnected(context: Context?): Boolean {
        if (context != null) {
            val mConnectivityManager = context
                    .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val mMobileNetworkInfo = mConnectivityManager
                    .getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
            if (mMobileNetworkInfo != null) {
                return mMobileNetworkInfo.isAvailable
            }
        }
        return false
    }

    fun getConnectedType(context: Context?): Int {
        if (context != null) {
            val mConnectivityManager = context
                    .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val mNetworkInfo = mConnectivityManager.activeNetworkInfo
            if (mNetworkInfo != null && mNetworkInfo.isAvailable) {
                return mNetworkInfo.type
            }
        }
        return -1
    }

    /**
     * 判断是否有网络连接
     * 可以是公网或内网地址，域名或者ip
     * @param address ping 的地址，可以换成任何一种可靠的外网
     * @return
     */
    fun ping(address: String): Boolean {

        var result: String? = null
        try {
            // ping网址3次
            val p = Runtime.getRuntime().exec("ping -c 3 -w 100 $address")
            // 读取ping的内容，可以不加
            val input = p.inputStream
            val br = BufferedReader(InputStreamReader(input))
            val stringBuilder = StringBuilder()
            var content: String = br.readLine()
            while (content != null) {
                stringBuilder.append(content)
                content = br.readLine()
            }
            showLog("result content : " + stringBuilder.toString())
            // ping的状态
            val status = p.waitFor()
            if (status == 0) {
                result = "success"
                return true
            } else {
                result = "failed"
            }
        } catch (e: IOException) {
            result = "IOException"
        } catch (e: InterruptedException) {
            result = "InterruptedException"
        } finally {
            showLog("result = " + result!!)
        }
        return false
    }
}

object FileUtil {
    fun deleteFiles(file: File, deleteDir: Boolean) {
        if (!file.exists()) return
        val files = file.listFiles()
        files.forEach {
            if (it.isDirectory) {
                deleteFiles(it, !deleteDir)
            } else {
                it.delete()
            }
        }
        if (deleteDir) {
            file.delete()
        }
    }

    fun isHtml(url: String?): Boolean {
        if (url.isNullOrEmpty()) return false
        return url.toLowerCase().endsWith("html") or url.toLowerCase().endsWith("htm")
    }
}