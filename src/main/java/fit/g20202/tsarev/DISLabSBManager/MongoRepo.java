package fit.g20202.tsarev.DISLabSBManager;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface MongoRepo extends MongoRepository<ManagerService.Request, String> {
}
