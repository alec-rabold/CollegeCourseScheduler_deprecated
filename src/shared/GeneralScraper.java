package shared;

import java.net.*;
import java.text.NumberFormat;
import java.util.*;
import java.io.*;

public abstract class GeneralScraper {
    protected PrintWriter out;

    public String REGISTRATION_SEARCH_PAGE;

    protected UserOptions userOptions = new UserOptions();

    protected Map<Integer, List<Course>> countIndexedCourses = new TreeMap<>();
    protected List<List<Course>> sizeSortedCourses = new ArrayList<>();
    protected List<String> chosenCourses = new ArrayList<>();
    protected int numChosenCourses = 0;

    protected URL Registration_URL;
    protected String parameters;
    protected boolean departmentSet = false;
    protected String departmentSingle = "";
    protected int sectionMeetingCounter = 0;

    protected long numValidSchedules = 0;
    protected long numPermutations = 0;

    protected long timeStart = System.currentTimeMillis();
    protected long timeEnd = 0L;

    public class UserOptions {
        /** Professors */
        public String[] wantedProfessors = null;
        public void setWantedProfessors(String[] professors) {
            this.wantedProfessors = professors;
        }
        public String[] unwantedProfessors = null;
        public void setUnwantedProfessors(String[] professors) {
            this.unwantedProfessors = professors;
        }
        public String[] excludeProfessors = null;
        public void setExcludedProfessors(String[] professors) {
            this.excludeProfessors = professors;
        }
        /** Unavailable times */
        public long[] unavTimesBitBlocks = null;
        public void setUnavTimesBitBlocks(long[] unavTimes) {
            this.unavTimesBitBlocks = unavTimes;
        }
        /** Show waitlisted classes */
        public boolean showWaitlisted = true;
        public void setShowWaitlisted(String bool) {
            if(bool.equals("false")) this.showWaitlisted = false;
        }
        /** Show online (hybrid) classes */
        public boolean showOnlineClasses = true;
        public void setShowOnlineClasses(String bool) {
            if(bool.equals("false")) this.showOnlineClasses = false;
        }
        /** Tight vs. relaxed schedule */
        public int spreadPreference = 1;
        public void setSpreadPreference(String preference) {
            if(preference.equals("relaxed")) this.spreadPreference = -1;
        }
        /** Shorter classes vs. fewer days */
        public int numDaysPreference = 1;
        public void setNumDaysPreference(String preference) {
            if(preference.equals("more")) this.spreadPreference = -1;
        }
    }

    protected boolean isMobile = false;
    protected void setMobile(String condition) {
        if(condition.equals("true")) isMobile = true;
    }

    public class Course {
        public String courseID;
        public String schedNumber;
        public String title;
        public  String units;
        public  String seats;
        public Course relatedCourse; // So far, only necessary for UCSB (due to poor registration interface)
        public  List<String> locations = new ArrayList<>();
        public  List<String> times = new ArrayList<String>();
        public  List<String> instructors = new ArrayList<String>();
        public  List<String> days = new ArrayList<String>();
        public boolean isComplete() {
            return (courseID != null && schedNumber != null && title != null && units != null &&
                    seats != null && !times.isEmpty());
        }
    }

    public class Schedule implements Comparable<Schedule> {
        public  List<Course> courses;
        public  int numGoodProfessors;
        public  int numBadProfessors;
        public  int timeTightness;
        public  int numDays;
        public  Integer algorithmValue;
        public  Schedule(List<Course> courses, int numGoodProf, int numBadProf, int timeTightness, int numDays) {
            this.courses = courses;
            this.numGoodProfessors = numGoodProf;
            this.numBadProfessors = numBadProf;
            this.timeTightness = timeTightness;
            this.numDays = numDays;
            this.algorithmValue = (userOptions.spreadPreference * 2 * timeTightness) - 45*numGoodProf + 55*numBadProf + (userOptions.numDaysPreference * (numDays * 30));
        }
        @Override
        public int compareTo(Schedule b) {
            // return this.algorithmValue.compareTo(b.algorithmValue);
            return b.algorithmValue.compareTo(this.algorithmValue);
        }
    }

    public abstract void iterateInput(String[] chosenCourses, PrintWriter outWriter) throws Exception;

    protected abstract void parseRegistrationData(String department) throws Exception;

    protected abstract int[] convDaysToArray(String days);

