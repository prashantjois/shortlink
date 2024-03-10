package ca.jois.shortlink.persistence

object Database {
  object DbFields {
    enum class ShortLinks(val fieldName: String) {
      ID("id"),
      GROUP("grp"),
      OWNER("owner"),
      CREATOR("creator"),
      CODE("code"),
      URL("url"),
      CREATED_AT("created_at"),
      EXPIRES_AT("expires_at"),
    }

    enum class ShortLinkGroups(val fieldName: String) {
      ID("id"),
      NAME("name"),
    }
  }

  object TableName {
    const val SHORTLINKS = "shortlinks"
    const val SHORTLINK_GROUPS = "shortlink_groups"
  }
}
