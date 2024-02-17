package fit.g20202.tsarev.DISLabSBManager;

import fit.g20202.tsarev.DISLabSBManager.DTO.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class ManagerController {

    ManagerService service;

    @Autowired
    ManagerController(ManagerService service){
        this.service = service;
    }

    @PostMapping("/api/hash/crack")
    public StartCrackResponseToClientDTO startCrack(
            @RequestBody StartCrackRequestDTO startCrackRequestDTO
    ){
        // response to client: id of the request
        return new StartCrackResponseToClientDTO(
                String.valueOf(service.processRequest(startCrackRequestDTO))
        );
    }

    @PatchMapping("/internal/api/manager/hash/crack/request")
    public ResponseToWorkerDTO takeResultFromWorker(
            @RequestBody ResultFromWorkerDTO resultFromWorker
    ){
        return service.saveResult(resultFromWorker);
    }

    @GetMapping("/api/hash/status")
    public ResultResponseToClientDTO getResult(
            @RequestParam(value="requestId", required = true) String requestId
    ){
        ManagerService.Query query = service.queries.get(Integer.parseInt(requestId));
        if (query == null) {
            return new ResultResponseToClientDTO("ERROR", null);
        } else {
            List<String> result = query.result;
            if (result.isEmpty()) {
                return new ResultResponseToClientDTO("IN_PROGRESS", null);
            } else {
                return new ResultResponseToClientDTO("READY", result);
            }
        }
    }

}
