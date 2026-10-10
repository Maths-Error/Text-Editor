# MongoDB Setup

Docker is required for the local MongoDB setup.

MongoDB Compass is optional. It can be useful for visually checking databases and collections.

After Batch 4B, created documents are stored in MongoDB. They should appear in Mongo Express inside the
`collab_text_editor` database, usually in a `documents` collection.

## Start MongoDB

From the project root, run:

```bash
docker compose up -d mongo
```

## Check Running Containers

```bash
docker ps
```

## Open mongosh

```bash
docker exec -it collab-text-editor-mongo mongosh
```

## Default Connection String

```text
mongodb://localhost:27017/collab_text_editor
```

The server uses this value by default:

```properties
spring.data.mongodb.uri=${MONGODB_URI:mongodb://localhost:27017/collab_text_editor}
spring.mongodb.uri=${MONGODB_URI:mongodb://localhost:27017/collab_text_editor}
```

## Stop MongoDB Without Deleting Data

```bash
docker compose stop mongo
```

## Delete All MongoDB Data

Only run this when intentionally resetting local MongoDB data:

```bash
docker compose down -v
```

## Atlas Or Remote MongoDB

Do not commit Atlas credentials or passwords. If using Atlas later, set `MONGODB_URI` as an environment variable.
