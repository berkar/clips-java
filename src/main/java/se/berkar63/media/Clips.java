package se.berkar63.media;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Useful links that helps with file and/or ffmpeg usage:
 * <p>
 * - https://www.labnol.org/internet/useful-ffmpeg-commands/28490/
 * - http://www.tecmint.com/ffmpeg-commands-for-video-audio-and-image-conversion-in-linux/
 * - http://www.oodlestechnologies.com/blogs/8-Useful-FFmpeg-Commands-For-Beginners
 * - https://www.maketecheasier.com/ffmpeg-commands-media-file-conversions/
 * <p>
 * Description of row:
 * -source Test-I.avi -result Test-I.mp4 [-start hh:mm:ss] [-stop hh:mm:ss | -length SECs] [-audio [off | Music-file]] [-fade SECs] [-deshark [1 - 6]]
 */
public class Clips {

    private static String itsFfmpeg = "ffmpeg";

    /**
     * Class for handling of the row that sets in the (default) init.txt
     */
    class Row {
        String itsInfile;   // Video to start with
        String itsStart;    // hh:MM:ss
        String itsStop;     // hh:MM:ss
        String itsLength;   // Number of seconds
        String itsOutfile;  // The vidoe to fix
        String itsAudio;    // Audio either off or the music to use
        String itsFade;     // Fading on bort audio and video, both in/out
        Integer itsDeshark; // Which anti value, to deshark with

        Boolean itsOk = true;
        String itsMessage = "OK";

        Row(String theRow) {
            // -source test/test.mp4 -result target/test.mp4 -start 00:01:25 -stop 30 -audio off -fade off
            if (theRow != null) {
                String[] aTmp = theRow.split(" ");
                int aCount = 0;
                while (aCount < aTmp.length) {
                    if (aTmp[aCount].startsWith("-")) {
                        String aCode = aTmp[aCount++];
                        String aValue = aTmp[aCount++];
                        switch (aCode) {
                            case "-source":
                                itsInfile = aValue;
                                // Check if it exists
                                if (!isFileAvailable(itsInfile)) {
                                    itsOk = false;
                                    itsMessage = "The -source file cannot be found!";
                                }
                                break;
                            case "-result":
                                itsOutfile = aValue;
                                break;
                            case "-start":
                                itsStart = aValue;
                                break;
                            case "-stop":
                                itsStop = aValue;
                                break;
                            case "-length":
                                itsLength = aValue;
                                break;
                            case "-audio":
                                itsAudio = aValue;
                                // Check if it exists
                                if (!itsAudio.equalsIgnoreCase("off") && !isFileAvailable(itsAudio)) {
                                    itsOk = false;
                                    itsMessage = "The -audio file cannot be found!";
                                }
                                break;
                            case "-fade":
                                itsFade = aValue;
                                break;
                            case "-deshark":
                                itsDeshark = Integer.valueOf(aValue);
                                break;
                        }
                    }
                }
                if (itsOk && itsFade != null && itsStop == null && itsLength == null) {
                    itsOk = false;
                    itsMessage = "The -stop OR -length is required when -fade is set!";
                }
                if (itsOk && itsStop != null && itsLength != null) {
                    itsOk = false;
                    itsMessage = "Either -stop OR -length can uses! Not both ...";
                }
            } else {
                itsOk = false;
                itsMessage = "No Row given!";
            }
        }

        @Override
        public String toString() {
            return "-source " + itsInfile + " -result " + itsOutfile +
                    (itsStart != null ? (" -start " + itsStart) : "") +
                    (itsStop != null ? (" -stop " + itsStop) : "") +
                    (itsAudio != null ? (" -audio " + itsAudio) : "") +
                    (itsLength != null ? (" -length " + itsLength) : "") +
                    (itsFade != null ? (" -fade " + itsFade) : "") +
                    (itsDeshark != null ? (" -deshark " + itsDeshark) : "");
        }
    }

    /**
     * The result to represent for each dependent needed
     */
    static class TmpFilename {
        Integer itsCounter = 1;
        String itsPath;
        String itsName;
        String itsPostfix;

        TmpFilename() {
        }

