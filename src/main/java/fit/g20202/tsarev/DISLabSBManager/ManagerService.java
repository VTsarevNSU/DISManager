package fit.g20202.tsarev.DISLabSBManager;

import fit.g20202.tsarev.DISLabSBManager.DTO.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.sql.Date;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Service
@EnableScheduling
public class ManagerService {

    @Autowired
    private MongoTemplate mongoTemplate;

    private final AtomicInteger lastRequestId;
    public static final int WORKERS_COUNT = 3;//todo 3
    public static final int ALPHABET_SIZE = 36;
    private final Object mutex = new Object();

    ManagerService(){
        lastRequestId = new AtomicInteger(0);
    }

    @Scheduled(fixedDelay = 500000)//todo
    public void updateStatus(){

        synchronized (mutex) {

            Query query = new Query();

            LocalDateTime ldt = LocalDateTime.now();
            DateTimeFormatter dtf = DateTimeFormatter.ISO_DATE_TIME;
            String s = ldt.format(dtf); // "1980-01-01T00:00:00"

            Criteria filterCriteria = Criteria.where("dueDate").lte(s);
            query.addCriteria(filterCriteria);
            mongoTemplate.findAllAndRemove(query, Request.class);//todo fails

        }

    }

    public List<TaskForWorkerDTO> processRequest(StartCrackRequestDTO startCrackRequestDTO){

        int requestIdATM = lastRequestId.addAndGet(1);

        synchronized (mutex) {
            Request newRequest = new Request(requestIdATM);
            mongoTemplate.insert(newRequest);
            Request request = mongoTemplate.findOne(
                    Query.query(Criteria.where("requestId").is(requestIdATM)),
                    Request.class
            );
        }

        String hash = startCrackRequestDTO.hash();
        String maxLength = startCrackRequestDTO.maxLength();

        // send requests to the workers
        RestTemplate restTemplate = new RestTemplate();

        int[] symbols = new int[WORKERS_COUNT];
        int remainder = ALPHABET_SIZE - (ALPHABET_SIZE / WORKERS_COUNT) * WORKERS_COUNT;
        for (int i = 0; i < WORKERS_COUNT; i++){
            if (remainder > 0){
                symbols[i] = ALPHABET_SIZE / WORKERS_COUNT + 1;
            } else {
                symbols[i] = ALPHABET_SIZE / WORKERS_COUNT;
                remainder--;
            }
        }

        int startSymbolPos;
        int endSymbolPos = -1;

        List<TaskForWorkerDTO> tasks = new ArrayList<>();

        for (int i = 1; i <= WORKERS_COUNT; i++){

            startSymbolPos = endSymbolPos + 1;
            endSymbolPos = startSymbolPos + symbols[i - 1] - 1;

            System.out.println(startSymbolPos);
            System.out.println(endSymbolPos);
            System.out.println();

            TaskForWorkerDTO newTask = new TaskForWorkerDTO(
                    String.valueOf(requestIdATM), hash, String.valueOf(startSymbolPos), String.valueOf(endSymbolPos), startCrackRequestDTO.maxLength()
            );
            tasks.add(newTask);

        }

        return tasks;
    }

    ResponseToWorkerDTO saveResult(ResultFromWorkerDTO resultFromWorker){
        Integer requestId = Integer.parseInt(resultFromWorker.requestId());
        synchronized (mutex) {

            Request request = mongoTemplate.findOne(
                    Query.query(Criteria.where("requestId").is(requestId)),
                    Request.class
            );

            Query query = new Query();
            query.addCriteria(Criteria.where("requestId").is(requestId));
            Update update = new Update();
            update.set("result", Stream.concat(request.result.stream(), resultFromWorker.result().stream()).toList());

            //update.set("dueDate", LocalDateTime.now().plusSeconds(60));
            LocalDateTime ldt = LocalDateTime.now().plusSeconds(30);
            DateTimeFormatter dtf = DateTimeFormatter.ISO_DATE_TIME;
            String s = ldt.format(dtf); // "1980-01-01T00:00:00"
            update.set("dueDate", s);

            mongoTemplate.updateFirst(query, update, Request.class);

            //System.out.println(request.dueDate);

        }

        return new ResponseToWorkerDTO();
    }

    public ResultResponseToClientDTO createResponseToClient(String requestId) {
        synchronized (mutex){
            Request request = mongoTemplate.findOne(
                    Query.query(Criteria.where("requestId").is(Integer.parseInt(requestId))),
                    Request.class
            );

            if (request == null) {
                return new ResultResponseToClientDTO("ERROR", null);
            } else {
                List<String> result = request.result;
                if (result.isEmpty()) {
                    return new ResultResponseToClientDTO("IN_PROGRESS", null);
                } else {
                    return new ResultResponseToClientDTO("READY", result);
                }
            }
        }
    }

    class Request {
        Integer requestId;
        List<String> result;
        LocalDateTime dueDate;

        Request(Integer requestId){
            this.requestId = requestId;
            result = new ArrayList<String>();
            dueDate = null;
        }
    }

}
