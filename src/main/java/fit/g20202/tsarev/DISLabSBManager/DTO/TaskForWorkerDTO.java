package fit.g20202.tsarev.DISLabSBManager.DTO;

import java.io.Serializable;

public record TaskForWorkerDTO(String requestId, String hash, String firstSymbolPos, String lastSymbolPos, String maxLength, String part) implements Serializable {
}
