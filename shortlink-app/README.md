# ShortLink Web Server

This module allows you to run a local server that exposes CRUD APIs to manage shortlinks.

## Getting Started

To start the server, run the `main()` function in [App.kt](src/main/kotlin/App.kt)

## API

### POST /create

Request:
```json
{
  "url": "https://example.com",
  "username": "user"
}
```

Response:
```json
{
  "owner": {
    "identifier": "user"
  },
  "code": {
    "value": "JdP0"
  },
  "url": "https://example.com",
  "creator": {
    "identifier": "user"
  },
  "createdAt": 1707072661478
}
```

### GET /get

Request:
```json
{
  "code": "JdP0"
}
```

Response:
```json
{
  "creator": {
    "identifier": "user"
  },
  "owner": {
    "identifier": "user"
  },
  "url": "https://example.com",
  "code": {
    "value": "JdP0"
  },
  "createdAt": 1707072661478
}
```

### PUT /update/url

Request:
```json
{
  "code": "JdP0",
  "url": "https://example.com/new"
}
```

Response:
```json
{
  "creator": {
    "identifier": "user"
  },
  "owner": {
    "identifier": "user"
  },
  "url": "https://example.com/new",
  "code": {
    "value": "JdP0"
  },
  "createdAt": 1707072661478
}
```

### PUT /update/expiry

Request:
```json
{
  "code": "JdP0",
  "expiresAt": 2707072661478
}
```

Response:
```json
{
  "creator": {
    "identifier": "user"
  },
  "owner": {
    "identifier": "user"
  },
  "url": "https://example.com/",
  "code": {
    "value": "JdP0"
  },
  "createdAt": 2707072661478
}
```

### DELETE /delete

Request:
```json
{
  "code": "JdP0",
}
```

Response:
```json
{}
```

### Scripts

Sample scripts that exercise these endpoint are given in the [scripts folder](../scripts)