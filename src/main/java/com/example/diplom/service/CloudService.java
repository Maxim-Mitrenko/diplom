package com.example.diplom.service;

import com.example.diplom.exeption.FileNotFoundException;
import com.example.diplom.model.entity.File;
import com.example.diplom.model.entity.FileInfo;
import com.example.diplom.model.login.LoginRequest;
import com.example.diplom.model.login.LoginResponse;
import com.example.diplom.repository.FileRepository;
import com.example.diplom.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CloudService {

    private final UserRepository userRepository;
    private final FileRepository fileRepository;
    private final AuthenticationManager authenticationManager;

    public CloudService(UserRepository userRepository, FileRepository fileRepository, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.fileRepository = fileRepository;
        this.authenticationManager = authenticationManager;
    }

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getLogin(), request.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        var user = userRepository.findByEmail(userPrincipal.getUsername());
        var token = user.createAuthToken();
        userRepository.save(user);
        log.info("???????????????????????? " + user.getEmail() + " ?????????????? ???????????? ??????????????????????!");
        return new LoginResponse(token);
    }

    public void logout(String authToken) {
        var user = userRepository.findByAuthToken(authToken.split(" ")[1].trim());
        if (user != null) {
            user.setAuthToken(null);
            userRepository.save(user);
            log.info("?????????????????????????????? ?????????? ???????????????????????? " + user.getEmail() + " ??????????????????????!");
        }
    }

    public void upload(String authToken, MultipartFile file, String filename) throws IOException {
        var user = userRepository.findByAuthToken(authToken.split(" ")[1].trim());
        var info = new FileInfo(filename, file.getSize());
        fileRepository.save(new File(file.getBytes(), info, user));
        log.info("???????? " + filename + " ?????????????? ????????????????!");
    }

    public void delete(String authToken, String filename) {
        var user = userRepository.findByAuthToken(authToken.split(" ")[1].trim());
        var file = fileRepository.findByFileInfoFilenameAndUser(filename, user);
        if (file == null) throw new FileNotFoundException("???????? ???? ????????????!");
        fileRepository.delete(file);
        log.info("???????? " + filename + " ?????????????? ????????????!");
    }

    public ResponseEntity<byte[]> get(String authToken, String filename) {
        var user = userRepository.findByAuthToken(authToken.split(" ")[1].trim());
        var file =  fileRepository.findByFileInfoFilenameAndUser(filename, user);
        if (file == null) throw new FileNotFoundException("???????? " + filename + " ???? ????????????!");
        log.info("???????? " + filename + " ?????? ????????????!");
        return ResponseEntity.ok(file.getFile());
    }

    public void editName(String authToken, String name, String filename) {
        var user = userRepository.findByAuthToken(authToken.split(" ")[1].trim());
        var file = fileRepository.findByFileInfoFilenameAndUser(filename, user);
        if (file == null) throw new FileNotFoundException("???????? " + filename + " ???? ????????????!");
        fileRepository.delete(file);
        file.getFileInfo().setFilename(name);
        fileRepository.save(file);
        log.info("?????? ?????????? " + filename + " ???????? ???????????????? ???? " + name);
    }

    public List<FileInfo> list(String authToken, int limit) {
        var user = userRepository.findByAuthToken(authToken.split(" ")[1].trim());
        log.info("?????? ?????????????? ???????????? ???????????? ???????????????????????? " + user.getEmail());
        return fileRepository.findByUser(user)
                .stream()
                .map(File::getFileInfo)
                .limit(limit)
                .collect(Collectors.toList());
    }
}
