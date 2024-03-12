package fit.g20202.tsarev.DISLabSBManager;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.util.Collection;
import java.util.Collections;

@Configuration
@EnableMongoRepositories(basePackages = "fit.g20202.tsarev.DISLabSBManager.disrepo")
public class MongoConfiguration extends AbstractMongoClientConfiguration {

    private static final String databaseName = "disrepo";

    @Override
    protected String getDatabaseName() {
        return databaseName;
    }

    @Override
    public MongoClient mongoClient() {
        // mongodb://127.0.0.1:27017,127.0.0.1:27018,127.0.0.1:27019/?replicaSet=rs0
        ConnectionString connectionString = new ConnectionString("mongodb://mongodb1:27017,mongodb2:27018,mongodb3:27019/?replicaSet=rs0");
        //ConnectionString connectionString = new ConnectionString("mongodb://mongo1/" + databaseName);
        MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .build();

        return MongoClients.create(mongoClientSettings);
    }

    @Override
    public Collection getMappingBasePackages() {
        return Collections.singleton("fit.g20202.tsarev.DISLabSBManager");//todo what is it
    }

}
