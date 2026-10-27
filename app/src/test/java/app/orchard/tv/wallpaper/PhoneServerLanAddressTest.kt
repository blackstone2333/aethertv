package app.orchard.tv.wallpaper

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkAddress
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNetwork
import org.robolectric.shadows.ShadowNetworkCapabilities
import org.robolectric.shadows.ShadowNetworkInfo
import org.robolectric.util.ReflectionHelpers
import java.net.InetAddress

/** Synthetic addresses only; no server, VPN, device, or real network configuration is changed. */
@Suppress("DEPRECATION")
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [23, 34])
class PhoneServerLanAddressTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val connectivity = shadowOf(cm)

    @Before fun offline() {
        connectivity.setActiveNetworkInfo(null)
        connectivity.clearAllNetworks()
        connectivity.setDefaultNetworkActive(true)
    }

    private fun network(type: Int, transport: Int?, vararg addresses: String, active: Boolean = false): Network {
        val info = ShadowNetworkInfo.newInstance(NetworkInfo.DetailedState.CONNECTED, type, 0, true, true)
        val network = ShadowNetwork.newInstance(type)
        if (active) connectivity.setActiveNetworkInfo(info)
        connectivity.addNetwork(network, info)
        val caps = transport?.let { ShadowNetworkCapabilities.newInstance().apply { shadowOf(this).addTransportType(it) } }
        connectivity.setNetworkCapabilities(network, caps)
        connectivity.setLinkProperties(network, LinkProperties().apply {
            addresses.forEach { address ->
                // These framework builders are hidden from the SDK, but available to the test VM.
                val link = ReflectionHelpers.callConstructor(LinkAddress::class.java,
                    ReflectionHelpers.ClassParameter.from(InetAddress::class.java, InetAddress.getByName(address.substringBefore('/'))),
                    ReflectionHelpers.ClassParameter.from(Int::class.javaPrimitiveType, address.substringAfter('/').toInt()))
                ReflectionHelpers.callInstanceMethod<Boolean>(this, "addLinkAddress",
                    ReflectionHelpers.ClassParameter.from(LinkAddress::class.java, link))
            }
        })
        return network
    }

    @Test fun wifiIsReachableInsteadOfDefaultProxyTunnel() {
        network(ConnectivityManager.TYPE_VPN, NetworkCapabilities.TRANSPORT_VPN, "198.18.0.1/32", active = true)
        network(ConnectivityManager.TYPE_WIFI, NetworkCapabilities.TRANSPORT_WIFI, "10.20.30.40/24")
        assertEquals("10.20.30.40", PhoneServer.lanAddress(context))
    }

    @Test fun ethernetIsReachableInsteadOfDefaultProxyTunnel() {
        network(ConnectivityManager.TYPE_VPN, NetworkCapabilities.TRANSPORT_VPN, "198.19.255.254/32", active = true)
        network(ConnectivityManager.TYPE_ETHERNET, NetworkCapabilities.TRANSPORT_ETHERNET, "10.20.40.50/24")
        assertEquals("10.20.40.50", PhoneServer.lanAddress(context))
    }

    @Test fun activePhysicalNetworkWinsWhenWifiAndEthernetBothExist() {
        network(ConnectivityManager.TYPE_WIFI, NetworkCapabilities.TRANSPORT_WIFI, "10.20.30.40/24")
        network(ConnectivityManager.TYPE_ETHERNET, NetworkCapabilities.TRANSPORT_ETHERNET, "10.20.40.50/24", active = true)
        assertEquals("10.20.40.50", PhoneServer.lanAddress(context))
    }

    @Test fun rejectsUnreachableAddressesIncludingBothHalvesOfBenchmarkRange() {
        for (address in listOf("127.0.0.1/8", "169.254.2.3/16", "198.18.0.1/15", "198.19.255.254/15", "0.0.0.0/32")) {
            network(ConnectivityManager.TYPE_WIFI, NetworkCapabilities.TRANSPORT_WIFI, address, active = true)
            assertNull(address, PhoneServer.lanAddress(context))
        }
    }

    @Test fun benchmarkFilterDoesNotExcludeAdjacentUsableRange() {
        network(ConnectivityManager.TYPE_WIFI, NetworkCapabilities.TRANSPORT_WIFI, "198.20.0.8/24", active = true)
        assertEquals("198.20.0.8", PhoneServer.lanAddress(context))
    }

    @Test fun fallsBackWhenFirmwareDoesNotExposeTransportCapabilities() {
        network(ConnectivityManager.TYPE_WIFI, null, "10.20.30.40/24", active = true)
        assertEquals("10.20.30.40", PhoneServer.lanAddress(context))
    }

    @Test fun ordinaryPrivateVpnAddressCannotBecomeTheFallback() {
        network(ConnectivityManager.TYPE_VPN, NetworkCapabilities.TRANSPORT_VPN, "10.8.0.2/24", active = true)
        assertNull(PhoneServer.lanAddress(context))
    }

    @Test fun vpnReportingWifiTransportIsStillNotThePhysicalLan() {
        val vpn = network(ConnectivityManager.TYPE_VPN, NetworkCapabilities.TRANSPORT_VPN, "10.8.0.2/24", active = true)
        shadowOf(cm.getNetworkCapabilities(vpn)!!).addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        network(ConnectivityManager.TYPE_ETHERNET, NetworkCapabilities.TRANSPORT_ETHERNET, "10.20.40.50/24")
        assertEquals("10.20.40.50", PhoneServer.lanAddress(context))
    }

    @Test fun physicalLanDoesNotRequireAReportedDefaultNetwork() {
        network(ConnectivityManager.TYPE_WIFI, NetworkCapabilities.TRANSPORT_WIFI, "10.20.30.40/24")
        assertEquals("10.20.30.40", PhoneServer.lanAddress(context))
    }

    @Test fun offlineReturnsUnavailable() {
        assertNull(PhoneServer.lanAddress(context))
    }

    @Test fun ipv6OnlyNetworkReturnsUnavailableForIpv4ServerUrl() {
        network(ConnectivityManager.TYPE_WIFI, NetworkCapabilities.TRANSPORT_WIFI, "2001:db8::40/64", active = true)
        assertNull(PhoneServer.lanAddress(context))
    }

    @Test fun usableIpv4AfterUnreachableAddressIsStillSelected() {
        network(ConnectivityManager.TYPE_WIFI, NetworkCapabilities.TRANSPORT_WIFI,
            "169.254.2.3/16", "198.19.0.1/15", "10.20.30.40/24", active = true)
        assertEquals("10.20.30.40", PhoneServer.lanAddress(context))
    }
}