    protected abstract void createSizeSortedCourses();

    protected abstract void appendParameter(String addParam);

    protected abstract void setTerm(String season, String year);

    protected abstract void setSearch(String s) throws MalformedURLException;

    protected abstract String formatURL(String url);

    protected abstract int rowspanFormula(int startHour, int startMin, int endHour, int endMin);

    protected String getParameters() {
        return this.parameters;
    }

    /** Analyze and rank all valid permutations */
    public final void analyzePermutations() {
        try {
            createSizeSortedCourses();

            int numCourses = this.sizeSortedCourses.size();
            this.numChosenCourses = numCourses;

            // "Better" classes will bubble up with a limit of 25
            PriorityQueue<Schedule> validSchedules = new PriorityQueue<>();

            // Sort the course lists by size to allow for efficient dynamic programming techniques
            Collections.sort(sizeSortedCourses, new Comparator<List>(){
                public int compare(List a1, List a2) {
                    return a1.size() - a2.size(); // assumes you want biggest to smallest
                }
            });

            // Insert into map indexed by number of courses
            for(int i = 0; i < sizeSortedCourses.size(); i++) {
                countIndexedCourses.put(i, sizeSortedCourses.get(i));
            }

            // Speed up permutations with dynamic programming
            Map<String, long[]> storedTimeblocks = new HashMap<>();

            int[] iterationVariables = new int[numCourses];
            long[] badBlock = {-1L, -1L, -1L, -1L, -1L};

            permutations:
            while (iterationVariables[0] < (countIndexedCourses.get(0).size())) {
                if(Thread.interrupted()) {
                    printTables(validSchedules, true);
                    return;
                }
                this.numPermutations++;
                if(numPermutations % 500000 == 0) {
                    System.out.println(numPermutations);
                }
                List<Course> temp = new ArrayList<>();
                boolean schedulesCollide = false;
                long[] combinedTimeBlocks = new long[5]; // 5 days

                for(int i = 0; i < numCourses; i++)
                    temp.add(countIndexedCourses.get(i).get(iterationVariables[i]));

                StringBuilder permutationString = new StringBuilder();
                for(int i = Math.max((numCourses - 2), 0); i >= 0; i--) permutationString.append(iterationVariables[i]); // 000000, 000001, 000002
                String permString = permutationString.toString();

                /** This section is primarily for UCSB's unique registration interface */
                for(Course crs : temp) {
                    if(crs.relatedCourse != null) {
                        // Make sure that the string of classes contains the relatedCourse
                        if(!temp.contains(crs.relatedCourse)) {
                            if(permString.length() > 2) storedTimeblocks.put(permString, badBlock); //TODO: Check this works with 3+ classes
                            iterationVariables = incrementIterationVariables(iterationVariables);
                            continue permutations;
                        }
                    }
                }
                /** End section */

                // Re-use and recycle
                if(storedTimeblocks.containsKey(permString)) {
                    long[] curBlock = storedTimeblocks.get(permString);
                    if(Arrays.equals(curBlock,badBlock)){
                        iterationVariables = incrementIterationVariables(iterationVariables);
                        continue;
                    }
                    else combinedTimeBlocks = storedTimeblocks.get(permString);
                }
                // Create reusable timeblock
                else {
                    iterateBlocks:
                    for(int i = Math.max((numCourses - 2), 0); i >= 0; i--) {
                        for (int multipleDaySlots = 0; multipleDaySlots < countIndexedCourses.get(i).get(iterationVariables[i]).days.size(); multipleDaySlots++) {
                            int[] daysArray = convDaysToArray(countIndexedCourses.get(i).get(iterationVariables[i]).days.get(multipleDaySlots));
                            for (int j = 0; j < daysArray.length; j++) {
                                if (daysArray[j] == 1) {
                                    long bits = convertTimesToBits(countIndexedCourses.get(i).get(iterationVariables[i]).times.get(multipleDaySlots));
                                    if ((bits & combinedTimeBlocks[j]) != 0) {
                                        storedTimeblocks.put(permString, badBlock);
                                        schedulesCollide = true;
                                        iterationVariables = incrementIterationVariables(iterationVariables);
                                        break iterateBlocks;
                                    } else {
                                        combinedTimeBlocks[j] = (bits | combinedTimeBlocks[j]);
                                    }
                                }
                            }
                        }
                    }
                    if(!schedulesCollide) storedTimeblocks.put(permString, Arrays.copyOf(combinedTimeBlocks, combinedTimeBlocks.length));
                }

                // Check the class[0 - (n-2] against the class[(n-1)]
                // Saves times since class[(n-1)] has the largest size() of all courses
                // and class[(n-1)] timeblocks easily accessed from a map
                if(numCourses > 1) {
                    int lastCourse = numCourses - 1;
                    checkLast:
                    for (int multipleDaySlots = 0; multipleDaySlots < countIndexedCourses.get(lastCourse).get(iterationVariables[lastCourse]).days.size(); multipleDaySlots++) {
                        int[] daysArray = convDaysToArray(countIndexedCourses.get(lastCourse).get(iterationVariables[lastCourse]).days.get(multipleDaySlots));
                        for (int i = 0; i < daysArray.length; i++) {
                            if (daysArray[i] == 1) {
                                long bits = convertTimesToBits(countIndexedCourses.get(lastCourse).get(iterationVariables[lastCourse]).times.get(multipleDaySlots));
                                if ((bits & combinedTimeBlocks[i]) != 0) {
                                    // storedTimeblocks.put(permString, badBlock);
                                    schedulesCollide = true;
                                    break checkLast;
                                } else {
                                    combinedTimeBlocks[i] = (bits | combinedTimeBlocks[i]);
                                }
                            }
                        }
                    }
                }

                iterationVariables = incrementIterationVariables(iterationVariables);

                /** USER PREFERENCES */
                // "If current schedule is valid"
                if (!schedulesCollide) {
                    boolean addToSchedule = true;

                    /** Check if conflicts with user's unavailable time(s) */
                    for (int i = 0; i < 5; i++) {
                        if ((userOptions.unavTimesBitBlocks[i] & combinedTimeBlocks[i]) != 0)
                            addToSchedule = false;
                    }

                    /** Schedule Distance (tightness) */
                    int scheduleDistance = 0;
                    for (int whichDay = 0; whichDay < 5; whichDay++) {
                        StringBuilder timeDistance = new StringBuilder(Long.toBinaryString(combinedTimeBlocks[whichDay]));
                        // Remove leading and trailing zeros
                        if (timeDistance.length() > 1) {
                            for (int i = 0; timeDistance.charAt(i) == '0'; i++)
                                timeDistance.deleteCharAt(i);
                            for (int i = timeDistance.length() - 1; timeDistance.charAt(i) == '0'; i--)
                                timeDistance.deleteCharAt(i);
                            // Count number of zeros (indicates distance in class times) [less zeros == tighter schedule]
                            for (int i = 0; i < timeDistance.length(); i++)
                                if (timeDistance.charAt(i) == '0') scheduleDistance++;
                        }
                    }

                    // Schedule variables
                    int numWanted = 0;
                    int numUnwanted = 0;
                    int numDays = 0;
                    breakLoop:
                    for (Course crs : temp) {
                        /** Tally number of days in schedule */
                        for (String days : crs.days)
                            numDays = numDays + days.length();

                        /** Analyze user's optional preferences */
                        if (optionsToCheck()) {
                            for (String professor : crs.instructors) {

                                // A. Narang --> Narang, A.
                                professor = (professor.substring(3) + ", " + professor.substring(0, 3)).toUpperCase().trim();

                                /** Prioritize preferred professors */
                                if (userOptions.wantedProfessors != null) {
                                    for (String wantedProfessor : userOptions.wantedProfessors)
                                        if (professor.equals(wantedProfessor.toUpperCase().trim())) numWanted++;
                                }
                                /** De-prioritize unwanted professors */
                                if (userOptions.unwantedProfessors != null) {
                                    for (String unwantedProfessor : userOptions.unwantedProfessors)
                                        if (professor.equals(unwantedProfessor.toUpperCase().trim())) numUnwanted++;
                                }
                                /** Exclude professors */
                                if (userOptions.excludeProfessors != null) {
                                    for (String excludedProfessor : userOptions.excludeProfessors) {
                                        if (professor.equals(excludedProfessor.toUpperCase().trim())) {
                                            addToSchedule = false;
                                            break breakLoop;
                                        }
                                    }
                                }
                            }
                            /** Remove waitlisted classes */
                            if (!userOptions.showWaitlisted) {
                                int numSeatsAvailable = Integer.parseInt(crs.seats.substring(0, crs.seats.indexOf("/")));
                                if (numSeatsAvailable <= 0) {
                                    addToSchedule = false;
                                    break breakLoop;
                                }
                            }
                            /** Remove hybrid/online classes */
                            if (!userOptions.showOnlineClasses) {
                                for (String location : crs.locations) {
                                    if (location.contains("ON-LINE")) {
                                        addToSchedule = false;
                                        break breakLoop;
                                    }
                                }
                            }
                        }
                    }
                    // Add if fits all preferences
                    if (addToSchedule) {
                        Schedule sched = new Schedule(temp, numWanted, numUnwanted, scheduleDistance, numDays); //allows algorithm to update before adding to PQ
                        validSchedules.add(sched);
                        this.numValidSchedules++;
                        // Keep the number of schedules equal to 25 (or less)
                        if (validSchedules.size() > 25)
                            validSchedules.poll();
                    }
                }
            }
            printTables(validSchedules, false);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    /** Print 25 (or less) carousel tables of schedules */
    protected final void printTables(PriorityQueue<Schedule> validSchedules, boolean wasInterrupted) {
        int size = validSchedules.size();

        List<List<Course>> correctOrder = new ArrayList<>();
        for(int i = size-1; i >= 0; i--) {
            correctOrder.add(0, validSchedules.poll().courses);
        }

        /** Tables header */
        out.println("<div class='block-area'>");
        out.println("<div class='row'>");
        out.println("<div class='col-xs-12' style='text-align:center'>");
        out.println("<h2 class='numSched'>Schedule <span id='numSched' class='numSched' data-count='1'>" + Math.min(1, size) + "</span> of " + Math.min(25, size) + "</h2>");
        if(!wasInterrupted) out.println("<i>There are " + NumberFormat.getNumberInstance(Locale.US).format(this.numValidSchedules) + " valid permutations of your schedule - we filtered out " + NumberFormat.getNumberInstance(Locale.US).format((numPermutations - numValidSchedules)) + " that didn't work</i>");
        else out.println("<i>There were too many permutations for your schedule so we optimized the first " + NumberFormat.getNumberInstance(Locale.US).format(numPermutations) + "." +  "<br>We found " + NumberFormat.getNumberInstance(Locale.US).format(this.numValidSchedules) + " valid permutations of your schedule and filtered out " + NumberFormat.getNumberInstance(Locale.US).format((numPermutations - numValidSchedules)) + " that didn't work</i>");
        out.println("<br>");
        out.println("<i id='numValid' data-count='" + Math.min(25, size) + "'>Check out your top " + Math.min(25, size) + "!</i>");
        out.println("</div>");
        out.println("</div>");
        out.println("<div id='myCarousel' class='carousel slide' data-ride='carousel' data-interval='false'>");
        out.println("<div class='carousel-inner'>");

        /** Create 25 (or less) tables */
        Map<String, Integer> courseColors = new HashMap<>();
        for(int numTables = 0; numTables < (Math.min(25, size)); numTables++) {
            List<Course> curSchedule = correctOrder.get(numTables);

            if (numTables == 0)
                out.println("<div class='item active'>");
            else
                out.println("<div class='item'>");

            out.println("<table id='optimized-table'>");
            out.println("<tr>");
            out.println("<th class='opt-table-label opt-table-time'></th>");
            if(!isMobile) {
                out.println("<th class='opt-table-label'>Monday</th>");
                out.println("<th class='opt-table-label'>Tuesday</th>");
                out.println("<th class='opt-table-label'>Wednesday</th>");
                out.println("<th class='opt-table-label'>Thursday</th>");
                out.println("<th class='opt-table-label'>Friday</th>");
            }
            else if(isMobile) {
                out.println("<th class='opt-table-label'>M</th>");
                out.println("<th class='opt-table-label'>T</th>");
                out.println("<th class='opt-table-label'>W</th>");
                out.println("<th class='opt-table-label'>Th</th>");
                out.println("<th class='opt-table-label'>F</th>");
            }
            out.println("</tr>");

            int[] filledDays = new int[6]; // to keep track of column offset due to rowspan attribute
            for (int i = 800; i < 2100; i += 15) {
                String time = ((Integer) i).toString();
                boolean bottomBorder = true;
                out.println("<tr>");
                if (time.substring(time.length() - 2).equals("00") || time.substring(time.length() - 2).equals("30")) {
                    out.println("<td rowspan='2' class='opt-table-label opt-table-time'>" + time24to12(i) + "</td>");
                    bottomBorder = false;
                }
                for (int day = 0; day < 5; day++) {
                    boolean filled = false;
                    for (Course course : curSchedule) {
                        int sections = Math.max(course.locations.size(),Math.max(course.instructors.size(),Math.max(course.days.size(),course.times.size())));
                        for (int j = 0; j < sections; j++) {
                            String course_instructor = null;
                            String course_days = null;
                            String course_time = null;
                            String course_location = null;
                            int course_num = j;

                            // Handles irregularities in WebPortal registration
                            while(course_instructor == null || course_days == null || course_time == null || course_location == null) {
                                if(course_instructor == null) {
                                    try {
                                        course_instructor = course.instructors.get(course_num);
                                    }
                                    catch(Exception e) {
                                        course_instructor = "[No professor found]";
                                    }
                                }
                                if(course_days == null) {
                                    try {
                                        course_days = course.days.get(course_num);
                                    }
                                    catch(Exception e) {
                                        course_days = "[No days found]";
                                    }
                                }
                                if(course_time == null) {
                                    try {
                                        course_time = course.times.get(course_num);
                                    }
                                    catch(Exception e) {
                                        course_time = "[No times found]";
                                    }
                                }
                                if(course_location == null) {
                                    try {
                                        course_location = course.locations.get(course_num);
                                    }
                                    catch(Exception e) {
                                        course_location = "[No location found]";
                                    }
                                }
                                course_num--;
                            }

                            /** Create table elements for course meeting times */
                            // *If the course starts at time 'i'
                            int[] classDays = convDaysToArray(course_days);
                            String string_of_i = ((Integer) i).toString();
                            // Fixes comparing 800 against 0800
                            if(i < 1000)
                                string_of_i = "0" + string_of_i;
                            if((classDays[day] == 1) && (string_of_i.equals(course_time.substring(0, course_time.indexOf("-"))))) {
                                int rowSpan = calculateRowspan(course, j);
                                String uniqueIdentifier = (course.schedNumber.contains("***")) ? removeIllegalChars(course.courseID + "noSchedNum") : removeIllegalChars(course.courseID + "_" + course.schedNumber);
                                out.print("<td rowspan='" + rowSpan + "' class='tableCourse opt-class-" + getCourseColor(course.title, courseColors));
                                out.print(" " + uniqueIdentifier + "_" + j + "h'");
                                if(bottomBorder)
                                    out.print(" bottomBorder");
                                out.print(" data-toggle='modal' data-courseid='" + uniqueIdentifier + "_" + j + "' data-target='#tableModal'");
                                out.println(">");
                                out.println("<p id='courseID'><b>" + course.courseID.toUpperCase() + "</b></p>");
                                out.print("<div ");
                                    if(isMobile) out.print("style='display:none' ");
                                    out.println(">");
                                    out.print("<p id='title'><i>");
                                    out.print("<p id='title'><i>");
                                    if (course.title.length() > 40) {
                                        int cutoff = course.title.indexOf(" ");
                                        while (cutoff >= 0 && cutoff < 35) {
                                            cutoff = course.title.indexOf(" ", cutoff + 1);
                                        }
                                        if(cutoff < 0) cutoff = 35;
                                        out.print(course.title.substring(0, cutoff).toUpperCase() + "...");
                                    } else out.print(course.title.toUpperCase());
                                    out.print("</i></p>");
                                    out.print("</i></p>");
                                    out.println("<p id='instructors'>" + course_instructor + "</p>");
                                    out.println("<p id='times'>" + time24to12(course_time) + "</p>");
                                    out.println("<p id='schedNum'>Schedule #: " + course.schedNumber + "</p>");
                                out.println("</div>");

                                /** Modal data (hidden)*/
                                out.print("<div ");
                                    out.print("style='display:none' ");
                                    out.println("class='" + uniqueIdentifier + "_" + j + " temp'>");
                                        out.println("<p id='title'><i>" + course.title.toUpperCase() + "</i></p>");
                                        out.println("<p id='instructors'>" + course_instructor + "</p>");
                                        out.println("<p id='times'>" + time24to12(course_time) + "</p>");
                                        out.println("<p id='location'>" + course_location + "</p>");
                                        out.println("<p id='schedNum'>Schedule #: " + course.schedNumber + "</p>");
                                        out.print("<p id='seats'");
                                        // Check if waitlisted; if so red text
                                        if(course.seats.contains("-") || course.seats.charAt(0) == '0') out.print(" class='text-red'");
                                        out.println(">Available seats: " + course.seats + "</p>");
                                out.println("</div>");
                                out.println("</td>");
                                filledDays[day] = filledDays[day] + (rowSpan - 1);
                                filled = true;
                            }
                        }
                    }
                    if (!filled) {
                        if (filledDays[day] == 0) {
                            out.print("<td");
                            if (bottomBorder)
                                out.print(" class='bottomBorder'");
                            out.println("></td>");

                        }
                        else filledDays[day] = filledDays[day] - 1;
                    }
                }
                out.println("</tr>");
                // Change 8:60, 8:75, 8:90, etc.
                if (time.substring(time.length() - 2).equals("45"))
                    i += 40;
            }
            out.print("</table>");
            out.println("</div>");
        }

        out.println("</div>");
        /** Table navigation arrows */
        if(size > 0) {
            out.println("<a id='left-table' class='left carousel-control' href='#myCarousel' data-slide='prev'>");
            out.println("<span class='glyphicon glyphicon-chevron-left'></span>");
            out.println("<span class='sr-only'>Previous</span>");
            out.println("</a>");
            out.println("<a id='right-table' class='right carousel-control' href='#myCarousel' data-slide='next'>");
            out.println("<span class='glyphicon glyphicon-chevron-right'></span>");
            out.println("<span class='sr-only'>Next</span>");
            out.println("</a>");
        }
        out.println("</div>");
        out.println("</div>");

        this.timeEnd = System.currentTimeMillis();
    }

    /**
    * This method will convert a String representation of a time-block to a binary
    * representation of 15min increments starting at 8:00am (800)
    * Ex: "800-850" --> 1111 (8:45, 8:30, 8:15, 8:00)
    *  "1030-1120" --> 0011 1100 0000 0000 (11:15, 11:00, 10:45, 10:30)
     */
    protected final static long convertTimesToBits(String timeBlock) {
        String startTime = timeBlock.substring(0, timeBlock.indexOf("-"));
        String endTime = timeBlock.substring(timeBlock.indexOf("-") + 1);
        // 8:00 offset (800 becomes 000) ==> multiplier: 8 --> 0
        // divide by 2 for three vs four digit numbers
        int startTimeHundreds = Integer.parseInt(startTime.substring(0,(startTime.length()/2)));
        int endTimeHundreds = Integer.parseInt(endTime.substring(0,(endTime.length()/2)));
        // spot in bundle of 4 bits (00, 15, 30, 45) --> (0, 1, 2, or 3)
        int startTimeTens = Integer.parseInt(startTime.substring(startTime.length()-2));
        int endTimeTens = Integer.parseInt(endTime.substring(endTime.length()-2));
        // Construct the bit String; represents both time and length of time
        StringBuilder bitBuilder = new StringBuilder();
        int numOnes = (((endTimeTens - startTimeTens) < 0 ? endTimeTens-startTimeTens+60 : endTimeTens-startTimeTens)  / 15) + 1;
        int onesFourMultiplier = endTimeHundreds - startTimeHundreds;
        // check for hour cutoff (ex. 845-900) shouldn't have 6 ones, only 2
        if((endTimeTens - startTimeTens) < 0) onesFourMultiplier--;
        int numZeros = startTimeTens / 15;
        int zerosFourMultiplier = startTimeHundreds - 8;

        // Build the bits
        for(int i = 0; i < onesFourMultiplier; i++) bitBuilder.append("1111");
        for(int i = 0; i < numOnes; i++) bitBuilder.append("1");
        for(int i = 0; i < zerosFourMultiplier; i++) bitBuilder.append("0000");
        for(int i = 0; i < numZeros; i++) bitBuilder.append("0");

        String bitValue = bitBuilder.toString();
        return Long.parseLong(bitValue, 2);
    }

    protected String time24to12(int time) {
        String s = ((Integer)time).toString();
        int hr = Integer.parseInt((s.length() == 3) ? s.substring(0,1) : s.substring(0,2));
        String min = s.substring(s.length()-2, s.length());

        return (((hr<=12) ? hr : hr-12) + ":" + min + " " + ((hr>=12) ? "pm" : "am"));
    }
    protected String time24to12(String timeframe) {
        StringBuilder timeblock = new StringBuilder();

        String s = timeframe.substring(0,timeframe.indexOf("-"));
        int hr = Integer.parseInt((s.length() == 3) ? s.substring(0,1) : s.substring(0,2));
        String min = s.substring(s.length()-2, s.length());
        timeblock.append(((hr<=12) ? hr : hr-12) + ":" + min + ((hr>=12) ? "pm" : "am"));

        timeblock.append(" - ");

        s = timeframe.substring(timeframe.indexOf("-") + 1);
        hr = Integer.parseInt((s.length() == 3) ? s.substring(0,1) : s.substring(0,2));
        min = s.substring(s.length()-2, s.length());
        timeblock.append(((hr<=12) ? hr : hr-12) + ":" + min + ((hr>=12) ? "pm" : "am"));

        return timeblock.toString();
    }
    protected int calculateRowspan(Course course, int j) {
        String t = course.times.get(j).substring(0, course.times.get(j).indexOf("-"));
        int startHour = Integer.parseInt((t.length() == 3) ? t.substring(0, 1) : t.substring(0, 2));
        int startMin = Integer.parseInt((t.length() == 3) ? t.substring(1, 3) : t.substring(2, 4));

        t = course.times.get(j).substring(course.times.get(j).indexOf("-") + 1);
        int endHour = Integer.parseInt((t.length() == 3) ? t.substring(0, 1) : t.substring(0, 2));
        int endMin = Integer.parseInt((t.length() == 3) ? t.substring(1, 3) : t.substring(2, 4));
        if(endMin > 30 && endMin < 45) endMin = 45;

        int rowSpan = rowspanFormula(startHour, startMin, endHour, endMin);

        return rowSpan;
    }
    protected int getCourseColor(String identifier, Map<String, Integer> courseColors) {
        if(courseColors.containsKey(identifier))
            return courseColors.get(identifier);
        else {
            List<Integer> unusedColors = new ArrayList<>();
            for(int i = 1; i < 8; i++) {
                if (!courseColors.containsValue(i))
                    unusedColors.add(i);
            }
            if(!unusedColors.isEmpty()) {
                int num = unusedColors.get(0);
                courseColors.put(identifier, num);
                return num;
            }
            else {
                int duplicateColor = (int)(Math.random()*8 + 1 );
                courseColors.put(identifier, duplicateColor);
                return duplicateColor;
            }

        }
    }

    protected String removeIllegalChars(String s) {
        return s.replaceAll("\\s+","").replaceAll("[\\[\\](){}]","");
    }

    protected boolean optionsToCheck() {
        return (userOptions.wantedProfessors != null || userOptions.unwantedProfessors != null || userOptions.excludeProfessors != null || !userOptions.showWaitlisted || !userOptions.showOnlineClasses);
    }

    protected long getElapsedTime() {
        return (timeEnd - timeStart);
    }

    protected int[] incrementIterationVariables(int[] iterationVariables) {
        Map<Integer, List<Course>> possibleCourses = this.countIndexedCourses;
        int numCourses = this.numChosenCourses;

        // Increment the variables up
        iterationVariables[numCourses - 1] += 1;
        for (int i = numCourses - 1; i > 0; i--) {
            if (iterationVariables[i] == possibleCourses.get(i).size()) {
                iterationVariables[i] = 0;
                iterationVariables[i - 1] += 1;
            }
        }
        return iterationVariables;
    }

    protected void checkIfPresent(String query) {
        int startIndex = 0, endIndex = 0;
        boolean nextParamFound = false;
        if(this.parameters != null) {
            if(this.parameters.contains(query)) {
                StringBuilder removeParam = new StringBuilder(this.parameters);
                startIndex = this.parameters.indexOf(query);
                for(int i = startIndex; i < this.parameters.length(); i++) {
                    if(this.parameters.charAt(i) == '&') {
                        endIndex = i;
                        nextParamFound = true;
                    }
                }
                if(!nextParamFound) {
                    endIndex = this.parameters.length();
                }
                this.parameters = removeParam.delete(startIndex, endIndex).toString();
            }
        }
    }
}
