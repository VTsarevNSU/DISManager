package fit.g20202.tsarev.DISLabSBManager;

import fit.g20202.tsarev.DISLabSBManager.DTO.*;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Service
@EnableScheduling
public class ManagerService {

    private final AtomicInteger lastRequestId;
    private final LinkedHashMap<Integer, Query> queries;
    public static final int WORKERS_COUNT = 1;//todo 3
    public static final int ALPHABET_SIZE = 36;
    private final Object mutex = new Object();

    ManagerService(){
        queries = new LinkedHashMap<Integer, Query>();
        lastRequestId = new AtomicInteger(0);
    }

    @Scheduled(fixedDelay = 10000)
    public void updateStatus(){

        synchronized (mutex) {
            for (Map.Entry<Integer, Query> integerQueryEntry : queries.entrySet()) {
                var dueDate = integerQueryEntry.getValue().dueDate;
                if (dueDate != null && dueDate.isBefore(LocalDateTime.now())){
                    queries.remove(integerQueryEntry.getKey());
                }
            }
        }

    }

    public List<TaskForWorkerDTO> processRequest(StartCrackRequestDTO startCrackRequestDTO){

        int requestIdATM = lastRequestId.addAndGet(1);

        synchronized (mutex) {
            queries.putLast(requestIdATM, new Query());
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
            ManagerService.Query query = this.queries.get(requestId);
            query.result = Stream.concat(query.result.stream(), resultFromWorker.result().stream()).toList();
            query.dueDate = LocalDateTime.now().plusSeconds(60);
            System.out.println(query.dueDate);
        }

        return new ResponseToWorkerDTO();
    }

    public ResultResponseToClientDTO createResponseToClient(String requestId) {
        synchronized (mutex){
            ManagerService.Query query = queries.get(Integer.parseInt(requestId));
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

    class Query {

        LocalDateTime dueDate;
        List<String> result;

        Query(){
            result = new ArrayList<String>();
        }
    }

}
