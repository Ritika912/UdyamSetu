package com.example.udyamsetubackend.contact.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Body-size enforcement. The tests cover a declared length that is wrong, and a body with no declared length
 * (chunked), because the limit must hold on the bytes actually read.
 */
class ContactBodySizeFilterTest {

    private static final int LIMIT = ContactBodySizeFilter.MAX_BODY_BYTES;

    private final ContactBodySizeFilter filter = new ContactBodySizeFilter();

    @Test
    void bodyExactlyAtTheLimitIsAcceptedAndReplayedToTheChain() throws Exception {
        byte[] body = "a".repeat(LIMIT).getBytes(StandardCharsets.UTF_8);
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request(body, LIMIT), response, chain);

        assertNotNull(chain.getRequest(), "a body at the limit must pass");
        assertEquals(200, response.getStatus());
        assertEquals(LIMIT, chain.getRequest().getInputStream().readAllBytes().length);
    }

    @Test
    void bodyOneByteOverTheLimitIsRefusedWith413Json() throws Exception {
        byte[] body = "a".repeat(LIMIT + 1).getBytes(StandardCharsets.UTF_8);
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request(body, LIMIT + 1), response, chain);

        assertEquals(413, response.getStatus());
        assertEquals("application/json", response.getContentType());
        assertTrue(response.getContentAsString().contains(ContactBodySizeFilter.TOO_LARGE_MESSAGE));
        assertNull(chain.getRequest());
    }

    @Test
    void declaredLengthOverTheLimitIsRefusedEvenBeforeReading() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("small".getBytes(StandardCharsets.UTF_8), LIMIT + 1), response, chain);

        assertEquals(413, response.getStatus());
        assertNull(chain.getRequest());
    }

    @Test
    void understatedDeclaredLengthCannotSmuggleAnOversizedBody() throws Exception {
        // The header says 10 bytes, but the stream carries far more. Only the bytes actually read can count.
        byte[] body = "a".repeat(LIMIT * 2).getBytes(StandardCharsets.UTF_8);
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request(body, 10), response, chain);

        assertEquals(413, response.getStatus());
        assertNull(chain.getRequest());
    }

    @Test
    void chunkedBodyWithNoDeclaredLengthIsEnforcedByBytesRead() throws Exception {
        byte[] body = "a".repeat(LIMIT + 100).getBytes(StandardCharsets.UTF_8);
        MockHttpServletRequest request = request(body, -1); // -1: no Content-Length, as with chunked transfer
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertEquals(413, response.getStatus());
        assertNull(chain.getRequest());
    }

    @Test
    void readAtMostNeverBuffersBeyondTheLimit() throws IOException {
        byte[] body = "a".repeat(LIMIT * 3).getBytes(StandardCharsets.UTF_8);

        byte[] read = ContactBodySizeFilter.readAtMost(new java.io.ByteArrayInputStream(body), LIMIT + 1);

        assertEquals(LIMIT + 1, read.length);
    }

    /**
     * A POST whose declared length is exactly {@code declaredLength} (or -1 for none), regardless of the body that
     * is actually sent. The mock would otherwise derive its own length from the body.
     */
    private static MockHttpServletRequest request(byte[] body, long declaredLength) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/contact") {
            @Override
            public long getContentLengthLong() {
                return declaredLength;
            }
        };
        request.setContent(body);
        return request;
    }
}
