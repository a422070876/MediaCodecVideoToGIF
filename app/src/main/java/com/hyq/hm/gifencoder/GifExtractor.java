package com.hyq.hm.gifencoder;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import com.hyq.hm.bitmap.FastYUVtoRGB;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by 海米 on 2018/9/12.
 */

public class GifExtractor {
    private static final String VIDEO = "video/";

    private Context context;
    private MediaExtractor videoExtractor;
    private int trackIndex;
    private MediaFormat format = null;
    private long duration = 0;
    public GifExtractor(Context context,String path){
        this.context = context;
        try {
            videoExtractor = new MediaExtractor();
            videoExtractor.setDataSource(path);
            for (int i = 0; i < videoExtractor.getTrackCount(); i++) {
                format = videoExtractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith(VIDEO)) {
                    videoExtractor.selectTrack(i);
                    trackIndex = i;
                    duration = format.getLong(MediaFormat.KEY_DURATION)/1000;
                    break;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public long getDuration() {
        return duration;
    }
    public void encoder(String gifPath){
        encoder(gifPath,0,duration);
    }
    public void encoder(String gifPath,long begin, long end){
        encoder(gifPath,begin,end,0);
    }
    public void encoder(String gifPath,long begin, long end,int fps){
        encoder(gifPath,begin,end,fps,-1,-1);
    }

    public void encoder(final String gifPath, final long begin, final long end, final int fps, final int gifWidth, final int gifHeight){
        if(begin > duration){
            throw new RuntimeException("开始时间不能大于视频时长");
        }
        if(end <= begin){
            throw new RuntimeException("开始时间大于结束时间");
        }
        Thread thread = new Thread(){
            @Override
            public void run() {
                super.run();
                long endTime = duration;
                if (end < duration){
                    endTime = end;
                }
                long time1 = System.currentTimeMillis();
                videoExtractor.seekTo(begin*1000,trackIndex);
                FastYUVtoRGB fastYUVtoRGB = new FastYUVtoRGB(context);

                String mime = format.getString(MediaFormat.KEY_MIME);
                MediaCodec videoDecoder = null;
                try {
                    videoDecoder = MediaCodec.createDecoderByType(mime);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
                videoDecoder.configure(format, null, null, 0);
                videoDecoder.start();

                int width = format.getInteger(MediaFormat.KEY_WIDTH);
                int height = format.getInteger(MediaFormat.KEY_HEIGHT);

                GIFEncoder encoder = null;

                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

                long frameTime = 1000/fps;
                long startTime = begin;
                while (true){
                    int run = extractorVideoInputBuffer(videoExtractor,videoDecoder);
                    if(run == 1){
                        int outIndex = videoDecoder.dequeueOutputBuffer(info, 500000);
                        if(outIndex >= 0){
                            long time = info.presentationTimeUs/1000;
                            if(time >= begin && time <= endTime){
                                if(time >= startTime){
                                    Image image = videoDecoder.getOutputImage(outIndex);
                                    Bitmap bitmap = fastYUVtoRGB.convertYUVtoRGB(getDataFromImage(image),width,height);
                                    if(gifWidth != -1 && gifHeight != -1){
                                        bitmap = Bitmap.createScaledBitmap(bitmap,gifWidth,gifHeight,true);
                                    }else{
                                        bitmap = Bitmap.createScaledBitmap(bitmap,width/4,height/4,true);
                                    }
                                    if(encoder == null){
                                        encoder = new GIFEncoder();
                                        encoder.setFrameRate(fps);
                                        encoder.init(bitmap);
                                        encoder.start(gifPath);
                                    }else{
                                        encoder.addFrame(bitmap);
                                    }
                                    int p = (int) ((startTime - begin)*100/(endTime - begin));
                                    Log.d("====================","p = "+ p);//进度
                                    startTime += frameTime;
                                }

                            }
                            videoDecoder.releaseOutputBuffer(outIndex, true /* Surface init */);
                            if(time >= endTime){
                                break;
                            }
                        }
                    }else if(run == -1){
                        break;
                    }
                }
                if(encoder != null){
                    encoder.finish();
                }
                Log.d("================","encoder->time = "+(System.currentTimeMillis() - time1));
                Log.d("================","over");
                videoDecoder.stop();
                videoDecoder.release();

            }
        };
        thread.start();
    }
    public void release(){
        videoExtractor.release();
    }
    private int extractorVideoInputBuffer(MediaExtractor mediaExtractor,MediaCodec mediaCodec){
        int inputIndex = mediaCodec.dequeueInputBuffer(500000);
        if (inputIndex >= 0) {
            ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputIndex);
            long sampleTime = mediaExtractor.getSampleTime();
            int sampleSize = mediaExtractor.readSampleData(inputBuffer, 0);
            if (mediaExtractor.advance()) {
                mediaCodec.queueInputBuffer(inputIndex, 0, sampleSize, sampleTime, 0);
                return 1;
            } else {
                if(sampleSize > 0){
                    mediaCodec.queueInputBuffer(inputIndex, 0, sampleSize, sampleTime, 0);
                    return 1;
                }else{
                    return -1;
                }

            }
        }
        return 0;
    }
    private byte[] getDataFromImage(Image image) {
        Rect crop = image.getCropRect();
        int format = image.getFormat();
        int width = crop.width();
        int height = crop.height();
        Image.Plane[] planes = image.getPlanes();
        byte[] data = new byte[width * height * ImageFormat.getBitsPerPixel(format) / 8];
        byte[] rowData = new byte[planes[0].getRowStride()];
        int channelOffset = 0;
        int outputStride = 1;
        for (int i = 0; i < planes.length; i++) {
            switch (i) {
                case 0:
                    channelOffset = 0;
                    outputStride = 1;
                    break;
                case 1:
                    channelOffset = width * height + 1;
                    outputStride = 2;
                    break;
                case 2:
                    channelOffset = width * height;
                    outputStride = 2;
                    break;
            }
            ByteBuffer buffer = planes[i].getBuffer();
            int rowStride = planes[i].getRowStride();
            int pixelStride = planes[i].getPixelStride();

            int shift = (i == 0) ? 0 : 1;
            int w = width >> shift;
            int h = height >> shift;
            buffer.position(rowStride * (crop.top >> shift) + pixelStride * (crop.left >> shift));
            for (int row = 0; row < h; row++) {
                int length;
                if (pixelStride == 1 && outputStride == 1) {
                    length = w;
                    buffer.get(data, channelOffset, length);
                    channelOffset += length;
                } else {
                    length = (w - 1) * pixelStride + 1;
                    buffer.get(rowData, 0, length);
                    for (int col = 0; col < w; col++) {
                        data[channelOffset] = rowData[col * pixelStride];
                        channelOffset += outputStride;
                    }
                }
                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length);
                }
            }
        }
        return data;
    }
    private Bitmap compressToJpeg( Image image) {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();

        Rect rect = image.getCropRect();
        YuvImage yuvImage = new YuvImage(getDataFromImage(image), ImageFormat.NV21, rect.width(), rect.height(), null);
        yuvImage.compressToJpeg(rect, 100, outStream);
        return BitmapFactory.decodeByteArray(outStream.toByteArray(),0,outStream.size());
    }
}
