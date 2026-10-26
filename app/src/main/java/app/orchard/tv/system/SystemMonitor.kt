package app.orchard.tv.system

import android.app.ActivityManager
import android.content.Context
import android.net.TrafficStats
import android.os.Build
import android.os.Debug
import android.os.Process
import android.os.StatFs
import android.os.SystemClock
import android.text.format.Formatter
import app.orchard.tv.ui.components.clearBakes
import org.json.JSONObject
import java.io.File
import java.net.NetworkInterface

/** Read-only snapshot; unsupported vendor metrics are explicitly absent. */
class SystemMonitor(private val context: Context) {
    private var lastWall = SystemClock.elapsedRealtime()
    private var lastCpu = Process.getElapsedCpuTime()
    private var lastRx = TrafficStats.getTotalRxBytes()
    private var lastTx = TrafficStats.getTotalTxBytes()

    @Synchronized fun snapshot(): JSONObject {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memory = ActivityManager.MemoryInfo().also(am::getMemoryInfo)
        val pss = Debug.MemoryInfo().also(Debug::getMemoryInfo).totalPss * 1024L
        val disk = StatFs(context.filesDir.path)
        val now = SystemClock.elapsedRealtime(); val elapsed = (now - lastWall).coerceAtLeast(1)
        val cpu = Process.getElapsedCpuTime()
        val rx = TrafficStats.getTotalRxBytes(); val tx = TrafficStats.getTotalTxBytes()
        val appCpu = ((cpu - lastCpu) * 100f / elapsed / Runtime.getRuntime().availableProcessors()).coerceIn(0f, 100f)
        val ips = runCatching {
            NetworkInterface.getNetworkInterfaces().toList().filter { it.isUp && !it.isLoopback }.flatMap { n ->
                n.inetAddresses.toList().filter { !it.isLoopbackAddress && !it.isLinkLocalAddress }.map { "${n.name} · ${it.hostAddress}" }
            }.joinToString("\n")
        }.getOrDefault("")
        val result = JSONObject().put("model", "${Build.MANUFACTURER} ${Build.MODEL}").put("android", Build.VERSION.RELEASE)
            .put("cpu", if (Build.VERSION.SDK_INT >= 31) "${Build.SOC_MANUFACTURER} ${Build.SOC_MODEL}" else Build.HARDWARE).put("cores", Runtime.getRuntime().availableProcessors()).put("appCpuPercent", appCpu.toDouble())
            .put("memoryLow", memory.lowMemory).put("memoryAvailable", memory.availMem).put("memoryTotal", memory.totalMem).put("memoryUsed", memory.totalMem - memory.availMem).put("appMemory", pss)
            .put("storageTotal", disk.totalBytes).put("storageUsed", disk.totalBytes - disk.availableBytes)
            .put("network", SystemProbe.network(context).kind.name).put("addresses", ips)
            .put("rxPerSecond", if (rx >= lastRx && lastRx >= 0) (rx - lastRx) * 1000 / elapsed else -1)
            .put("txPerSecond", if (tx >= lastTx && lastTx >= 0) (tx - lastTx) * 1000 / elapsed else -1)
        lastWall = now; lastCpu = cpu; lastRx = rx; lastTx = tx
        return result
    }

    companion object {
        fun bytes(context: Context, n: Long): String = Formatter.formatShortFileSize(context, n.coerceAtLeast(0))

        /** Evict only reproducible tile cache files; user files, transfers and wallpapers stay. */
        fun clean(context: Context): Long {
            clearBakes()
            app.orchard.tv.wallpaper.Wallpapers.clearThumbnailCache()
            var removed = 0L
            File(context.cacheDir, "tiles").listFiles()?.filter { it.isFile }?.forEach {
                val size = it.length(); if (it.delete()) removed += size
            }
            return removed
        }
    }
}
