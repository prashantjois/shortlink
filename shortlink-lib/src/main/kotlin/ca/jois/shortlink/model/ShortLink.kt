package ca.jois.shortlink.model

import java.net.URL
import java.time.Clock

/**
 * A short link entity. This is the core entity of the short link system. It represents a URL that
 * has been encoded into a short code.
 *
 * @param url The URL we are encoding into the short code
 * @param code The short code associated with the URL
 * @param group The group this short link belongs to
 * @param creator The user who created this short link
 * @param owner The user who owns this short link
 * @param createdAt Timestamp of when this short link was created in epoch milliseconds
 * @param expiresAt Optional: Timestamp for when this short link expires in epoch milliseconds. If
 *   null, it means the shortlink never expires.
 */
data class ShortLink(
  val url: URL,
  val code: ShortCode,
  val group: ShortLinkGroup,
  val creator: ShortLinkUser,
  val owner: ShortLinkUser,
  val createdAt: Long,
  val expiresAt: Long? = null,
) {
  /** Returns true if the short link never expires. */
  fun doesNotExpire() = expiresAt == null

  /** Returns true if the short link has expired. */
  context(Clock)
  fun isExpired() = expiresAt != null && millis() > expiresAt
}
