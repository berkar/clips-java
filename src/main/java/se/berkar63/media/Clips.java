package se.berkar63.media;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Clips {

    static String itsFfmpeg = "ffmpeg";

    /**
     * Class for handling of the row that sets in the (default) init.txt
     */
    class Row {
        String itsInfile;
        String itsStart;
        String itsStop;
        String itsOutfile;
        String itsAudio;
        String itsFade;
        Integer itsDeshark;

        Boolean itsOk = true;

        public Row(String theRow) {
            // -source test/test.mp4 -result target/test.mp4 -start 00:01:25 -stop 30 -audio off -fade off
            String[] aTmp = theRow.split(" ");
            if (aTmp != null) {
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
                            case "-audio":
                                itsAudio = aValue;
                                // Check if it exists
                                if (!itsAudio.equalsIgnoreCase("off") && !isFileAvailable(itsAudio)) {
                                    itsOk = false;
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
            }
        }

        @Override
        public String toString() {
            return "-source " + itsInfile + " -result " + itsOutfile + " -start " + itsStart + " -stop " + itsStop + (itsAudio != null ? (" -audio " + itsAudio) : "") + (itsFade != null ? (" -fade " + itsFade) : "");
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

        public String toStringWithPostfix(String thePostfix) {
            return (itsPath != null ? (itsPath + "/") : "") + itsName + "_" + itsCounter + "." + thePostfix;
        }
    }

    /**
     * Read from the applied  configuration file, e.g. init.txt
     */
    Row[] init(String theInitFilename) {
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
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
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
     * Take care of each sub-request, from the request
     */
    static void process(TmpFilename theTmpFilename, String... theArguments) {
        Process aCommand;
        try {
            List<String> aArgumentList = new ArrayList<>();
            for (int i = 0; i < theArguments.length; i++) {
                aArgumentList.add(theArguments[i]);
            }
            if (theTmpFilename != null) {
                aArgumentList.add(theTmpFilename.toString());
            }
            aCommand = new ProcessBuilder(aArgumentList).start();
            aCommand.waitFor();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handle of a row request, with several work cases done
     */
    static void execute(Row theRow) throws IOException {
        Process aCommand;
        TmpFilename aTmpFilename = new TmpFilename(theRow.itsOutfile); // Counter == 1

        // Create the first build clip
        process(aTmpFilename,
                itsFfmpeg,
                "-i", theRow.itsInfile,
                "-c", "copy",
                "-y",
                "-ss", theRow.itsStart,
                "-t", theRow.itsStop
        );

        // Check if audio should be removed on the clip
        if (theRow.itsAudio != null && theRow.itsAudio.equalsIgnoreCase("off")) {
            TmpFilename aNext = aTmpFilename.next();
            process(aNext,
                    itsFfmpeg,
                    "-i", aTmpFilename.toString(),
                    "-an",
                    "-y"
            );
            aTmpFilename = aNext;
        } else {
            // Add other audio to the clip
            if (theRow.itsAudio != null && !theRow.itsAudio.equalsIgnoreCase("off")) {
                // The text has to be the sound file name (incl path)
                TmpFilename aNext = aTmpFilename.next();
                process(aNext,
                        itsFfmpeg,
                        "-i", aTmpFilename.toString(),
                        "-i", theRow.itsAudio,
                        "-c", "copy",
                        "-map", "0:v", "-map", "1:a", "-shortest",
                        "-y"
                );
                aTmpFilename = aNext;
            }
        }

        // Check if the fade should be handled (in/out)
        if (theRow.itsFade != null) {
            // Handle the audio to fade in/out
            TmpFilename aNext = aTmpFilename.next();
            Integer aFadeLength = Integer.decode(theRow.itsFade);
            Integer aLength = Integer.decode(theRow.itsStop);

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
            aTmpFilename = aNext;

            // Handle the video to fade in
            aNext = aTmpFilename.next();
            process(aNext,
                    itsFfmpeg,
                    "-i", aTmpFilename.toString(),
                    "-y",
                    "-vf", String.format(
                            "fade=in:0:%d",
                            aFadeLength * 30 // Nr of frames (1s == 30frames)
                    )
            );
            aTmpFilename = aNext;

            // Handle the video to fade out
            aNext = aTmpFilename.next();
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
            aTmpFilename = aNext;
        }

        // Check if the video is to be deshake
        if (theRow.itsDeshark != null && theRow.itsDeshark > 0) {
            // Create the prepare file
            TmpFilename aNext = aTmpFilename.next();
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

            // Execute deshark
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

            aTmpFilename = aNext;
        }

        // Fix the resulting filename from the latest build clip
        aCommand = new ProcessBuilder(
                "cp",
                aTmpFilename.toString(),
                theRow.itsOutfile
        ).start();

    }

    /**
     * A Java startup method. Most requested due to test implementation
     */
    static void run(String theInputFile) throws IOException {
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
    public static void main(String[] args) throws IOException {
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
        System.out.printf("Finished: [%ds]\n", ((System.currentTimeMillis() - timeMillis) / 1000));
    }

    static boolean isFileAvailable(String theFilename) {
        // Check if the file exists!
        File aTmp = new File(theFilename);
        if (aTmp.exists() && !aTmp.isDirectory()) {
            return true;
        }
        System.out.println("File is NOT correct: " + theFilename);
        return false;
    }
}
