package repository;

import model.AdminData;

import java.time.YearMonth;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Definition of an Admin Data Repository
 */
public interface AdminDataRepository {

    CompletionStage<YearMonth> getCurrentPeriod();

    CompletionStage<Optional<AdminData>> lookup(YearMonth referencePeriod, String key);

}
