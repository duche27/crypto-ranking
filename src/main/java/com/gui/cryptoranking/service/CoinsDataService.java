package com.gui.cryptoranking.service;

import com.gui.cryptoranking.enums.TimePeriod;
import com.gui.cryptoranking.model.CoinData;
import com.gui.cryptoranking.model.CoinInfo;
import com.gui.cryptoranking.model.CoinPriceHistory;
import com.gui.cryptoranking.model.Coins;
import com.gui.cryptoranking.utils.HttpUtils;
import io.github.dengliming.redismodule.redisjson.RedisJSON;
import io.github.dengliming.redismodule.redisjson.args.GetArgs;
import io.github.dengliming.redismodule.redisjson.args.SetArgs;
import io.github.dengliming.redismodule.redisjson.utils.GsonUtils;
import io.github.dengliming.redismodule.redistimeseries.RedisTimeSeries;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@Slf4j
public class CoinsDataService {

    private static String dollarCurrency = "yhjMzLPhuIDl";
    private static String euroCurrency = "5k-_VTxqtCEI";
    private static String euroTetherCurrency = "Okg3HKa3L";

    public String GET_COINS_API = "https://coinranking1.p.rapidapi.com/coins?referenceCurrencyUuid=" + euroCurrency
            + "&timePeriod=" + TimePeriod.twenty_four_hours.value() + "&tiers=1&orderBy=marketCap&orderDirection=desc&limit=50&offset=0";
    public static final String GET_COIN_HISTORY_API = "https://coinranking1.p.rapidapi.com/coin/";
    public static final String COIN_HISTORY_TIME_PERIOD_PARAM = "/history?timePeriod=";
    public static final List<String> timePeriods = List.of(TimePeriod.values().toString());
    public static final String REDIS_KEY_COINS = "coins";

    private final RestTemplate restTemplate;
    private final RedisJSON redisJSON;
    private final RedisTimeSeries redisTimeSeries;

    public CoinsDataService(RestTemplate restTemplate, RedisJSON redisJSON, RedisTimeSeries redisTimeSeries) {
        this.restTemplate = restTemplate;
        this.redisJSON = redisJSON;
        this.redisTimeSeries = redisTimeSeries;
    }

    public void fetchCoins() {

        log.info("fechtCoins() method called");

        // usamos el método creado HttpUtils.getHttpEntity() para setear MediaType y headers necesarios (apiHost y apiKey)
        ResponseEntity<Coins> coinsResponseEntity = restTemplate.exchange(
                GET_COINS_API,
                HttpMethod.GET, HttpUtils.getHttpEntity(),
                Coins.class);

        storeCoinstoRedisJSON(coinsResponseEntity.getBody());
    }

    public void fetchCoinHistory(String coinUuid) {

        log.info("fetchCoinHistory() method called");

        List<CoinInfo> allCoins = getAllCoinsInfoFromRedisJSON();

        allCoins.forEach(coin -> {
            timePeriods.forEach(timePeriod -> {
                fetchCoinHistoryForTimePeriod(coin, timePeriod);
            });
        });
    }

    private void storeCoinstoRedisJSON(Coins coins) {

        // guardamos en Redis en la key pasada, en el path especificado y en formato JSON
        redisJSON.set(
                REDIS_KEY_COINS,
                SetArgs.Builder.create(".", GsonUtils.toJson(coins)));
    }

    private List<CoinInfo> getAllCoinsInfoFromRedisJSON() {

        // obtenemos de Redis toda la info histórica de todas las monedas previamente guardadas en el path y con el formato especificado
        CoinData coinData = redisJSON.get(
                REDIS_KEY_COINS,
                CoinData.class,
                new GetArgs().path(".data").indent("\t").newLine("\n").space(" "));

        return coinData.getCoins();
    }

    private void fetchCoinHistoryForTimePeriod(CoinInfo coin, String timePeriod) {

        log.info("Fetching coin history for: COIN " + coin.getName() + " - TIME PERIOD " + timePeriod);

        // obtenemos histórico de precios para cada moneda en un período específico con otro endpoint del servicio
        ResponseEntity<CoinPriceHistory> coinHistoryResponseEntity = restTemplate.exchange(
                GET_COIN_HISTORY_API + coin.getUuid() + COIN_HISTORY_TIME_PERIOD_PARAM + timePeriod,
                HttpMethod.GET, HttpUtils.getHttpEntity(),
                CoinPriceHistory.class);

        // guardamos en Redis en la key pasada, en el path especificado y en formato JSON
        redisJSON.set(coin.getUuid() + timePeriod,
                SetArgs.Builder.create(".", GsonUtils.toJson(coinHistoryResponseEntity.getBody())));
    }
}
