CREATE TABLE catalog.categories (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    parent_category_id uuid,
    name varchar(150) NOT NULL,
    slug varchar(160) NOT NULL,
    description text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_categories PRIMARY KEY (id),
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_category_id) REFERENCES catalog.categories (id) ON DELETE RESTRICT,
    CONSTRAINT uq_categories_slug UNIQUE (slug),
    CONSTRAINT ck_categories_not_self_parent CHECK (parent_category_id IS NULL OR parent_category_id <> id)
);
CREATE INDEX ix_categories_parent_category_id ON catalog.categories (parent_category_id);

CREATE TABLE catalog.brands (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    name varchar(150) NOT NULL,
    slug varchar(160) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_brands PRIMARY KEY (id),
    CONSTRAINT uq_brands_slug UNIQUE (slug)
);

CREATE TABLE catalog.products (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    primary_category_id uuid NOT NULL,
    brand_id uuid,
    name varchar(200) NOT NULL,
    slug varchar(220) NOT NULL,
    description text,
    publication_state varchar(20) NOT NULL DEFAULT 'draft',
    is_customizable boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_products PRIMARY KEY (id),
    CONSTRAINT fk_products_primary_category FOREIGN KEY (primary_category_id) REFERENCES catalog.categories (id) ON DELETE RESTRICT,
    CONSTRAINT fk_products_brand FOREIGN KEY (brand_id) REFERENCES catalog.brands (id) ON DELETE SET NULL,
    CONSTRAINT uq_products_slug UNIQUE (slug),
    CONSTRAINT ck_products_publication_state CHECK (publication_state IN ('draft', 'published', 'archived'))
);
CREATE INDEX ix_products_primary_category_id ON catalog.products (primary_category_id);
CREATE INDEX ix_products_brand_id ON catalog.products (brand_id);
CREATE INDEX ix_products_publication_state ON catalog.products (publication_state);

CREATE TABLE catalog.attributes (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    name varchar(150) NOT NULL,
    code varchar(80) NOT NULL,
    value_type varchar(20) NOT NULL,
    unit varchar(30),
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_attributes PRIMARY KEY (id),
    CONSTRAINT uq_attributes_code UNIQUE (code),
    CONSTRAINT ck_attributes_value_type CHECK (value_type IN ('text', 'numeric', 'boolean'))
);
