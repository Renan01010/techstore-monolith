CREATE TABLE catalog.category_attributes (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    category_id uuid NOT NULL,
    attribute_id uuid NOT NULL,
    is_required boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_category_attributes PRIMARY KEY (id),
    CONSTRAINT fk_category_attributes_category FOREIGN KEY (category_id) REFERENCES catalog.categories (id) ON DELETE CASCADE,
    CONSTRAINT fk_category_attributes_attribute FOREIGN KEY (attribute_id) REFERENCES catalog.attributes (id) ON DELETE RESTRICT,
    CONSTRAINT uq_category_attributes_pair UNIQUE (category_id, attribute_id)
);
CREATE INDEX ix_category_attributes_attribute_id ON catalog.category_attributes (attribute_id);

CREATE TABLE catalog.product_variants (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    product_id uuid NOT NULL,
    sku varchar(80) NOT NULL,
    name varchar(200) NOT NULL,
    variant_state varchar(20) NOT NULL DEFAULT 'active',
    price numeric(12, 2) NOT NULL,
    currency char(3) NOT NULL DEFAULT 'BRL',
    length_mm numeric(10, 2),
    width_mm numeric(10, 2),
    height_mm numeric(10, 2),
    weight_kg numeric(10, 3),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_product_variants PRIMARY KEY (id),
    CONSTRAINT fk_product_variants_product FOREIGN KEY (product_id) REFERENCES catalog.products (id) ON DELETE RESTRICT,
    CONSTRAINT uq_product_variants_sku UNIQUE (sku),
    CONSTRAINT ck_product_variants_state CHECK (variant_state IN ('active', 'inactive')),
    CONSTRAINT ck_product_variants_price CHECK (price >= 0),
    CONSTRAINT ck_product_variants_currency CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_product_variants_length CHECK (length_mm IS NULL OR length_mm >= 0),
    CONSTRAINT ck_product_variants_width CHECK (width_mm IS NULL OR width_mm >= 0),
    CONSTRAINT ck_product_variants_height CHECK (height_mm IS NULL OR height_mm >= 0),
    CONSTRAINT ck_product_variants_weight CHECK (weight_kg IS NULL OR weight_kg >= 0)
);
CREATE INDEX ix_product_variants_product_id ON catalog.product_variants (product_id);

CREATE TABLE catalog.product_images (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    product_id uuid NOT NULL,
    storage_reference varchar(500) NOT NULL,
    alt_text varchar(300),
    sort_order integer NOT NULL,
    is_primary boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_product_images PRIMARY KEY (id),
    CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES catalog.products (id) ON DELETE CASCADE,
    CONSTRAINT uq_product_images_product_order UNIQUE (product_id, sort_order),
    CONSTRAINT ck_product_images_sort_order CHECK (sort_order >= 0)
);
CREATE INDEX ix_product_images_product_id ON catalog.product_images (product_id);
CREATE UNIQUE INDEX uq_product_images_primary ON catalog.product_images (product_id) WHERE is_primary;

CREATE TABLE catalog.variant_attribute_values (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    product_variant_id uuid NOT NULL,
    attribute_id uuid NOT NULL,
    value_text text,
    value_numeric numeric(18, 4),
    value_boolean boolean,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_variant_attribute_values PRIMARY KEY (id),
    CONSTRAINT fk_variant_attribute_values_variant FOREIGN KEY (product_variant_id) REFERENCES catalog.product_variants (id) ON DELETE CASCADE,
    CONSTRAINT fk_variant_attribute_values_attribute FOREIGN KEY (attribute_id) REFERENCES catalog.attributes (id) ON DELETE RESTRICT,
    CONSTRAINT uq_variant_attribute_values_pair UNIQUE (product_variant_id, attribute_id),
    CONSTRAINT ck_variant_attribute_values_one_value CHECK (num_nonnulls(value_text, value_numeric, value_boolean) = 1)
);
CREATE INDEX ix_variant_attribute_values_attribute_id ON catalog.variant_attribute_values (attribute_id);

-- A value is valid only if the product's primary category is assigned the attribute
-- and the populated column matches the attribute type.
CREATE FUNCTION catalog.validate_variant_attribute_value() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    v_category_id uuid;
    v_type varchar(20);
BEGIN
    SELECT p.primary_category_id INTO v_category_id
    FROM catalog.product_variants v
    JOIN catalog.products p ON p.id = v.product_id
    WHERE v.id = NEW.product_variant_id
    FOR SHARE OF p;

    PERFORM 1 FROM catalog.category_attributes ca
    WHERE ca.category_id = v_category_id AND ca.attribute_id = NEW.attribute_id
    FOR SHARE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'attribute % is not assigned to the primary category of the variant product', NEW.attribute_id
            USING ERRCODE = 'check_violation';
    END IF;

    SELECT value_type INTO v_type FROM catalog.attributes WHERE id = NEW.attribute_id FOR SHARE;
    IF (v_type = 'text' AND NEW.value_text IS NULL)
       OR (v_type = 'numeric' AND NEW.value_numeric IS NULL)
       OR (v_type = 'boolean' AND NEW.value_boolean IS NULL) THEN
        RAISE EXCEPTION 'value does not match attribute type %', v_type USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER trg_variant_attribute_values_validate
    BEFORE INSERT OR UPDATE ON catalog.variant_attribute_values
    FOR EACH ROW EXECUTE FUNCTION catalog.validate_variant_attribute_value();

