CREATE TABLE cart.carts (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_carts PRIMARY KEY (id),
    CONSTRAINT fk_carts_user FOREIGN KEY (user_id) REFERENCES identity.users (id) ON DELETE CASCADE
);
CREATE INDEX ix_carts_user_id ON cart.carts (user_id);

CREATE TABLE cart.cart_items (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    cart_id uuid NOT NULL,
    product_variant_id uuid NOT NULL,
    quantity integer NOT NULL,
    unit_price_snapshot numeric(12, 2),
    price_currency char(3),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_cart_items PRIMARY KEY (id),
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES cart.carts (id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_items_product_variant FOREIGN KEY (product_variant_id) REFERENCES catalog.product_variants (id) ON DELETE RESTRICT,
    CONSTRAINT uq_cart_items_cart_variant UNIQUE (cart_id, product_variant_id),
    CONSTRAINT ck_cart_items_quantity CHECK (quantity > 0),
    -- NULL price means unknown; 0 is a known zero price.
    CONSTRAINT ck_cart_items_price_known CHECK (
        (unit_price_snapshot IS NULL AND price_currency IS NULL)
        OR (unit_price_snapshot IS NOT NULL AND unit_price_snapshot >= 0 AND price_currency IS NOT NULL AND price_currency ~ '^[A-Z]{3}$')
    )
);
CREATE INDEX ix_cart_items_product_variant_id ON cart.cart_items (product_variant_id);
