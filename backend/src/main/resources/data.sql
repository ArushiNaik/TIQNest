INSERT INTO users (user_id, name, email, password, role)
VALUES ('user_demo-admin', 'Christine Gerald', 'Christine@demo.com',
        'pwd', 'ADMIN'),
       ('user_demo-employee-1', 'Bahman', 'Bahman@demo.com',
        'pwd', 'EMPLOYEE'),
       ('user_demo-employee-2', 'Amin', 'Amin@demo.com',
        'pwd', 'EMPLOYEE')
ON CONFLICT (user_id) DO UPDATE
SET name = EXCLUDED.name, email = EXCLUDED.email,
    password = EXCLUDED.password, role = EXCLUDED.role;
