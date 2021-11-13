/*
 * _________  ____ ______________.___.____       _________
 * \_   ___ \|    |   \__    ___/|   |    |     /   _____/
 * /    \  \/|    |   / |    |   |   |    |     \_____  \
 * \     \___|    |  /  |    |   |   |    |___  /        \
 *  \______  /______/   |____|   |___|_______ \/_______  /
 *         \/                                \/        \/
 *
 * Copyright (C) 2018 — 2021 Bobai Kato. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package art.cutils.value;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import art.cutils.function.Accepter;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Syndicate simplifies and represent a specific operation of the Executor Service, InvokeAll. Use
 * this class when you need to invoke a group of processes in parallel and expect some results in
 * the future.
 *
 * <p>Ideally, use Syndicated within the {@code try}-with-resources statement, to ensure the current
 * Executor Service is shut down when all operations are completed. Otherwise, the close method has
 * to be called manually to shutdown.
 *
 * <p>try(final Syndicate<Integer> syndicate = Syndicate.init()){
 *
 * <p>} catch (Exception e) {
 *
 * <p>}
 *
 * @param <T> the type of the values from the tasks.
 * @author Bobai Kato {https://github.com/B0BAI}
 * @since 1.0
 */
public final class Syndicate<T> implements AutoCloseable {

  /**
   * An Executor that provides methods to manage termination and methods that can produce a Future
   * for tracking progress of one or more asynchronous tasks.
   */
  private final ExecutorService executorService;

  /** This list of task to be executed. */
  private final Collection<Callable<T>> callableTaskList = new ArrayList<>();

  /**
   * Creates a new instance of {@link Syndicate} with your preferred instance of {@link
   * ExecutorService} to power the Syndicate ops.
   *
   * @param executorService instance of {@link ExecutorService}
   * @param <T> the type of the values from the tasks
   * @return new instance of {@link Syndicate}
   */
  public static <T> Syndicate<T> init(final ExecutorService executorService) {
    return new Syndicate<>(executorService);
  }

  /**
   * Creates a new instance of {@link Syndicate} with default {@link
   * Executors#newCachedThreadPool()}
   *
   * @param <T> the type of the values from the tasks
   * @return new instance of {@link Syndicate}
   */
  public static <T> Syndicate<T> init() {
    return new Syndicate<>();
  }

  // Sealed constructor
  private Syndicate() {
    this.executorService = Executors.newCachedThreadPool();
  }

  // Sealed constructor
  private Syndicate(final ExecutorService executorService) {
    this.executorService = executorService;
  }

  /**
   * Use to add a task of Callable type
   *
   * @param callableTask instance of a {@link Callable}, the task to be executed as part of the
   *     Syndicate
   * @return existing instance of {@link Syndicate}
   */
  public Syndicate<T> add(Callable<T> callableTask) {
    this.callableTaskList.add(callableTask);
    return this;
  }

  /**
   * Created the {@link Conductor} to initiate the processing of task in the Syndicate
   *
   * @return new instance of a {@link Conductor}
   */
  public Conductor<T> execute() {
    return new Conductor<>(this.executorService, this.callableTaskList);
  }

  /**
   * Shuts down {@link ExecutorService}, relinquishing any underlying resources. This method is
   * invoked automatically on objects managed by the {@code try}-with-resources statement.
   *
   * @throws Exception if this resource cannot be closed
   * @apiNote While this interface method is declared to throw {@code Exception}, implementers are
   *     <em>strongly</em> encouraged to declare concrete implementations of the {@code close}
   *     method to throw more specific exceptions, or to throw no exception at all if the close
   *     operation cannot fail.
   *     <p>Cases where the close operation may fail require careful attention by implementers. It
   *     is strongly advised to relinquish the underlying resources and to internally <em>mark</em>
   *     the resource as closed, prior to throwing the exception. The {@code close} method is
   *     unlikely to be invoked more than once and so this ensures that the resources are released
   *     in a timely manner. Furthermore it reduces problems that could arise when the resource
   *     wraps, or is wrapped, by another resource.
   *     <p><em>Implementers of this interface are also strongly advised to not have the {@code
   *     close} method throw {@link InterruptedException}.</em>
   *     <p>This exception interacts with a thread's interrupted status, and runtime misbehavior is
   *     likely to occur if an {@code InterruptedException} is {@linkplain Throwable#addSuppressed
   *     suppressed}.
   *     <p>More generally, if it would cause problems for an exception to be suppressed, the {@code
   *     AutoCloseable.close} method should not throw it.
   *     <p>Note that unlike the {@link Closeable#close close} method of {@link Closeable}, this
   *     {@code close} method is <em>not</em> required to be idempotent. In other words, calling
   *     this {@code close} method more than once may have some visible side effect, unlike {@code
   *     Closeable.close} which is required to have no effect if called more than once.
   *     <p>However, implementers of this interface are strongly encouraged to make their {@code
   *     close} methods idempotent.
   */
  @Override
  public void close() throws Exception {
    this.executorService.shutdown();
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }

