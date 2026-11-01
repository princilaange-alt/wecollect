package com.example.data.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

/**
 * Manara-maso ny fifandraisana amin'ny tambajotra (Network Monitor).
 * Tonga dia mandray avy hatrany (instant detection) raha vao tapaka ny internet
 * na miverina amin'ny alalan'ny NetworkCallback sy BroadcastReceiver.
 */
class NetworkMonitor(private val context: Context) {

    private val connectivityManager: ConnectivityManager? = try {
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    } catch (e: Throwable) {
        null
    }

    val isOnline: Flow<Boolean> = callbackFlow {
        // Handefa ny toe-javatra misy ankehitriny avy hatrany
        trySend(isCurrentlyConnected())

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                try {
                    trySend(true)
                } catch (_: Throwable) {}
            }

            override fun onLost(network: Network) {
                try {
                    // Tonga dia miova Hors-ligne direct tsy misy fihodinana
                    trySend(false)
                } catch (_: Throwable) {
                    trySend(false)
                }
            }

            override fun onUnavailable() {
                try {
                    trySend(false)
                } catch (_: Throwable) {}
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                try {
                    val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    if (!hasInternet) {
                        trySend(false)
                    } else {
                        val isValidated = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                        } else {
                            true
                        }
                        val hasKnownTransport = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
                        val isNotRestricted = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                        val isOnline = isValidated || hasKnownTransport || isNotRestricted
                        trySend(isOnline)
                    }
                } catch (_: Throwable) {
                    trySend(false)
                }
            }
        }

        var registeredCallback = false
        try {
            val cm = connectivityManager
            if (cm != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    cm.registerDefaultNetworkCallback(callback)
                    registeredCallback = true
                } else {
                    val request = NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build()
                    cm.registerNetworkCallback(request, callback)
                    registeredCallback = true
                }
            }
        } catch (_: Throwable) {
            trySend(isCurrentlyConnected())
        }

        // Fanampiny: BroadcastReceiver mba handray avy hatrany ny CONNECTIVITY_ACTION avy amin'ny Android OS
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                try {
                    trySend(isCurrentlyConnected())
                } catch (_: Throwable) {}
            }
        }
        var registeredReceiver = false
        try {
            @Suppress("DEPRECATION")
            val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
            context.applicationContext.registerReceiver(receiver, filter)
            registeredReceiver = true
        } catch (_: Throwable) {}

        awaitClose {
            if (registeredCallback) {
                try {
                    connectivityManager?.unregisterNetworkCallback(callback)
                } catch (_: Throwable) {}
            }
            if (registeredReceiver) {
                try {
                    context.applicationContext.unregisterReceiver(receiver)
                } catch (_: Throwable) {}
            }
        }
    }.conflate()

    fun isCurrentlyConnected(): Boolean {
        return try {
            val cm = connectivityManager ?: return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val activeNetwork = cm.activeNetwork ?: return false
                val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
                val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                val isValidated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                val hasKnownTransport = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
                val isNotRestricted = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                hasInternet && (isValidated || hasKnownTransport || isNotRestricted)
            } else {
                @Suppress("DEPRECATION")
                val activeInfo = cm.activeNetworkInfo ?: return false
                @Suppress("DEPRECATION")
                activeInfo.isConnected
            }
        } catch (e: Throwable) {
            false
        }
    }
}

