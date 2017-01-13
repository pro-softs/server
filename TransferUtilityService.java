package com.app.ojam.Constants;

import android.app.Activity;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.os.EnvironmentCompat;
import android.util.Log;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferService;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.app.ojam.Activities.NewJam;
import com.app.ojam.Objects.Jam;
import com.app.ojam.Objects.Profile;
import com.app.ojam.Objects.User;
import com.app.ojam.Retrofit.RestClient;
import com.github.johnpersano.supertoasts.SuperActivityToast;
import com.github.johnpersano.supertoasts.SuperToast;
import com.github.johnpersano.supertoasts.util.Style;
import com.google.gson.Gson;

import org.ffmpeg.android.Clip;
import org.ffmpeg.android.FfmpegController;
import org.ffmpeg.android.ShellUtils;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by pR0 on 18-10-2016.
 */
public class TransferUtilityService extends Service {

    private static String BUCKET_NAME = "ojamdata";
    final String NOTIFICATION = "com.app.ojam.NOTIFICATION";

    HashMap<Jam, TransferObserver> uploader;
    HashMap<String, TransferObserver> uploader_add;

    LocalBroadcastManager broadcastManager;

    PrefManager prefManager = new PrefManager(Application.ctx);

    AmazonS3Client s3;
    TransferUtility transferUtility;
    RestClient restClient;
    public static final String PATH = Environment.getExternalStorageDirectory() + "/android/data/com.app.ojam";
    Gson gson;
    private final IBinder tranferBind = new TransferBinder();

    FfmpegController controller;
    private Object object = new Object();

    /*public TransferUtilityService() {
        super("TransferUtilityService");
    }*/

    public class TransferBinder extends Binder {
        public TransferUtilityService getService() {
            return TransferUtilityService.this;
        }
    }

    //activity will bind to service
    @Override
    public IBinder onBind(Intent intent) {
        return tranferBind;
    }

    //release resources when unbind
    @Override
    public boolean onUnbind(Intent intent){
        return false;
    }

