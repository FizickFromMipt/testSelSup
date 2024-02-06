package ru.pevnenko;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class CrptApi {
    private final int requestLimit;
    private final long timeIntervalMillis;
    private final ReentrantLock lock = new ReentrantLock();
    private long lastRequestTime = System.currentTimeMillis();
    private int requestCount = 0;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.requestLimit = requestLimit;
        this.timeIntervalMillis = timeUnit.toMillis(1);
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public void createDocument(Object document, String signature) {
        try {
            lock.lock();
            checkRequestLimit();

            // Сериализация объекта в JSON с использованием Jackson
            String requestBody = objectMapper.writeValueAsString(new CrptRequest(document, signature));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Обработка ответа API
            System.out.println("Response code: " + response.statusCode());
            System.out.println("Response body: " + response.body());

            requestCount++;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    private void checkRequestLimit() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastRequestTime >= timeIntervalMillis) {
            lastRequestTime = currentTime;
            requestCount = 0;
        }
        if (requestCount >= requestLimit) {
            try {
                Thread.sleep(timeIntervalMillis - (currentTime - lastRequestTime));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            lastRequestTime = System.currentTimeMillis();
            requestCount = 0;
        }
    }

    private static class CrptRequest {
        private final Object document;
        private final String signature;

        public CrptRequest(Object document, String signature) {
            this.document = document;
            this.signature = signature;
        }

        public Object getDocument() {
            return document;
        }

        public String getSignature() {
            return signature;
        }
    }
}
