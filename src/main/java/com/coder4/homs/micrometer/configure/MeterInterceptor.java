package com.coder4.homs.micrometer.configure;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

// https://www.baeldung.com/spring-mvc-handlerinterceptor
public class MeterInterceptor implements HandlerInterceptor {

    @Autowired
    private MeterRegistry meterRegistry;

    private ThreadLocal<Long> tlTimer = new ThreadLocal<>();

    private static Optional<String> getMethod(HttpServletRequest request, Object handler) {
        if (handler instanceof HandlerMethod) {
            return Optional.of(String.format("%s_%s_%s", ((HandlerMethod) handler).getBeanType().getSimpleName(),
                    ((HandlerMethod) handler).getMethod().getName(), request.getMethod()));
        } else {
            return Optional.empty();
        }
    }

    private void recordTimeDistribution(HttpServletRequest request, Object handler, long ms) {
        Optional<String> methodOp = getMethod(request, handler);
        if (methodOp.isPresent()) {
            DistributionSummary.builder("app_requests_time_ms")
                    .tag("method", methodOp.get())
                    .publishPercentiles(0.5, 0.95)
                    .publishPercentileHistogram()
                    .register(meterRegistry)
                    .record(ms);
        }
    }

    public Optional<Counter> getCounterOfTotalCounts(HttpServletRequest request, Object handler) {
        Optional<String> methodOp = getMethod(request, handler);
        if (methodOp.isPresent()) {
            return Optional.of(meterRegistry.counter("app_requests_total_counts", "method",
                    methodOp.get()));
        } else {
            return Optional.empty();
        }
    }

    public Optional<Counter> getCounterOfExceptionCounts(HttpServletRequest request, Object handler) {
        Optional<String> methodOp = getMethod(request, handler);
        if (methodOp.isPresent()) {
            return Optional.of(meterRegistry.counter("app_requests_exption_counts", "method",
                    methodOp.get()));
        } else {
            return Optional.empty();
        }
    }

    public Optional<Counter> getCounterOfRespCodeCounts(HttpServletRequest request, HttpServletResponse response,
                                                        Object handler) {
        Optional<String> methodOp = getMethod(request, handler);
        if (methodOp.isPresent()) {
            return Optional.of(meterRegistry.counter(String.format("app_requests_resp%d_counts", response.getStatus()),
                    "method", methodOp.get()));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        tlTimer.set(System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // record time
        recordTimeDistribution(request, handler, System.currentTimeMillis() - tlTimer.get());
        tlTimer.remove();

        // total counts
        getCounterOfTotalCounts(request, handler).ifPresent(counter -> counter.increment());
        // different response code count
        getCounterOfRespCodeCounts(request, response, handler).ifPresent(counter -> counter.increment());
        if (ex != null) {
            // exception counts
            getCounterOfExceptionCounts(request, handler).ifPresent(counter -> counter.increment());
        }
    }

}
