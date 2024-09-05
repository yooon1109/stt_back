package com.example.stt.infrastructure.audio;

import javax.sound.sampled.*;

public final class MicrophoneStreamer {
    private static final int SAMPLE_RATE = 8000;
    private static final int BITS_PER_SAMPLE = 16;
    private static final int BUFFER_SIZE = 1024;

    private TargetDataLine microphone;
    private AudioFormat format;

    public MicrophoneStreamer() throws LineUnavailableException {
        format = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                SAMPLE_RATE,
                BITS_PER_SAMPLE,
                1,
                1 * (BITS_PER_SAMPLE / 8),
                SAMPLE_RATE,
                false);

        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        if (!AudioSystem.isLineSupported(info)) {
            throw new LineUnavailableException("Microphone not supported");
        }

        microphone = (TargetDataLine) AudioSystem.getLine(info);
        microphone.open(format);
        microphone.start();
    }

    public int read(byte[] b) {
        return microphone.read(b, 0, b.length);
    }

    public void close() {
        microphone.stop();
        microphone.close();
    }
}