-- time_entries Tabelle
CREATE TABLE time_entries (
                              id INTEGER PRIMARY KEY AUTOINCREMENT,
                              date TEXT NOT NULL,
                              start_time TEXT NOT NULL,
                              end_time TEXT,
                              description TEXT NOT NULL,
                              is_break BOOLEAN DEFAULT 0,
                              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- consolidated_entries Tabelle
CREATE TABLE consolidated_entries (
                                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                                      date TEXT NOT NULL,
                                      start_time TEXT NOT NULL,
                                      end_time TEXT NOT NULL,
                                      description TEXT NOT NULL,
                                      duration_minutes INTEGER NOT NULL,
                                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- activity_descriptions Tabelle (für Autocomplete)
CREATE TABLE activity_descriptions (
                                       id INTEGER PRIMARY KEY AUTOINCREMENT,
                                       description TEXT UNIQUE NOT NULL,
                                       usage_count INTEGER DEFAULT 1,
                                       last_used TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);