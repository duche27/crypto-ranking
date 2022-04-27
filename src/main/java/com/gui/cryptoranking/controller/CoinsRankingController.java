package com.gui.cryptoranking.controller;

import com.gui.cryptoranking.model.CoinInfo;
import com.gui.cryptoranking.model.HistoryData;
import com.gui.cryptoranking.service.CoinsDataService;
import com.gui.cryptoranking.utils.Utility;
import io.github.dengliming.redismodule.redistimeseries.Sample;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@Slf4j
@CrossOrigin("http://localhost:3000")
@RequestMapping("api/v1/coins")
public class CoinsRankingController {

    private final CoinsDataService coinsDataService;

    public CoinsRankingController(CoinsDataService coinsDataService) {
        this.coinsDataService = coinsDataService;
    }

    @GetMapping
    public ResponseEntity<List<CoinInfo>> fetchAllCoins() {

        log.info("Fetching all coins");

//        return ResponseEntity.ok(coinsDataService.fetchAllCoinsFromRedisJSON());
        return ResponseEntity.ok().body(coinsDataService.getAllCoinsInfoFromRedisJSON());
    }

    @GetMapping("/{symbol}/{timePeriod}")
    public List<HistoryData> fetchCoinHistoryPerTimePeriod(
            @PathVariable String symbol, @PathVariable String timePeriod) {

        log.info("Fetching coin history for {} within the time period of {}", symbol, timePeriod);

        // recuperamos TS (TimeSeries) de la moneda y el periodo de tiempo, y tenemos que mapearlo a una lista de HistoryData
        List<Sample.Value> coinsTSdata = coinsDataService.fetchCoinHistoryPerTimePeriodFromRedisTS(symbol, timePeriod);

        return coinsTSdata.stream()
                .map(value -> new HistoryData(
                        Utility.convertUnixTimeToDate(value.getTimestamp()),
                        Utility.round(value.getValue(), 2))
                ).collect(Collectors.toList());
    }
}
