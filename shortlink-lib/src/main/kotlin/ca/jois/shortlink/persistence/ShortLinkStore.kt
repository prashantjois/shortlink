package ca.jois.shortlink.persistence

import ca.jois.shortlink.model.ShortCode
import ca.jois.shortlink.model.ShortLink
import ca.jois.shortlink.model.ShortLinkGroup
import ca.jois.shortlink.model.ShortLinkUser
import java.net.URL
import java.time.Clock

interface ShortLinkStore {
  /**
   * A generic container for a paginated result of entries.
   *
   * @param T The type of entry being paginated.
   * @param nextPageKey A key that can be used to retrieve the next page of short links. If null,
   *   indicates there are no additional pages.
   */
  data class PaginatedResult<T>(val entries: List<T>, val nextPageKey: String?)

  /**
   * Retrieves all [ShortLink] entries in the store that are owned by the given [owner] in a
   * paginated manner.
   *
   * @param group The group of the short links to retrieve.
   * @param owner The owner of the short links to retrieve.
   * @param paginationKey A key that can be used to retrieve the next page of short links. If this
   *   value is null, the first page of short links will be retrieved. Subsequent pages can be
   *   retrieved by passing the value returned in the [PaginatedResult.nextPageKey] field of the
   *   previous result.
   */
  suspend fun listByGroupAndOwner(
    group: ShortLinkGroup,
    owner: ShortLinkUser,
    paginationKey: String? = null,
    limit: Int? = null,
  ): PaginatedResult<ShortLink>

  /**
   * Persists the given [shortLink] in a thread-safe manner.
   *
   * @throws [DuplicateShortCodeException] if an entry already exists with the given
   *   [ShortLink.code]
   */
  suspend fun create(shortLink: ShortLink): ShortLink

  /**
   * Retrieves the [ShortLink] associated with the given [code]. Returns null if no [ShortLink] is
   * associated to the code, or if the [ShortLink] entry is expired.
   *
   * @param code The short code used to reference the short link
   * @param excludeExpired Return null if a matching entry exists but is expired
   */
  context(Clock)
  suspend fun get(
    code: ShortCode,
    group: ShortLinkGroup,
    excludeExpired: Boolean = true
  ): ShortLink?

  /**
   * Updates the URL associated with the provided [ShortCode].
   *
   * @param code The [ShortCode] representing the short link to be updated.
   * @param url The new URL the code should point to
   * @param updater An optional user who is updating the short link. This will be used to
   *   determine whether the user has permission to update the short link.
   * @throws NotFoundOrNotPermittedException if the specified short code does not exist.
   */
  suspend fun update(code: ShortCode, url: URL, group: ShortLinkGroup, updater: ShortLinkUser)

  /**
   * Updates the expiry associated with the provided [ShortCode].
   *
   * @param code The [ShortCode] representing the short link to be updated.
   * @param expiresAt A new expiration timestamp for the short link (in milliseconds since epoch).
   *   If null, updates the code to not expire.
   * @param updater An optional user who is updating the short link. This will be used to
   *   determine whether the user has permission to update the short link.
   * @return The updated [ShortLink] object.
   * @throws NotFoundOrNotPermittedException if the specified short code does not exist.
   */
  suspend fun update(
    code: ShortCode,
    expiresAt: Long?,
    group: ShortLinkGroup,
    updater: ShortLinkUser
  )

  /**
   * Deletes a short link using its unique [ShortCode].
   *
   * @param code The [ShortCode] representing the short link to be deleted.
   * @param deleter An optional user who is deleting the short link. This will be used to
   *   determine whether the user has permission to delete the short link.
   * @throws NotFoundOrNotPermittedException if the specified short code does not exist.
   */
  suspend fun delete(code: ShortCode, group: ShortLinkGroup, deleter: ShortLinkUser)

  class DuplicateShortCodeException(group: ShortLinkGroup, code: ShortCode) :
    RuntimeException("Link with code ${code.value} already exists in group ${group.name}")

  class NotFoundOrNotPermittedException(group: ShortLinkGroup, code: ShortCode) :
    RuntimeException(
      "ShortLink with code ${code.value} in group ${group.name} not found or user not permitted to modify.",
    )
}
