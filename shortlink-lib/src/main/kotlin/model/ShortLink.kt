package model

import java.net.URL
import java.time.Clock

data class ShortLink(
    /** The URL we are encoding into the short code */
    val url: URL,
    /** The short code associated with the URL */
    val code: ShortCode,
    /** Timestamp of when this short link was created in epoch milliseconds */
    val createdAt: Long,
    /** Optional: Timestamp for when this short link expires in epoch milliseconds */
    val expiresAt: Long? = null,
) {
    fun doesNotExpire() = expiresAt == null

    context(Clock)
    fun isExpired() = expiresAt == null || instant().toEpochMilli() > expiresAt
}
