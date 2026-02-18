package com.example.currencyparser.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class ThreadingConfig {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService currencyExecutorService() {
        return Executors.newFixedThreadPool(6, namedFactory("currency-worker-", false));
    }

    @Bean(destroyMethod = "shutdown")
    public ScheduledExecutorService currencyScheduledExecutorService() {
        return Executors.newSingleThreadScheduledExecutor(namedFactory("currency-scheduler-", true));
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public WebClient webClient() {
        return WebClient.builder().build();
    }

    private ThreadFactory namedFactory(String prefix, boolean daemon) {
        AtomicInteger counter = new AtomicInteger(1);
        return runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName(prefix + counter.getAndIncrement());
            thread.setDaemon(daemon);
            return thread;
        };
    }
}
