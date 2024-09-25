package ca.jois.shortlink.model

/**
 * A group for short links. Provides a scope for a set of [ShortLink] entities, such that every
 * shortlink within a group must draw from a unique set of codes. Codes do not have to be unique
 * across groups.
 *
 * @property name The name of the group.
 */
data class ShortLinkGroup(val name: String) {
  companion object {
    /** The default group for short links. */
    val UNGROUPED = ShortLinkGroup("UNGROUPED")
  }
}
