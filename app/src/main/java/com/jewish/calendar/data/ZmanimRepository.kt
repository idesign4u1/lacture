package com.jewish.calendar.data

import android.location.Location
import com.jewish.calendar.model.ZmanimModel
import com.kosherjava.zmanim.ComplexZmanimCalendar
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.kosherjava.zmanim.util.GeoLocation
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZmanimRepository @Inject constructor() {

    fun calculateZmanim(
        date: Date,
        location: Location,
        cityName: String = "מיקומי הנוכחי",
        candleLightingOffset: Int = 18
    ): ZmanimModel {
        // Use Israel timezone explicitly when coordinates are within Israel bounds.
        // This avoids errors on devices whose system timezone is set incorrectly.
        val tz = if (location.latitude in 29.0..34.0 && location.longitude in 34.0..36.0) {
            TimeZone.getTimeZone("Asia/Jerusalem")
        } else {
            TimeZone.getDefault()
        }

        val cal = Calendar.getInstance(tz).apply { time = date }

        val geoLocation = GeoLocation(
            cityName,
            location.latitude,
            location.longitude,
            location.altitude,
            tz
        )

        val zcal = ComplexZmanimCalendar(geoLocation).apply {
            calendar = cal
        }

        val jewishCal = JewishCalendar(cal).apply { inIsrael = true }
        val isShabbat = jewishCal.dayOfWeek == Calendar.SATURDAY
        val isErevShabbat = jewishCal.dayOfWeek == Calendar.FRIDAY

        // Candle lighting = 18 min before sunset on Erev Shabbat / Erev Yom Tov
        val candleLighting = if (isErevShabbat || jewishCal.isErevYomTov) {
            zcal.sunset?.let { sunset ->
                Date(sunset.time - candleLightingOffset * 60 * 1000L)
            }
        } else null

        return ZmanimModel(
            date = date,
            cityName = cityName,
            alotHashachar = zcal.alosHashachar,
            misheyakir = zcal.misheyakir10Point2Degrees,
            sunrise = zcal.sunrise,
            sofZmanKriatShmaMGA = zcal.sofZmanShmaMGA,
            sofZmanKriatShmaGRA = zcal.sofZmanShmaGRA,
            sofZmanTfilaMGA = zcal.sofZmanTfilaMGA,
            sofZmanTfilaGRA = zcal.sofZmanTfilaGRA,
            chatzot = zcal.chatzos,
            minchaGedola = zcal.minchaGedola,
            minchaKetana = zcal.minchaKetana,
            plagHamincha = zcal.plagHamincha,
            sunset = zcal.sunset,
            tzaitHakochavim = zcal.tzais,
            tzaitHakochavimRT = zcal.tzais72,
            candleLighting = candleLighting,
            isShabbat = isShabbat,
            isErevShabbat = isErevShabbat,
            isYomTov = jewishCal.isYomTovAssurBemelacha
        )
    }

    // Default zmanim for Jerusalem when no location available
    fun getJerusalemZmanim(date: Date): ZmanimModel {
        val jerusalemLocation = Location("").apply {
            latitude  = 31.7683
            longitude = 35.2137
            altitude  = 786.0
        }
        return calculateZmanim(date, jerusalemLocation, "ירושלים")
    }

    // Default zmanim for Tel Aviv
    fun getTelAvivZmanim(date: Date): ZmanimModel {
        val tlvLocation = Location("").apply {
            latitude  = 32.0853
            longitude = 34.7818
            altitude  = 5.0
        }
        return calculateZmanim(date, tlvLocation, "תל אביב")
    }
}
