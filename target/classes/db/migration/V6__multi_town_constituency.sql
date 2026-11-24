-- Flyway V6: Multi-Town Constituency Architecture
-- Adds constituency and body_type to tenants, and provisions multi-town local entities

-- 1. Add constituency and body_type to tenants table
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS constituency VARCHAR(150) NOT NULL DEFAULT 'Garautha';
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS body_type VARCHAR(50) NOT NULL DEFAULT 'NAGAR_PALIKA';

-- Update existing Gursarai tenant
UPDATE tenants
SET constituency = 'Garautha', body_type = 'NAGAR_PALIKA_PARISHAD'
WHERE slug = 'gursarai';

-- 2. Seed Erich Nagar Panchayat (Town 2 in Garautha Constituency)
INSERT INTO tenants (id, name, slug, district, state, pincode, contact_phone, contact_email, constituency, body_type, active)
VALUES (
    'a0000000-0000-0000-0000-000000000002',
    'Erich Nagar Panchayat',
    'erich',
    'Jhansi',
    'Uttar Pradesh',
    '284302',
    '9450000002',
    'admin@erich.gov.in',
    'Garautha',
    'NAGAR_PANCHAYAT',
    TRUE
) ON CONFLICT (slug) DO UPDATE SET constituency = 'Garautha', body_type = 'NAGAR_PANCHAYAT';

-- 3. Seed Garautha Nagar Panchayat (Town 3 in Garautha Constituency)
INSERT INTO tenants (id, name, slug, district, state, pincode, contact_phone, contact_email, constituency, body_type, active)
VALUES (
    'a0000000-0000-0000-0000-000000000003',
    'Garautha Nagar Panchayat',
    'garautha',
    'Jhansi',
    'Uttar Pradesh',
    '284205',
    '9450000003',
    'admin@garautha.gov.in',
    'Garautha',
    'NAGAR_PANCHAYAT',
    TRUE
) ON CONFLICT (slug) DO UPDATE SET constituency = 'Garautha', body_type = 'NAGAR_PANCHAYAT';

-- 4. Add constituency to users table for MLA constituency scoping
ALTER TABLE users ADD COLUMN IF NOT EXISTS constituency VARCHAR(150);

-- 5. Seed independent Town Administrators (Password: Admin@123)
-- BCrypt hash of "Admin@123" is $2a$10$bZl5krMU4x.iogvz.bnY0OeN9Orsg9kmpZrzrQxBpcJWuXwLdphqi
INSERT INTO users (id, email, password, full_name, phone, role, tenant_id, constituency, active)
VALUES 
    (
        'b0000000-0000-0000-0000-000000000011',
        'admin@gursarai.gov.in',
        '$2a$10$bZl5krMU4x.iogvz.bnY0OeN9Orsg9kmpZrzrQxBpcJWuXwLdphqi',
        'Gursarai Town Administrator',
        '9450011001',
        'ADMIN',
        'a0000000-0000-0000-0000-000000000001',
        'Garautha',
        TRUE
    ),
    (
        'b0000000-0000-0000-0000-000000000012',
        'admin@erich.gov.in',
        '$2a$10$bZl5krMU4x.iogvz.bnY0OeN9Orsg9kmpZrzrQxBpcJWuXwLdphqi',
        'Erich Town Administrator',
        '9450011002',
        'ADMIN',
        'a0000000-0000-0000-0000-000000000002',
        'Garautha',
        TRUE
    ),
    (
        'b0000000-0000-0000-0000-000000000013',
        'admin@garautha.gov.in',
        '$2a$10$bZl5krMU4x.iogvz.bnY0OeN9Orsg9kmpZrzrQxBpcJWuXwLdphqi',
        'Garautha Town Administrator',
        '9450011003',
        'ADMIN',
        'a0000000-0000-0000-0000-000000000003',
        'Garautha',
        TRUE
    ),
    (
        'b0000000-0000-0000-0000-000000000099',
        'mla@garautha.gov.in',
        '$2a$10$bZl5krMU4x.iogvz.bnY0OeN9Orsg9kmpZrzrQxBpcJWuXwLdphqi',
        'Hon. MLA Shri Anand Verma',
        '9450099001',
        'MLA',
        'a0000000-0000-0000-0000-000000000001',
        'Garautha',
        TRUE
    )
ON CONFLICT (email) DO NOTHING;

-- 6. Seed sample Vendors for Erich (Independent Town Marketplace)
INSERT INTO marketplace_vendors (id, tenant_id, business_name, business_name_hi, owner_name, phone, email, address, status, is_active)
VALUES (
    'c0000000-0000-0000-0000-000000000002',
    'a0000000-0000-0000-0000-000000000002',
    'Erich Kisan Krishi Sewa Kendra',
    'एरच किसान कृषि सेवा केंद्र',
    'Devendra Singh',
    '9450022002',
    'kisan@erich.test',
    'Main Market Road, Erich Nagar Panchayat',
    'approved',
    TRUE
) ON CONFLICT DO NOTHING;

