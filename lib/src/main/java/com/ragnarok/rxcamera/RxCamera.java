package com.ragnarok.rxcamera;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Point;
import android.hardware.Camera;
import android.view.SurfaceView;
import android.view.TextureView;

import com.ragnarok.rxcamera.action.RxCameraActionBuilder;
import com.ragnarok.rxcamera.config.RxCameraConfig;
import com.ragnarok.rxcamera.request.RxCameraRequestBuilder;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;


/**
 * Created by ragnarok on 15/10/25.
 * The RxCamera library interface
 */
public class RxCamera  {

    private static final String TAG = "RxCamera";

    private RxCameraInternal cameraInternal = new RxCameraInternal();

    private Matrix rotateMatrix = null;

    /**
     * open the camera
     * @param context
     * @param config
     * @return
     */
    public static Observable<RxCamera> open(final Context context, final RxCameraConfig config) {
        return Observable.create(new ObservableOnSubscribe<RxCamera>() {
            @Override
            public void subscribe(ObservableEmitter<RxCamera> subscriber) throws Exception {
                RxCamera rxCamera = new RxCamera(context, config);
                if (rxCamera.cameraInternal.openCameraInternal()) {
                    subscriber.onNext(rxCamera);
                    subscriber.onComplete();
                } else {
                    subscriber.onError(rxCamera.cameraInternal.openCameraException());
                }
            }
        });
    }

    /**
     * open camera and start preview, bind a {@link SurfaceView}
     * @param context
     * @param config
     * @param surfaceView
     * @return
     */
    public static Observable<RxCamera> openAndStartPreview(Context context, RxCameraConfig config, final SurfaceView surfaceView) {
        return open(context, config).flatMap(new Function<RxCamera, ObservableSource<RxCamera>>() {
            @Override
            public ObservableSource<RxCamera> apply(RxCamera rxCamera) throws Exception {
                return rxCamera.bindSurface(surfaceView);
            }
        }).flatMap(new Function<RxCamera, Observable<RxCamera>>() {
            @Override
            public Observable<RxCamera> apply(RxCamera rxCamera) {
                return rxCamera.startPreview();
            }
        });
    }

    /**
     * open camera and start preview, bind a {@link TextureView}
     * @param context
     * @param config
     * @param textureView
     * @return
     */
    public static Observable<RxCamera> openAndStartPreview(Context context, RxCameraConfig config, final TextureView textureView) {
        return open(context, config).flatMap(new Function<RxCamera, ObservableSource<RxCamera>>() {
            @Override
            public Observable<RxCamera> apply(RxCamera rxCamera) {
                return rxCamera.bindTexture(textureView);
            }
        }).flatMap(new Function<RxCamera, ObservableSource<RxCamera>>() {
            @Override
            public Observable<RxCamera> apply(RxCamera rxCamera) {
                return rxCamera.startPreview();
            }
        });
    }

    private RxCamera(Context context, RxCameraConfig config) {
        this.cameraInternal = new RxCameraInternal();
        this.cameraInternal.setConfig(config);
        this.cameraInternal.setContext(context);
        rotateMatrix = new Matrix();
        rotateMatrix.postRotate(config.cameraOrientation, 0.5f, 0.5f);
    }

    public Matrix getRotateMatrix() {
        return rotateMatrix;
    }

    /**
     * bind a {@link SurfaceView} as the camera preview surface
     * @param surfaceView
     * @return
     */
    public Observable<RxCamera> bindSurface(final SurfaceView surfaceView) {
        return Observable.create(new ObservableOnSubscribe<RxCamera>() {
            @Override
            public void subscribe(ObservableEmitter<RxCamera> subscriber) throws Exception {
                boolean result = cameraInternal.bindSurfaceInternal(surfaceView);
                if (result) {
                    subscriber.onNext(RxCamera.this);
                    subscriber.onComplete();
                } else {
                    subscriber.onError(cameraInternal.bindSurfaceFailedException());
                }
            }
        });
    }

