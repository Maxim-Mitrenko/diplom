package com.example.diplom.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class File {

    @Lob
    private byte[] file;

    @EmbeddedId
    private FileInfo fileInfo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "email")
    private User user;
}
