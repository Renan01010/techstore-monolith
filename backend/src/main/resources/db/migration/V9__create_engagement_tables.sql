CREATE TABLE engagement.product_reviews (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    product_id uuid NOT NULL,
    user_id uuid,
    rating smallint NOT NULL,
    review_text text,
    moderation_state varchar(20) NOT NULL DEFAULT 'pending',
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_product_reviews PRIMARY KEY (id),
    CONSTRAINT fk_product_reviews_product FOREIGN KEY (product_id) REFERENCES catalog.products (id) ON DELETE RESTRICT,
    -- Nullable only so a review can outlive a removed account (anonymization).
    CONSTRAINT fk_product_reviews_user FOREIGN KEY (user_id) REFERENCES identity.users (id) ON DELETE SET NULL,
    CONSTRAINT ck_product_reviews_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT ck_product_reviews_moderation_state CHECK (moderation_state IN ('pending', 'approved', 'rejected'))
);
CREATE INDEX ix_product_reviews_product_id ON engagement.product_reviews (product_id);
CREATE INDEX ix_product_reviews_user_id ON engagement.product_reviews (user_id);

CREATE TABLE engagement.favorites (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL,
    product_id uuid NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_favorites PRIMARY KEY (id),
    CONSTRAINT fk_favorites_user FOREIGN KEY (user_id) REFERENCES identity.users (id) ON DELETE CASCADE,
    CONSTRAINT fk_favorites_product FOREIGN KEY (product_id) REFERENCES catalog.products (id) ON DELETE CASCADE,
    CONSTRAINT uq_favorites_user_product UNIQUE (user_id, product_id)
);
CREATE INDEX ix_favorites_product_id ON engagement.favorites (product_id);
