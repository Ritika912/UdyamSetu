package com.example.udyamsetubackend.contact.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Enforces a hard limit of {@value #MAX_BODY_BYTES} bytes on the request body of {@code /api/contact}.
 *
 * <p>The declared {@code Content-Length} is checked first, as a fast path. It is not trusted, because chunked
 * requests have none and a client can lie about it. The body itself is read with a byte limit, so at most
 * {@code MAX_BODY_BYTES + 1} bytes are ever buffered. A body within the limit is replayed to the rest of the
 * chain from memory.
 */
public final class ContactBodySizeFilter extends OncePerRequestFilter {

    static final int MAX_BODY_BYTES = 16 * 1024;
    static final String TOO_LARGE_MESSAGE = "The request body must not exceed 16 KB.";

    private static final int READ_BUFFER_BYTES = 4096;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getContentLengthLong() > MAX_BODY_BYTES) {
            JsonFilterErrors.write(response, HttpStatus.CONTENT_TOO_LARGE, TOO_LARGE_MESSAGE);
            return;
        }
        byte[] body;
        try (InputStream in = request.getInputStream()) {
            body = readAtMost(in, MAX_BODY_BYTES + 1);
        }
        if (body.length > MAX_BODY_BYTES) {
            JsonFilterErrors.write(response, HttpStatus.CONTENT_TOO_LARGE, TOO_LARGE_MESSAGE);
            return;
        }
        chain.doFilter(new CachedBodyRequest(request, body), response);
    }

    /** Reads at most {@code limit} bytes, stopping at end of stream. */
    static byte[] readAtMost(InputStream in, int limit) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[READ_BUFFER_BYTES];
        int total = 0;
        while (total < limit) {
            int read = in.read(buffer, 0, Math.min(buffer.length, limit - total));
            if (read == -1) {
                break;
            }
            out.write(buffer, 0, read);
            total += read;
        }
        return out.toByteArray();
    }

    /** Serves an already-read body, so the controller can still read it after the size check. */
    private static final class CachedBodyRequest extends HttpServletRequestWrapper {

        private final byte[] body;

        CachedBodyRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream in = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return in.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    throw new UnsupportedOperationException("Asynchronous reads are not supported for this request.");
                }

                @Override
                public int read() {
                    return in.read();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
