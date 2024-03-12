package fit.g20202.tsarev.DISLabSBManager.DTO;

import java.util.List;

public record ResultFromWorkerDTO(String requestId, List<String> result, Integer part) {
}
