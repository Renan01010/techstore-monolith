CREATE TABLE identity.users (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    email varchar(320) NOT NULL,
    password_hash varchar(255) NOT NULL,
    full_name varchar(200) NOT NULL,
    phone varchar(40),
    account_state varchar(30) NOT NULL DEFAULT 'pending_verification',
    access_role varchar(30) NOT NULL DEFAULT 'customer',
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT ck_users_email_normalized CHECK (email = lower(btrim(email)) AND email <> ''),
    CONSTRAINT ck_users_account_state CHECK (account_state IN ('pending_verification', 'active', 'suspended', 'deactivated')),
    CONSTRAINT ck_users_access_role CHECK (access_role IN ('customer', 'administrator'))
);
CREATE UNIQUE INDEX uq_users_email ON identity.users (email);

CREATE TABLE identity.user_addresses (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL,
    address_type varchar(30) NOT NULL DEFAULT 'shipping',
    recipient_name varchar(200) NOT NULL,
    postal_code varchar(20) NOT NULL,
    street varchar(200) NOT NULL,
    street_number varchar(30) NOT NULL,
    complement varchar(200),
    district varchar(120),
    city varchar(120) NOT NULL,
    state_code varchar(10) NOT NULL,
    country_code char(2) NOT NULL DEFAULT 'BR',
    is_default boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_user_addresses PRIMARY KEY (id),
    CONSTRAINT fk_user_addresses_user FOREIGN KEY (user_id) REFERENCES identity.users (id) ON DELETE CASCADE,
    CONSTRAINT ck_user_addresses_type CHECK (address_type IN ('shipping', 'billing', 'other'))
);
CREATE INDEX ix_user_addresses_user_id ON identity.user_addresses (user_id);
CREATE UNIQUE INDEX uq_user_addresses_default ON identity.user_addresses (user_id) WHERE is_default;

CREATE TABLE identity.email_verification_tokens (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL,
    token_digest varchar(128) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    expires_at timestamptz NOT NULL,
    used_at timestamptz,
    CONSTRAINT pk_email_verification_tokens PRIMARY KEY (id),
    CONSTRAINT fk_email_verification_tokens_user FOREIGN KEY (user_id) REFERENCES identity.users (id) ON DELETE CASCADE,
    CONSTRAINT uq_email_verification_tokens_digest UNIQUE (token_digest),
    CONSTRAINT ck_email_verification_tokens_expiry CHECK (expires_at > created_at)
);
CREATE INDEX ix_email_verification_tokens_user_id ON identity.email_verification_tokens (user_id);

CREATE TABLE identity.password_reset_tokens (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL,
    token_digest varchar(128) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    expires_at timestamptz NOT NULL,
    used_at timestamptz,
    CONSTRAINT pk_password_reset_tokens PRIMARY KEY (id),
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id) REFERENCES identity.users (id) ON DELETE CASCADE,
    CONSTRAINT uq_password_reset_tokens_digest UNIQUE (token_digest),
    CONSTRAINT ck_password_reset_tokens_expiry CHECK (expires_at > created_at)
);
CREATE INDEX ix_password_reset_tokens_user_id ON identity.password_reset_tokens (user_id);

CREATE TABLE identity.user_sessions (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL,
    session_digest varchar(128) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz,
    CONSTRAINT pk_user_sessions PRIMARY KEY (id),
    CONSTRAINT fk_user_sessions_user FOREIGN KEY (user_id) REFERENCES identity.users (id) ON DELETE CASCADE,
    CONSTRAINT uq_user_sessions_digest UNIQUE (session_digest),
    CONSTRAINT ck_user_sessions_expiry CHECK (expires_at > created_at)
);
CREATE INDEX ix_user_sessions_user_id ON identity.user_sessions (user_id);
