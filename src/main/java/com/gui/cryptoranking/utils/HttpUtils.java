package com.gui.cryptoranking.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Collections;

@Slf4j
public class HttpUtils {

    private static String xRapidAPIhost = "coinranking1.p.rapidapi.com";
    private static String xRapidAPIkey = "3f9727135emsh4c15435be0bd699p1cbd09jsnb0b85aa6c933";

    public static HttpEntity<String> getHttpEntity() {
        log.info("apiHost: " + xRapidAPIhost);
        log.info("apiKey: " + xRapidAPIkey);
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("X-RapidAPI-Host", xRapidAPIhost);
        headers.set("X-RapidAPI-Key", xRapidAPIkey);

        return new HttpEntity<>(null, headers);
    }
}
