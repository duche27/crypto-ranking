package com.gui.cryptoranking.service;

import com.gui.cryptoranking.model.*;
import com.gui.cryptoranking.utils.HttpUtils;
import io.github.dengliming.redismodule.redisjson.RedisJSON;
import io.github.dengliming.redismodule.redisjson.args.GetArgs;
import io.github.dengliming.redismodule.redisjson.args.SetArgs;
import io.github.dengliming.redismodule.redisjson.utils.GsonUtils;
import io.github.dengliming.redismodule.redistimeseries.DuplicatePolicy;
import io.github.dengliming.redismodule.redistimeseries.RedisTimeSeries;
import io.github.dengliming.redismodule.redistimeseries.Sample;
import io.github.dengliming.redismodule.redistimeseries.TimeSeriesOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.lang.Double.parseDouble;
import static java.lang.Long.parseLong;

@Service
@Slf4j
public class CoinsDataService {

    private static String dollarCurrency = "yhjMzLPhuIDl";
    private static String euroCurrency = "5k-_VTxqtCEI";
    private static String euroTetherCurrency = "Okg3HKa3L";

    public String GET_COINS_API = "https://coinranking1.p.rapidapi.com/coins?referenceCurrencyUuid=" + euroCurrency
            + "&timePeriod=" + timePeriods.get(0) + "&tiers=1&orderBy=marketCap&orderDirection=desc&limit=50&offset=0";
    public static final String GET_COIN_HISTORY_API = "https://coinranking1.p.rapidapi.com/coin/";
    public static final String COIN_HISTORY_TIME_PERIOD_PARAM = "/history?timePeriod=";
    public static final List<String> timePeriods = List.of("24h", "7d", "30d", "3m", "1y", "3y", "5y");
    public static final String REDIS_KEY_COINS = "coins";

    private final RestTemplate restTemplate;
    private final RedisJSON redisJSON;
    private final RedisTimeSeries redisTimeSeries;

    public CoinsDataService(RestTemplate restTemplate, RedisJSON redisJSON, RedisTimeSeries redisTimeSeries) {
        this.restTemplate = restTemplate;
        this.redisJSON = redisJSON;
        this.redisTimeSeries = redisTimeSeries;
    }

    // llamamos al servicio y recuperamos la info de las 50 primeras monedas y las guardamos como JSON en Redis
    public void fetchCoins() {

        log.info("fechtCoins() method called");

        // usamos el método creado HttpUtils.getHttpEntity() para setear MediaType y headers necesarios (apiHost y apiKey)
        ResponseEntity<Coins> coinsResponseEntity = restTemplate.exchange(
                GET_COINS_API,
                HttpMethod.GET, HttpUtils.getHttpEntity(),
                Coins.class);

        Coins coins = coinsResponseEntity.getBody();

        // guardamos en Redis en la key pasada, en el path especificado y en formato JSON
        redisJSON.set(
                REDIS_KEY_COINS,
                SetArgs.Builder.create(".", GsonUtils.toJson(coins)));
    }

    // recuperamos las monedas de Redis y para cada una llamamos al servicio para recuperar la serie histórica para cada periodo
    public void fetchAllCoinsHistory() {

        log.info("fetchCoinHistory() method called");

        List<CoinInfo> allCoins = getAllCoinsInfoFromRedisJSON();

        allCoins.forEach(coin -> {
            timePeriods.forEach(timePeriod -> {
                fetchCoinHistoryForTimePeriod(coin, timePeriod);
            });
        });
    }

    // recuperamos todas las monedas de Redis (en JSON)
    public List<CoinInfo> getAllCoinsInfoFromRedisJSON() {

        // obtenemos de Redis toda la info histórica de todas las monedas previamente guardadas en el path y con el formato especificado
        CoinData coinData = redisJSON.get(
                REDIS_KEY_COINS,
                CoinData.class,
                new GetArgs().path(".data").indent("\t").newLine("\n").space(" "));

        return coinData.getCoins();
    }

    // guardamos en Redis todas las TS para la posterior llamada a nuestra API
    private void fetchCoinHistoryForTimePeriod(CoinInfo coin, String timePeriod) {

        log.info("Fetching coin history for: COIN " + coin.getName() + " - TIME PERIOD " + timePeriod);

        // obtenemos histórico de precios para cada moneda en un período específico con otro endpoint del servicio
        ResponseEntity<CoinPriceHistory> coinHistoryResponseEntity = restTemplate.exchange(
                GET_COIN_HISTORY_API + coin.getUuid() + COIN_HISTORY_TIME_PERIOD_PARAM + timePeriod,
                HttpMethod.GET, HttpUtils.getHttpEntity(),
                CoinPriceHistory.class);

        List<CoinPriceHistoryExchangeRate> coinPriceHistoryExchangeRates = Objects.requireNonNull
                (coinHistoryResponseEntity.getBody()).getData().getHistory();

        // guardamos en Redis en la key pasada, en el path especificado y en formato TS
        // RedisTimeSeries es cada "tabla" TS (TimeSeries) que guarda en Redis
        // se compone de un Sample: nombre tabla (key) + datos de tabla: x,y = key,value = timestamp,price (o lo q sea con timestamp xq es una TS)
        // y de unas opciones: sin comprimir y la política de publicidad en este caso
        coinPriceHistoryExchangeRates.stream()
                .filter(ch -> ch.getPrice() != null && ch.getTimestamp() != null)       // comprobamos que ni key ni value son null
                .forEach(ch -> {
                    redisTimeSeries.add(
                            new Sample(
                                    coin.getSymbol() + ":" + timePeriod, Sample.Value.of(
                                    parseLong(ch.getTimestamp()), parseDouble(ch.getPrice()))),
                            new TimeSeriesOptions().unCompressed().duplicatePolicy(DuplicatePolicy.LAST));
                });
    }

    // recuperamos de Redis todas las TS para servirlas
    public List<Sample.Value> fetchCoinHistoryPerTimePeriodFromRedisTS(String symbol, String timePeriod) {

        // en la info recuperamos firstTimestamp y lastTimestamp para obtener el rango de tiempo
        Map<String, Object> TSinfoForSymbol = redisTimeSeries.info(symbol + ":" + timePeriod);

        Long firstTimestamp = (Long) TSinfoForSymbol.get("firstTimestamp");
        Long lastTimestamp = (Long) TSinfoForSymbol.get("lastTimestamp");

        String key = symbol + ":" + timePeriod;

        return redisTimeSeries.range(key, firstTimestamp, lastTimestamp);
    }
}
