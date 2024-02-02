package ca.jois.shortlink.testhelpers.clock

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.time.Duration
import kotlin.time.toJavaDuration

class TestClock(
    private var instant: Instant = Instant.ofEpochMilli(0),
    private var zoneId: ZoneId = ZoneId.systemDefault(),
) : Clock() {
    private val self = this

    override fun instant() = instant

    override fun withZone(zone: ZoneId): Clock {
        this.zoneId = zone
        return this
    }

    override fun getZone() = zoneId

    fun advanceClockBy(duration: Duration): TestClock {
        val newClock = offset(self, duration.toJavaDuration())
        this.instant = newClock.instant()
        return this
    }

    fun Duration.fromNow(): Instant = offset(self, toJavaDuration()).instant()
}
