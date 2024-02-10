package ca.jois.shortlink.model

/**
 * A user who is associated with a short link (e.g. the creator or owner). The user is identified by
 * [identifier] which is a string representation of the user's unique identifier either in the
 * system or in an external system.
 */
data class ShortLinkUser(
    /** The user's unique identifier */
    val identifier: String,
)
