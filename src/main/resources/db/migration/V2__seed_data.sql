-- Flyway V2: Seed Data for Smart Village & Town Foundation
-- Seed default Gursarai tenant
INSERT INTO tenants (id, name, slug, district, state, pincode, contact_phone, contact_email, active)
VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'Gursarai Nagar Palika',
    'gursarai',
    'Jhansi',
    'Uttar Pradesh',
    '284202',
    '9450000000',
    'admin@gursarai.gov.in',
    TRUE
) ON CONFLICT (slug) DO NOTHING;

-- Seed default Super Admin account (password: Admin@123)
-- BCrypt hash of "Admin@123" is $2a$10$bZl5krMU4x.iogvz.bnY0OeN9Orsg9kmpZrzrQxBpcJWuXwLdphqi
INSERT INTO users (id, email, password, full_name, phone, role, tenant_id, active)
VALUES (
    'b0000000-0000-0000-0000-000000000001',
    'admin@smartvillagetown.org',
    '$2a$10$bZl5krMU4x.iogvz.bnY0OeN9Orsg9kmpZrzrQxBpcJWuXwLdphqi',
    'Platform Administrator',
    '9876543210',
    'SUPER_ADMIN',
    'a0000000-0000-0000-0000-000000000001',
    TRUE
) ON CONFLICT (email) DO NOTHING;

-- Seed default categories for Gursarai
INSERT INTO marketplace_categories (tenant_id, name, name_hi, slug, icon, description, sort_order, active)
VALUES 
('a0000000-0000-0000-0000-000000000001', 'Grocery & Kirana', 'किराना और राशन', 'grocery-kirana', '🛒', 'Daily staples, spices, packaged food', 1, TRUE),
('a0000000-0000-0000-0000-000000000001', 'Fresh Vegetables & Fruits', 'ताज़ी सब्जियाँ और फल', 'vegetables-fruits', '🥦', 'Farm-fresh vegetables and local fruits', 2, TRUE),
('a0000000-0000-0000-0000-000000000001', 'Dairy & Milk', 'डेयरी और दूध', 'dairy-milk', '🥛', 'Fresh milk, paneer, curd, ghee', 3, TRUE),
('a0000000-0000-0000-0000-000000000001', 'Pharmacy & Medicines', 'दवाइयाँ', 'pharmacy-medicines', '💊', 'Medicines and healthcare supplies', 4, TRUE),
('a0000000-0000-0000-0000-000000000001', 'Electronics & Mobile Repair', 'इलेक्ट्रॉनिक्स और मोबाइल मरम्मत', 'electronics-repair', '📱', 'Mobile recharge, accessories, repair services', 5, TRUE),
('a0000000-0000-0000-0000-000000000001', 'Plumbing & Electrical Services', 'प्लंबर और बिजली मिस्त्री', 'plumbing-electrical', '🔧', 'Home visit repair and technician bookings', 6, TRUE)
ON CONFLICT (tenant_id, slug) DO NOTHING;
