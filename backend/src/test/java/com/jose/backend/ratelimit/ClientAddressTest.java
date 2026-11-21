package com.jose.backend.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientAddressTest {

    private static String keyOf(String address) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(address);
        return ClientAddress.keyOf(request);
    }

    @Test
    void anIpv4AddressIsItsOwnKey() {
        assertThat(keyOf("203.0.113.7")).isEqualTo("203.0.113.7");
    }

    @Test
    void ipv6AddressesOfTheSameBlockShareTheKey() {
        assertThat(keyOf("2001:db8:1:2:aaaa:bbbb:cccc:dddd"))
                .isEqualTo(keyOf("2001:db8:1:2:1111:2222:3333:4444"));
        assertThat(keyOf("2001:db8:1:2::1")).isNotEqualTo(keyOf("2001:db8:1:3::1"));
    }

    @Test
    void anIpv4AddressMappedInIpv6IsTheSameClient() {
        assertThat(keyOf("::ffff:203.0.113.7")).isEqualTo("203.0.113.7");
    }
}
