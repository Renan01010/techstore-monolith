CREATE TABLE commercial.contact_requests (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    user_id uuid,
    contact_name varchar(200) NOT NULL,
    contact_email varchar(320) NOT NULL,
    contact_phone varchar(40),
    message text,
    request_state varchar(20) NOT NULL DEFAULT 'received',
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_contact_requests PRIMARY KEY (id),
    CONSTRAINT fk_contact_requests_user FOREIGN KEY (user_id) REFERENCES identity.users (id) ON DELETE SET NULL,
    CONSTRAINT ck_contact_requests_state CHECK (request_state IN ('received', 'in_progress', 'completed', 'cancelled'))
);
CREATE INDEX ix_contact_requests_user_id ON commercial.contact_requests (user_id);
CREATE INDEX ix_contact_requests_state ON commercial.contact_requests (request_state);

CREATE TABLE commercial.contact_request_items (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    contact_request_id uuid NOT NULL,
    product_id uuid,
    product_variant_id uuid,
    item_description varchar(500) NOT NULL,
    sku_snapshot varchar(80),
    quantity integer NOT NULL,
    customization_notes text,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_contact_request_items PRIMARY KEY (id),
    CONSTRAINT fk_contact_request_items_request FOREIGN KEY (contact_request_id) REFERENCES commercial.contact_requests (id) ON DELETE RESTRICT,
    CONSTRAINT fk_contact_request_items_product FOREIGN KEY (product_id) REFERENCES catalog.products (id) ON DELETE SET NULL,
    CONSTRAINT fk_contact_request_items_variant FOREIGN KEY (product_variant_id) REFERENCES catalog.product_variants (id) ON DELETE SET NULL,
    CONSTRAINT ck_contact_request_items_quantity CHECK (quantity > 0)
);
CREATE INDEX ix_contact_request_items_request_id ON commercial.contact_request_items (contact_request_id);
CREATE INDEX ix_contact_request_items_product_id ON commercial.contact_request_items (product_id);
CREATE INDEX ix_contact_request_items_variant_id ON commercial.contact_request_items (product_variant_id);