    @Override
    public void onCreate() {

        super.onCreate();
        uploader = new HashMap<>();
        uploader_add = new HashMap<>();

        prefManager = new PrefManager(Application.ctx);

        restClient = RestClient.getInstance();

        broadcastManager = LocalBroadcastManager.getInstance(this);

        credentialsProvider();
        setTransferUtility();

        gson = new Gson();
        try {
            controller = new FfmpegController(this, new File("temp"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void newJamSave(Jam newJam) {
        try {
            Log.e("TransferUtiityService", "start saving");
            startUploadNew(newJam, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addJam(Jam jam) {
        
        try {
            startUploadNew(jam, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void justSave(final Jam newJam) {
        Log.e("jusdt saving", "in service");

        restClient.startJam(newJam, new RestClient.ResultReadyCallback() {
            @Override
            public void resultReady(String result) {
                if (result.equals("saved")) {
                    Log.e("saved", "jam - " + newJam.getName());
                    broadcastNotify(newJam, "success", true);
                } else if (result.equals("not_saved")) {
                    Log.e("not_saved", "jam - " + newJam.getName());
                    broadcastNotify(newJam, "not_exist", true);
                } else {
                    Log.e("failed saving", "jam - " + newJam.getName());
                    broadcastNotify(newJam, "uploaded", true);
                }
            }

            @Override
            public void resultReady(Jam jam) {

            }

            @Override
            public void resultReady(User user) {

            }

            @Override
            public void resultReady(List<Jam> jams) {

            }

            @Override
            public void resultReady(Profile profile) {

            }
        });
    }

    public void cancel(Jam jam) {
        if(uploader.containsKey(jam))
            uploader.remove(jam);
    }

    public void justSaveAdd(final Jam jam) {
            if (jam.getJammers().size() == 2) {
                restClient.jamSec(jam.getJammers().get(1).getId(), jam.get_id(), jam.getMusic_path(), new RestClient.ResultReadyCallback() {

                    @Override
                    public void resultReady(String result) {
                        if (result.equals("saved")) {
                            Log.e("saved", "jam - " + jam.getName());
                               broadcastNotify(jam, "success", false);
                        } else if (result.equals("not_saved")) {
                            Log.e("not_saved", "jam - " + jam.getName());
                            broadcastNotify(jam, "not_exist", true);
                        } else {
                            Log.e("failed saving", "jam - " + jam.getName());
                            broadcastNotify(jam, "uploaded", false);
                        }
                    }

                    @Override
                    public void resultReady(Jam jam) {

                    }

                    @Override
                    public void resultReady(User user) {

                    }

                    @Override
                    public void resultReady(List<Jam> jams) {

                    }

                    @Override
                    public void resultReady(Profile profile) {

                    }
                });
            } else if (jam.getJammers().size() == 3) {
                restClient.jamThird(jam.getJammers().get(2).getId(), jam.get_id(), jam.getMusic_path(), new RestClient.ResultReadyCallback() {

                    @Override
                    public void resultReady(String result) {
                        if (result.equals("saved")) {
                            Log.e("saved", "jam - " + jam.getName());
                            broadcastNotify(jam, "success", false);
                        } else if (result.equals("not_saved")) {
                            Log.e("not_saved", "jam - " + jam.getName());
                            broadcastNotify(jam, "not_exist", true);
                        } else {
                            Log.e("failed saving", "jam - " + jam.getName());
                            broadcastNotify(jam, "uploaded", false);
                        }
                    }

                    @Override
                    public void resultReady(Jam jam) {

                    }

                    @Override
                    public void resultReady(User user) {

                    }

                    @Override
                    public void resultReady(List<Jam> jams) {

                    }

                    @Override
                    public void resultReady(Profile profile) {

                    }
                });
            }
    }

    public void credentialsProvider() {
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                Application.ctx,"us-east-1:4f917d0f-34d3-41fd-909c-3baa2e84157c",
                Regions.US_EAST_1
        );

        Map<String, String> logins = new HashMap<String, String>();

        Log.e("fb_key_here", prefManager.getToken());

        prefManager = new PrefManager(Application.ctx);

        if(!prefManager.getToken().isEmpty()) {
            logins.put("graph.facebook.com", prefManager.getToken());
            credentialsProvider.setLogins(logins);
            setAmazonS3Client(credentialsProvider);
        } else {
            Log.e("error", "no fb key erer er erer ");
        }
    }

    public void setAmazonS3Client(CognitoCachingCredentialsProvider credentialsProvider) {
        s3 = new AmazonS3Client(credentialsProvider);
        s3.setRegion(Region.getRegion(Regions.US_EAST_1));
    }

    public void setTransferUtility() {
        transferUtility = new TransferUtility(s3, Application.ctx);
    }

    public TransferObserver setFileToUplaod(String keyName, String fileName) {

        TransferObserver transferObserver = transferUtility.upload(
                BUCKET_NAME,      //The bucket to upload to
                keyName,     //The key for the uploaded object
                new File(PATH + "/imp/" + fileName + ".flac")      // The file where the data to upload exists
        );

        return transferObserver;
    }

    /*public TransferObserver setVideoFileToUplaod(String keyName, String fileName) {
        TransferObserver transferObserver = transferUtility.upload(
                BUCKET_NAME,      //The bucket to upload to
                keyName+"_v.mp4",     //The key for the uploaded object
                new File(PATH + "/out.mp4")      // The file where the data to upload exists
        );

        return transferObserver;
    } */

    public void transferObserverListener(final TransferObserver transferObserver, final Jam jam, final boolean create){
        transferObserver.setTransferListener(new TransferListener(){

            @Override
            public void onStateChanged(int id, TransferState state) {
                Log.e("state changed_audio", state.name());

                if(state.equals(TransferState.COMPLETED)) {

                        final File f2 = new File(PATH + "/imp/" + jam.getMusic_path() + ".flac");
                        f2.delete();
                    //TransferObserver video = setVideoFileToUplaod(jam.getMusic_path(), jam.getMusic_path());
                    //videoTransfer(video, jam, create);

                    if (create) {
                        restClient.startJam(jam, new RestClient.ResultReadyCallback() {
                            @Override
                            public void resultReady(String result) {
                                if (result.equals("saved")) {
                                    Log.e("saved", "jam - " + jam.getName());
                                    broadcastNotify(jam, "success", true);
                                } else {
                                    Log.e("failed saving", "jam - " + jam.getName());
                                    broadcastNotify(jam, "uploaded", true);
                                }
                            }

                            @Override
                            public void resultReady(Jam jam) {

                            }

                            @Override
                            public void resultReady(User user) {

                            }

                            @Override
                            public void resultReady(List<Jam> jams) {

                            }

                            @Override
                            public void resultReady(Profile profile) {

                            }
                        });
                    } else {

                        if (jam.getJammers().size() == 2) {
                            Log.e("uploading_sec", "hreer") ;
                            restClient.jamSec(jam.getJammers().get(1).getId(), jam.get_id(), jam.getMusic_path(), new RestClient.ResultReadyCallback() {

                                @Override
                                public void resultReady(String result) {
                                    if (result.equals("saved")) {
                                        Log.e("saved", "jam - " + jam.getName());
                                        broadcastNotify(jam, "success", false);
                                    } else if(result.equals("not_saved")){
                                        Log.e("not_saved", "jam - " + jam.getName());
                                        broadcastNotify(jam, "not_exist", true);
                                    } else {
                                        Log.e("failed saving", "jam - " + jam.getName());
                                        broadcastNotify(jam, "uploaded", false);
                                    }
                                }

                                @Override
                                public void resultReady(Jam jam) {

                                }

                                @Override
                                public void resultReady(User user) {

                                }

                                @Override
                                public void resultReady(List<Jam> jams) {

                                }

                                @Override
                                public void resultReady(Profile profile) {

                                }
                            });
                        } else if (jam.getJammers().size() == 3) {
                            restClient.jamThird(jam.getJammers().get(2).getId(), jam.get_id(), jam.getMusic_path(), new RestClient.ResultReadyCallback() {

                                @Override
                                public void resultReady(String result) {
                                    if (result.equals("saved")) {
                                        Log.e("saved", "jam - " + jam.getName());
                                        broadcastNotify(jam, "success", false);
                                    } else if(result.equals("not_saved")){
                                        Log.e("not_saved", "jam - " + jam.getName());
                                        broadcastNotify(jam, "no_exist", true);
                                    } else {
                                        Log.e("failed saving", "jam - " + jam.getName());
                                        broadcastNotify(jam, "uploaded", false);
                                    }
                                }

                                @Override
                                public void resultReady(Jam jam) {

                                }

                                @Override
                                public void resultReady(User user) {

                                }

                                @Override
                                public void resultReady(List<Jam> jams) {

                                }

                                @Override
                                public void resultReady(Profile profile) {

                                }
                            });
                        }
                    }
                } else if(state.equals(TransferState.FAILED)) {
                    broadcastNotify(jam, "failed_audio", false);
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                int percentage = (int) (bytesCurrent/bytesTotal * 100);
                Log.e("percentage_audio", percentage + " " + id);
            }

            @Override
            public void onError(int id, Exception ex) {
                Log.e("err_audio", ex.getMessage() + " " + id);
            }
        });
    }

    /*public void videoTransfer(TransferObserver transferObserver, final Jam jam, final boolean create) {
        transferObserver.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                Log.e("state changed_video", state.name());
                if(state.equals(TransferState.COMPLETED)) {

                    final File f2 = new File(PATH + "/out.mp4");
                    f2.delete();

                } else if(state.equals(TransferState.FAILED)) {
                    broadcastNotify(jam, "failed_video", false);
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                int percentage = (int) (bytesCurrent/bytesTotal * 100);
                Log.e("percentage_video", percentage + " " + id);
            }

            @Override
            public void onError(int id, Exception ex) {
                Log.e("err_video", ex.getMessage());
            }
        });
    } */

   /* @Override
    protected void onHandleIntent(Intent intent) {

        if(intent == null) {
            Log.e("intentservice", "null");
        } else {
            Log.e("called", "service");
        }

        String type = intent.getStringExtra("type");
        if(type.equals("share")) {
            String f = intent.getStringExtra("file_name");
            download(f);
        } else {
            String jam = intent.getStringExtra("jam");
            Jam j = gson.fromJson(jam, Jam.class);

            if (type.equals("new_jam")) {
                Log.e("at onHandleIntent", prefManager.getToken());
                newJamSave(j);
            }
            else if (type.equals("add_jam")) {
                Log.e("add_jam", "clivkeed");
                addJam(j);
            } else if (type.equals("just_save"))
                justSave(j);
            else if (type.equals("cancel"))
                cancel(j);
            else if (type.equals("just_save_add")) {
                Log.e("just_save_add", "clivkeed");
                justSaveAdd(j);
            } else {
                boolean b = intent.getBooleanExtra("create", false);
                /*if (type.equals("only_video"))
                    onlyVideo(j, b);
                else
                if(type.equals("save_again"))
                    startAgain(j ,b);
            }
        }
    } */

    public void startUploadNew(final Jam jam, final boolean create) throws IOException {
        final String name = jam.getMusic_path();

        Log.e("upload", "added to watch list");

        try {
            controller.convertToMp2(PATH + "/imp/", name, new ShellUtils.ShellCallback() {
                @Override
                public void shellOut(String shellLine) {
                    Log.e("output_mp2", shellLine);
                }
                @Override
                public void processComplete(int exitValue) {
                    Log.e("completed_flac", "converted");

                    final File f2 = new File(PATH + "/imp/" + name + ".wav");
                    f2.delete();

                    TransferObserver upload = setFileToUplaod(name, name);

                    if(!uploader.containsKey(jam)) {
                        Log.e("put jam", "in_uploader");
                        uploader.put(jam, upload);
                    }

                    if(create)
                        transferObserverListener(upload, jam, true);
                    else {
                        Log.e("add_jam_upload", "clivkeed");
                        transferObserverListener(upload, jam, false);
                    }

                    Log.e("upload", "added to listener");


                    Log.e("jam_upload", "yeh");
                    //skip.performClick();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        /*try {
            controller.createWave(PATH + "/imp/" + name + ".wav", Environment.getExternalStorageDirectory().getAbsolutePath()
                    + "/wave.png", new ShellUtils.ShellCallback() {
                @Override
                public void shellOut(String shellLine) {
                    Log.e("wave", shellLine);
                }

                @Override
                public void processComplete(int exitValue) {
                    try {
                        controller.filters(new ShellUtils.ShellCallback() {
                            @Override
                            public void shellOut(String shellLine) {
                                Log.e("filters", shellLine);
                            }

                            @Override
                            public void processComplete(int exitValue) {

                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            controller.createSlideshowFromImagesAndAudio(Application.ctx.getFilesDir().getAbsolutePath()
                    , PATH + "/imp/" + name + ".wav",
                    PATH + "/out.mp4", new ShellUtils.ShellCallback() {
                        @Override
                        public void shellOut(String shellLine) {
                            Log.e("video", shellLine);
                        }

                        @Override
                        public void processComplete(int exitValue) {
                            Log.e("video", "conversion complete");

                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        } */
    }

    /*public void onlyVideo(Jam jam, boolean create) {
        TransferObserver upload = setVideoFileToUplaod(jam.getMusic_path(), jam.getMusic_path());
        if(create)
            videoTransfer(upload, jam, true);
        else
            videoTransfer(upload, jam, false);

        Log.e("video uploading", "added");
    } */


    public void startAgain(Jam jam, boolean create) {
        TransferObserver upload = setFileToUplaod(jam.getMusic_path(), jam.getMusic_path());

        if(!uploader.containsKey(jam))
            uploader.put(jam, upload);

        if(create)
            transferObserverListener(upload, jam, true);
        else
            transferObserverListener(upload, jam, false);

        Log.e("upload_again", "added to listener");
    }

    public void broadcastNotify(Jam jam, String state, boolean create) {
        Intent i = new Intent("posting");
        i.putExtra("create", create);

        if(state.equals("success") || state.equals("not_exist")) {
            if(state.equals("success"))
                i.putExtra("notice", "Your jam \'" + jam.getName() + "\' have been posted.");
            else
                i.putExtra("notice", "Cannot post. \'" + jam.getName() + "\' not found.");
            i.putExtra("success", true);
            i.putExtra("uploaded", true);
            uploader.remove(jam);
        } else if(state.equals("uploaded")) {
            i.putExtra("notice", "Your jam \'" + jam.getName() + "\' could not be posted.");
            i.putExtra("uploaded", true);
            i.putExtra("success", false);

            Gson gson = new Gson();
            String json = gson.toJson(jam);
            i.putExtra("jam", json);
        } else if(state.contains("failed_audio")) {
            i.putExtra("notice", "Your jam \'" + jam.getName() + "\' could not be posted.");
            i.putExtra("uploaded", false);
            i.putExtra("success", false);
            i.putExtra("type", "audio");

            Gson gson = new Gson();
            String json = gson.toJson(jam);
            i.putExtra("jam", json);
        } else if(state.contains("failed_video")) {
            i.putExtra("notice", "Your jam \'" + jam.getName() + "\' could not be posted.");
            i.putExtra("uploaded", false);
            i.putExtra("success", false);
            i.putExtra("type", "video");

            Gson gson = new Gson();
            String json = gson.toJson(jam);
            i.putExtra("jam", json);
        } else if(state.equals("not_exist")) {
            i.putExtra("notice", "Your Jam \'" + jam.getName() + "\' already posted. ");
            i.putExtra("uploaded", true);
            i.putExtra("success", true);
        }

        i.setAction(NOTIFICATION);
        sendOrderedBroadcast(i, null);
    }

    public void download(final String fileName) {
        File download = new File(PATH + "/share.mp4");
        TransferObserver downloader = transferUtility.download("ojamdata", fileName, download);
        downloader.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                if(state.equals(TransferState.COMPLETED)) {
                    Intent i = new Intent("downloaded");
                    i.putExtra("success", true);
                    i.setAction("share");
                    sendBroadcast(i);
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                Log.e("download_share", fileName + "  " + bytesCurrent + "");
            }

            @Override
            public void onError(int id, Exception ex) {

                Log.e("error_share", ex.getMessage());
                Intent i = new Intent("downloaded");
                i.putExtra("success", false);
                i.setAction("share");
                sendBroadcast(i);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e("Detroyted", "TrandferUtilityServie");
    }
}