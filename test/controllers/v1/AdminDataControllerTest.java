package controllers.v1;

import model.AdminData;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import play.cache.CacheApi;
import play.cache.DefaultCacheApi;
import play.i18n.Messages;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Http;
import play.mvc.Result;
import repository.AdminDataRepository;

import java.time.YearMonth;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static play.mvc.Http.Status.*;

@RunWith(MockitoJUnitRunner.class)
public class AdminDataControllerTest {

    private static final YearMonth JUNE_2017 = YearMonth.of(2017, 06);
    private static Cache testCache;

    @Mock
    private AdminDataRepository mockAdminDataRepository;
    @Mock
    private Messages messages;
    private AdminDataController controller;
    private CacheApi cacheApi;

    @BeforeClass
    public static void init() {
        CacheManager singletonManager = CacheManager.create();
        singletonManager.addCache("testCache");
        testCache = singletonManager.getCache("testCache");
    }

    @Before
    public void setup() {
        cacheApi = new DefaultCacheApi(new TestCache(testCache));
        Http.Context mockContext = mock(Http.Context.class);
        Http.Context.current.set(mockContext);
        when(mockContext.messages()).thenReturn(messages);
        when(messages.at(anyString(), any())).thenReturn("Mock error message");
        controller = new AdminDataController(new HttpExecutionContext(ForkJoinPool.commonPool()), mockAdminDataRepository, cacheApi);
        when(mockAdminDataRepository.lookup(any(), anyString())).thenReturn(CompletableFuture.completedFuture(Optional.of(new AdminData(JUNE_2017, "12345"))));
    }

    @Test
    public void lookup() throws ExecutionException, InterruptedException {
        CompletionStage<Result> stage = controller.lookup("201706", "12345");
        assertResult(stage, OK);
    }

    @Test
    public void lookupWasCached() throws ExecutionException, InterruptedException {
        String cacheKey = AdminDataController.createCacheKey(YearMonth.of(2018, 05), "12345");
        assertNull("Response should not be in the cache", cacheApi.get(cacheKey));
        controller.lookup("201805", "12345");
        assertNotNull("Response should be in the cache", cacheApi.get(cacheKey));
    }

    @Test
    public void lookupNullPeriod() throws ExecutionException, InterruptedException {
        when(mockAdminDataRepository.getCurrentPeriod()).thenReturn(CompletableFuture.completedFuture(JUNE_2017));
        CompletionStage<Result> stage = controller.lookup(null, "12345");
        assertResult(stage, OK);
    }

    @Test
    public void lookupNotFound() throws ExecutionException, InterruptedException {
        when(mockAdminDataRepository.lookup(any(), anyString())).thenReturn(CompletableFuture.completedFuture(Optional.empty()));
        assertResult(controller.lookup("201706", "54321"), NOT_FOUND);
    }

/*    @Test
    public void lookupRuntimeException() throws ExecutionException, InterruptedException {
        when(mockAdminDataRepository.lookup(any(), any(), anyString())).thenThrow(new RuntimeException());
        assertResult(controller.lookup(AdminDataType.VAT.toString(), "201706", "54321"), NOT_FOUND);
    }*/

    @Test
    public void lookupNullId() throws ExecutionException, InterruptedException {
        assertResult(controller.lookup("201706", null), BAD_REQUEST);
    }

    @Test
    public void lookupBadPeriod() throws ExecutionException, InterruptedException {
        assertResult(controller.lookup("2017", "12345"), BAD_REQUEST);
    }

    @Test
    public void lookupBadAdminDataType() throws ExecutionException, InterruptedException {
        assertResult(controller.lookup("XXX","12345"), BAD_REQUEST);
    }

    private void assertResult(CompletionStage<Result> stage, int expectedStatus) throws ExecutionException, InterruptedException {
        Result result = stage.toCompletableFuture().get();
        assertEquals("Unexpected HTTP status", expectedStatus, result.status());
        if (expectedStatus != NOT_FOUND) {
            assertEquals("Unexpected HTTP content type", "application/json", result.contentType().get());
            assertEquals("Unexpected HTTP character set", "UTF-8", result.charset().get());
        }
    }

}
