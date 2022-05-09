package util;

/**
 * Reference CS601 class example
 * https://github.com/CS601-F21/code-examples/blob/main/Threads/src/main/java/concurrent/CS601BlockingQueue.java
 *
 * @author marisatania
 */
public class BlockingQueue<T> {
  private T[] items;
  private int start;
  private int end;
  private int size;
  private int capacity;
  private static final int DEFAULT_CAPACITY = 11;

  /**
   * Constructor of the blocking queue for capacity not specified.
   */
  public BlockingQueue() {
    init(DEFAULT_CAPACITY);
  }

  /**
   * Constructor of the blocking queue.
   *
   * @param capacity the capacity of the blocking queue
   */
  public BlockingQueue(int capacity) {
    init(capacity);
  }

  /**
   * Method to initialize the queue.
   *
   * @param capacity the capacity of the blocking queue
   */
  private void init(int capacity) {
    this.items = (T[]) new Object[capacity];
    this.capacity = capacity;
    this.start = 0;
    this.end = -1;
    this.size = 0;
  }

  /**
   * A blocking method for putting an item into the queue.
   *
   * @param item the item to put in the queue
   */
  public synchronized void put(T item) {
    while (size == capacity) {
      // blocking
      try {
        wait();
      } catch (InterruptedException e) {
        System.err.println("Interrupted: " + e);
      }
    }

    end = (end + 1) % capacity;
    items[end] = item;
    size++;
    if (size == 1) this.notifyAll();
  }

  /**
   * Checks if the queue is empty.
   *
   * @return true if the queue is empty and vice versa.
   */
  public synchronized boolean isEmpty() {
    return size == 0;
  }

  /**
   * Method to return the head of the queue,
   * or if the queue is empty,
   * wait up to the timeout amount of time for
   * a new item to be added.
   * If the queue is still empty after timeout amount,
   * return null.
   *
   * @param timeoutMillis amount of time to wait
   * @return head of the queue or null if queue
   * is empty after certain amount of time
   */
  public synchronized T poll(long timeoutMillis) {
    while (size == 0) {
      try {
        wait(timeoutMillis);
      } catch (InterruptedException e) {
        System.err.println("Interrupted: " + e);
      }
      if (size == 0) {
        return null;
      }
    }
    T item = items[start];
    start = (start + 1) % capacity;
    size--;

    if (size == capacity - 1) this.notifyAll();
    return item;
  }

  public synchronized T take() {
    while(size == 0) {
      try {
        this.wait();
      } catch (InterruptedException e) {
        System.err.println("Interrupted: " + e.getMessage());
      }
    }

    T item = items[start];
    start = (start+1)%items.length;
    size--;
        /*
        If the queue was previously full and a new slot has now opened
        notify any waiters in the put method.
         */
    if(size == items.length-1) {
      this.notifyAll();
    }

    return item;
  }


}