    if (o instanceof Syndicate) {
      final Syndicate<?> syndicate = (Syndicate<?>) o;
      return new EqualsBuilder()
          .append(executorService, syndicate.executorService)
          .append(callableTaskList, syndicate.callableTaskList)
          .isEquals();
    }
    return false;
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
        .append(executorService)
        .append(callableTaskList)
        .toHashCode();
  }

  @Override
  public String toString() {
    return "Syndicate{"
        + "executorService="
        + executorService
        + ", callableTaskList="
        + callableTaskList
        + '}';
  }

  /**
   * Represents the actor to process task within the syndicates
   *
   * @param <T> the type of the values from the tasks
   */
  public static final class Conductor<T> {

    /**
     * An Executor that provides methods to manage termination and methods that can produce a Future
     * for tracking progress of one or more asynchronous tasks.
     */
    private final ExecutorService executorService;

    /** This list of task to be executed. */
    private final Collection<? extends Callable<T>> taskList;

    /** timeout the maximum time to wait */
    private long timeout = 0L;

    /** unit the time unit of the timeout argument */
    private TimeUnit unit;

    // Sealed Constructor
    private Conductor(
        final ExecutorService executorService, final Collection<? extends Callable<T>> taskList) {
      this.executorService = executorService;
      this.taskList = taskList;
    }

    /**
     * Use to set processing timeout.
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return existing instance of a {@link Conductor}
     */
    public Conductor<T> setTimeOut(final long timeout, final TimeUnit unit) {
      this.timeout = timeout;
      this.unit = unit;
      return this;
    }

    /**
     * Executes the given tasks, passes a list of Futures holding their status and results when all
     * complete to {@link Accepter}. Future.isDone is true for each element of the returned list.
     * Note that a completed task could have terminated either normally or by throwing an exception.
     * The results of this method are undefined if the given collection is modified while this
     * operation is in progress.
     *
     * @param futureResults instance of {@link Accepter} to take the list of Futures hold the
     *     results.
     * @throws Exception a list of exceptions could be thrown
     *     <p>InterruptedException – if interrupted while waiting, in which case unfinished tasks
     *     are cancelled.
     *     <p>NullPointerException – if tasks, any of its elements, or unit are null
     *     <p>RejectedExecutionException – if any task cannot be scheduled for execution
     */
    public void onComplete(final Accepter<List<Future<T>>> futureResults) throws Exception {
      if (this.timeout > 0L && Objects.nonNull(this.unit)) {
        futureResults.accept(this.executorService.invokeAll(taskList, this.timeout, this.unit));
      } else {
        futureResults.accept(this.executorService.invokeAll(taskList));
      }
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }

      if (o instanceof Conductor) {
        final Conductor<?> conductor = (Conductor<?>) o;

        return new EqualsBuilder()
            .append(timeout, conductor.timeout)
            .append(executorService, conductor.executorService)
            .append(taskList, conductor.taskList)
            .append(unit, conductor.unit)
            .isEquals();
      }
      return false;
    }

    @Override
    public int hashCode() {
      return new HashCodeBuilder(17, 37)
          .append(executorService)
          .append(taskList)
          .append(timeout)
          .append(unit)
          .toHashCode();
    }

    @Override
    public String toString() {
      return "Conductor{"
          + "executorService="
          + executorService
          + ", taskList="
          + taskList
          + ", timeout="
          + timeout
          + ", unit="
          + unit
          + '}';
    }
  }
}