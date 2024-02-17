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

    AtomicInteger lastRequestId;
    LinkedHashMap<Integer, Query> queries;
    static final int WORKERS_COUNT = 3;
    static final int ALPHABET_SIZE = 36;
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

    public Integer processRequest(StartCrackRequestDTO startCrackRequestDTO){

        int requestIdATM = lastRequestId.addAndGet(1);

        queries.putLast(requestIdATM, new Query());

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

        for (int i = 1; i <= WORKERS_COUNT; i++){

            startSymbolPos = endSymbolPos + 1;
            endSymbolPos = startSymbolPos + symbols[i - 1] - 1;

            System.out.println(startSymbolPos);
            System.out.println(endSymbolPos);
            System.out.println();

            HttpEntity<TaskForWorkerDTO> request = new HttpEntity<>(new TaskForWorkerDTO(
                    String.valueOf(requestIdATM), hash, String.valueOf(startSymbolPos), String.valueOf(endSymbolPos), startCrackRequestDTO.maxLength()
            ));

            ResponseEntity<ResponseFromWorkerDTO> response =
                    restTemplate.postForEntity(
                            //"http://localhost:8081/internal/api/worker/hash/crack/task",
                            "http://worker" + i + ":8080/internal/api/worker/hash/crack/task",
                            request,
                            ResponseFromWorkerDTO.class);

            if (response.getStatusCode().is2xxSuccessful()){
                System.out.println("Worker " + i + " got the task");
            }

        }

        return requestIdATM;
    }

    ResponseToWorkerDTO saveResult(ResultFromWorkerDTO resultFromWorker){
        Integer requestId = Integer.parseInt(resultFromWorker.requestId());
        ManagerService.Query query = this.queries.get(requestId);
        // thread-safe addition of new results to already existing ones
        synchronized (mutex) {
            query.result = Stream.concat(query.result.stream(), resultFromWorker.result().stream()).toList();
        }

        query.dueDate = LocalDateTime.now().plusSeconds(60);
        System.out.println(query.dueDate.toString());
        return new ResponseToWorkerDTO();
    }

    class Query {

        LocalDateTime dueDate;
        List<String> result;

        Query(){
            result = new ArrayList<String>();
        }
    }

}
