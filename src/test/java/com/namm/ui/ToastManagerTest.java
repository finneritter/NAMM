package com.namm.ui;

import com.namm.ui.ToastManager.Category;
import com.namm.ui.ToastManager.ToastType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ToastManager queue logic.
 *
 * Since render() depends on NammConfig and GuiGraphics (Minecraft runtime),
 * these tests focus on the thread-safe pending queue behavior via post(),
 * the ToastType/Category enums, and concurrency guarantees.
 */
class ToastManagerTest {

    private ToastManager manager;

    @BeforeEach
    void setUp() throws Exception {
        // Create a fresh instance for each test (bypass singleton)
        var constructor = ToastManager.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        manager = constructor.newInstance();
    }

    // --- Singleton ---

    @Test
    void get_returnsSameInstance() {
        assertSame(ToastManager.get(), ToastManager.get());
    }

    // --- post() adds to pending queue ---

    @Test
    void post_addsEventToPendingQueue() throws Exception {
        manager.post("Hello", ToastType.SUCCESS, Category.MACRO_TOGGLED);

        ConcurrentLinkedQueue<?> queue = getPendingQueue();
        assertEquals(1, queue.size());
    }

    @Test
    void post_multipleEvents_allQueued() throws Exception {
        manager.post("First", ToastType.SUCCESS, Category.MACRO_TOGGLED);
        manager.post("Second", ToastType.ERROR, Category.ERROR);
        manager.post("Third", ToastType.INFO, Category.CHAT_COMMAND);

        ConcurrentLinkedQueue<?> queue = getPendingQueue();
        assertEquals(3, queue.size());
    }

    @Test
    void post_preservesMessageInEvent() throws Exception {
        manager.post("Test message", ToastType.INFO, Category.CHAT_COMMAND);

        ConcurrentLinkedQueue<?> queue = getPendingQueue();
        Object event = queue.peek();
        assertNotNull(event);

        // Access the record's message field
        Field messageField = event.getClass().getDeclaredField("message");
        messageField.setAccessible(true);
        assertEquals("Test message", messageField.get(event));
    }

    @Test
    void post_preservesTypeInEvent() throws Exception {
        manager.post("Test", ToastType.ERROR, Category.ERROR);

        ConcurrentLinkedQueue<?> queue = getPendingQueue();
        Object event = queue.peek();
        assertNotNull(event);

        Field typeField = event.getClass().getDeclaredField("type");
        typeField.setAccessible(true);
        assertEquals(ToastType.ERROR, typeField.get(event));
    }

    @Test
    void post_preservesCategoryInEvent() throws Exception {
        manager.post("Test", ToastType.SUCCESS, Category.PROFILE_SWITCHED);

        ConcurrentLinkedQueue<?> queue = getPendingQueue();
        Object event = queue.peek();
        assertNotNull(event);

        Field categoryField = event.getClass().getDeclaredField("category");
        categoryField.setAccessible(true);
        assertEquals(Category.PROFILE_SWITCHED, categoryField.get(event));
    }

    // --- Thread safety of post() ---

    @Test
    void post_concurrentPosts_allEventsQueued() throws Exception {
        int threadCount = 10;
        int postsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < postsPerThread; i++) {
                        manager.post(
                            "Thread-" + threadId + "-" + i,
                            ToastType.INFO,
                            Category.MACRO_TOGGLED
                        );
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // release all threads at once
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "All threads should complete");
        executor.shutdown();

        ConcurrentLinkedQueue<?> queue = getPendingQueue();
        assertEquals(threadCount * postsPerThread, queue.size(),
            "All concurrent posts should be present in the queue");
    }

    @Test
    void post_concurrentPosts_noEventsLost() throws Exception {
        int totalPosts = 500;
        ExecutorService executor = Executors.newFixedThreadPool(8);
        List<String> expectedMessages = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < totalPosts; i++) {
            final String msg = "msg-" + i;
            expectedMessages.add(msg);
            executor.submit(() -> manager.post(msg, ToastType.SUCCESS, Category.MACRO_TOGGLED));
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        ConcurrentLinkedQueue<?> queue = getPendingQueue();
        assertEquals(totalPosts, queue.size(), "No events should be lost during concurrent posting");
    }

    // --- Queue ordering ---

    @Test
    void post_maintainsFifoOrderFromSingleThread() throws Exception {
        manager.post("first", ToastType.INFO, Category.CHAT_COMMAND);
        manager.post("second", ToastType.SUCCESS, Category.MACRO_TOGGLED);
        manager.post("third", ToastType.ERROR, Category.ERROR);

        ConcurrentLinkedQueue<?> queue = getPendingQueue();
        List<String> messages = new ArrayList<>();
        for (Object event : queue) {
            Field messageField = event.getClass().getDeclaredField("message");
            messageField.setAccessible(true);
            messages.add((String) messageField.get(event));
        }

        assertEquals(List.of("first", "second", "third"), messages);
    }

    // --- Empty queue ---

    @Test
    void pendingQueue_initiallyEmpty() throws Exception {
        ConcurrentLinkedQueue<?> queue = getPendingQueue();
        assertTrue(queue.isEmpty());
    }

    @Test
    void activeToasts_initiallyEmpty() throws Exception {
        Field activeField = ToastManager.class.getDeclaredField("activeToasts");
        activeField.setAccessible(true);
        List<?> active = (List<?>) activeField.get(manager);
        assertTrue(active.isEmpty());
    }

    // --- ToastType enum ---

    @Test
    void toastType_hasExactlyThreeValues() {
        assertEquals(3, ToastType.values().length);
    }

    @Test
    void toastType_containsExpectedValues() {
        assertAll(
            () -> assertNotNull(ToastType.valueOf("SUCCESS")),
            () -> assertNotNull(ToastType.valueOf("ERROR")),
            () -> assertNotNull(ToastType.valueOf("INFO"))
        );
    }

    // --- Category enum ---

    @Test
    void category_hasExactlyFiveValues() {
        assertEquals(5, Category.values().length);
    }

    @Test
    void category_containsExpectedValues() {
        assertAll(
            () -> assertNotNull(Category.valueOf("MACRO_TOGGLED")),
            () -> assertNotNull(Category.valueOf("CHAT_COMMAND")),
            () -> assertNotNull(Category.valueOf("PROFILE_SWITCHED")),
            () -> assertNotNull(Category.valueOf("IMPORT_EXPORT")),
            () -> assertNotNull(Category.valueOf("ERROR"))
        );
    }

    // --- Helpers ---

    @SuppressWarnings("unchecked")
    private ConcurrentLinkedQueue<?> getPendingQueue() throws Exception {
        Field field = ToastManager.class.getDeclaredField("pendingEvents");
        field.setAccessible(true);
        return (ConcurrentLinkedQueue<?>) field.get(manager);
    }
}
