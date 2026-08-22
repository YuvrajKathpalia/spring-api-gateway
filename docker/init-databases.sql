-- Creates one database per service (the "database per service" pattern).
-- Runs automatically on first Postgres startup via the docker-entrypoint-initdb.d hook.
CREATE DATABASE userdb;
CREATE DATABASE productdb;
