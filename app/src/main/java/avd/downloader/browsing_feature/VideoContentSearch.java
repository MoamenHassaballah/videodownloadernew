

package avd.downloader.browsing_feature;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.HttpsURLConnection;

import avd.downloader.R;

public abstract class VideoContentSearch extends Thread {
    private Context context;
    private String url;
    private String page;
    private String title;
    private String[] filters;
    private int numLinksInspected;
//    public String website= "http://m.youtube.com/";

    private static final String TAG = "smarttest";

    public abstract void onStartInspectingURL();

    public abstract void onFinishedInspectingURL(boolean finishedAll);

    public abstract void onVideoFound(String size, String type, String link, String name,
                                      String page, boolean chunked, String website);

    public VideoContentSearch(Context context, String url, String page, String title) {
        this.context = context;
        this.url = url;
        this.page = page;
        this.title = title;
        numLinksInspected = 0;
    }

    @Override
    public void run() {
        String urlLowerCase = url.toLowerCase();
        filters = context.getResources().getStringArray(R.array.videourl_filters);
        boolean urlMightBeVideo = false;
        for (String filter : filters) {
            if (urlLowerCase.contains(filter)) {
                urlMightBeVideo = true;
                break;
            }
        }
        if (urlMightBeVideo) {
            numLinksInspected++;
            onStartInspectingURL();
            Log.i(TAG, "retreiving headers from " + url);

            HttpURLConnection uCon = null;
            try {
                uCon = (HttpURLConnection) new URL(url).openConnection();
                uCon.setRequestProperty("Accept-Encoding", "identity");
                uCon.connect();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (uCon != null) {
                String contentType = uCon.getHeaderField("content-type");

                if (contentType != null) {
                    contentType = contentType.toLowerCase();
                    if (contentType.contains("video") || contentType.contains("audio") || contentType.contains(".mp3")
                       ) {

                        addVideoToList(uCon,url, page, title, contentType);
                    } else if (contentType.equals("application/x-mpegurl") ||
                            contentType.equals("application/vnd.apple.mpegurl")) {
                        addVideosToListFromM3U8(uCon, page, title, contentType);
                    } else Log.i(TAG, "Not a video. Content type = " +
                            contentType);
                } else {
                    Log.i(TAG, "no content type");
                }
            } else Log.i(TAG, "no connection");

            numLinksInspected--;
            boolean finishedAll = false;
            if (numLinksInspected <= 0) {
                finishedAll = true;
            }
            onFinishedInspectingURL(finishedAll);
        }
    }

    private void addVideoToList(HttpURLConnection uCon, final String url, String page, String title, String contentType) {
        try {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        URL myUrl = new URL(url);
                        URLConnection urlConnection = myUrl.openConnection();
                        urlConnection.setRequestProperty("Accept-Encoding", "identity");
                        urlConnection.connect();
                        int file_size = urlConnection.getContentLength();
                        Log.i("sasa", "file_size = " + file_size);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            String size = uCon.getHeaderField("Content-Length");
            String link = uCon.getHeaderField("Location");
            Log.i("lenth",String.valueOf(uCon.getContentLength()));
            if (link == null) {
                link = uCon.getURL().toString();
            }

            String host = new URL(page).getHost();
            String website = null;
            boolean chunked = false;
            if (host.contains("twitter.com") && contentType.equals("video/mp2t")) {
                return;
            }

            if (host.contains("vimeo.com") && contentType.equals("video/mp2t")) {
                return;
            }


            String name = "video";
            if (title != null) {
                if (contentType.contains("audio")) {
                    name = title;
                } else {
                    name = title;
                }
            } else if (contentType.contains("audio")) {
                name = "audio";
            }else if (contentType.contains("image")){
                name="image";
            }
            if (host.contains("dailymotion.com")) {
                chunked = true;
                website = "dailymotion.com";
                link = link.replaceAll("(frag\\()+(\\d+)+(\\))", "FRAGMENT");
                size = null;
            } else if (host.contains("vimeo.com") && link.endsWith("m4s")) {
                chunked = true;
                website = "vimeo.com";
                link = link.replaceAll("(segment-)+(\\d+)", "SEGMENT");
                size = null;
            } else if (host.contains("facebook.com") && link.contains("bytestart")) {
                int b = link.lastIndexOf("&bytestart");
                int f = link.indexOf("fbcdn");
                if (b > 0) {
                    link = "https://video.xx." + link.substring(f, b);
                }
                URLConnection fbCon;
                fbCon = new URL(link).openConnection();
                fbCon.setRequestProperty("Accept-Encoding", "identity");
                fbCon.connect();
                size = fbCon.getHeaderField("content-length");
                website = "facebook.com";
            }

            String type;
            switch (contentType) {
                case "video/webm":
                case "audio/webm":
                    type = "webm";
                    break;
                case "video/mp2t":
                case "video/MP2T":
                    type = "ts";
                    break;
                case "audio/mpeg":
                    type = "mp3";
                    break;
                case "image/jpeg":
                    type ="jpg";
                    break;
                case "image/png":
                    type ="png";
                    break;
                case "video/mp4":
                default:
                    type = "mp4";
                    break;
            }

            onVideoFound(size, type, link, name, page, chunked, website);
            String videoFound = "name:" + name + "\n" +
                    "link:" + link + "\n" +
                    "type:" + contentType + "\n" +
                    "size:" + size;
            Log.i(TAG, videoFound);
        } catch (IOException | NullPointerException e) {
            Log.i("error",e.getMessage());
            e.printStackTrace();
            Log.e("smarttest", "Exception in adding video to " +
                    "list");
        }
    }

    private void addVideosToListFromM3U8(URLConnection uCon, String page, String title, String
            contentType) {
        try {
            String host;
            host = new URL(page).getHost();
            if (host.contains("twitter.com") || host.contains("metacafe.com") || host.contains
                    ("myspace.com")) {
                InputStream in = uCon.getInputStream();
                InputStreamReader inReader = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    inReader = new InputStreamReader(in, StandardCharsets.UTF_8);
                }
                BufferedReader buffReader = new BufferedReader(inReader);
                String line;
                String prefix = null;
                String type = null;
                String name = "video";
                String website = null;
                if (title != null) {
                    name = title;
                }
                if (host.contains("twitter.com")) {
                    prefix = "https://video.twimg.com";
                    type = "ts";
                    website = "twitter.com";
                } else if (host.contains("metacafe.com")) {
                    String link = uCon.getURL().toString();
                    prefix = link.substring(0, link.lastIndexOf("/") + 1);
                    website = "metacafe.com";
                    type = "mp4";
                } else if (host.contains("myspace.com")) {
                    String link = uCon.getURL().toString();
                    website = "myspace.com";
                    type = "ts";

                    onVideoFound(null, type, link, name, page, true, website);
                    String videoFound = "name:" + name + "\n" +
                            "link:" + link + "\n" +
                            "type:" + contentType + "\n" +
                            "size: null";
                    Log.i(TAG, videoFound);
                    return;
                }
                while ((line = buffReader.readLine()) != null) {
                    if (line.endsWith(".m3u8")) {
                        String link = prefix + line;
                        onVideoFound(null, type, link, name, page, true, website);
                        String videoFound = "name:" + name + "\n" +
                                "link:" + link + "\n" +
                                "type:" + contentType + "\n" +
                                "size: null";
                        Log.i(TAG, videoFound);
                    }

                }
                in.close();
                inReader.close();
                buffReader.close();
            } else {
                Log.i("smarttest", "Content type is " + contentType + " but site is not " +
                        "supported");
            }


        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}