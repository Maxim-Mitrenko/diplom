package com.example.diplom.exeption;

public class FileNotFoundException extends IllegalArgumentException {

    public FileNotFoundException() {
    }

    public FileNotFoundException(String s) {
        super(s);
    }

    public FileNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileNotFoundException(Throwable cause) {
        super(cause);
    }
}
