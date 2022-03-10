package com.ckontur.edms.utils;

import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@UtilityClass
public class DateUtils {
    public static Date of(LocalDate localDate) {
        return of(localDate, ZoneId.systemDefault());
    }

    public static Date of(LocalDate localDate, ZoneId zone) {
        return Date.from(localDate.atStartOfDay().atZone(zone).toInstant());
    }

    public static Date of(LocalDateTime localDateTime) {
        return of(localDateTime, ZoneId.systemDefault());
    }

    public static Date of(LocalDateTime localDateTime, ZoneId zone) {
        return Date.from(localDateTime.atZone(zone).toInstant());
    }
}
