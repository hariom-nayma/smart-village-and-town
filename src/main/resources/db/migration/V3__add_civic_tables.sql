-- Flyway V3: Add missing Civic tables (Announcements, Initiatives, Volunteers)
CREATE TABLE IF NOT EXISTS announcements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    category VARCHAR(80),
    summary TEXT,
    content TEXT NOT NULL,
    image_url TEXT,
    published BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_announcements_tenant ON announcements(tenant_id, published);

CREATE TABLE IF NOT EXISTS initiatives (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(150) NOT NULL,
    summary TEXT,
    description TEXT,
    status VARCHAR(50),
    target_area VARCHAR(150),
    start_date DATE,
    end_date DATE,
    progress INT NOT NULL DEFAULT 0,
    image_url TEXT,
    published BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_initiatives_tenant ON initiatives(tenant_id, published);

CREATE TABLE IF NOT EXISTS volunteers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(150),
    area VARCHAR(150),
    skills TEXT,
    availability VARCHAR(100),
    status VARCHAR(30) NOT NULL DEFAULT 'new',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_volunteers_tenant ON volunteers(tenant_id, status);
