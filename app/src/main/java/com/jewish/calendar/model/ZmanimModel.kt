package com.jewish.calendar.model

import java.util.Date

data class ZmanimModel(
    val date: Date,
    val cityName: String,
    val alotHashachar: Date?,
    val misheyakir: Date?,
    val sunrise: Date?,
    val sofZmanKriatShmaMGA: Date?,
    val sofZmanKriatShmaGRA: Date?,
    val sofZmanTfilaMGA: Date?,
    val sofZmanTfilaGRA: Date?,
    val chatzot: Date?,
    val minchaGedola: Date?,
    val minchaKetana: Date?,
    val plagHamincha: Date?,
    val sunset: Date?,
    val tzaitHakochavim: Date?,
    val tzaitHakochavimRT: Date?,  // Rabenu Tam
    val candleLighting: Date?,     // Friday / Erev Yom Tov (18 min before sunset)
    val isShabbat: Boolean,
    val isErevShabbat: Boolean,
    val isYomTov: Boolean
)

/** Looks up a zman Date by the alarm key string used throughout the app. */
fun ZmanimModel.getZmanByKey(key: String): java.util.Date? = when (key) {
    "alotHashachar"    -> alotHashachar
    "sunrise"          -> sunrise
    "sofZmanShmaGRA"   -> sofZmanKriatShmaGRA
    "sofZmanShmaMGA"   -> sofZmanKriatShmaMGA
    "sofZmanTfilaGRA"  -> sofZmanTfilaGRA
    "sofZmanTfilaMGA"  -> sofZmanTfilaMGA
    "chatzot"          -> chatzot
    "minchaGedola"     -> minchaGedola
    "minchaKetana"     -> minchaKetana
    "plagHamincha"     -> plagHamincha
    "candleLighting"   -> candleLighting
    "sunset"           -> sunset
    "tzaitHakochavim"  -> tzaitHakochavim
    "tzaitHakochavimRT"-> tzaitHakochavimRT
    else               -> null
}

data class ZmanimCalculationMethod(
    val name: String,
    val description: String
)

val CALCULATION_METHODS = listOf(
    ZmanimCalculationMethod("גר\"א", "שיטת הגר\"א - 13.5 דקות"),
    ZmanimCalculationMethod("מג\"א", "שיטת המגן אברהם - 16.1 מעלות"),
    ZmanimCalculationMethod("רבנו תם", "72 דקות אחרי השקיעה")
)
