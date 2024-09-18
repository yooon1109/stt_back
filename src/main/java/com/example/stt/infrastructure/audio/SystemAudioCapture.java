package com.example.stt.infrastructure.audio;

import javax.sound.sampled.*;

public class SystemAudioCapture {
    private TargetDataLine targetLine;

    public void systemAudio() {
        try {
            AudioFormat format = new AudioFormat(44100, 16, 2, true, true);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)) {
                System.out.println("Line not supported");
                System.exit(0);
            }

            targetLine = (TargetDataLine) AudioSystem.getLine(info);
            targetLine.open(format);
            targetLine.start();

//            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
//            line.open(format);
//            line.start();

            byte[] buffer = new byte[4096];
            int bytesRead;

//            while (true) {
//
//                // Process the audio data here
//            }

        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }
    public int read(byte[] b) {
        return targetLine.read(b, 0, b.length);
    }
}
