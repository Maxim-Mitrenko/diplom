package com.example.diplom.controller;

import com.example.diplom.model.entity.FileInfo;
import com.example.diplom.model.login.LoginRequest;
import com.example.diplom.model.login.LoginResponse;
import com.example.diplom.service.CloudService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
public class CloudController {

    private final CloudService service;

    public CloudController(CloudService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return service.login(request);
    }

    @GetMapping("/login")
    public void logout(@RequestHeader("auth-token") String authToken) {
        service.logout(authToken);
    }

    @PostMapping("/file")
    public void upload(@RequestHeader("auth-token") String authToken, @RequestPart MultipartFile file, @RequestParam String filename) throws IOException {
        service.upload(authToken, file, filename);
    }

    @DeleteMapping("/file")
    public void delete(@RequestHeader("auth-token") String authToken, @RequestParam String filename) {
        service.delete(authToken, filename);
    }

    @GetMapping("/file")
    public ResponseEntity<byte[]> get(@RequestHeader("auth-token") String authToken, @RequestParam String filename) {
        return service.get(authToken, filename);
    }

    @PutMapping("/file")
    public void editName(@RequestHeader("auth-token") String authToken, @RequestBody String name, @RequestParam String filename) {
        service.editName(authToken, name.split(":")[1].replace("}", "").replace("\"", "").trim(), filename);
    }

    @GetMapping("/list")
    public List<FileInfo> list(@RequestHeader("auth-token") String authToken, @RequestParam int limit) {
        return service.list(authToken, limit);
    }
}
