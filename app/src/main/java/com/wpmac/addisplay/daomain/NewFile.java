package com.wpmac.addisplay.daomain;

import org.xutils.db.annotation.Column;
import org.xutils.db.annotation.Table;

/**
 * Created by wpmac on 16/6/8.
 */
@Table(name="mp4file_info")
public class NewFile {
    @Column(name="id",isId = true,autoGen = true,property = "")
    private int id;
    @Column(name = "file_Name")
    private String fileName;
    @Column(name = "file_Size")
    private long fileSize;
    @Column(name = "file_Path")
    private String filePath;

    public NewFile(String fileName, long fileSize, String filePath) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.filePath = filePath;
    }

    public NewFile() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public String toString() {
        return "NewFile{" +
                "fileName='" + fileName + '\'' +
                ", fileSize=" + fileSize +'\'' +
                ", filePath=" + filePath +
                '}';
    }
}
