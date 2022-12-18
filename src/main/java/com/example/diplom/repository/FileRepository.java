package com.example.diplom.repository;

import com.example.diplom.model.entity.File;
import com.example.diplom.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileRepository extends JpaRepository<File, Long> {

    File findByFileInfoFilenameAndUser(String fileName, User user);

    List<File> findByUser(User user);
}
