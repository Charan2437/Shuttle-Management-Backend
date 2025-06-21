-- =============================================
-- EXTENSIONS
-- =============================================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =============================================
-- CORE USER TABLES
-- =============================================

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL CHECK (role IN ('student', 'admin', 'super_admin')),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Students table
CREATE TABLE IF NOT EXISTS students (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    student_id VARCHAR(50) UNIQUE NOT NULL,
    wallet_balance INTEGER DEFAULT 0,
    profile_image_url TEXT,
    phone_number VARCHAR(20),
    emergency_contact VARCHAR(255),
    enrollment_date DATE,
    graduation_date DATE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Admins table
CREATE TABLE IF NOT EXISTS admins (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    employee_id VARCHAR(50) UNIQUE,
    department VARCHAR(100),
    last_login TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- =============================================
-- LOOKUP TABLES
-- =============================================

-- Permission types
CREATE TABLE IF NOT EXISTS permission_types (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    category VARCHAR(50),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Admin permissions
CREATE TABLE IF NOT EXISTS admin_permissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    admin_id UUID NOT NULL REFERENCES admins(id) ON DELETE CASCADE,
    permission_type_id UUID NOT NULL REFERENCES permission_types(id) ON DELETE CASCADE,
    granted_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    granted_by UUID REFERENCES users(id),
    expires_at TIMESTAMPTZ,
    is_active BOOLEAN DEFAULT true,
    UNIQUE(admin_id, permission_type_id)
);

-- Facility types
CREATE TABLE IF NOT EXISTS facility_types (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    icon VARCHAR(50),
    category VARCHAR(50),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Transaction types
CREATE TABLE IF NOT EXISTS transaction_types (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Booking status types
CREATE TABLE IF NOT EXISTS booking_status_types (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    color VARCHAR(7),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- =============================================
-- LOCATION AND ROUTE TABLES
-- =============================================

-- Bus stops
CREATE TABLE IF NOT EXISTS stops (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    latitude DECIMAL(10,8) NOT NULL,
    longitude DECIMAL(11,8) NOT NULL,
    address TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Stop facilities
CREATE TABLE IF NOT EXISTS stop_facilities (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    stop_id UUID NOT NULL REFERENCES stops(id) ON DELETE CASCADE,
    facility_type_id UUID NOT NULL REFERENCES facility_types(id) ON DELETE CASCADE,
    condition VARCHAR(50) DEFAULT 'good' CHECK (condition IN ('excellent','good','fair','poor','out_of_order')),
    last_maintained DATE,
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(stop_id, facility_type_id)
);

-- Routes
CREATE TABLE IF NOT EXISTS routes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    color VARCHAR(7) NOT NULL,
    estimated_duration INTEGER NOT NULL,
    base_fare INTEGER NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Route operating hours
CREATE TABLE IF NOT EXISTS route_operating_hours (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    route_id UUID NOT NULL REFERENCES routes(id) ON DELETE CASCADE,
    day_of_week INTEGER NOT NULL CHECK (day_of_week BETWEEN 0 AND 6),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(route_id, day_of_week)
);

-- Route stops (extended with segment metadata)
CREATE TABLE IF NOT EXISTS route_stops (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    route_id UUID NOT NULL REFERENCES routes(id) ON DELETE CASCADE,
    stop_id UUID NOT NULL REFERENCES stops(id) ON DELETE CASCADE,
    stop_order INTEGER NOT NULL,
    distance_from_previous DECIMAL(8,2),   -- km from the prior stop
    avg_travel_time_min INTEGER,           -- minutes from the prior stop
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(route_id, stop_order),
    UNIQUE(route_id, stop_id)
);

-- Peak hours
CREATE TABLE IF NOT EXISTS peak_hours (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    route_id UUID NOT NULL REFERENCES routes(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    multiplier DECIMAL(3,2) DEFAULT 1.5,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Peak hour days
CREATE TABLE IF NOT EXISTS peak_hour_days (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    peak_hour_id UUID NOT NULL REFERENCES peak_hours(id) ON DELETE CASCADE,
    day_of_week INTEGER NOT NULL CHECK (day_of_week BETWEEN 0 AND 6),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(peak_hour_id, day_of_week)
);

-- =============================================
-- SHUTTLE & OCCUPANCY TABLES (NEW)
-- =============================================

-- Shuttles
CREATE TABLE IF NOT EXISTS shuttles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    shuttle_no VARCHAR(50) UNIQUE NOT NULL,
    capacity INTEGER NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Map shuttles to routes
CREATE TABLE IF NOT EXISTS shuttle_routes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    shuttle_id UUID NOT NULL REFERENCES shuttles(id) ON DELETE CASCADE,
    route_id   UUID NOT NULL REFERENCES routes(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(shuttle_id, route_id)
);

-- Shuttle occupancy time series
CREATE TABLE IF NOT EXISTS shuttle_occupancy (
    shuttle_id     UUID       REFERENCES shuttles(id) ON DELETE CASCADE,
    recorded_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    occupied_seats INTEGER    NOT NULL CHECK (occupied_seats >= 0),
    PRIMARY KEY(shuttle_id, recorded_at)
);

-- =============================================
-- BOOKING AND TRANSACTION TABLES
-- =============================================

-- Bookings
CREATE TABLE IF NOT EXISTS bookings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    student_id UUID NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    route_id   UUID NOT NULL REFERENCES routes(id),
    from_stop_id UUID NOT NULL REFERENCES stops(id),
    to_stop_id   UUID NOT NULL REFERENCES stops(id),
    scheduled_time TIMESTAMPTZ NOT NULL,
    status_id      UUID NOT NULL REFERENCES booking_status_types(id),
    points_deducted INTEGER NOT NULL,
    booking_reference VARCHAR(50) UNIQUE NOT NULL,
    notes TEXT,
    cancelled_at TIMESTAMPTZ,
    cancelled_by UUID REFERENCES users(id),
    cancellation_reason TEXT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Transfer bookings
CREATE TABLE IF NOT EXISTS transfer_bookings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    main_booking_id UUID NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    from_stop_id   UUID NOT NULL REFERENCES stops(id),
    to_stop_id     UUID NOT NULL REFERENCES stops(id),
    transfer_stop_id UUID NOT NULL REFERENCES stops(id),
    estimated_wait_time INTEGER,
    transfer_order     INTEGER NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(main_booking_id, transfer_order)
);

-- Wallet transactions
CREATE TABLE IF NOT EXISTS wallet_transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    student_id UUID NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    transaction_type_id UUID NOT NULL REFERENCES transaction_types(id),
    amount INTEGER NOT NULL,
    booking_id UUID REFERENCES bookings(id),
    description TEXT NOT NULL,
    reference_id VARCHAR(100),
    processed_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- =============================================
-- SYSTEM TABLES
-- =============================================

-- System settings
CREATE TABLE IF NOT EXISTS system_settings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    setting_key VARCHAR(255) UNIQUE NOT NULL,
    setting_value TEXT NOT NULL,
    data_type VARCHAR(50) DEFAULT 'string' CHECK (data_type IN ('string','integer','boolean','json')),
    description TEXT,
    category VARCHAR(100),
    is_public BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Audit logs
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    table_name VARCHAR(255) NOT NULL,
    record_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL CHECK (action IN ('INSERT','UPDATE','DELETE')),
    old_values JSONB,
    new_values JSONB,
    changed_by UUID REFERENCES users(id),
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Notifications
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) DEFAULT 'info' CHECK (type IN ('info','warning','error','success')),
    is_read BOOLEAN DEFAULT false,
    read_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS route_schedule (
  id             UUID      PRIMARY KEY DEFAULT uuid_generate_v4(),
  route_id       UUID      NOT NULL REFERENCES routes(id)   ON DELETE CASCADE,
  stop_id        UUID      NOT NULL REFERENCES stops(id)    ON DELETE CASCADE,
  day_of_week    INTEGER   NOT NULL CHECK (day_of_week BETWEEN 0 AND 6),
  arrival_time   TIME      NOT NULL,
  departure_time TIME      NOT NULL,
  UNIQUE(route_id, stop_id, day_of_week, departure_time)
);
