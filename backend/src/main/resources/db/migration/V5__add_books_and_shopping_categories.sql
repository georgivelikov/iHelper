-- Books and Shopping became starter categories. Give them to every existing account that does not
-- already have a live category of that name, at the end of its order. Nothing is renamed or removed,
-- and the placeholder user (which cannot sign in) is left alone.
INSERT INTO category (id, name, color, owner_id, position, created_at, updated_at)
SELECT gen_random_uuid(),
       s.name,
       s.color,
       u.id,
       COALESCE(p.max_position, -1) + row_number() OVER (PARTITION BY u.id ORDER BY s.ord),
       now(),
       now()
  FROM app_user u
 CROSS JOIN (VALUES (1, 'Books', '#f59e0b'), (2, 'Shopping', '#10b981')) AS s(ord, name, color)
  LEFT JOIN (SELECT owner_id, max(position) AS max_position
               FROM category
              WHERE deleted_at IS NULL
              GROUP BY owner_id) p ON p.owner_id = u.id
 WHERE u.deleted_at IS NULL
   AND u.id <> '00000000-0000-0000-0000-000000000001'
   AND NOT EXISTS (SELECT 1
                     FROM category c
                    WHERE c.owner_id = u.id
                      AND c.deleted_at IS NULL
                      AND lower(c.name) = lower(s.name));
