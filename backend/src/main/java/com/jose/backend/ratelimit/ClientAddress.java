package com.jose.backend.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HexFormat;

final class ClientAddress {

    private ClientAddress() {}

    /**
     * The key used to count the requests of a client. An IPv6 client usually controls a whole
     * /64 block, so those addresses are grouped by their prefix: otherwise changing the
     * address inside the block would bypass the limit.
     */
    static String keyOf(HttpServletRequest request) {
        String address = request.getRemoteAddr();
        if (address == null || address.indexOf(':') < 0) {
            return String.valueOf(address);
        }
        try {
            // The text is an IP literal: this does not query any DNS server
            InetAddress inet = InetAddress.getByName(address);
            if (inet instanceof Inet6Address) {
                return HexFormat.of().formatHex(inet.getAddress(), 0, 8);
            }
            return inet.getHostAddress();
        } catch (UnknownHostException e) {
            return address;
        }
    }
}
