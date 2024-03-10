# ShortLink Web Server

This module allows you to run a local server that exposes CRUD APIs to manage shortlinks.

## Getting Started

* To start the server, run the `main()` function in [App.kt](src/main/kotlin/App.kt)
* Create a new shortlink by running [scripts/create](../scripts/create) or hitting the `/api/create` endpoint manually.
* Navigate to https://localhost:8080/r/{code} (replace `{code}` with the output of the shortlink create API repsonse) to
  be redirected to the domain you created the shortlink for.

## API

These are served from the `/api` path.

### GET /listByOwner

Request:

```json
{
  "owner": "user",
  "group": "group1"
}
```

Response:

```json
{
  "entries": [
    {
      "url": "https://example.com",
      "code": {
        "value": "xJNV"
      },
      "group": {
        "name": "group1"
      },
      "creator": {
        "identifier": "user"
      },
      "owner": {
        "identifier": "user"
      },
      "createdAt": 1709435880288
    },
    {
      "url": "https://example.com",
      "code": {
        "value": "0Clz"
      },
      "group": {
        "name": "group1"
      },
      "creator": {
        "identifier": "user"
      },
      "owner": {
        "identifier": "user"
      },
      "createdAt": 1709435880973
    }
  ]
}
```

### POST /create

Request:

```json
{
  "url": "https://example.com",
  "username": "user",
  "group": "group1"
}
```

Response:

```json
{
  "url": "https://example.com",
  "code": {
    "value": "0Clz"
  },
  "group": {
    "name": "group1"
  },
  "creator": {
    "identifier": "user"
  },
  "owner": {
    "identifier": "user"
  },
  "createdAt": 1709435880973
}
```

### GET /get

Request:

```json
{
  "code": "0Clz",
  "group": "group1"
}
```

Response:

```json
{
  "url": "https://example.com/new",
  "code": {
    "value": "0Clz"
  },
  "group": {
    "name": "group1"
  },
  "creator": {
    "identifier": "user"
  },
  "owner": {
    "identifier": "user"
  },
  "createdAt": 1709435880973
}
```

### PUT /update/url

Request:

```json
{
  "code": "0Clz",
  "url": "https://example.com/new",
  "group": "group1"
}
```

Response:

```json
{
  "url": "https://example.com/new",
  "code": {
    "value": "0Clz"
  },
  "group": {
    "name": "group1"
  },
  "creator": {
    "identifier": "prashant"
  },
  "owner": {
    "identifier": "prashant"
  },
  "createdAt": 1709435880973
}
```

### PUT /update/expiry

Request:

```json
{
  "code": "OClz",
  "expiresAt": 2707072661478,
  "group": "group1"
}
```

Response:

```json
{
  "url": "https://example.com/new",
  "code": {
    "value": "0Clz"
  },
  "group": {
    "name": "group1"
  },
  "creator": {
    "identifier": "prashant"
  },
  "owner": {
    "identifier": "prashant"
  },
  "createdAt": 1709435880973,
  "expiresAt": 2707010574211
}
```

### DELETE /delete

Request:

```json
{
  "code": "OClz",
  "group": "group1"
}
```

Response:

```json
{}
```

### Scripts

Sample scripts that exercise these endpoint are given in the [scripts folder](../scripts)