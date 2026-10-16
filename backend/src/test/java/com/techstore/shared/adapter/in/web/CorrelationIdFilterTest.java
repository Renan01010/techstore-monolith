package com.techstore.shared.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void echoesSafeIncomingCorrelationIdAndClearsLoggingContext() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "request-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isEqualTo("request-123"));

        assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME)).isEqualTo("request-123");
        assertThat(request.getAttribute(CorrelationIdFilter.MDC_KEY)).isEqualTo("request-123");
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void generatesCorrelationIdWhenIncomingValueIsMissingOrUnsafe() throws Exception {
        for (String incoming : new String[] { null, "not safe\nvalue", "x".repeat(129) }) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            if (incoming != null) {
                request.addHeader(CorrelationIdFilter.HEADER_NAME, incoming);
            }
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, (servletRequest, servletResponse) -> { });

            String generatedId = response.getHeader(CorrelationIdFilter.HEADER_NAME);
            assertThat(generatedId).isNotBlank().matches("[0-9a-f-]{36}");
            assertThat(request.getAttribute(CorrelationIdFilter.MDC_KEY)).isEqualTo(generatedId);
            assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
        }
    }
}
