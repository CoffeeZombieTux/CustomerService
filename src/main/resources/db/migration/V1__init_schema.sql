CREATE TABLE customers (
                           id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                           role        VARCHAR(20)  NOT NULL,
                           email       VARCHAR(70)  NOT NULL UNIQUE,
                           password    VARCHAR(100) NOT NULL,
                           first_name  VARCHAR(50)  NOT NULL,
                           last_name   VARCHAR(50)  NOT NULL,
                           phone       VARCHAR(20)  NOT NULL,
                           enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
                           created_at  TIMESTAMPTZ  NOT NULL,
                           updated_at  TIMESTAMPTZ  NOT NULL
);

CREATE TABLE addresses (
                           id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                           type        VARCHAR(20)  NOT NULL,
                           street      VARCHAR(255) NOT NULL,
                           city        VARCHAR(40)  NOT NULL,
                           postal_code VARCHAR(10)  NOT NULL,
                           country     VARCHAR(2)   NOT NULL,
                           customer_id BIGINT       NOT NULL REFERENCES customers(id),
                           created_at  TIMESTAMPTZ  NOT NULL,
                           updated_at  TIMESTAMPTZ  NOT NULL
);

CREATE TABLE bonus_accounts (
                                id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                balance     INTEGER      NOT NULL DEFAULT 0,
                                customer_id BIGINT       NOT NULL UNIQUE REFERENCES customers(id),
                                created_at  TIMESTAMPTZ  NOT NULL,
                                updated_at  TIMESTAMPTZ  NOT NULL
);

CREATE TABLE credit_transactions (
                                     id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                     transaction_type VARCHAR(20)  NOT NULL,
                                     amount           INTEGER      NOT NULL,
                                     idempotency_key  UUID,
                                     bonus_account_id BIGINT       NOT NULL REFERENCES bonus_accounts(id),
                                     created_at  TIMESTAMPTZ  NOT NULL,
                                     updated_at  TIMESTAMPTZ  NOT NULL
);

CREATE TABLE refresh_tokens (
                                id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                session_id  UUID         NOT NULL UNIQUE,
                                token_hash  VARCHAR(64)  NOT NULL UNIQUE,
                                expired_at  TIMESTAMPTZ  NOT NULL,
                                customer_id BIGINT       NOT NULL REFERENCES customers(id),
                                created_at  TIMESTAMPTZ  NOT NULL,
                                updated_at  TIMESTAMPTZ  NOT NULL
);

CREATE TABLE activation_tokens (
                                id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                token       UUID         NOT NULL UNIQUE,
                                expired_at  TIMESTAMPTZ  NOT NULL,
                                customer_id BIGINT       NOT NULL REFERENCES customers(id),
                                created_at  TIMESTAMPTZ  NOT NULL,
                                updated_at  TIMESTAMPTZ  NOT NULL
);

CREATE TABLE customer_agreements (
                                     id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                     customer_id    BIGINT      NOT NULL REFERENCES customers(id),
                                     agreement_type VARCHAR(30) NOT NULL,
                                     version        INTEGER     NOT NULL,
                                     accepted       BOOLEAN     NOT NULL,
                                     created_at  TIMESTAMPTZ  NOT NULL,
                                     updated_at  TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_customer_agreements_customer_id_type ON customer_agreements(customer_id, agreement_type);
CREATE INDEX idx_refresh_tokens_customer_id_expired_at ON refresh_tokens(customer_id, expired_at);
CREATE INDEX idx_addresses_customer_id                 ON addresses(customer_id);
CREATE INDEX idx_activation_tokens_customer_id         ON activation_tokens(customer_id);
CREATE INDEX idx_credit_transactions_idempotency
    ON credit_transactions(idempotency_key, bonus_account_id, created_at);

ALTER TABLE bonus_accounts
    ADD CONSTRAINT chk_balance_non_negative CHECK (balance >= 0);