-- 7. Seed sample Vendors for Garautha (Independent Town Marketplace)
INSERT INTO marketplace_vendors (id, tenant_id, business_name, business_name_hi, owner_name, phone, email, address, status, is_active)
VALUES (
    'c0000000-0000-0000-0000-000000000003',
    'a0000000-0000-0000-0000-000000000003',
    'Garautha Agro & Kirana Mart',
    'गरौठा एग्रो एवं किराना मार्ट',
    'Mahesh Chandra',
    '9450022003',
    'agro@garautha.test',
    'Tehsil Road, Garautha',
    'approved',
    TRUE
) ON CONFLICT DO NOTHING;

-- 8. Seed Marketplace Categories for Erich and Garautha
INSERT INTO marketplace_categories (tenant_id, name, name_hi, slug, icon, description, sort_order, active)
VALUES 
('a0000000-0000-0000-0000-000000000002', 'Grocery & Kirana', 'किराना और राशन', 'grocery-kirana', '🛒', 'Daily staples in Erich', 1, TRUE),
('a0000000-0000-0000-0000-000000000002', 'Fresh Farm Produce', 'ताज़ी उपज', 'farm-produce', '🥦', 'Farm-fresh vegetables in Erich', 2, TRUE),
('a0000000-0000-0000-0000-000000000003', 'Grocery & Kirana', 'किराना और राशन', 'grocery-kirana', '🛒', 'Daily staples in Garautha', 1, TRUE),
('a0000000-0000-0000-0000-000000000003', 'Agro Tools & Seeds', 'कृषि उपकरण व बीज', 'agro-seeds', '🌱', 'Seeds & farm supplies in Garautha', 2, TRUE)
ON CONFLICT (tenant_id, slug) DO NOTHING;

-- 9. Seed Delivery Partners for Erich and Garautha
INSERT INTO users (id, email, password, full_name, phone, role, tenant_id, constituency, active)
VALUES 
    (
        'b0000000-0000-0000-0000-000000000021',
        'driver.erich@smartvillagetown.org',
        '$2a$10$bZl5krMU4x.iogvz.bnY0OeN9Orsg9kmpZrzrQxBpcJWuXwLdphqi',
        'Sonu Kushwaha (Erich Fleet)',
        '9450033001',
        'DELIVERY_AGENT',
        'a0000000-0000-0000-0000-000000000002',
        'Garautha',
        TRUE
    ),
    (
        'b0000000-0000-0000-0000-000000000022',
        'driver.garautha@smartvillagetown.org',
        '$2a$10$bZl5krMU4x.iogvz.bnY0OeN9Orsg9kmpZrzrQxBpcJWuXwLdphqi',
        'Raju Ahirwar (Garautha Fleet)',
        '9450033002',
        'DELIVERY_AGENT',
        'a0000000-0000-0000-0000-000000000003',
        'Garautha',
        TRUE
    ),
    (
        'b0000000-0000-0000-0000-000000000099',
        'mla@garautha.gov.in',
        '$2a$10$bZl5krMU4x.iogvz.bnY0OeN9Orsg9kmpZrzrQxBpcJWuXwLdphqi',
        'Hon. MLA Shri Anand Verma',
        '9450099001',
        'MLA',
        'a0000000-0000-0000-0000-000000000001',
        'Garautha',
        TRUE
    )
ON CONFLICT (email) DO NOTHING;

INSERT INTO marketplace_delivery_agents (id, tenant_id, auth_user_id, full_name, email, phone, address, vehicle_type, vehicle_number, is_active, availability_status)
VALUES 
    (
        'd0000000-0000-0000-0000-000000000002',
        'a0000000-0000-0000-0000-000000000002',
        'b0000000-0000-0000-0000-000000000021',
        'Sonu Kushwaha (Erich Fleet)',
        'driver.erich@smartvillagetown.org',
        '9450033001',
        'Erich Town Ward 4',
        'auto_rickshaw',
        'UP 93 AB 4002',
        TRUE,
        'available'
    ),
    (
        'd0000000-0000-0000-0000-000000000003',
        'a0000000-0000-0000-0000-000000000003',
        'b0000000-0000-0000-0000-000000000022',
        'Raju Ahirwar (Garautha Fleet)',
        'driver.garautha@smartvillagetown.org',
        '9450033002',
        'Garautha Bus Stand',
        'pickup',
        'UP 93 TC 8901',
        TRUE,
        'available'
    )
ON CONFLICT (auth_user_id) DO NOTHING;

-- 10. Seed Transport Vehicles for Erich and Garautha
INSERT INTO marketplace_transport_vehicles (id, tenant_id, delivery_agent_id, vehicle_type, make_model, registration_number, passenger_capacity, cargo_weight_kg, approval_status, is_active)
VALUES 
    (
        'e0000000-0000-0000-0000-000000000002',
        'a0000000-0000-0000-0000-000000000002',
        'd0000000-0000-0000-0000-000000000002',
        'auto_rickshaw',
        'Bajaj RE Compact',
        'UP 93 AB 4002',
        4,
        150.00,
        'approved',
        TRUE
    ),
    (
        'e0000000-0000-0000-0000-000000000003',
        'a0000000-0000-0000-0000-000000000003',
        'd0000000-0000-0000-0000-000000000003',
        'pickup',
        'Mahindra Bolero Maxi Truck',
        'UP 93 TC 8901',
        2,
        1200.00,
        'approved',
        TRUE
    )
ON CONFLICT DO NOTHING;
