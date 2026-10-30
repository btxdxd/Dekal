package btxds.dekal;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

public class AppLock {

    private static FileChannel channel;
    private static FileLock lock;

    public static boolean acquire() {
        try {
            File lockFile = new File("dekal.lock");

            channel = new RandomAccessFile(lockFile, "rw").getChannel();

            lock = channel.tryLock();

            if (lock == null) {
                channel.close();
                return false;
            }

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    if (lock != null && lock.isValid()) lock.release();
                    if (channel != null && channel.isOpen()) channel.close();
                } catch (Exception ignored) {}
            }));

            return true;
        } catch (Exception e) {
            return true;
        }
    }

}