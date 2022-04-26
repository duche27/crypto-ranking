package com.gui.cryptoranking;

import com.gui.cryptoranking.service.CoinsDataService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class ApplicationStartup implements ApplicationListener<ApplicationReadyEvent> {

    private final CoinsDataService coinsDataService;

    public ApplicationStartup(CoinsDataService coinsDataService) {
        this.coinsDataService = coinsDataService;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {

        coinsDataService.fetchCoins();
    }
}
