package btxds.dekal;

public class Main {

    public static void main(String[] args) {
        if (!AppLock.acquire()) System.exit(0);

        if (!DownloadManager.ensureJre()) System.exit(0);
        if (!DownloadManager.ensureLauncher()) System.exit(0);

        DownloadManager.launch();
        System.exit(0);
    }

}