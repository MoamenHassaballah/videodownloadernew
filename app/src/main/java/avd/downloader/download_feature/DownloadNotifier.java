

package avd.downloader.download_feature;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;

import java.io.File;

import avd.downloader.LMvdApp;
import avd.downloader.R;

import static android.content.Context.NOTIFICATION_SERVICE;

public class DownloadNotifier {
    private final int ID = 77777;
    private Intent downloadServiceIntent;
    private Handler handler;
    private NotificationManager notificationManager;
    private DownloadingRunnable downloadingRunnable;

    private class DownloadingRunnable implements Runnable {
        @Override
        public void run() {


//            Intent snoozeIntent = new Intent(this, MyBroadcastReceiver.class);
//            snoozeIntent.setAction(Intent.);
//            snoozeIntent.putExtra(EXTRA_NOTIFICATION_ID, 0);
//            PendingIntent snoozePendingIntent =
//                    PendingIntent.getBroadcast(LMvdApp.getInstance().getApplicationContext(), 0, snoozeIntent, 0);


            String filename = downloadServiceIntent.getStringExtra("name") + "." +
                    downloadServiceIntent.getStringExtra("type");
            Notification.Builder NB;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NB = new Notification.Builder(LMvdApp.getInstance().getApplicationContext(), "download_01")
                        .setStyle(new Notification.BigTextStyle());
            } else {
                NB = new Notification.Builder(LMvdApp.getInstance().getApplicationContext())
                        .setSound(null)
                        .setPriority(Notification.PRIORITY_LOW);
            }
            NB.setContentTitle("Downloading " + filename)
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .setLargeIcon(BitmapFactory.decodeResource(LMvdApp.getInstance()
                            .getApplicationContext().getResources(), R.mipmap.ic_launcher_round))
                    .setAutoCancel(true);
//                     .addAction(R.drawable.ic_pause, "Pause", pendingIntentPause)
//                    .addAction(R.drawable.ic_cancel, "Cancel", pendingIntentCancel);

            if (downloadServiceIntent.getBooleanExtra("chunked", false)) {
                File file = new File(Environment.getExternalStoragePublicDirectory(Environment
                        .DIRECTORY_DOWNLOADS), filename);
                String downloaded;
                if (file.exists()) {
                    downloaded = android.text.format.Formatter.formatFileSize(LMvdApp.getInstance
                            ().getApplicationContext(), file.length());
                } else {
                    downloaded = "0KB";
                }
                NB.setProgress(100, 0, false)
                        .setContentText(downloaded);
                notificationManager.notify(ID, NB.build());
                handler.postDelayed(this, 1000);
            } else {
                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename);
                String sizeString="10";
                if (downloadServiceIntent.getStringExtra("size")!=null){
                    sizeString = downloadServiceIntent.getStringExtra("size");
                }
                int progress = (int) Math.ceil(((double) file.length() / (double) Long.parseLong
                        (sizeString)) * 100);
                progress = progress >= 100 ? 100 : progress;
                String downloaded = android.text.format.Formatter.formatFileSize(LMvdApp
                        .getInstance().getApplicationContext(), file.length());
                String total = android.text.format.Formatter.formatFileSize(LMvdApp.getInstance()
                        .getApplicationContext(), Long.parseLong
                        (sizeString));
                NB.setProgress(100, progress, false)
                        .setContentText(downloaded + "/" + total + "   " + progress + "%");
                notificationManager.notify(ID, NB.build());
                handler.postDelayed(this, 1000);
            }
        }
    }

    DownloadNotifier(Intent downloadServiceIntent) {
        this.downloadServiceIntent = downloadServiceIntent;
        notificationManager = (NotificationManager) LMvdApp.getInstance().getApplicationContext().getSystemService
                (NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(new NotificationChannel("download_01",
                    "Download Notification", NotificationManager.IMPORTANCE_LOW));
            notificationManager.createNotificationChannel(new NotificationChannel("download_02",
                    "Download Notification", NotificationManager.IMPORTANCE_HIGH));

            notificationManager
                    .getNotificationChannel("download_01")
                    .setSound(null, null);


        }
        HandlerThread thread = new HandlerThread("downloadNotificationThread");
        thread.start();
        handler = new Handler(thread.getLooper());
    }
    void notifyDownloading() {
        downloadingRunnable = new DownloadingRunnable();
        downloadingRunnable.run();
    }
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    void notifyDownloadFinished() {
        handler.removeCallbacks(downloadingRunnable);
        notificationManager.cancel(ID);

        String filename = downloadServiceIntent.getStringExtra("name") + "." +
                downloadServiceIntent.getStringExtra("type");
        Notification.Builder NB;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NB = new Notification.Builder(LMvdApp.getInstance().getApplicationContext(), "download_02")
                    .setTimeoutAfter(1500)
                    .setContentTitle("Download Finished")
                    .setContentText(filename)
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .setLargeIcon(BitmapFactory.decodeResource(LMvdApp.getInstance().getApplicationContext().getResources(),
                            R.mipmap.ic_launcher_round))
                    .setAutoCancel(true);
            notificationManager.notify(8888, NB.build());
        } else {
            NB = new Notification.Builder(LMvdApp.getInstance().getApplicationContext())
                    .setTicker("Download Finished")
                    .setPriority(Notification.PRIORITY_LOW)

                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .setLargeIcon(BitmapFactory.decodeResource(LMvdApp.getInstance().getApplicationContext().getResources(),
                            R.mipmap.ic_launcher_round))
                    .setAutoCancel(true);
            notificationManager.notify(8888, NB.build());
            notificationManager.cancel(8888);
        }
    }
    void cancel() {
        if (downloadingRunnable != null) {
            handler.removeCallbacks(downloadingRunnable);
        }
        String ns = NOTIFICATION_SERVICE;
        NotificationManager nMgr = (NotificationManager) LMvdApp.getInstance().getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
        nMgr.cancel(888);

    }
}
