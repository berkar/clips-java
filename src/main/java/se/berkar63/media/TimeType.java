package se.berkar63.media;

/**
 * Helper of the hh:MM:ss - seconds handling
 */
public class TimeType {

    private TimeType() {
    }

    static Integer getSeconds(String theValue) {
        if (theValue.contains(":")) {

            int aHours = 0;
            int aMinutes = 0;
            int aSeconds = 0;

            if (theValue.length() == 8) {
                // 00:00:00
                aHours = Integer.parseInt(theValue.substring(0, 2));
                aMinutes = Integer.parseInt(theValue.substring(3, 5));
                aSeconds = Integer.parseInt(theValue.substring(6));
            } else if (theValue.length() == 5) {
                // 00:00
                aMinutes = Integer.parseInt(theValue.substring(0, 2));
                aSeconds = Integer.parseInt(theValue.substring(3));
            } else {
                System.err.println("Wrong type of time: [" + theValue + "] Correct with [00:00:00 | 00:00 | 00]");
            }

            return aHours * 3600 + aMinutes * 60 + aSeconds;
        } else {
            return Integer.parseInt(theValue);
        }
    }

    static String getTime(Integer theSeconds) {
        return String.format("%02d:%02d:%02d",
                (theSeconds / 3600),
                (theSeconds / 60) % 60,
                theSeconds % 60
        );
    }

    public static void main(String[] theArgs) {
        test("00:00:30", "00:30", "30");
        test("00:02:05", "02:05", "125");
        test("01:01:05", "61:05", "3665");
    }

    //

    /**
     * Check that theClassicTime and theSeconds are correct in the flow
     */
    private static void test(String theClassicTime, String theSmallTime, String theSeconds) {
        String aLocalTime;
        Integer aSeconds;

        System.out.printf("[%s]\t-> %ds -> [%s] => [%s]\n",
                theClassicTime,
                (aSeconds = TimeType.getSeconds(theClassicTime)),
                (aLocalTime = TimeType.getTime(aSeconds)),
                (theClassicTime.equals(aLocalTime) && theSeconds.equals(aSeconds.toString()) ? "OK" : "NOK")
        );
        System.out.printf("[%s]\t\t-> %ds -> [%s] => [%s]\n",
                theSmallTime,
                (aSeconds = TimeType.getSeconds(theSmallTime)),
                (aLocalTime = TimeType.getTime(aSeconds)),
                (theClassicTime.equals(aLocalTime) && theSeconds.equals(aSeconds.toString()) ? "OK" : "NOK")
        );
        System.out.printf("[%s]\t\t-> %ds -> [%s] => [%s]\n\n",
                theSeconds,
                (aSeconds = TimeType.getSeconds(theSeconds)),
                (aLocalTime = TimeType.getTime(aSeconds)),
                (theClassicTime.equals(aLocalTime) && theSeconds.equals(aSeconds.toString()) ? "OK" : "NOK")
        );
    }
}
