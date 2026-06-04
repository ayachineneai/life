package life.infra.proxy

import java.net.InetSocketAddress
import java.net.Proxy

data class ProxyConfig(
    val host: String,
    val port: Int
) {
    fun toProxy(): Proxy {
        return Proxy(Proxy.Type.HTTP, InetSocketAddress(host, port))
    }
}
