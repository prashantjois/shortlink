package ca.jois.shortlink.persistence

enum class DbFields(val fieldName: String) {
    ID("id"),
    GROUP("grp"),
    OWNER("owner"),
    CREATOR("creator"),
    CODE("code"),
    URL("url"),
    CREATED_AT("created_at"),
    EXPIRES_AT("expires_at"),
}
