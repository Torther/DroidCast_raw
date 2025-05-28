package ink.mol.droidcast_raw

import android.os.Build

object NativeLibHelper {
    fun loadLibs(): Boolean {
        // Try load native lib, disable native copy if failed
        try {
            val classpath = System.getenv("CLASSPATH")
            val abi = Build.SUPPORTED_ABIS[0]
            if (classpath != null && abi != null) {
                System.load("$classpath!/lib/$abi/libdroidcast_raw.so")
            } else {
                System.loadLibrary("droidcast_raw")
            }
            return true
        } catch (e: UnsatisfiedLinkError) {
            e.printStackTrace()
        }
        return false
    }
}