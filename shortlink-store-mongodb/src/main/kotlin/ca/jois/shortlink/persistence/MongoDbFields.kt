package ca.jois.shortlink.persistence

enum class MongoDbFields(val fieldName: String) {
    ID("_id"),
    OWNER("owner"),
    CREATOR("creator"),
    CODE("code"),
    URL("url"),
    CREATED_AT("createdAt"),
    EXPIRES_AT("expiresAt"),
}
