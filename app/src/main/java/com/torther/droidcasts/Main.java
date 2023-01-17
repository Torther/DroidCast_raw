package com.torther.droidcasts;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.util.Pair;

import android.text.TextUtils;


import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.http.Multimap;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Locale;

/**
 * Created by seanzhou on 3/14/17.
 * Modify by Torther on 16/1/23
 */
public class Main {
    private static final String sTAG = Main.class.getName();

    private static final String IMAGE_BMP = "image/bmp";
    private static final String WIDTH = "width";
    private static final String HEIGHT = "height";
    private static final int SCREENSHOT_DELAY_MILLIS = 1500;

    private static int width = 0;
    private static int height = 0;

    private static int port = 53516;

    private static DisplayUtil displayUtil;
    private static Handler handler;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static void main(String[] args) {
        resolveArgs(args);

        AsyncHttpServer httpServer =
                new AsyncHttpServer() {
                    @Override
                    protected boolean onRequest(
                            AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                        return super.onRequest(request, response);
                    }
                };

        Looper.prepare();

        Looper looper = Looper.myLooper();
        System.out.println(">>> DroidCast main entry");

        handler = new Handler(looper);

        displayUtil = new DisplayUtil();

        AsyncServer server = new AsyncServer();
        httpServer.get("/screenshot", new AnyRequestCallback());

        httpServer.websocket(
                "/src",
                (webSocket, request) -> {

                    Pair<Integer, Integer> pair = getDimension();
                    displayUtil.setRotateListener(
                            rotate -> {
                                System.out.println(">>> rotate to " + rotate);

                                // delay for the new rotated screen
                                handler.postDelayed(
                                        () -> {
                                            Pair<Integer, Integer> dimen = getDimension();
                                            sendScreenshotData(webSocket, dimen.first, dimen.second);
                                        },
                                        SCREENSHOT_DELAY_MILLIS);
                            });

                    sendScreenshotData(webSocket, pair.first, pair.second);
                });

        httpServer.listen(server, port);

        Looper.loop();
    }

    private static void resolveArgs(String[] args) {
        if (args.length > 0) {
            String[] params = args[0].split("=");

            if ("--port".equals(params[0])) {
                try {
                    port = Integer.parseInt(params[1]);
                    System.out.println(sTAG + " | Port set to " + port);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @NonNull
    private static Pair<Integer, Integer> getDimension() {
        Point displaySize = displayUtil.getCurrentDisplaySize();

        int width = 1080;
        int height = 1920;
        if (displaySize != null) {
            width = displaySize.x;
            height = displaySize.y;
        }
        return new Pair<>(width, height);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private static void sendScreenshotData(WebSocket webSocket, int width, int height) {
        try {
            byte[] inBytes =
                    getScreenImageInBytes(
                            width,
                            height,
                            (w, h, rotation) -> {
                                JSONObject obj = new JSONObject();
                                try {
                                    obj.put("width", w);
                                    obj.put("height", h);
                                    obj.put("rotation", rotation);

                                    webSocket.send(obj.toString());
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            });
            webSocket.send(inBytes);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private static byte[] getScreenImageInBytes(
            int w,
            int h,
            @Nullable ImageDimensionListener resolver)
            throws IOException {

        int destWidth = w;
        int destHeight = h;
        Bitmap bitmap = ScreenCaptorUtils.screenshot(destWidth, destHeight);

        if (bitmap == null) {
            System.out.printf(
                    Locale.ENGLISH,
                    ">>> failed to generate image with resolution %d:%d%n",
                    Main.width,
                    Main.height);

            destWidth /= 2;
            destHeight /= 2;

            bitmap = ScreenCaptorUtils.screenshot(destWidth, destHeight);
        }

        System.out.printf(
                Locale.ENGLISH,
                "Bitmap generated with resolution %d:%d, process id %d | thread id %d%n",
                destWidth,
                destHeight,
                Process.myPid(),
                Process.myTid());

        int screenRotation = displayUtil.getScreenRotation();

        if (screenRotation != 0) {
            switch (screenRotation) {
                case 1: // 90 degree rotation (counter-clockwise)
                    bitmap = displayUtil.rotateBitmap(bitmap, -90f);
                    break;
                case 3: // 270 degree rotation
                    bitmap = displayUtil.rotateBitmap(bitmap, 90f);
                    break;
                case 2: // 180 degree rotation
                    bitmap = displayUtil.rotateBitmap(bitmap, 180f);
                    break;
                default:
                    break;
            }
        }

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        System.out.println("Bitmap final dimens : " + width + "|" + height);
        if (resolver != null) {
            resolver.onResolveDimension(width, height, screenRotation);
        }

        Bitmap softBitmap = bitmap.copy(Bitmap.Config.RGB_565, true);

        ByteBuffer buffer = ByteBuffer.allocate(softBitmap.getByteCount());
        softBitmap.copyPixelsToBuffer(buffer);


        // "Make sure to call Bitmap.recycle() as soon as possible, once its content is not
        // needed anymore."
        bitmap.recycle();
        softBitmap.recycle();

        return buffer.array();
    }

    interface ImageDimensionListener {
        void onResolveDimension(int width, int height, int rotation);
    }

    static class AnyRequestCallback implements HttpServerRequestCallback {
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
            try {
                Multimap pairs = request.getQuery();

                String width = pairs.getString(WIDTH);
                String height = pairs.getString(HEIGHT);

                if (!TextUtils.isEmpty(width) && !TextUtils.isEmpty(height) &&
                        TextUtils.isDigitsOnly(width) && TextUtils.isDigitsOnly(height)) {
                    Main.width = Integer.parseInt(width);
                    Main.height = Integer.parseInt(height);
                }

                if (Main.width == 0 || Main.height == 0) {
                    // dimension initialization
                    Point point = displayUtil.getCurrentDisplaySize();

                    if (point != null && point.x > 0 && point.y > 0) {
                        Main.width = point.x;
                        Main.height = point.y;
                    } else {
                        Main.width = 480;
                        Main.height = 800;
                    }
                }

                int destWidth = Main.width;
                int destHeight = Main.height;

                byte[] bytes = getScreenImageInBytes(destWidth, destHeight, null);

                response.send(IMAGE_BMP, bytes);
                response.send("application/octet-stream",bytes);
            } catch (Exception e) {
                e.printStackTrace();

                response.code(500);
                String template = ":(  Failed to generate the screenshot on device / emulator : %s - %s - Android OS : %s";
                String error = String.format(Locale.ENGLISH, template, Build.MANUFACTURER, Build.DEVICE, Build.VERSION.RELEASE);
                response.send(error);
            }
        }
    }
}