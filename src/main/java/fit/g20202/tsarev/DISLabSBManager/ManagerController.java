package fit.g20202.tsarev.DISLabSBManager;

import fit.g20202.tsarev.DISLabSBManager.DTO.*;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class ManagerController {

    ManagerService service;

    @Autowired
    AmqpTemplate amqpTemplate;

    @Autowired
    ManagerController(ManagerService service){
        this.service = service;
    }

    @PostMapping("/api/hash/crack")
    public StartCrackResponseToClientDTO startCrack(
            @RequestBody StartCrackRequestDTO startCrackRequestDTO
    ){

        List<TaskForWorkerDTO> tasks = service.processRequest(startCrackRequestDTO);

        for (int i = 1; i <= ManagerService.WORKERS_COUNT; i++){
            amqpTemplate.convertAndSend("exchange", "workerKey", tasks.get(i - 1));
        }

        // response to client: id of the request after saving to DB
        return new StartCrackResponseToClientDTO(tasks.getFirst().requestId());
    }

    @PatchMapping("/internal/api/manager/hash/crack/request")
    public ResponseToWorkerDTO takeResultFromWorker(
            @RequestBody ResultFromWorkerDTO resultFromWorker
    ){
        return service.saveResult(resultFromWorker);
    }

    @RabbitListener(queues = "manager_queue")
    public void getResult(
            ResultFromWorkerDTO resultFromWorker
    ) {
        service.saveResult(resultFromWorker);
    }

    @GetMapping("/api/hash/status")
    public ResultResponseToClientDTO getResult(
            @RequestParam(value="requestId", required = true) String requestId
    ){
        return service.createResponseToClient(requestId);
    }

}
