/*
* Copyright 2014 Vasya Drobushkov
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.krossovochkin.clocklivewallpaper;

import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Vasya Drobushkov <vasya.drobushkov@gmail.com> on 19.02.14.
 */
public class ClockLiveWallpaperService extends WallpaperService {

    private static final float MAX_X_OFFSET = 1.0f;
    private static final int Y_ROTATION_MULTIPLIER = -50;
    private static final int UPDATE_TIME_MILLIS = 40;
    private static final String DATE_FORMAT_PATTERN = "kk:mm:ss";

    private final Handler handler = new Handler();

    @Override
    public Engine onCreateEngine() {
        return new ClockEngine();
    }

    private class ClockEngine extends Engine {

        private final Matrix matrix = new Matrix();
        private final Camera camera = new Camera();
        private final Paint paint = new Paint();
        private float centerX;
        private float centerY;
        private float xOffset;
        private boolean isVisible;

        private final Runnable drawRunnable = new Runnable() {
            public void run() {
                drawFrame();
            }
        };

        public ClockEngine() {
            this.paint.setColor(getResources().getColor(R.color.text_color));
            this.paint.setAntiAlias(true);
            this.paint.setStrokeWidth(getResources().getDimension(R.dimen.text_stroke_width));
            this.paint.setStrokeCap(Paint.Cap.ROUND);
            this.paint.setStyle(Paint.Style.STROKE);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            handler.removeCallbacks(drawRunnable);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            isVisible = visible;
            if (visible) {
                drawFrame();
            } else {
                handler.removeCallbacks(drawRunnable);
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);

            centerX = width / 2.0f;
            centerY = height / 2.0f;
            drawFrame();
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            isVisible = false;
            handler.removeCallbacks(drawRunnable);
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset) {
            super.onOffsetsChanged(xOffset, yOffset, xOffsetStep, yOffsetStep, xPixelOffset, yPixelOffset);

            this.xOffset = xOffset;
            drawFrame();
        }

        void drawFrame() {
            final SurfaceHolder holder = getSurfaceHolder();

            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                if (canvas != null) {
                    drawTime(canvas);
                }
            } finally {
                if (canvas != null) holder.unlockCanvasAndPost(canvas);
            }

            // Reschedule the next redraw
            handler.removeCallbacks(drawRunnable);
            if (isVisible) {
                handler.postDelayed(drawRunnable, UPDATE_TIME_MILLIS);
            }
        }

        void drawTime(Canvas canvas) {
            canvas.save();
            canvas.drawColor(getResources().getColor(R.color.background_color));

            camera.save();
            camera.rotateX(0);
            camera.rotateY(Y_ROTATION_MULTIPLIER * (xOffset - MAX_X_OFFSET / 2));
            camera.rotateZ(0);
            camera.getMatrix(matrix);

            matrix.postTranslate(centerX, centerY);

            canvas.concat(matrix);
            camera.restore();

            paint.setTextSize(getResources().getDimension(R.dimen.text_size));
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(getTimeString(), 0.0f, 0.0f, paint);

            canvas.restore();
        }

        private String getTimeString() {
            Date date = Calendar.getInstance().getTime();
            DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_PATTERN);
            return dateFormat.format(date);
        }
    }
}
