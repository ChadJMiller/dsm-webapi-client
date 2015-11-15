package net.jacqg.dsm.webapi.client.filestation.exception;

import net.jacqg.dsm.webapi.client.exception.DsmWebApiErrorException;

public class FileAlreadyExistsException extends DsmWebApiErrorException {

    private final String parentFolderPath;
    private final String fileName;

    public FileAlreadyExistsException(int errorCode, String parentPath, String fileName) {
        super(String.format("File already exists: %s/%s", parentPath, fileName), errorCode);
        this.parentFolderPath = parentPath;
        this.fileName = fileName;
    }

    public String getParentFolderPath() {
        return parentFolderPath;
    }

    public String getFileName() {
        return fileName;
    }
}
