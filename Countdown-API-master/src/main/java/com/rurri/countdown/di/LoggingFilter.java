package com.rurri.countdown.di;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.UUID;

public class LoggingFilter implements javax.servlet.Filter {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        if (servletRequest instanceof HttpServletRequest) {
            String requestId = ((HttpServletRequest) servletRequest).getHeader("Request-Id");
            if (requestId == null) {
                requestId = UUID.randomUUID().toString();
            }
            org.slf4j.MDC.put("request_id", requestId);
            org.slf4j.MDC.put("ip", servletRequest.getRemoteAddr());

            org.slf4j.MDC.put("request_url", ((HttpServletRequest) servletRequest).getRequestURL().toString());

            long startTime = System.currentTimeMillis();

            try {
                filterChain.doFilter(servletRequest,servletResponse);
            } finally {
                long totalTime = System.currentTimeMillis() - startTime;
                String zeroPadded = "99999";
                if (totalTime < 99999) {
                    zeroPadded = String.format("%05d", totalTime);
                }
                org.slf4j.MDC.put("page_load_time", zeroPadded);

                logger.info("Page finished loading in " + totalTime + "ms");
                for (Object key : org.slf4j.MDC.getCopyOfContextMap().keySet()) {
                    org.slf4j.MDC.remove((String)key);
                }
            }

        }
    }

    @Override
    public void destroy() {

    }
}