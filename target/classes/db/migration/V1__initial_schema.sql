-- Flyway V1: Initial Production Schema for Smart Village & Town Foundation
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Tenants table
CREATE TABLE IF NOT EXISTS tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(80) NOT NULL UNIQUE,
    district VARCHAR(100),
    state VARCHAR(100),
    pincode VARCHAR(10),
    contact_phone VARCHAR(20),
    contact_email VARCHAR(150),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(180) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    phone VARCHAR(20),
    role VARCHAR(50) NOT NULL,
    tenant_id UUID REFERENCES tenants(id) ON DELETE SET NULL,
    vendor_id UUID,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Audit Logs
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    actor_id UUID,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(80),
    entity_id VARCHAR(100),
    details TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Civic Complaints & Constituency Field Visits
CREATE TABLE IF NOT EXISTS complaints (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    tracking_id VARCHAR(40) NOT NULL UNIQUE,
    citizen_name VARCHAR(150) NOT NULL,
    citizen_phone VARCHAR(20) NOT NULL,
    citizen_email VARCHAR(150),
    category VARCHAR(100) NOT NULL,
    ward_code VARCHAR(50),
    ward VARCHAR(120),
    village VARCHAR(120),
    priority VARCHAR(30) NOT NULL DEFAULT 'normal',
    status VARCHAR(30) NOT NULL DEFAULT 'submitted',
    department VARCHAR(100),
    assigned_to UUID REFERENCES users(id) ON DELETE SET NULL,
    follow_up_at TIMESTAMPTZ,
    duplicate_of UUID REFERENCES complaints(id) ON DELETE SET NULL,
    resolution_note TEXT,
    description TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS complaint_updates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    complaint_id UUID NOT NULL REFERENCES complaints(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL,
    note TEXT,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS complaint_field_visits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    complaint_id UUID NOT NULL REFERENCES complaints(id) ON DELETE CASCADE,
    scheduled_at TIMESTAMPTZ NOT NULL,
    assigned_to UUID REFERENCES users(id) ON DELETE SET NULL,
    purpose VARCHAR(1000) NOT NULL,
    internal_notes TEXT,
    outcome VARCHAR(50),
    completed_at TIMESTAMPTZ,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Marketplace Categories & Vendors
CREATE TABLE IF NOT EXISTS marketplace_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    name_hi VARCHAR(100),
    slug VARCHAR(100) NOT NULL,
    icon VARCHAR(20) DEFAULT '🛍️',
    description TEXT,
    sort_order INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(tenant_id, slug)
);

CREATE TABLE IF NOT EXISTS marketplace_vendors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    business_name VARCHAR(180) NOT NULL,
    business_name_hi VARCHAR(180),
    owner_name VARCHAR(120) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(150),
    address TEXT,
    description TEXT,
    logo_url TEXT,
    cover_url TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'pending',
    delivery_available BOOLEAN NOT NULL DEFAULT TRUE,
    pickup_available BOOLEAN NOT NULL DEFAULT TRUE,
    min_order_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
    delivery_charge NUMERIC(12,2) NOT NULL DEFAULT 0,
    upi_id VARCHAR(100),
    upi_qr_url TEXT,
    location_latitude NUMERIC(10,7),
    location_longitude NUMERIC(10,7),
    location_label VARCHAR(250),
    opening_time TIME,
    closing_time TIME,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS marketplace_category_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    vendor_id UUID NOT NULL REFERENCES marketplace_vendors(id) ON DELETE CASCADE,
    requested_name VARCHAR(100) NOT NULL,
    reason TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'pending',
    category_id UUID REFERENCES marketplace_categories(id) ON DELETE SET NULL,
    reviewed_by UUID REFERENCES users(id) ON DELETE SET NULL,
    reviewed_at TIMESTAMPTZ,
    review_note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Products & Multi-Images
CREATE TABLE IF NOT EXISTS marketplace_products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    vendor_id UUID NOT NULL REFERENCES marketplace_vendors(id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES marketplace_categories(id) ON DELETE RESTRICT,
    name VARCHAR(200) NOT NULL,
    name_hi VARCHAR(200),
    description TEXT,
    image_url TEXT,
    price NUMERIC(12,2) NOT NULL,
    unit VARCHAR(50) NOT NULL DEFAULT 'piece',
    stock_quantity NUMERIC(12,3) NOT NULL DEFAULT 0,
    min_quantity NUMERIC(12,3) NOT NULL DEFAULT 1,
    max_quantity NUMERIC(12,3),
    booking_charge NUMERIC(12,2) NOT NULL DEFAULT 0,
    visiting_charge NUMERIC(12,2) NOT NULL DEFAULT 0,
    attributes TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    featured BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS marketplace_product_images (
    product_id UUID NOT NULL REFERENCES marketplace_products(id) ON DELETE CASCADE,
    image_url TEXT NOT NULL
);

-- Customer Profiles
CREATE TABLE IF NOT EXISTS marketplace_customer_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    auth_user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    default_delivery_address TEXT,
    profile_photo_path TEXT,
    approval_status VARCHAR(30) NOT NULL DEFAULT 'pending',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    approved_at TIMESTAMPTZ,
    reviewed_by UUID REFERENCES users(id) ON DELETE SET NULL,
    reviewed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Orders & Order Items
CREATE TABLE IF NOT EXISTS marketplace_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    vendor_id UUID NOT NULL REFERENCES marketplace_vendors(id) ON DELETE RESTRICT,
    customer_auth_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    order_number VARCHAR(60) NOT NULL UNIQUE,
    customer_name VARCHAR(150) NOT NULL,
    customer_phone VARCHAR(20) NOT NULL,
    customer_email VARCHAR(150),
    delivery_address TEXT NOT NULL,
    delivery_method VARCHAR(30) NOT NULL DEFAULT 'delivery',
    payment_method VARCHAR(30) NOT NULL DEFAULT 'upi',
    payment_status VARCHAR(30) NOT NULL DEFAULT 'pending',
    payment_review_status VARCHAR(30) NOT NULL DEFAULT 'not_required',
    order_status VARCHAR(30) NOT NULL DEFAULT 'placed',
    subtotal NUMERIC(12,2) NOT NULL DEFAULT 0,
    delivery_charge NUMERIC(12,2) NOT NULL DEFAULT 0,
    prepaid_discount NUMERIC(12,2) NOT NULL DEFAULT 0,
    total NUMERIC(12,2) NOT NULL DEFAULT 0,
    notes TEXT,
    delivery_agent_id UUID,
    delivery_otp_hash VARCHAR(100),
    delivery_otp_sent_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    payment_collected_at TIMESTAMPTZ,
    payment_collected_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS marketplace_order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES marketplace_orders(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES marketplace_products(id) ON DELETE RESTRICT,
    product_name VARCHAR(200) NOT NULL,
    unit VARCHAR(50) NOT NULL,
    quantity NUMERIC(12,3) NOT NULL,
    unit_price NUMERIC(12,2) NOT NULL,
    line_total NUMERIC(12,2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Delivery Fleet
CREATE TABLE IF NOT EXISTS marketplace_delivery_agents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    auth_user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    address TEXT,
    vehicle_type VARCHAR(60),
    vehicle_number VARCHAR(60),
    profile_photo_path TEXT,
    vehicle_photo_path TEXT,
    aadhaar_photo_path TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    availability_status VARCHAR(30) NOT NULL DEFAULT 'offline',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS marketplace_delivery_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL UNIQUE REFERENCES marketplace_orders(id) ON DELETE CASCADE,
    delivery_agent_id UUID NOT NULL REFERENCES marketplace_delivery_agents(id) ON DELETE RESTRICT,
    status VARCHAR(40) NOT NULL DEFAULT 'assigned',
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    accepted_at TIMESTAMPTZ,
    picked_up_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    delivery_note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS marketplace_delivery_codes (
    order_id UUID PRIMARY KEY REFERENCES marketplace_orders(id) ON DELETE CASCADE,
    code VARCHAR(10) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS marketplace_delivery_reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    order_id UUID NOT NULL UNIQUE REFERENCES marketplace_orders(id) ON DELETE CASCADE,
    delivery_agent_id UUID NOT NULL REFERENCES marketplace_delivery_agents(id) ON DELETE RESTRICT,
    customer_auth_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rating INT NOT NULL CHECK(rating BETWEEN 1 AND 5),
    review TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Payments
CREATE TABLE IF NOT EXISTS marketplace_payment_submissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    order_id UUID NOT NULL REFERENCES marketplace_orders(id) ON DELETE CASCADE,
    vendor_id UUID NOT NULL REFERENCES marketplace_vendors(id) ON DELETE CASCADE,
    customer_auth_id UUID REFERENCES users(id) ON DELETE SET NULL,
    amount NUMERIC(12,2) NOT NULL,
    utr_number VARCHAR(30),
    proof_path TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'submitted',
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    reviewed_at TIMESTAMPTZ,
    reviewed_by UUID REFERENCES users(id) ON DELETE SET NULL,
    review_note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Local Services Bookings
CREATE TABLE IF NOT EXISTS marketplace_service_bookings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    vendor_id UUID NOT NULL REFERENCES marketplace_vendors(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES marketplace_products(id) ON DELETE RESTRICT,
    customer_auth_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    booking_number VARCHAR(60) NOT NULL UNIQUE,
    customer_name VARCHAR(150) NOT NULL,
    customer_phone VARCHAR(20) NOT NULL,
    requested_for TIMESTAMPTZ,
    service_address TEXT NOT NULL,
    notes TEXT,
    estimated_total NUMERIC(12,2) NOT NULL DEFAULT 0,
    booking_charge NUMERIC(12,2) NOT NULL DEFAULT 0,
    visiting_charge NUMERIC(12,2) NOT NULL DEFAULT 0,
    upfront_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
    remaining_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
    payment_status VARCHAR(30) NOT NULL DEFAULT 'pending',
    booking_status VARCHAR(40) NOT NULL DEFAULT 'pending_payment',
    completion_code VARCHAR(10),
    completion_code_created_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    cancellation_reason TEXT,
    cancelled_by VARCHAR(50),
    cancelled_at TIMESTAMPTZ,
    refund_status VARCHAR(30) DEFAULT 'not_applicable',
    refund_amount NUMERIC(12,2),
    refund_requested_at TIMESTAMPTZ,
    refund_processed_at TIMESTAMPTZ,
    refund_note TEXT,
    remaining_payment_status VARCHAR(30) DEFAULT 'not_required',
    remaining_paid_amount NUMERIC(12,2),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS marketplace_service_payment_submissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    booking_id UUID NOT NULL REFERENCES marketplace_service_bookings(id) ON DELETE CASCADE,
    vendor_id UUID NOT NULL REFERENCES marketplace_vendors(id) ON DELETE CASCADE,
    customer_auth_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    amount NUMERIC(12,2) NOT NULL,
    utr_number VARCHAR(30),
    proof_path TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'submitted',
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    reviewed_at TIMESTAMPTZ,
    reviewed_by UUID REFERENCES users(id) ON DELETE SET NULL,
    review_note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS marketplace_service_reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    booking_id UUID NOT NULL UNIQUE REFERENCES marketplace_service_bookings(id) ON DELETE CASCADE,
    vendor_id UUID NOT NULL REFERENCES marketplace_vendors(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES marketplace_products(id) ON DELETE RESTRICT,
    customer_auth_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    customer_name VARCHAR(150),
    rating INT NOT NULL CHECK(rating BETWEEN 1 AND 5),
    review TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Transport Fleet & Bookings
CREATE TABLE IF NOT EXISTS marketplace_transport_vehicles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    delivery_agent_id UUID NOT NULL REFERENCES marketplace_delivery_agents(id) ON DELETE CASCADE,
    vehicle_type VARCHAR(50) NOT NULL,
    make_model VARCHAR(100) NOT NULL,
    registration_number VARCHAR(50) NOT NULL,
    passenger_capacity INT NOT NULL DEFAULT 0,
    cargo_weight_kg NUMERIC(10,2) NOT NULL DEFAULT 0,
    cargo_area_sqft NUMERIC(10,2) NOT NULL DEFAULT 0,
    cargo_length_cm NUMERIC(10,2),
    cargo_width_cm NUMERIC(10,2),
    cargo_height_cm NUMERIC(10,2),
    supports_passengers BOOLEAN NOT NULL DEFAULT TRUE,
    supports_goods BOOLEAN NOT NULL DEFAULT FALSE,
    vehicle_photo_path TEXT,
    rc_document_path TEXT,
    driving_licence_path TEXT,
    insurance_document_path TEXT,
    permit_document_path TEXT,
    approval_status VARCHAR(40) NOT NULL DEFAULT 'pending',
    admin_note TEXT,
    reviewed_by UUID REFERENCES users(id) ON DELETE SET NULL,
    reviewed_at TIMESTAMPTZ,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(tenant_id, registration_number)
);

CREATE TABLE IF NOT EXISTS marketplace_transport_rates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    delivery_agent_id UUID NOT NULL REFERENCES marketplace_delivery_agents(id) ON DELETE CASCADE,
    vehicle_id UUID NOT NULL REFERENCES marketplace_transport_vehicles(id) ON DELETE CASCADE,
    service_type VARCHAR(50) NOT NULL,
    rate_per_km NUMERIC(10,2) NOT NULL,
    minimum_fare NUMERIC(10,2) NOT NULL DEFAULT 0,
    waiting_rate_per_hour NUMERIC(10,2) NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(vehicle_id, service_type)
);

CREATE TABLE IF NOT EXISTS marketplace_transport_bookings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_number VARCHAR(60) NOT NULL UNIQUE,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    customer_auth_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    customer_name VARCHAR(150) NOT NULL,
    customer_phone VARCHAR(20) NOT NULL,
    service_type VARCHAR(50) NOT NULL,
    vehicle_type VARCHAR(50) NOT NULL,
    vehicle_id UUID REFERENCES marketplace_transport_vehicles(id),
    delivery_agent_id UUID REFERENCES marketplace_delivery_agents(id),
    rate_id UUID REFERENCES marketplace_transport_rates(id),
    source_address VARCHAR(500) NOT NULL,
    source_latitude NUMERIC(10,7),
    source_longitude NUMERIC(10,7),
    destination_address VARCHAR(500) NOT NULL,
    destination_latitude NUMERIC(10,7),
    destination_longitude NUMERIC(10,7),
    distance_km NUMERIC(10,2) NOT NULL,
    travel_at TIMESTAMPTZ,
    passenger_count INT NOT NULL DEFAULT 0,
    goods_description TEXT,
    package_count INT NOT NULL DEFAULT 0,
    cargo_weight_kg NUMERIC(10,2) NOT NULL DEFAULT 0,
    cargo_area_sqft NUMERIC(10,2) NOT NULL DEFAULT 0,
    cargo_length_cm NUMERIC(10,2),
    cargo_width_cm NUMERIC(10,2),
    cargo_height_cm NUMERIC(10,2),
    handling_notes TEXT,
    rate_per_km NUMERIC(10,2) NOT NULL,
    minimum_fare NUMERIC(10,2) NOT NULL DEFAULT 0,
    estimated_fare NUMERIC(12,2) NOT NULL,
    final_fare NUMERIC(12,2),
    payment_method VARCHAR(30) NOT NULL DEFAULT 'pay_driver',
    payment_status VARCHAR(30) NOT NULL DEFAULT 'pending',
    booking_status VARCHAR(40) NOT NULL DEFAULT 'requested',
    customer_note TEXT,
    cancellation_reason TEXT,
    accepted_at TIMESTAMPTZ,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Notifications
CREATE TABLE IF NOT EXISTS marketplace_notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    recipient_auth_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(120) NOT NULL,
    message TEXT NOT NULL,
    campaign_id UUID,
    recipient_type VARCHAR(30),
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
