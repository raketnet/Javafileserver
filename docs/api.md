# HTTP API

Base URL: `http://localhost:7070`

| Method | Path | Purpose |
|---|---|---|
| GET | `/health` | Liveness |
| GET | `/swagger` | Interactive Swagger UI |
| GET | `/openapi.yaml` | OpenAPI specification |
| POST | `/api/v1/files` | Upload multipart field `file` |
| GET | `/api/v1/files` | List metadata; `limit` and `offset` |
| GET | `/api/v1/files/{id}/metadata` | Read metadata |
| GET | `/api/v1/files/{id}` | Download bytes |
| DELETE | `/api/v1/files/{id}` | Delete file |

Uploads return HTTP 201 with a Location header and a JSON FileRecord. Download is intentionally served as an attachment with `nosniff`. No authentication is implemented: do not expose the service directly to untrusted networks.
