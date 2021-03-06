package com.github.gauthierj.dsm.webapi.client.filestation.upload;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.gauthierj.dsm.webapi.client.DsmWebapiResponse;
import com.github.gauthierj.dsm.webapi.client.authentication.AuthenticationHolder;
import com.github.gauthierj.dsm.webapi.client.filestation.exception.FileAlreadyExistsException;
import com.github.gauthierj.dsm.webapi.client.AbstractDsmServiceImpl;
import com.github.gauthierj.dsm.webapi.client.DsmUrlProvider;
import com.github.gauthierj.dsm.webapi.client.exception.DsmWebApiClientException;
import com.github.gauthierj.dsm.webapi.client.filestation.common.OverwriteBehavior;
import com.github.gauthierj.dsm.webapi.client.timezone.TimeZoneUtil;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Could not succeed to implement this with RestTemplate
 * Implemented with Apache's HttpClient and Jackson's object mapper
 */
@Component
public class UploadServiceImpl extends AbstractDsmServiceImpl implements UploadService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UploadServiceImpl.class);

    private static final String API_ID = "SYNO.FileStation.Upload";

    private static final String DELIMITER = "--AaB03x";
    private static final String CRLF = "\r\n";

    @Autowired
    private DsmUrlProvider dsmUrlProvider;

    @Autowired
    private AuthenticationHolder authenticationHolder;

    @Autowired
    private TimeZoneUtil timeZoneUtil;

    @Autowired
    private ObjectMapper objectMapper;

    public UploadServiceImpl() {
        super(API_ID);
    }

    @Override
    public void uploadFile(String parentPath, String name, byte[] content) {
        uploadFile(UploadRequest.createBuilder(parentPath, name, content).build());
    }

    @Override
    public void uploadFile(UploadRequest uploadRequest) {
        DsmWebapiResponse response = doUploadRequest(uploadRequest);
        if(!response.isSuccess()) {
            switch (response.getError().getCode()) {
                case 1805:
                    throw new FileAlreadyExistsException(response.getError(), uploadRequest.getParentFolderPath(), uploadRequest.getFileName());
                case 414:
                    throw new FileAlreadyExistsException(response.getError(), uploadRequest.getParentFolderPath(), uploadRequest.getFileName());
                    // TODO handle other cases
            }
        }
    }

    private DsmWebapiResponse doUploadRequest(UploadRequest uploadRequest) {
        try {
            String request = createRequest(uploadRequest);
            LOGGER.debug("Upload Request Body: \n{}", request);
            Content content = Request.Post(createUrl())
                    .addHeader("Content-type", String.format("multipart/form-data, boundary=%s", DELIMITER.substring(2)))
                    .bodyByteArray(request.getBytes())
                    .execute().returnContent();
            LOGGER.debug("Response body: {}", content.asString());
            return objectMapper.readValue(content.asBytes(), DsmWebapiResponse.class);
        } catch (IOException e) {
            throw new DsmWebApiClientException("Could not upload file.", e);
        }
    }

    private String createUrl() {
        return UriComponentsBuilder.fromHttpUrl(dsmUrlProvider.getDsmUrl())
                .path("/webapi/FileStation/api_upload.cgi")
                .toUriString();
    }

    private String createRequest(UploadRequest uploadRequest) {
        StringBuilder request = new StringBuilder();
        appendParameter(request, "api", getApiId());
        appendParameter(request, "version", "1");
        appendParameter(request, "method", "upload");
        appendParameter(request, "_sid", authenticationHolder.getLoginInformation().getSid());
        appendParameter(request, "dest_folder_path", uploadRequest.getParentFolderPath());
        appendParameter(request, "create_parents", Boolean.toString(uploadRequest.isCreateParents()));
        appendOverwriteBehaviorIfNeeded(request, uploadRequest.getOverwriteBehavior());
        appendTimeParameterIfNeeded(request, "mtime", uploadRequest.getLastModificationTime());
        appendTimeParameterIfNeeded(request, "crtime", uploadRequest.getCreationTime());
        appendTimeParameterIfNeeded(request, "atime", uploadRequest.getLastAccessTime());
        appendFileParameter(request, uploadRequest.getFileName(), uploadRequest.getContent());
        return request.toString();
    }

    private void appendOverwriteBehaviorIfNeeded(StringBuilder request, OverwriteBehavior overwriteBehavior) {
        switch (overwriteBehavior) {
            case OVERWRITE:
                appendParameter(request, "overwrite", "true");
                break;
            case SKIP:
                appendParameter(request, "overwrite", "true");
                break;
            case ERROR:
                // Default behavior: no parameter to add
                break;
            default:
                throw new AssertionError("Cannot happen");
        }
    }

    private void appendTimeParameterIfNeeded(StringBuilder request, String parameterName, Optional<LocalDateTime> time) {
        if(time.isPresent()) {
            long unixTime = time.get().toEpochSecond(timeZoneUtil.getZoneOffset()) * 1000;
            appendParameter(request, parameterName, Long.toString(unixTime));
        }
    }

    private void appendParameter(StringBuilder request, String parameterName, String parameterValue) {
        request
                .append(DELIMITER)
                .append(CRLF)
                .append(String.format("Content-Disposition: form-data; name=\"%s\"", parameterName))
                .append(CRLF)
                .append(parameterValue)
                .append(CRLF);
    }

    private void appendFileParameter(StringBuilder request, String fileName, byte[] fileContent) {
        request
                .append(DELIMITER)
                .append(CRLF)
                .append(String.format("Content-Disposition: form-data; name=\"file\"; filename=\"%s\"", fileName))
                .append(CRLF)
                .append("Content-Type: application/octet-stream")
                .append(CRLF)
                .append(CRLF)
                .append(new String(fileContent))
                .append(CRLF)
                .append(CRLF)
                .append(DELIMITER)
                .append("--");
    }
}
