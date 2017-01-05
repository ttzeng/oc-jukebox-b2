package com.intel.otc.brillo.examples;

import android.util.Log;

import java.util.concurrent.TimeUnit;

public class LcdDisplayManager implements Runnable,
        Mp3Player.OnMediaStateChangeListener,
        OcResourceColorRGB.OnRGBChangedListener,
        OcResourceBrightness.OnBrightnessChangeListener
{
    private static final String TAG = LcdDisplayManager.class.getSimpleName();
    private static final int Service_Interval_In_Msec = 500;
    private static final byte charSpeaker = 0;
    private static final byte charmapSpeaker[] = { 0x02, 0x06, 0x1E, 0x1E, 0x1E, 0x06, 0x02, 0x00 };

    private Mp3Player mp3Player;
    private int timeEscapedInMsec = 0;
    private LcdRgbBacklight lcd;

    public LcdDisplayManager(Mp3Player player) {
        mp3Player = player;
        lcd = new LcdRgbBacklight();
    }

    @Override
    public void run() {
        Log.d(TAG, "LCD display manager started");

        lcd.begin(16, 2, LcdRgbBacklight.LCD_5x10DOTS);
        lcd.createChar(charSpeaker, charmapSpeaker);
        boolean showTimeEscaped = false;
        while (true)
            try {
                TimeUnit.MILLISECONDS.sleep(Service_Interval_In_Msec);
                lcd.setCursor(14, 1);
                lcd.write(new String(new char[] { charSpeaker }));
                displayAudioVolume();
                Mp3Player.MediaState state = mp3Player.getCurrentState();
                switch (state) {
                    case Idle:
                        continue;
                    case Playing:
                        timeEscapedInMsec += Service_Interval_In_Msec;
                        showTimeEscaped = true;
                        break;
                    case Paused:
                        showTimeEscaped = !showTimeEscaped;
                        break;
                }
                int second = timeEscapedInMsec / 1000;
                int minute = second / 60;
                second %= 60;
                int hour = minute / 60;
                minute %= 60;
                display(1, showTimeEscaped? (toLeadingZeroNumber(minute) + ":" + toLeadingZeroNumber(second)) : "     ");
            } catch (InterruptedException e) {
                // Ignore sleep nterruption
            }
    }

    @Override
    public void onMediaStateChanged(Mp3Player.MediaState state) {
        switch (state) {
            case Idle:
                lcd.clear();
                timeEscapedInMsec = 0;
                break;
            case Playing:
                display(0, mp3Player.getCurrentTitle());
                break;
        }
    }

    @Override
    public void onBrightnessChanged(int brightness) {
        if (0 <= brightness && brightness <= 100) {
            int c = brightness * 255 / 100;
            lcd.setRGB(c, c, c);
        }
    }

    @Override
    public void onRGBChangedListener(int red, int green, int blue) {
        lcd.setRGB(red, green, blue);
    }

    private void display(int row, String s) {
        lcd.setCursor(0, row);
        lcd.write(s);
    }

    private void displayAudioVolume() {
        int level = Math.round(9f * mp3Player.getCurrentVolume() / mp3Player.getMaxVolume());
        lcd.write((mp3Player.isMuted() || level == 0)? "x" : String.valueOf(level));
    }

    private String toLeadingZeroNumber(int n) {
        n %= 100;
        return ((n < 10)? "0" : "") + n;
    }
}
