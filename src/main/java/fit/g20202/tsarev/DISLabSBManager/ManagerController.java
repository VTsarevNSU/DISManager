package fit.g20202.tsarev.DISLabSBManager;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import fit.g20202.tsarev.DISLabSBManager.DTO.*;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

@RestController
//@RabbitListener(queues = "queue1")
//@EnableRabbit
public class ManagerController {

    ManagerService service;

    @Autowired
    AmqpTemplate amqpTemplate;

    /*@Autowired
    Queue queue1;*/

    /*@Autowired
    Queue workerQueue;*/

    @Autowired
    ManagerController(ManagerService service){
        this.service = service;
    }

    @PostMapping("/api/hash/crack")
    public StartCrackResponseToClientDTO startCrack(
            @RequestBody StartCrackRequestDTO startCrackRequestDTO
    ){
        // response to client: id of the request
        /*return new StartCrackResponseToClientDTO(
                String.valueOf(service.processRequest(startCrackRequestDTO))
        );*/

        List<TaskForWorkerDTO> tasks = service.processRequest(startCrackRequestDTO);
        tasks.forEach(task -> {
            amqpTemplate.convertAndSend("exchange", "workerKey", task);
        });
        return new StartCrackResponseToClientDTO(tasks.getFirst().requestId());
    }

    /*@RabbitHandler
    public void onMessage(String message) {
        System.out.println("lmao");
        System.out.println("received from queue1 : " + message);
    }*/

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
