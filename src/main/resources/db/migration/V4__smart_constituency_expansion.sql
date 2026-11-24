-- Flyway V4: Smart Constituency & MLA Command Center Expansion
-- Features: Village Heatmap, Multi-Complaint Field Inspections, Department Referrals, Development Projects, Public Hearings

-- 1. Extend complaints with anonymity, geo-coords, overdue tracking, and citizen feedback
ALTER TABLE complaints
    ADD COLUMN IF NOT EXISTS is_anonymous BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS latitude NUMERIC(10, 7),
    ADD COLUMN IF NOT EXISTS longitude NUMERIC(10, 7),
    ADD COLUMN IF NOT EXISTS is_overdue BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS due_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS citizen_feedback_rating INT,
    ADD COLUMN IF NOT EXISTS citizen_feedback_notes TEXT;

CREATE INDEX IF NOT EXISTS idx_complaints_village_cat ON complaints(tenant_id, village, category);
CREATE INDEX IF NOT EXISTS idx_complaints_overdue ON complaints(tenant_id, is_overdue, due_at);

-- 2. Extend complaint_field_visits with multi-action reports, photos, and next follow-up
ALTER TABLE complaint_field_visits
    ADD COLUMN IF NOT EXISTS village VARCHAR(120),
    ADD COLUMN IF NOT EXISTS latitude NUMERIC(10, 7),
    ADD COLUMN IF NOT EXISTS longitude NUMERIC(10, 7),
    ADD COLUMN IF NOT EXISTS action_taken TEXT,
    ADD COLUMN IF NOT EXISTS pending_action TEXT,
    ADD COLUMN IF NOT EXISTS before_photos TEXT,
    ADD COLUMN IF NOT EXISTS after_photos TEXT,
    ADD COLUMN IF NOT EXISTS next_follow_up_at TIMESTAMP WITH TIME ZONE;

-- 3. Multi-complaint batching for coordinated village inspections
CREATE TABLE IF NOT EXISTS complaint_field_visit_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    field_visit_id UUID NOT NULL REFERENCES complaint_field_visits(id) ON DELETE CASCADE,
    complaint_id UUID NOT NULL REFERENCES complaints(id) ON DELETE CASCADE,
    observations TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_visit_items_visit ON complaint_field_visit_items(field_visit_id);
CREATE INDEX IF NOT EXISTS idx_visit_items_complaint ON complaint_field_visit_items(complaint_id);

-- 4. External Department Referral & Follow-up Tracker
CREATE TABLE IF NOT EXISTS constituency_department_referrals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    complaint_id UUID NOT NULL REFERENCES complaints(id) ON DELETE CASCADE,
    department_name VARCHAR(150) NOT NULL,
    official_reference_number VARCHAR(100),
    date_of_referral DATE NOT NULL,
    follow_up_date DATE,
    supporting_documents TEXT,
    response_received TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'referred', -- referred, work_initiated, verified_resolution, rejected
    action_outcome TEXT,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_dept_referrals_tenant_status ON constituency_department_referrals(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_dept_referrals_complaint ON constituency_department_referrals(complaint_id);

-- 5. Public Development Project Tracker
CREATE TABLE IF NOT EXISTS constituency_development_projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    project_name VARCHAR(200) NOT NULL,
    village VARCHAR(120),
    ward VARCHAR(120),
    category VARCHAR(100) NOT NULL,
    responsible_department VARCHAR(150),
    approved_cost NUMERIC(14, 2),
    start_date DATE,
    expected_completion_date DATE,
    status VARCHAR(50) NOT NULL DEFAULT 'planned', -- planned, in_progress, delayed, completed
    milestones TEXT,
    photos TEXT,
    completion_report TEXT,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_dev_projects_tenant_status ON constituency_development_projects(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_dev_projects_category ON constituency_development_projects(category);

-- 6. Public Hearings & Appointment Calendar
CREATE TABLE IF NOT EXISTS constituency_public_hearings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    hearing_date DATE NOT NULL,
    hearing_title VARCHAR(200) NOT NULL,
    location VARCHAR(200) NOT NULL,
    max_slots INT NOT NULL DEFAULT 50,
    status VARCHAR(50) NOT NULL DEFAULT 'scheduled', -- scheduled, completed, cancelled
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_hearings_tenant_date ON constituency_public_hearings(tenant_id, hearing_date);

-- 7. Citizen Hearing Appointments
CREATE TABLE IF NOT EXISTS constituency_hearing_appointments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    hearing_id UUID REFERENCES constituency_public_hearings(id) ON DELETE SET NULL,
    citizen_name VARCHAR(150) NOT NULL,
    citizen_phone VARCHAR(20) NOT NULL,
    village VARCHAR(120),
    issue_category VARCHAR(100) NOT NULL,
    issue_description TEXT NOT NULL,
    preferred_date DATE NOT NULL,
    slot_number INT,
    status VARCHAR(50) NOT NULL DEFAULT 'requested', -- requested, confirmed, rescheduled, cancelled, attended, no_show
    admin_notes TEXT,
    follow_up_complaint_id UUID REFERENCES complaints(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_hearing_appointments_tenant ON constituency_hearing_appointments(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_hearing_appointments_phone ON constituency_hearing_appointments(citizen_phone);
