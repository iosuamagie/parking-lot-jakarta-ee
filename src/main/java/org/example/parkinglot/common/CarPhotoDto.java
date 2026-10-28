package org.example.parkinglot.common;

public class CarPhotoDto {
    private Long id;
    private String filename;
    private String fileType;
    private byte[] fileContent;

    // No-argument constructor
    public CarPhotoDto() {
    }

    // Constructor with all fields
    public CarPhotoDto(Long id, String filename, String fileType, byte[] fileContent) {
        this.id = id;
        this.filename = filename;
        this.fileType = fileType;
        this.fileContent = fileContent;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getFilename() {
        return filename;
    }

    public String getFileType() {
        return fileType;
    }

    public byte[] getFileContent() {
        return fileContent;
    }
}