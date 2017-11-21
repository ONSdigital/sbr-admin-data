package controllers.v1;

import com.google.common.base.Strings;
import model.AdminData;
import play.cache.CacheApi;
import play.i18n.Messages;
import play.libs.Json;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.With;
import repository.AdminDataRepository;

import javax.inject.Inject;
import java.time.DateTimeException;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

/**
 * Provides read only lookups to admin data
 */
@With(AdminDataAction.class)
public class AdminDataController extends Controller {

    private static final String CACHE_DELIMITER = "~";
    private static final int CACHE_EXPIRY_SECONDS = 60 * 10;
    private static final DateTimeFormatter YEAR_MONTH_FORMAT = DateTimeFormatter.ofPattern(AdminData.REFERENCE_PERIOD_FORMAT());
    private HttpExecutionContext ec;
    private AdminDataRepository repository;
    private CacheApi cache;

    @Inject
    public AdminDataController(HttpExecutionContext ec, AdminDataRepository repository, CacheApi cache) {
        this.ec = ec;
        this.repository = repository;
        this.cache = cache;
    }

    private YearMonth getCurrentPeriod() {
        //TODO: Implement caching and handle errors retrieving the current period
        try {
            return repository.getCurrentPeriod().toCompletableFuture().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public CompletionStage<Result> lookup(String referencePeriodStr, String id) {
        Messages messages = Http.Context.current().messages();

        // Validate id
        if (Strings.isNullOrEmpty(id))
            return CompletableFuture.completedFuture(badRequest(Json.toJson(messages.at("controller.invalid.id"))));

        // Validate period is a valid year and month
        YearMonth referencePeriod;
        if (Strings.isNullOrEmpty(referencePeriodStr)) {
            referencePeriod = getCurrentPeriod();
        } else {
            try {
                referencePeriod = YearMonth.parse(referencePeriodStr, YEAR_MONTH_FORMAT);
            } catch (DateTimeException e) {
                return CompletableFuture.completedFuture(badRequest(Json.toJson(messages.at("controller.invalid.period", AdminData.REFERENCE_PERIOD_FORMAT()))));
            }
        }

        // If key if in the cache then return the cached response else lookup the record and add it to the cache
        String cacheKey = createCacheKey(referencePeriod, id);
        return cache.getOrElse(cacheKey, () -> repository.lookup(referencePeriod, id).thenApplyAsync(optionalResource -> {
            return optionalResource.map(adminData ->
                    ok(Json.toJson(adminData))
            ).orElseGet(() ->
                    notFound()
            );
        }, ec.current()), CACHE_EXPIRY_SECONDS);
    }

    static String createCacheKey(YearMonth referencePeriod, String id) {
        return String.join(CACHE_DELIMITER, referencePeriod.toString(), id);
    }

}
