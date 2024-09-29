package com.example.displayffmpegstreamforvr.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import android.text.format.Formatter
import java.net.Inet4Address

object WifiUtil {
    fun getIpv4Address(context: Context): String? {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val connectivityManager = context.applicationContext.getSystemService(ConnectivityManager::class.java)
                val address: String? = connectivityManager.activeNetwork?.let { network ->
                    connectivityManager.getLinkProperties(network)?.let { properties ->
                        var res: String? = null
                        properties.linkAddresses.forEach { linkAddress ->
                            if (linkAddress.address is Inet4Address) {
                                linkAddress.address.hostAddress?.let { strAddr ->
                                    res = strAddr
                                    return@forEach
                                }
                            }
                        }
                        res
                    }
                }
                address
            } else {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                Formatter.formatIpAddress(wifiManager.connectionInfo.ipAddress)
            }
        }.onSuccess { return it }
        return null
    }
}