        TmpFilename(String theFilename) {
            // path: all, until the last '/'
            if (theFilename.lastIndexOf("/") >= 0) {
                itsPath = theFilename.substring(0, theFilename.lastIndexOf("/")) + "/tmp";

                Path path = Paths.get(itsPath);
                //if directory exists?
                if (!Files.exists(path)) {
                    try {
                        Files.createDirectories(path);
                    } catch (IOException e) {
                        //fail to create directory
                        e.printStackTrace();
                    }
                }
            }

            // name: until '.'
            itsName = theFilename.substring(theFilename.lastIndexOf("/") + 1, theFilename.lastIndexOf("."));

            // postfix: after '.'
            itsPostfix = theFilename.substring(theFilename.lastIndexOf(".") + 1);
        }

        TmpFilename next() {
            TmpFilename aNext = new TmpFilename();
            aNext.itsCounter = this.itsCounter + 1;
            aNext.itsPath = this.itsPath;
            aNext.itsName = this.itsName;
            aNext.itsPostfix = this.itsPostfix;
            return aNext;
        }

        @Override
        public String toString() {
            return toStringWithPostfix(itsPostfix);
        }

        String toStringWithPostfix(String thePostfix) {
            return (itsPath != null ? (itsPath + "/") : "") + itsName + "_" + itsCounter + "." + thePostfix;
        }
    }

