package eu.nebulous.utilityevaluator.communication.activemq.message;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONPointer;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class FetchNodeCandidatesMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final JSONPointer APPLICATION_ID_POINTER = new JSONPointer("/application/name");
    private static final JSONPointer CLOUD_PROVIDERS_POINTER = new JSONPointer("/cloud_providers");

    @NonNull
    private String applicationID;
    @NonNull
    private Map<String,String> cloudProviders;



    public FetchNodeCandidatesMessage(JSONObject generalApplicationMessage){

        this.applicationID = (@NonNull String) generalApplicationMessage.query(APPLICATION_ID_POINTER);
        JSONArray providersJSON = (JSONArray) generalApplicationMessage.query(CLOUD_PROVIDERS_POINTER);
        Map<String,String> cloudProviders = new HashMap<String,String>();
        
        for (final Object p : providersJSON) {
            JSONObject provider = (JSONObject) p;
            cloudProviders.put(provider.optString("type"), provider.optString("sal_key"));
        }
        this.cloudProviders = cloudProviders;
    }

    public FetchNodeCandidatesMessage(String appId, Map body, Map<String, Object> map){
        this.applicationID = appId;
        

    }
}
