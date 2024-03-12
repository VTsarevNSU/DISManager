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

    private final AtomicInteger nextRequestId;
    public static final int WORKERS_COUNT = 3;
    public static final int ALPHABET_SIZE = 36;
    private final Object mutex = new Object();

    ManagerService(){
        nextRequestId = new AtomicInteger(1);
    }

    @Scheduled(fixedDelay = 5000)
    public void updateStatus(){

        synchronized (mutex) {

            Query query = new Query();

            LocalDateTime ldt = LocalDateTime.now();
            DateTimeFormatter dtf = DateTimeFormatter.ISO_DATE_TIME;
            String s = ldt.format(dtf); // "1980-01-01T00:00:00"

            Criteria filterCriteria = Criteria.where("dueDate").lte(s);
            query.addCriteria(filterCriteria);
            //mongoTemplate.findAllAndRemove(query, Request.class);//todo fails
            mongoTemplate.findAndRemove(query, Request.class);

        }

    }

    public List<TaskForWorkerDTO> processRequest(StartCrackRequestDTO startCrackRequestDTO){

        int requestIdATM;

        synchronized (mutex) {
            while (true){
                requestIdATM = nextRequestId.get();

                Request request = mongoTemplate.findOne(
                        Query.query(Criteria.where("requestId").is(requestIdATM)),
                        Request.class
                );
                if (request == null){
                    Request newRequest = new Request(requestIdATM);
                    mongoTemplate.insert(newRequest);
                    break;
                } else {
                    nextRequestId.incrementAndGet();
                }
            }

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
                    String.valueOf(requestIdATM), hash, String.valueOf(startSymbolPos), String.valueOf(endSymbolPos), startCrackRequestDTO.maxLength(),
                    String.valueOf(i)
            );
            tasks.add(newTask);

            mongoTemplate.insert(newTask);

        }

        return tasks;

    }

    ResponseToWorkerDTO saveResult(ResultFromWorkerDTO resultFromWorker){

        System.out.println("Manager got partial task");

        Integer requestId = Integer.parseInt(resultFromWorker.requestId());

        synchronized (mutex) {

            Request request = mongoTemplate.findOne(
                    Query.query(Criteria.where("requestId").is(requestId)),
                    Request.class
            );

            if (request.partsLeft.contains(resultFromWorker.part())){

                request.partsLeft.remove(resultFromWorker.part());

                Query query = new Query();
                query.addCriteria(Criteria.where("requestId").is(requestId));
                Update update = new Update();

                update.set("result", Stream.concat(request.result.stream(), resultFromWorker.result().stream()).toList());
                update.set("partsLeft", request.partsLeft);

                if (request.partsLeft.isEmpty()){
                    update.set("status", "READY");

                    LocalDateTime ldt = LocalDateTime.now().plusSeconds(30);
                    DateTimeFormatter dtf = DateTimeFormatter.ISO_DATE_TIME;
                    String s = ldt.format(dtf); // "1980-01-01T00:00:00"
                    update.set("dueDate", s);

                    System.out.println("Manager got full task");
                }

                mongoTemplate.updateFirst(query, update, Request.class);

                Query queryDelete = new Query();
                query.addCriteria(Criteria.
                        where("requestId").is(requestId).
                        and("part").is(String.valueOf(resultFromWorker.part()))
                );
                mongoTemplate.findAndRemove(queryDelete, TaskForWorkerDTO.class);

            }

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
                String status = request.status;
                if (Objects.equals(status, "IN_PROGRESS")) {
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
        String status;
        Set<Integer> partsLeft;

        Request(Integer requestId){
            this.requestId = requestId;
            result = new ArrayList<String>();
            dueDate = null;
            status = "IN_PROGRESS";
            partsLeft = new HashSet<Integer>();
            for (int i = 1; i <= WORKERS_COUNT; i++){
                partsLeft.add(i);
            }
        }
    }

}
