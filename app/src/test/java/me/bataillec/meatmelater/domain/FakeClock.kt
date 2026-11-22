package me.bataillec.meatmelater.domain

import java.time.ZoneId

class FakeClock(
    var millis: Long,
    private val zone: ZoneId = ZoneId.of("Europe/Paris"),
) : Clock {
    override fun nowMillis(): Long = millis

    override fun zone(): ZoneId = zone
}