-- Changing a product's primary category must not orphan existing attribute values.
CREATE FUNCTION catalog.guard_product_category_change() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.primary_category_id IS DISTINCT FROM OLD.primary_category_id THEN
        -- Waits for in-flight value writes holding a share lock on this product.
        PERFORM 1 FROM catalog.products WHERE id = OLD.id FOR UPDATE;
        IF EXISTS (
            SELECT 1
            FROM catalog.variant_attribute_values vav
            JOIN catalog.product_variants v ON v.id = vav.product_variant_id
            WHERE v.product_id = OLD.id
              AND NOT EXISTS (
                  SELECT 1 FROM catalog.category_attributes ca
                  WHERE ca.category_id = NEW.primary_category_id AND ca.attribute_id = vav.attribute_id)
        ) THEN
            RAISE EXCEPTION 'category change would invalidate existing variant attribute values'
                USING ERRCODE = 'check_violation';
        END IF;
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER trg_products_guard_category_change
    BEFORE UPDATE OF primary_category_id ON catalog.products
    FOR EACH ROW EXECUTE FUNCTION catalog.guard_product_category_change();

-- Removing or re-pointing an assignment must not orphan existing attribute values.
CREATE FUNCTION catalog.guard_category_attribute_change() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    PERFORM 1 FROM catalog.products WHERE primary_category_id = OLD.category_id FOR NO KEY UPDATE;
    IF EXISTS (
        SELECT 1
        FROM catalog.variant_attribute_values vav
        JOIN catalog.product_variants v ON v.id = vav.product_variant_id
        JOIN catalog.products p ON p.id = v.product_id
        WHERE p.primary_category_id = OLD.category_id AND vav.attribute_id = OLD.attribute_id
    ) THEN
        RAISE EXCEPTION 'category attribute assignment is used by existing variant attribute values'
            USING ERRCODE = 'restrict_violation';
    END IF;
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER trg_category_attributes_guard_change
    BEFORE DELETE OR UPDATE OF category_id, attribute_id ON catalog.category_attributes
    FOR EACH ROW EXECUTE FUNCTION catalog.guard_category_attribute_change();

-- Changing an attribute type must not invalidate existing values.
CREATE FUNCTION catalog.guard_attribute_type_change() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.value_type IS DISTINCT FROM OLD.value_type
       AND EXISTS (SELECT 1 FROM catalog.variant_attribute_values WHERE attribute_id = OLD.id) THEN
        RAISE EXCEPTION 'attribute type cannot change while values exist' USING ERRCODE = 'restrict_violation';
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER trg_attributes_guard_type_change
    BEFORE UPDATE OF value_type ON catalog.attributes
    FOR EACH ROW EXECUTE FUNCTION catalog.guard_attribute_type_change();

-- Serialize variant state changes with product publication.
CREATE FUNCTION catalog.lock_variant_product() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        PERFORM 1 FROM catalog.products WHERE id = OLD.product_id FOR NO KEY UPDATE;
        RETURN OLD;
    END IF;
    PERFORM 1 FROM catalog.products WHERE id = NEW.product_id FOR NO KEY UPDATE;
    RETURN NEW;
END $$;

CREATE TRIGGER trg_product_variants_lock_product
    BEFORE INSERT OR UPDATE OF variant_state, product_id OR DELETE ON catalog.product_variants
    FOR EACH ROW EXECUTE FUNCTION catalog.lock_variant_product();

-- A published product needs at least one active variant when the transaction commits.
CREATE FUNCTION catalog.check_published_product_has_active_variant() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    v_ids uuid[];
    v_product_id uuid;
BEGIN
    IF TG_TABLE_NAME = 'products' THEN
        v_ids := ARRAY[NEW.id];
    ELSIF TG_OP = 'DELETE' THEN
        v_ids := ARRAY[OLD.product_id];
    ELSE
        v_ids := ARRAY[NEW.product_id, OLD.product_id];
    END IF;

    FOREACH v_product_id IN ARRAY v_ids LOOP
        IF EXISTS (SELECT 1 FROM catalog.products WHERE id = v_product_id AND publication_state = 'published')
           AND NOT EXISTS (SELECT 1 FROM catalog.product_variants WHERE product_id = v_product_id AND variant_state = 'active') THEN
            RAISE EXCEPTION 'published product % requires at least one active variant', v_product_id
                USING ERRCODE = 'check_violation';
        END IF;
    END LOOP;
    RETURN NULL;
END $$;

CREATE CONSTRAINT TRIGGER trg_products_published_requires_active_variant
    AFTER INSERT OR UPDATE OF publication_state ON catalog.products
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION catalog.check_published_product_has_active_variant();

CREATE CONSTRAINT TRIGGER trg_product_variants_published_requires_active_variant
    AFTER UPDATE OF variant_state, product_id OR DELETE ON catalog.product_variants
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION catalog.check_published_product_has_active_variant();
