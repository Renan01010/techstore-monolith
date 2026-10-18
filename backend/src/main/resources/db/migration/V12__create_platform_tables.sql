CREATE TABLE platform.audit_logs (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    actor_user_id uuid,
    action varchar(100) NOT NULL,
    target_type varchar(100),
    target_id varchar(100),
    details jsonb,
    occurred_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_audit_logs PRIMARY KEY (id),
    CONSTRAINT fk_audit_logs_actor_user FOREIGN KEY (actor_user_id) REFERENCES identity.users (id) ON DELETE SET NULL,
    CONSTRAINT ck_audit_logs_target_pair CHECK ((target_type IS NULL) = (target_id IS NULL))
);
CREATE INDEX ix_audit_logs_actor_user_id ON platform.audit_logs (actor_user_id);
CREATE INDEX ix_audit_logs_occurred_at ON platform.audit_logs (occurred_at);
CREATE INDEX ix_audit_logs_target ON platform.audit_logs (target_type, target_id);

COMMENT ON TABLE platform.audit_logs IS 'Never store passwords, tokens or credentials in details.';
COMMENT ON COLUMN platform.audit_logs.target_type IS 'Descriptive only: a polymorphic reference that PostgreSQL cannot validate with a foreign key; it may become stale.';
COMMENT ON COLUMN platform.audit_logs.target_id IS 'Descriptive only: see target_type; never used for cascading or authorization.';

CREATE TABLE platform.app_settings (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    setting_key varchar(150) NOT NULL,
    setting_value text,
    visibility varchar(20) NOT NULL DEFAULT 'internal',
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_app_settings PRIMARY KEY (id),
    CONSTRAINT uq_app_settings_key UNIQUE (setting_key),
    CONSTRAINT ck_app_settings_visibility CHECK (visibility IN ('public', 'internal', 'secret'))
);

CREATE TABLE platform.file_attachments (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    user_id uuid,
    product_id uuid,
    contact_request_id uuid,
    marketing_email_id uuid,
    file_name varchar(255) NOT NULL,
    content_type varchar(150) NOT NULL,
    size_bytes bigint NOT NULL,
    storage_reference varchar(500) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_file_attachments PRIMARY KEY (id),
    CONSTRAINT fk_file_attachments_user FOREIGN KEY (user_id) REFERENCES identity.users (id) ON DELETE CASCADE,
    CONSTRAINT fk_file_attachments_product FOREIGN KEY (product_id) REFERENCES catalog.products (id) ON DELETE CASCADE,
    CONSTRAINT fk_file_attachments_contact_request FOREIGN KEY (contact_request_id) REFERENCES commercial.contact_requests (id) ON DELETE CASCADE,
    CONSTRAINT fk_file_attachments_marketing_email FOREIGN KEY (marketing_email_id) REFERENCES marketing.marketing_emails (id) ON DELETE CASCADE,
    CONSTRAINT ck_file_attachments_one_owner CHECK (num_nonnulls(user_id, product_id, contact_request_id, marketing_email_id) = 1),
    CONSTRAINT ck_file_attachments_size CHECK (size_bytes >= 0)
);
CREATE INDEX ix_file_attachments_user_id ON platform.file_attachments (user_id);
CREATE INDEX ix_file_attachments_product_id ON platform.file_attachments (product_id);
CREATE INDEX ix_file_attachments_contact_request_id ON platform.file_attachments (contact_request_id);
CREATE INDEX ix_file_attachments_marketing_email_id ON platform.file_attachments (marketing_email_id);
