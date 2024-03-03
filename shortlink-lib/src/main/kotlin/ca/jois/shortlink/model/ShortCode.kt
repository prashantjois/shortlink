package ca.jois.shortlink.model

/**
 * Container class for a coded representation of a URL. Codes are unique within a group, but not
 * necessarily unique across groups.
 */
@JvmInline value class ShortCode(val value: String)