    /**
     * Read from the applied  configuration file, e.g. init.txt
     */
    private Row[] init(String theInitFilename) throws IOException {
        BufferedReader br = null;
        List<Row> aRowList = new ArrayList<>();

        // Read the init file source
        try {
            String aCurrentLine;
            br = new BufferedReader(new FileReader(theInitFilename));
            while ((aCurrentLine = br.readLine()) != null) {
                aCurrentLine = aCurrentLine.trim();
                if (!(aCurrentLine.startsWith("#") || aCurrentLine.length() == 0)) {
                    Row aRow = new Row(aCurrentLine);
                    if (aRow.itsOk) {
                        aRowList.add(aRow);
                    } else {
                        System.err.println("\t" + aRow.itsMessage + " => [" + aRow.toString() + "]");
                    }
                }
            }
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        return aRowList.toArray(new Row[0]);
    }

    /**
     * Take care of the first sub-request, from the request
     */
    private static void processStart(TmpFilename theTmpFilename, Row theRow) throws InterruptedException, IOException {
        List<String> aArgumentList = new ArrayList<>();

        aArgumentList.add(itsFfmpeg);

        aArgumentList.add("-i");
        aArgumentList.add(theRow.itsInfile);

        aArgumentList.add("-c");
        aArgumentList.add("copy");

        aArgumentList.add("-y");

        // Check if the start time is given
        if (theRow.itsStart != null && theRow.itsStart.trim().length() > 0) {
            aArgumentList.add("-ss");
            aArgumentList.add(theRow.itsStart);
        }

        // Check if the stop time is given
        if (theRow.itsStop != null && theRow.itsStop.trim().length() > 0) {
            // Check if stop time is time with 00:00:00 or 00 as secs
            aArgumentList.add("-to");
            aArgumentList.add(theRow.itsStop);
        }

        // Check if the length is given
        if (theRow.itsLength != null && theRow.itsLength.trim().length() > 0) {
            // Found seconds to use
            aArgumentList.add("-t");
            aArgumentList.add(theRow.itsLength);
        }

        if (theTmpFilename != null) {
            aArgumentList.add(theTmpFilename.toString());
        }

        Process aCommand = new ProcessBuilder(aArgumentList).start();
        aCommand.waitFor();
    }

    /**
     * Take care of each sub-request, from the request
     */
    private static void process(TmpFilename theTmpFilename, String... theArguments) throws InterruptedException, IOException {
        List<String> aArgumentList = new ArrayList<>();
        Collections.addAll(aArgumentList, theArguments);
        if (theTmpFilename != null) {
            aArgumentList.add(theTmpFilename.toString());
        }
        Process aCommand = new ProcessBuilder(aArgumentList).start();
        aCommand.waitFor();
    }

    /**
     * Handle of a row request, with several work cases done
     */
    private static void execute(Row theRow) throws InterruptedException, IOException {
        long timeMillis = System.currentTimeMillis();
        long timeMillisTmp = System.currentTimeMillis();
        TmpFilename aTmpFilename = new TmpFilename(theRow.itsOutfile); // Counter == 1

        // Starting
        System.out.printf("\n\t%s\n", theRow.itsInfile);

        // Create the first (start) build clip
        processStart(aTmpFilename, theRow);
        System.out.printf("\t\t[%02ds] First build clip file created! [%s]\n", ((System.currentTimeMillis() - timeMillisTmp) / 1000), aTmpFilename.toString());

        // Check if audio should be removed on the clip
        if (theRow.itsAudio != null && theRow.itsAudio.equalsIgnoreCase("off")) {
            TmpFilename aNext = aTmpFilename.next();
            timeMillisTmp = System.currentTimeMillis();
            process(aNext,
                    itsFfmpeg,
                    "-i", aTmpFilename.toString(),
                    "-an",
                    "-y"
            );
            System.out.printf("\t\t[%02ds] Removing audio!\t[%s]\n", ((System.currentTimeMillis() - timeMillisTmp) / 1000), aNext.toString());
            aTmpFilename = aNext;
        } else {
            // Add other audio to the clip
            if (theRow.itsAudio != null && !theRow.itsAudio.equalsIgnoreCase("off")) {
                // The text has to be the sound file name (incl path)
                TmpFilename aNext = aTmpFilename.next();
                timeMillisTmp = System.currentTimeMillis();
                process(aNext,
                        itsFfmpeg,
                        "-i", aTmpFilename.toString(),
                        "-i", theRow.itsAudio,
                        "-c", "copy",
                        "-map", "0:v", "-map", "1:a", "-shortest",
                        "-y"
                );
                System.out.printf("\t\t[%02ds] Setting audio from: %s\t[%s]\n", ((System.currentTimeMillis() - timeMillisTmp) / 1000), theRow.itsAudio, aNext.toString());
                aTmpFilename = aNext;
            }
        }

        // Check if the fade should be handled (in/out)
        if (theRow.itsFade != null) {
            // Handle the audio to fade in/out (stop is required)
            TmpFilename aNext = aTmpFilename.next();
            Integer aFadeLength = Integer.decode(theRow.itsFade);

            // Create handling of stop or length time
            int aStart = theRow.itsStart != null ? TimeType.getSeconds(theRow.itsStart) : 0;
            Integer aLength = (theRow.itsLength != null) ? Integer.parseInt(theRow.itsLength) : TimeType.getSeconds(theRow.itsStop) - aStart;

            if (theRow.itsAudio != null && !theRow.itsAudio.equalsIgnoreCase("off")) {
                // Only needed if audio is NOT off
                timeMillisTmp = System.currentTimeMillis();
                process(aNext,
                        itsFfmpeg,
                        "-i", aTmpFilename.toString(),
                        "-y",
                        "-af", String.format(
                                "afade=t=in:ss=0:d=%d,afade=t=out:st=%d:d=%d",
                                aFadeLength,
                                aLength - aFadeLength,
                                aFadeLength
                        )
                );
                System.out.printf("\t\t[%02ds] Fade in/out on the audio!\t[%s]\n", ((System.currentTimeMillis() - timeMillisTmp) / 1000), aNext.toString());
                aTmpFilename = aNext;
            }

            // Handle the video to fade in
            aNext = aTmpFilename.next();
            timeMillisTmp = System.currentTimeMillis();
            process(aNext,
                    itsFfmpeg,
                    "-i", aTmpFilename.toString(),
                    "-y",
                    "-vf", String.format(
                            "fade=in:0:%d",
                            aFadeLength * 30 // Nr of frames (1s == 30frames)
                    )
            );
            System.out.printf("\t\t[%02ds] Fade in on the video!\t[%s]\n", ((System.currentTimeMillis() - timeMillisTmp) / 1000), aNext.toString());
            aTmpFilename = aNext;

            // Handle the video to fade out
            aNext = aTmpFilename.next();
            timeMillisTmp = System.currentTimeMillis();
            process(aNext,
                    itsFfmpeg,
                    "-i", aTmpFilename.toString(),
                    "-y",
                    "-vf", String.format(
                            "fade=out:%d:%d",
                            (aLength - aFadeLength) * 30, // Nr of frames (1s == 30frames)
                            aFadeLength * 30 // Nr of frames (1s == 30frames)
                    )
            );
            System.out.printf("\t\t[%02ds] Fade out on the video!\t[%s]\n", ((System.currentTimeMillis() - timeMillisTmp) / 1000), aNext.toString());
            aTmpFilename = aNext;
        }

        // Check if the video is to be deshake
        if (theRow.itsDeshark != null && theRow.itsDeshark > 0) {
            // Create the prepare file
            TmpFilename aNext = aTmpFilename.next();
            timeMillisTmp = System.currentTimeMillis();
            process(null,
                    itsFfmpeg,
                    "-i", aTmpFilename.toString(),
                    "-y",
                    "-vf", String.format(
                            "vidstabdetect=stepsize=6:shakiness=%d:accuracy=9:result=%s",
                            theRow.itsDeshark,
                            aNext.toStringWithPostfix("trf")
                    ),
                    "-f", "null", "-"
            );
            System.out.printf("\t\t[%02ds] Creating CTRL file for deshark'ing!\t[%s]\n", ((System.currentTimeMillis() - timeMillisTmp) / 1000), aNext.toStringWithPostfix("trf"));

            // Execute deshark
            timeMillisTmp = System.currentTimeMillis();
            process(aNext,
                    itsFfmpeg,
                    "-i", aTmpFilename.toString(),
                    "-y",
                    "-vf", String.format(
                            "vidstabtransform=input=%s:zoom=1:smoothing=30,unsharp=5:5:0.8:3:3:0.4",
                            aNext.toStringWithPostfix("trf")
                    ),
                    "-vcodec", "libx264",
                    "-preset", "slow",
                    "-tune", "film",
                    "-crf", "18",
                    "-acodec", "copy"
            );
            System.out.printf("\t\t[%02ds] Creating deshark'd clip!\t[%s]\n", ((System.currentTimeMillis() - timeMillisTmp) / 1000), aNext.toString());

            aTmpFilename = aNext;
        }

        // Fix the resulting filename from the latest build clip
        timeMillisTmp = System.currentTimeMillis();
        new ProcessBuilder(
                "cp",
                aTmpFilename.toString(),
                theRow.itsOutfile
        ).start();
        System.out.printf("\t\t[%02ds] Copy from [%s] to [%s]!\n", ((System.currentTimeMillis() - timeMillisTmp) / 1000), aTmpFilename.toString(), theRow.itsOutfile);

        System.out.printf("\t%s\t[[ %ds ]]\n", theRow.itsOutfile, ((System.currentTimeMillis() - timeMillis) / 1000));
    }

    /**
     * A Java startup method. Most requested due to test implementation
     */
    static void run(String theInputFile) throws InterruptedException, IOException {
        Row[] aResult = new Clips().init(theInputFile);
        if (aResult.length > 0) {
            for (Row aRow : aResult) {
                execute(aRow);
            }
        }
    }

    /**
     * The ordinary main, that's required a configurations file
     */
    public static void main(String[] args) throws InterruptedException, IOException {
        System.out.println("Starting!");
        long timeMillis = System.currentTimeMillis();
        String aInputFile = "init.txt";
        if (args != null && args.length > 0) {
            aInputFile = args[0];
        }
        if (aInputFile.startsWith("-h")) {
            // Write the help ...
            System.out.println("\nDescription of a config row:");
            System.out.println("\t-source Input-file -result Output-file -start hh:mm:ss -stop SECs [-audio [off | Music-file]] [-fade SECs] [-deshark [1 - 6]]\n");
        } else {
            // Check if the file exists!
            if (isFileAvailable(aInputFile)) {
                run(aInputFile);
            }
        }
        System.out.printf("\nFinished: [[[ %ds ]]]\n", ((System.currentTimeMillis() - timeMillis) / 1000));
    }

    private static boolean isFileAvailable(String theFilename) {
        // Check if the file exists!
        File aTmp = new File(theFilename);
        return (aTmp.exists() && !aTmp.isDirectory());
    }
}