    /**
     * bind a {@link TextureView} as the camera preview surface
     * @param textureView
     * @return
     */
    public Observable<RxCamera> bindTexture(final TextureView textureView) {
        return Observable.create(new ObservableOnSubscribe<RxCamera>() {
            @Override
            public void subscribe(ObservableEmitter<RxCamera> subscriber) throws Exception {
                boolean result = cameraInternal.bindTextureInternal(textureView);
                if (result) {
                    subscriber.onNext(RxCamera.this);
                    subscriber.onComplete();
                } else {
                    subscriber.onError(cameraInternal.bindSurfaceFailedException());
                }
            }
        });
    }

    /**
     * start preview, must be called after bindTexture or bindSurface
     * @return
     */
    public Observable<RxCamera> startPreview() {
        return Observable.create(new ObservableOnSubscribe<RxCamera>() {
            @Override
            public void subscribe(ObservableEmitter<RxCamera> subscriber) throws Exception {
                boolean result = cameraInternal.startPreviewInternal();
                if (result) {
                    subscriber.onNext(RxCamera.this);
                    subscriber.onComplete();
                } else {
                    subscriber.onError(cameraInternal.startPreviewFailedException());
                }
            }
        });
    }

    /**
     * close the camera, return an Observable as the result
     * @return
     */
    public Observable<Boolean> closeCameraWithResult() {
        return Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(ObservableEmitter<Boolean> subscriber) throws Exception {
                subscriber.onNext(cameraInternal.closeCameraInternal());
                subscriber.onComplete();
            }
        });
    }

    /**
     * switch the camera, return an Observable indicated if switch success
     * @return
     */
    public Observable<Boolean> switchCamera() {
        return Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(ObservableEmitter<Boolean> subscriber) throws Exception {
                subscriber.onNext(cameraInternal.switchCameraInternal());
                subscriber.onComplete();
            }
        });
    }

    /**
     * return a {@link RxCameraRequestBuilder} which you can request the camera preview frame data
     * @return
     */
    public RxCameraRequestBuilder request() {
        return new RxCameraRequestBuilder(this);
    }

    /**
     * return a {@link RxCameraActionBuilder} which you can change the camera parameter in the fly
     * @return
     */
    public RxCameraActionBuilder action() {
        return new RxCameraActionBuilder(this);
    }

    /**
     * directly close the camera
     * @return true if close success
     */
    public boolean closeCamera() {
        return cameraInternal.closeCameraInternal();
    }

    public boolean isOpenCamera() {
        return cameraInternal.isOpenCamera();
    }

    public boolean isBindSurface() {
        return cameraInternal.isBindSurface();
    }

    /**
     * the config of this camera
     * @return
     */
    public RxCameraConfig getConfig() {
        return cameraInternal.getConfig();
    }

    /**
     * the native {@link android.hardware.Camera} object
     * @return
     */
    public Camera getNativeCamera() {
        return cameraInternal.getNativeCamera();
    }

    /**
     * the final preview size, mostly this is not the same as the one set in {@link RxCameraConfig}
     * @return
     */
    public Point getFinalPreviewSize() {
        return cameraInternal.getFinalPreviewSize();
    }

    public void installPreviewCallback(OnRxCameraPreviewFrameCallback previewCallback) {
        this.cameraInternal.installPreviewCallback(previewCallback);
    }

    public void uninstallPreviewCallback(OnRxCameraPreviewFrameCallback previewCallback) {
        this.cameraInternal.uninstallPreviewCallback(previewCallback);
    }

    public void installOneShotPreviewCallback(OnRxCameraPreviewFrameCallback previewFrameCallback) {
        this.cameraInternal.installOneShotPreviewCallback(previewFrameCallback);
    }

    public void uninstallOneShotPreviewCallback(OnRxCameraPreviewFrameCallback previewFrameCallback) {
        this.cameraInternal.uninstallOneShotPreviewCallback(previewFrameCallback);
    }
}
