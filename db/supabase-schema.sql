-- Enable pgvector extension
create extension if not exists vector;

-- Tracks table
create table tracks (
    id serial primary key,
    track_name text not null,
    artist_name text not null,
    album_name text,
    tags text[],
    mbid text
);

-- Listens table
create table listens (
    id serial primary key,
    track_id integer references tracks(id) on delete cascade,
    played_at timestamptz not null,
    source text
);

-- Track Embeddings table
create table track_embeddings (
    track_id integer references tracks(id) on delete cascade,
    embedding vector(1536) not null,
    primary key (track_id)
);

-- Indices for fast querying
create index idx_listens_played_at on listens(played_at);
create index idx_tracks_artist on tracks(artist_name);
create index tracks_mbid_idx on tracks(mbid);
-- pgvector ANN index
create index idx_track_embeddings_ann on track_embeddings using ivfflat (embedding vector_cosine_ops);