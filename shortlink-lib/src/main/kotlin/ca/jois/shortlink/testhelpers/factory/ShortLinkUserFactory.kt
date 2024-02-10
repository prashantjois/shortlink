package ca.jois.shortlink.testhelpers.factory

import ca.jois.shortlink.model.ShortLinkUser
import java.util.UUID

object ShortLinkUserFactory {
    fun build() = ShortLinkUser(identifier = randomString())

    private fun randomString() = UUID.randomUUID().toString()
}
