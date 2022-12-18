create TABLE file(
    file longblob,
    filename varchar(255),
    size bigint,
    email varchar(255),
    PRIMARY KEY (filename),
    FOREIGN KEY(email) REFERENCES user(email)
)