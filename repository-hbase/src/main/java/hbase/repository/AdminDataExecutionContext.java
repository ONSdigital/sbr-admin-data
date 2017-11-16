package hbase.repository;

import akka.actor.ActorSystem;
import scala.concurrent.ExecutionContext;
import scala.concurrent.ExecutionContextExecutor;

import javax.inject.Inject;

/**
 * Custom execution context wired to "admin-data.repository" thread pool
 */
public class AdminDataExecutionContext implements ExecutionContextExecutor {

    private final ExecutionContext executionContext;
    public static final String NAME = "admin-data-repository";

    @Inject
    public AdminDataExecutionContext(ActorSystem actorSystem) {
        this.executionContext = actorSystem.dispatchers().lookup(NAME);
    }

    @Override
    public void execute(Runnable runnable) {
        executionContext.execute(runnable);
    }

    @Override
    public void reportFailure(Throwable cause) {
        executionContext.reportFailure(cause);
    }

    @Override
    public ExecutionContext prepare() {
        return executionContext.prepare();
    }
}