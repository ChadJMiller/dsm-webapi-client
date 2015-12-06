package net.jacqg.dsm.webapi.client.filestation.info;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.jacqg.dsm.webapi.client.AbstractDsmServiceImpl;
import net.jacqg.dsm.webapi.client.DsmWebApiResponseError;
import net.jacqg.dsm.webapi.client.DsmWebapiRequest;
import net.jacqg.dsm.webapi.client.DsmWebapiResponse;
import org.springframework.stereotype.Service;

@Service
public class FileStationInformationServiceImpl extends AbstractDsmServiceImpl implements FileStationInformationService {

    public FileStationInformationServiceImpl() {
        super("SYNO.FileStation.Info");
    }

    @Override
    public FileStationInformation getFileStationInformation() {
        FileStationInformationResponse response = getDsmWebapiClient().call(new DsmWebapiRequest(getApiInfo().getApi(), "1", getApiInfo().getPath(), "getinfo"), FileStationInformationResponse.class);
        return response.getData();
    }

    public static class FileStationInformationResponse extends DsmWebapiResponse<FileStationInformation> {

        @JsonCreator
        public FileStationInformationResponse(@JsonProperty("success") boolean success,
                                              @JsonProperty("data") FileStationInformation data,
                                              @JsonProperty("error") DsmWebApiResponseError error) {
            super(success, data, error);
        }
    }
}
