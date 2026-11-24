-- Flyway V5: Mobile Login & OTP Verification System
-- Replaces manual PIN workflow with instant SMS/Mobile OTP authentication

CREATE TABLE IF NOT EXISTS mobile_otp_verifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID REFERENCES tenants(id) ON DELETE CASCADE,
    phone VARCHAR(20) NOT NULL,
    otp_code VARCHAR(10) NOT NULL,
    purpose VARCHAR(50) NOT NULL DEFAULT 'LOGIN',
    attempts INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 5,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    verified_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_otp_phone ON mobile_otp_verifications(phone);
CREATE INDEX IF NOT EXISTS idx_otp_phone_purpose ON mobile_otp_verifications(phone, purpose, verified);
CREATE INDEX IF NOT EXISTS idx_otp_created_at ON mobile_otp_verifications(phone, created_at);
