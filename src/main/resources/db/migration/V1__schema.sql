CREATE TABLE app_users (
 id BIGSERIAL PRIMARY KEY, email VARCHAR(255) NOT NULL UNIQUE, password_hash VARCHAR(255) NOT NULL,
 display_name VARCHAR(255) NOT NULL, role VARCHAR(40) NOT NULL, enabled BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TABLE refresh_tokens (
 id BIGSERIAL PRIMARY KEY, token VARCHAR(255) NOT NULL UNIQUE, expires_at TIMESTAMPTZ NOT NULL,
 revoked BOOLEAN NOT NULL DEFAULT FALSE, user_id BIGINT NOT NULL REFERENCES app_users(id)
);
CREATE TABLE venues (
 id BIGSERIAL PRIMARY KEY, name VARCHAR(255) NOT NULL, address_line VARCHAR(255), city VARCHAR(255) NOT NULL,
 timezone VARCHAR(255), total_capacity INTEGER
);
CREATE TABLE venue_sections (
 id BIGSERIAL PRIMARY KEY, name VARCHAR(255) NOT NULL, row_count INTEGER, seats_per_row INTEGER,
 price_tier VARCHAR(255), venue_id BIGINT NOT NULL REFERENCES venues(id)
);
CREATE TABLE seats (
 id BIGSERIAL PRIMARY KEY, row_label VARCHAR(255), seat_number INTEGER, accessible BOOLEAN NOT NULL DEFAULT FALSE,
 section_id BIGINT NOT NULL REFERENCES venue_sections(id), CONSTRAINT uq_seat_position UNIQUE(section_id,row_label,seat_number)
);
CREATE TABLE events (
 id BIGSERIAL PRIMARY KEY, title VARCHAR(255) NOT NULL, description VARCHAR(255), starts_at TIMESTAMPTZ NOT NULL,
 doors_open_at TIMESTAMPTZ, status VARCHAR(255), sales_open_at TIMESTAMPTZ, sales_close_at TIMESTAMPTZ,
 created_at TIMESTAMPTZ, venue_id BIGINT NOT NULL REFERENCES venues(id), organizer_id BIGINT NOT NULL REFERENCES app_users(id)
);
CREATE TABLE event_section_prices (
 id BIGSERIAL PRIMARY KEY, price NUMERIC(12,2), currency VARCHAR(255),
 event_id BIGINT NOT NULL REFERENCES events(id), section_id BIGINT NOT NULL REFERENCES venue_sections(id),
 CONSTRAINT uq_event_section_price UNIQUE(event_id,section_id)
);
CREATE TABLE seat_holds (
 id BIGSERIAL PRIMARY KEY, hold_reference VARCHAR(255) NOT NULL UNIQUE, expires_at TIMESTAMPTZ,
 status VARCHAR(255), created_at TIMESTAMPTZ, event_id BIGINT NOT NULL REFERENCES events(id),
 user_id BIGINT NOT NULL REFERENCES app_users(id)
);
CREATE TABLE seat_hold_seats (
 hold_id BIGINT NOT NULL REFERENCES seat_holds(id), seat_id BIGINT NOT NULL REFERENCES seats(id),
 PRIMARY KEY(hold_id,seat_id)
);
CREATE TABLE bookings (
 id BIGSERIAL PRIMARY KEY, booking_reference VARCHAR(255) NOT NULL UNIQUE, status VARCHAR(255),
 subtotal NUMERIC(12,2), fees NUMERIC(12,2), total NUMERIC(12,2), currency VARCHAR(255),
 created_at TIMESTAMPTZ, confirmed_at TIMESTAMPTZ, cancelled_at TIMESTAMPTZ, version BIGINT NOT NULL DEFAULT 0,
 event_id BIGINT NOT NULL REFERENCES events(id), user_id BIGINT NOT NULL REFERENCES app_users(id)
);
CREATE TABLE tickets (
 id BIGSERIAL PRIMARY KEY, ticket_code VARCHAR(255) NOT NULL UNIQUE, status VARCHAR(255), issued_at TIMESTAMPTZ,
 checked_in_at TIMESTAMPTZ, price_paid NUMERIC(12,2), booking_id BIGINT NOT NULL REFERENCES bookings(id),
 seat_id BIGINT NOT NULL REFERENCES seats(id), event_id BIGINT NOT NULL REFERENCES events(id),
 CONSTRAINT uq_ticket_event_seat UNIQUE(event_id,seat_id)
);
CREATE TABLE payments (
 id BIGSERIAL PRIMARY KEY, transaction_id VARCHAR(255), method VARCHAR(255), status VARCHAR(255),
 amount NUMERIC(12,2), currency VARCHAR(255), failure_code VARCHAR(255), paid_at TIMESTAMPTZ,
 booking_id BIGINT NOT NULL UNIQUE REFERENCES bookings(id)
);
CREATE TABLE outbox_messages (
 id BIGSERIAL PRIMARY KEY, aggregate_type VARCHAR(255), aggregate_id BIGINT, event_type VARCHAR(255),
 payload TEXT, created_at TIMESTAMPTZ, published_at TIMESTAMPTZ
);
CREATE TABLE processed_messages (
 id BIGSERIAL PRIMARY KEY, message_id VARCHAR(255) NOT NULL UNIQUE, consumed_at TIMESTAMPTZ
);
CREATE TABLE audit_entries (
 id BIGSERIAL PRIMARY KEY, actor_id BIGINT, action VARCHAR(255), entity_type VARCHAR(255),
 entity_id BIGINT, created_at TIMESTAMPTZ
);
CREATE INDEX idx_venue_city ON venues(city);
CREATE INDEX idx_section_venue ON venue_sections(venue_id);
CREATE INDEX idx_seat_section ON seats(section_id);
CREATE INDEX idx_event_date ON events(starts_at);
CREATE INDEX idx_event_status ON events(status);
CREATE INDEX idx_event_venue ON events(venue_id);
CREATE INDEX idx_event_organizer ON events(organizer_id);
CREATE INDEX idx_price_event ON event_section_prices(event_id);
CREATE INDEX idx_hold_event ON seat_holds(event_id);
CREATE INDEX idx_booking_event ON bookings(event_id);
CREATE INDEX idx_booking_user ON bookings(user_id);
CREATE INDEX idx_ticket_booking ON tickets(booking_id);
CREATE INDEX idx_ticket_event ON tickets(event_id);
CREATE INDEX idx_ticket_seat ON tickets(seat_id);
CREATE INDEX idx_outbox_pending ON outbox_messages(created_at) WHERE published_at IS NULL;
