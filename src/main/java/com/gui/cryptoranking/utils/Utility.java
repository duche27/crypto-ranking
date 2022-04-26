package com.gui.cryptoranking.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class Utility {

    public static String convertUnixTimeToDate(Long unixTime) {
        final DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

        return Instant.ofEpochSecond(unixTime)
                .atZone(ZoneId.of("GMT+1"))
                .format(formatter);
    }

    public static double round(double value, int places) {
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
