package com.gui.cryptoranking.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum TimePeriod {
    three_hours("3h"),
    twenty_four_hours("24h"),
    seven_days("7d"),
    thirty_days("30d"),
    three_months("3m"),
    one_year("1y"),
    three_years("3y"),
    five_years("5y");

    public String value;

    TimePeriod(String timePeriod) {
        this.setTimePeriod(timePeriod);
    }

    public String getTimePeriod() {
        return value;
    }

    public void setTimePeriod(String timePeriod) {
        this.value = timePeriod;
    }

    @JsonValue
    public String value() {
        return value;
    }
}