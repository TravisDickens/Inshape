package com.travis.inshape
import android.content.Context
import android.net.ConnectivityManager

class NetworkUtils {
    companion object{
        fun isNetworkAvailable(context : Context) : Boolean{
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnected
        }
    }
}