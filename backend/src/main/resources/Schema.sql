-- Users table (matches the User entity). Safe to run on every start.
CREATE TABLE IF NOT EXISTS users
(
    id       SERIAL PRIMARY KEY,
    user_id  VARCHAR(255) NOT NULL UNIQUE,
    name     VARCHAR(255) NOT NULL,
    email    VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role     VARCHAR(20)  NOT NULL CHECK (role IN ('EMPLOYEE', 'ADMIN'))
);