package com.coderpage.mine.app.tally.common.permission;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by joker on 2017/8/17.
 */

public class AudioRecordManager {
    public File file;
    private AudioRecord mRecorder;
    private DataOutputStream dos;
    private Thread recordThread;
    private volatile boolean isStart = false;
    private int bufferSize;
    /**
     * record thread
     */
    Runnable recordRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                if (mRecorder == null || mRecorder.getState() != AudioRecord.STATE_INITIALIZED) {
                    return;
                }
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
                int bytesRecord;
                byte[] tempBuffer = new byte[bufferSize];
                mRecorder.startRecording();
                while (isStart) {
                    if (mRecorder != null) {
                        bytesRecord = mRecorder.read(tempBuffer, 0, bufferSize);
                        if (bytesRecord == AudioRecord.ERROR_INVALID_OPERATION || bytesRecord ==
                                AudioRecord.ERROR_BAD_VALUE) {
                            continue;
                        }
                        if (bytesRecord != 0 && bytesRecord != -1) {
                            dos.write(tempBuffer, 0, bytesRecord);
                        } else {
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    };
    private long length;

    public AudioRecordManager() {
        bufferSize = AudioRecord.getMinBufferSize(8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat
                .ENCODING_PCM_16BIT);
        if (bufferSize == AudioRecord.ERROR_BAD_VALUE || bufferSize == AudioRecord.ERROR) {
            bufferSize = 4096;
        }
        mRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, 8000, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize * 2);
        if (mRecorder.getState() != AudioRecord.STATE_INITIALIZED) {
            mRecorder.release();
            mRecorder = null;
        }
    }

    public boolean getSuccess() {
        return length > 0;
    }

    /**
     * destroy record thread
     */
    private void destroyThread() {
        try {
            isStart = false;
            if (recordThread != null && recordThread.getState() != Thread.State.TERMINATED) {
                try {
                    recordThread.interrupt();
                } catch (Exception e) {
                    e.printStackTrace();
                    recordThread = null;
                }
            }
            recordThread = null;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            recordThread = null;
        }
    }

    /**
     * start record thread
     */
    private void startThread() {
        isStart = true;
        if (recordThread == null) {
            recordThread = new Thread(recordRunnable);
            recordThread.start();
        }
    }

    /**
     * save file
     */
    private void setPath(String path) throws IOException {
        file = new File(path);
        deleteFile();
        file.createNewFile();
        dos = new DataOutputStream(new FileOutputStream(file, true));
    }

    /**
     * start record
     */
    public void startRecord(String path) throws IOException, InterruptedException {
        setPath(path);
        startThread();
    }

    /**
     * stop record
     */
    public void stopRecord() throws IOException, InterruptedException {
        // specially for OPPO、XIAOMI、MEIZU、HUAWEI and so on
        Thread.sleep(250);
        destroyThread();
        if (mRecorder != null) {
            if (mRecorder.getState() == AudioRecord.STATE_INITIALIZED) {
                mRecorder.stop();
            }
            if (mRecorder != null) {
                mRecorder.release();
            }
        }
        closeOutputStream();
        length = file.length();
        deleteFile();
    }

    /**
     * release resources if stopRecord was never called
     */
    public void release() {
        destroyThread();
        if (mRecorder != null) {
            try {
                if (mRecorder.getState() == AudioRecord.STATE_INITIALIZED) {
                    mRecorder.stop();
                }
                mRecorder.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mRecorder = null;
        }
        closeOutputStream();
    }

    private void closeOutputStream() {
        if (dos != null) {
            try {
                dos.flush();
                dos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            dos = null;
        }
    }

    private void deleteFile() {
        if (file.exists()) {
            file.delete();
        }
    }
}
