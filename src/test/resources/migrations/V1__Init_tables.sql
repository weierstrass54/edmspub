CREATE TABLE users(
    id BIGSERIAL PRIMARY KEY,
    login TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL,
    first_name TEXT,
    middle_name TEXT,
    last_name TEXT,
    appointment TEXT NOT NULL,
    phone TEXT NOT NULL,
    email TEXT NOT NULL,
    permissions TEXT[] NOT NULL
);

CREATE TABLE keys(
    user_id BIGINT NOT NULL REFERENCES users (id),
    private_key TEXT NOT NULL,
    public_key TEXT NOT NULL,
    x509_certificate TEXT NOT NULL
);

CREATE TABLE documents(
    id BIGSERIAL PRIMARY KEY,
    author_id BIGINT NOT NULL REFERENCES users (id),
    type TEXT NOT NULL,
    path TEXT NOT NULL,
    size BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE sign_routes(
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL REFERENCES documents (id),
    user_id BIGINT NOT NULL REFERENCES users (id),
    ordinal INTEGER NOT NULL
);

CREATE TABLE signatures(
    sign_route_id BIGINT NOT NULL REFERENCES sign_routes (id),
    notes TEXT,
    signed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE sign_route_templates(
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL
);

CREATE TABLE sign_route_template_users(
    template_id BIGINT REFERENCES sign_route_templates (id),
    user_id BIGINT REFERENCES users (id)
);
