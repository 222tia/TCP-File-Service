package file_service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ReaderWriter {
    public static ReentrantLock lock = new ReentrantLock(); // Lock to sync threads

    public static boolean isWriterWriting = false; //Checks if writers are active
    public static Condition isNoWriter = lock.newCondition(); // Checks if there are any writers
    public static Condition isDoneReading = lock.newCondition(); // Checks when readers are done reading

    private static int writerNum = 0;
    private static int readerNum = 0;
    private static int resource = 0;

    private static class Reader implements Runnable{
        public void run() {
            lock.lock();
            try {
                while(isWriterWriting) {
                    isNoWriter.await();
                }
                readerNum++;
                System.out.println("Reader #" + readerNum + " has read resource as: " + resource);
                readerNum--;
                if(readerNum == 0){
                    isDoneReading.signal();
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }
    }

// Writes to the shared item

    private static class Writer implements Runnable{
        public void run() {
            lock.lock();
            try {
                while(isWriterWriting && readerNum ==0) {
                    isDoneReading.await();
                }
                isWriterWriting = true;
                resource++;
                writerNum++;
                System.out.println("Writer # " + writerNum + " wrote the resource to: " + resource);
                writerNum--;
                if(writerNum == 0){
                    isNoWriter.signal();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                isWriterWriting = false;
                lock.unlock();
            }
        }
    }

    public static void main(String[] args){
        ExecutorService es = Executors.newFixedThreadPool(4);
        for(int i=0; i<6; i++){
            es.submit(new ReaderWriter.Writer());
            es.submit(new ReaderWriter.Reader());
        }
        es.shutdown();
    }
}


