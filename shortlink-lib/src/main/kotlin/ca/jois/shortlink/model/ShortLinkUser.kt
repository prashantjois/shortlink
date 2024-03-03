package ca.jois.shortlink.model

/**
 * A user who is associated with a short link (e.g. the creator or owner). The user is identified by
 * [identifier] which is a string representation of the user's unique identifier either in the
 * system or in an external system.
 *
 * @param identifier The user's unique identifier
 */
data class ShortLinkUser(
    val identifier: String,
) {
    companion object {
        /**
         * A special user that represents an anonymous user. This user is used when a short link is
         * created without an associated user. The value is a UUID to avoid collision with a real
         * user's identifier as much as possible.
         */
        val ANONYMOUS = ShortLinkUser("ANON_a289b3279afd48c1a9419fce2b5bf132")
    }
}
