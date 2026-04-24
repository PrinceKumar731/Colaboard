package com.codejudge.model.enums;

public enum Language {

    JAVA("Solution.java"),
    PYTHON("solution.py"),
    CPP("solution.cpp");

    private final String fileName;

    Language(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }
}
