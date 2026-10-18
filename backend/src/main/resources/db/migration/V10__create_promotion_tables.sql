CREATE TABLE promotions.coupons (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    code varchar(60) NOT NULL,
    benefit_type varchar(20) NOT NULL,
    benefit_value numeric(12, 2) NOT NULL,
    currency char(3),
    valid_from timestamptz NOT NULL,
    valid_until timestamptz NOT NULL,
    max_total_uses integer,
    max_uses_per_user integer,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_coupons PRIMARY KEY (id),
    CONSTRAINT uq_coupons_code UNIQUE (code),
    CONSTRAINT ck_coupons_code_normalized CHECK (code = upper(btrim(code)) AND code <> ''),
    CONSTRAINT ck_coupons_benefit_type CHECK (benefit_type IN ('fixed_amount', 'percentage')),
    CONSTRAINT ck_coupons_benefit_value CHECK (
        benefit_value >= 0 AND (benefit_type <> 'percentage' OR benefit_value <= 100)
    ),
    CONSTRAINT ck_coupons_currency CHECK (
        (benefit_type = 'fixed_amount' AND currency IS NOT NULL AND currency ~ '^[A-Z]{3}$')
        OR (benefit_type = 'percentage' AND currency IS NULL)
    ),
    CONSTRAINT ck_coupons_validity CHECK (valid_until > valid_from),
    CONSTRAINT ck_coupons_max_total_uses CHECK (max_total_uses IS NULL OR max_total_uses > 0),
    CONSTRAINT ck_coupons_max_uses_per_user CHECK (max_uses_per_user IS NULL OR max_uses_per_user > 0)
);

CREATE TABLE promotions.coupon_usage (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    coupon_id uuid NOT NULL,
    user_id uuid,
    used_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_coupon_usage PRIMARY KEY (id),
    CONSTRAINT fk_coupon_usage_coupon FOREIGN KEY (coupon_id) REFERENCES promotions.coupons (id) ON DELETE RESTRICT,
    CONSTRAINT fk_coupon_usage_user FOREIGN KEY (user_id) REFERENCES identity.users (id) ON DELETE SET NULL
);
CREATE INDEX ix_coupon_usage_coupon_id ON promotions.coupon_usage (coupon_id);
CREATE INDEX ix_coupon_usage_user_id ON promotions.coupon_usage (user_id);
