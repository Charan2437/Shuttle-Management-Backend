-- Shuttle Management Database Schema Reference
-- Saved on 2025-06-19

-- This file contains the full SQL schema for the shuttle_management database, including all core, lookup, location, route, booking, transaction, and system tables.

-- =============================================
-- CORE USER TABLES
-- =============================================

-- Users table (base table for all users)
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL CHECK (role IN ('student', 'admin', 'super_admin')),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Students table (extends users for student-specific data)
CREATE TABLE students (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    student_id VARCHAR(50) UNIQUE NOT NULL,
    wallet_balance INTEGER DEFAULT 0, -- stored in points/cents
    profile_image_url TEXT,
    phone_number VARCHAR(20),
    emergency_contact VARCHAR(255),
    enrollment_date DATE,
    graduation_date DATE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Admins table (extends users for admin-specific data)
CREATE TABLE admins (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    employee_id VARCHAR(50) UNIQUE,
    department VARCHAR(100),
    last_login TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- =============================================
-- LOOKUP TABLES
-- =============================================

-- Permission types lookup table
CREATE TABLE permission_types (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    category VARCHAR(50),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Admin permissions junction table
CREATE TABLE admin_permissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    admin_id UUID NOT NULL REFERENCES admins(id) ON DELETE CASCADE,
    permission_type_id UUID NOT NULL REFERENCES permission_types(id) ON DELETE CASCADE,
    granted_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    granted_by UUID REFERENCES users(id),
    expires_at TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN DEFAULT true,
    UNIQUE(admin_id, permission_type_id)
);

-- Facility types lookup table
CREATE TABLE facility_types (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    icon VARCHAR(50),
    category VARCHAR(50),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Transaction types lookup table
CREATE TABLE transaction_types (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Booking status types lookup table
CREATE TABLE booking_status_types (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    color VARCHAR(7), -- hex color for UI
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- =============================================
-- LOCATION AND ROUTE TABLES
-- =============================================

-- Bus stops table
CREATE TABLE stops (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    latitude DECIMAL(10, 8) NOT NULL,
    longitude DECIMAL(11, 8) NOT NULL,
    address TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Stop facilities junction table
CREATE TABLE stop_facilities (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    stop_id UUID NOT NULL REFERENCES stops(id) ON DELETE CASCADE,
    facility_type_id UUID NOT NULL REFERENCES facility_types(id) ON DELETE CASCADE,
    condition VARCHAR(50) DEFAULT 'good' CHECK (condition IN ('excellent', 'good', 'fair', 'poor', 'out_of_order')),
    last_maintained DATE,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(stop_id, facility_type_id)
);

-- Routes table
CREATE TABLE routes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    color VARCHAR(7) NOT NULL, -- hex color code
    estimated_duration INTEGER NOT NULL, -- in minutes
    base_fare INTEGER NOT NULL, -- in points/cents
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Route operating hours (normalized to support different schedules)
CREATE TABLE route_operating_hours (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    route_id UUID NOT NULL REFERENCES routes(id) ON DELETE CASCADE,
    day_of_week INTEGER NOT NULL CHECK (day_of_week >= 0 AND day_of_week <= 6), -- 0=Sunday
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(route_id, day_of_week)
);

-- Route stops junction table
CREATE TABLE route_stops (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    route_id UUID NOT NULL REFERENCES routes(id) ON DELETE CASCADE,
    stop_id UUID NOT NULL REFERENCES stops(id) ON DELETE CASCADE,
    stop_order INTEGER NOT NULL,
    estimated_travel_time INTEGER, -- time to reach this stop from previous stop in minutes
    distance_from_previous DECIMAL(8,2), -- distance in kilometers
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(route_id, stop_order),
    UNIQUE(route_id, stop_id)
);

-- Peak hours table
CREATE TABLE peak_hours (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    route_id UUID NOT NULL REFERENCES routes(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL, -- e.g., "Morning Rush", "Evening Rush"
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    multiplier DECIMAL(3,2) DEFAULT 1.5,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Peak hour days junction table
CREATE TABLE peak_hour_days (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    peak_hour_id UUID NOT NULL REFERENCES peak_hours(id) ON DELETE CASCADE,
    day_of_week INTEGER NOT NULL CHECK (day_of_week >= 0 AND day_of_week <= 6), -- 0=Sunday
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(peak_hour_id, day_of_week)
);

-- =============================================
-- BOOKING AND TRANSACTION TABLES
-- =============================================

-- Bookings table
CREATE TABLE bookings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    student_id UUID NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    route_id UUID NOT NULL REFERENCES routes(id),
    from_stop_id UUID NOT NULL REFERENCES stops(id),
    to_stop_id UUID NOT NULL REFERENCES stops(id),
    scheduled_time TIMESTAMP WITH TIME ZONE NOT NULL,
    status_id UUID NOT NULL REFERENCES booking_status_types(id),
    points_deducted INTEGER NOT NULL,
    booking_reference VARCHAR(50) UNIQUE NOT NULL,
    notes TEXT,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    cancelled_by UUID REFERENCES users(id),
    cancellation_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Transfer bookings table (for multi-route journeys)
CREATE TABLE transfer_bookings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    main_booking_id UUID NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    from_route_id UUID NOT NULL REFERENCES routes(id),
    to_route_id UUID NOT NULL REFERENCES routes(id),
    transfer_stop_id UUID NOT NULL REFERENCES stops(id),
    estimated_wait_time INTEGER, -- in minutes
    transfer_order INTEGER NOT NULL, -- for multiple transfers
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(main_booking_id, transfer_order)
);

-- Wallet transactions table
CREATE TABLE wallet_transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    student_id UUID NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    transaction_type_id UUID NOT NULL REFERENCES transaction_types(id),
    amount INTEGER NOT NULL, -- in points/cents
    booking_id UUID REFERENCES bookings(id), -- nullable, for booking-related transactions
    description TEXT NOT NULL,
    reference_id VARCHAR(100), -- external reference (payment gateway, etc.)
    processed_by UUID REFERENCES users(id), -- admin who processed manual transactions
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- =============================================
-- SYSTEM TABLES
-- =============================================

-- System settings table
CREATE TABLE system_settings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    setting_key VARCHAR(255) UNIQUE NOT NULL,
    setting_value TEXT NOT NULL,
    data_type VARCHAR(50) DEFAULT 'string' CHECK (data_type IN ('string', 'integer', 'boolean', 'json')),
    description TEXT,
    category VARCHAR(100),
    is_public BOOLEAN DEFAULT false, -- whether setting can be read by non-admins
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Audit log table (for tracking changes)
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    table_name VARCHAR(255) NOT NULL,
    record_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL CHECK (action IN ('INSERT', 'UPDATE', 'DELETE')),
    old_values JSONB,
    new_values JSONB,
    changed_by UUID REFERENCES users(id),
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Notifications table
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) DEFAULT 'info' CHECK (type IN ('info', 'warning', 'error', 'success')),
    is_read BOOLEAN DEFAULT false,
    read_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
