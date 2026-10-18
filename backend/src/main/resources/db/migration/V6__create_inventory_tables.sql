CREATE TABLE inventory.suppliers (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    name varchar(200) NOT NULL,
    contact_name varchar(200),
    contact_email varchar(320),
    contact_phone varchar(40),
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_suppliers PRIMARY KEY (id)
);

CREATE TABLE inventory.inventory (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    product_variant_id uuid NOT NULL,
    quantity_on_hand integer NOT NULL DEFAULT 0,
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_inventory PRIMARY KEY (id),
    CONSTRAINT fk_inventory_product_variant FOREIGN KEY (product_variant_id) REFERENCES catalog.product_variants (id) ON DELETE RESTRICT,
    CONSTRAINT uq_inventory_product_variant UNIQUE (product_variant_id),
    CONSTRAINT ck_inventory_quantity_on_hand CHECK (quantity_on_hand >= 0)
);

CREATE TABLE inventory.inventory_movements (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    inventory_id uuid NOT NULL,
    supplier_id uuid,
    movement_type varchar(30) NOT NULL,
    quantity integer NOT NULL,
    reason varchar(300),
    reference varchar(120),
    occurred_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_inventory_movements PRIMARY KEY (id),
    CONSTRAINT fk_inventory_movements_inventory FOREIGN KEY (inventory_id) REFERENCES inventory.inventory (id) ON DELETE RESTRICT,
    CONSTRAINT fk_inventory_movements_supplier FOREIGN KEY (supplier_id) REFERENCES inventory.suppliers (id) ON DELETE RESTRICT,
    CONSTRAINT ck_inventory_movements_type CHECK (movement_type IN ('receipt', 'removal', 'adjustment_increase', 'adjustment_decrease')),
    CONSTRAINT ck_inventory_movements_quantity CHECK (quantity > 0)
);
CREATE INDEX ix_inventory_movements_inventory_id ON inventory.inventory_movements (inventory_id, occurred_at);
CREATE INDEX ix_inventory_movements_supplier_id ON inventory.inventory_movements (supplier_id);

-- Movement history is append-only; corrections are new compensating movements.
CREATE FUNCTION inventory.reject_movement_change() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'inventory_movements is append-only (% rejected)', TG_OP
        USING ERRCODE = 'restrict_violation';
END $$;

CREATE TRIGGER trg_inventory_movements_append_only
    BEFORE UPDATE OR DELETE ON inventory.inventory_movements
    FOR EACH ROW EXECUTE FUNCTION inventory.reject_movement_change();

CREATE TRIGGER trg_inventory_movements_no_truncate
    BEFORE TRUNCATE ON inventory.inventory_movements
    FOR EACH STATEMENT EXECUTE FUNCTION inventory.reject_movement_change();
