INSERT INTO venues (name, address_line, city, timezone, total_capacity) VALUES
 ('KIMEP Hall', '4 Abay Avenue', 'Almaty', 'Asia/Almaty', 720),
 ('Astana Arena Hall', '10 Qabanbay Batyr', 'Astana', 'Asia/Almaty', 720),
 ('Ulsan Convention Hall', '1 Convention Road', 'Ulsan', 'Asia/Seoul', 720);
-- 9 sections, 240 seats each = 2160 seats total. No hand-created schema.
INSERT INTO venue_sections (name, row_count, seats_per_row, price_tier, venue_id)
SELECT 'Section ' || v.sect, 20, 12, 'STANDARD', v.venue_id
FROM (SELECT venue_id, generate_series(1,3) AS sect FROM (SELECT id AS venue_id FROM venues) x) v;
INSERT INTO seats (row_label, seat_number, accessible, section_id)
SELECT chr(64 + row_no), seat_no, (row_no = 1 AND seat_no <= 2), s.id
FROM venue_sections s CROSS JOIN generate_series(1,20) row_no CROSS JOIN generate_series(1,12) seat_no;
