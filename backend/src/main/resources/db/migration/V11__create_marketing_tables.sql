CREATE TABLE marketing.newsletter_subscriptions (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    email varchar(320) NOT NULL,
    subscription_state varchar(30) NOT NULL DEFAULT 'pending_confirmation',
    subscribed_at timestamptz,
    unsubscribed_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_newsletter_subscriptions PRIMARY KEY (id),
    CONSTRAINT ck_newsletter_subscriptions_email CHECK (email = lower(btrim(email)) AND email <> ''),
    CONSTRAINT ck_newsletter_subscriptions_state CHECK (subscription_state IN ('pending_confirmation', 'subscribed', 'unsubscribed'))
);
CREATE UNIQUE INDEX uq_newsletter_subscriptions_email ON marketing.newsletter_subscriptions (email);

CREATE TABLE marketing.marketing_emails (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    campaign_name varchar(200) NOT NULL,
    subject varchar(300) NOT NULL,
    content_reference varchar(500),
    campaign_state varchar(20) NOT NULL DEFAULT 'draft',
    scheduled_at timestamptz,
    completed_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_marketing_emails PRIMARY KEY (id),
    CONSTRAINT ck_marketing_emails_state CHECK (campaign_state IN ('draft', 'scheduled', 'completed', 'cancelled'))
);

CREATE TABLE marketing.email_recipients (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    marketing_email_id uuid NOT NULL,
    user_id uuid,
    recipient_email varchar(320) NOT NULL,
    delivery_state varchar(20) NOT NULL DEFAULT 'pending',
    state_changed_at timestamptz NOT NULL DEFAULT now(),
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_email_recipients PRIMARY KEY (id),
    CONSTRAINT fk_email_recipients_marketing_email FOREIGN KEY (marketing_email_id) REFERENCES marketing.marketing_emails (id) ON DELETE RESTRICT,
    CONSTRAINT fk_email_recipients_user FOREIGN KEY (user_id) REFERENCES identity.users (id) ON DELETE SET NULL,
    CONSTRAINT uq_email_recipients_campaign_email UNIQUE (marketing_email_id, recipient_email),
    CONSTRAINT ck_email_recipients_email CHECK (recipient_email = lower(btrim(recipient_email)) AND recipient_email <> ''),
    CONSTRAINT ck_email_recipients_state CHECK (delivery_state IN ('pending', 'sent', 'delivered', 'bounced', 'failed', 'unsubscribed'))
);
CREATE INDEX ix_email_recipients_user_id ON marketing.email_recipients (user_id);
